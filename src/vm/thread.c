/*
 * thread.c - green threads, monitors and the scheduler.
 *
 * All Java threads run on the single native thread. The scheduler gives
 * each runnable thread a time slice of bytecodes in round-robin order.
 * Anything that would block (monitor contention, sleep, wait, waiting for
 * input) marks the thread as not runnable and leaves its pc on the blocking
 * instruction, which simply executes again once the thread is woken.
 */
#include "vm.h"
#include "../pal/pal.h"

#include <stdlib.h>

#define TIME_SLICE 2000   /* backward branches + calls */

void (*vm_poll_hook)(void);

Thread *thread_create(Object *jthread, Method *entry, Slot *args, int nargs)
{
    Thread *t = nj_alloc(sizeof *t);
    t->jthread = jthread;
    t->stack = nj_alloc(sizeof(Slot) * vm.opt.stack_slots);
    t->stack_limit = t->stack + vm.opt.stack_slots;
    t->max_frames = vm.opt.max_frames;
    t->frames = nj_alloc(sizeof(Frame) * t->max_frames);
    t->state = TS_RUNNABLE;
    t->priority = 5;
    for (int i = 0; i < nargs; i++)
        t->stack[i] = args[i];
    if (!interp_push_frame(t, entry, t->stack)) {
        thread_free(t);
        return NULL;
    }

    /* Append so threads are scheduled in creation order. */
    Thread **link = &vm.threads;
    while (*link)
        link = &(*link)->next;
    *link = t;
    vm.n_threads++;
    return t;
}

void thread_free(Thread *t)
{
    free(t->stack);
    free(t->frames);
    free(t);
}

Thread *thread_find(Object *jthread)
{
    for (Thread *t = vm.threads; t; t = t->next)
        if (t->jthread == jthread)
            return t;
    return NULL;
}

void sched_wake(Thread *t)
{
    t->state = TS_RUNNABLE;
    t->wake_time = 0;
}

/* ------------------------------------------------------------------------ */
/* Monitors                                                                 */

static Monitor *monitor_of(Object *o)
{
    if (!o->mon)
        o->mon = nj_alloc(sizeof(Monitor));
    return o->mon;
}

void monitor_free(Monitor *m)
{
    free(m);
}

static void wake_blocked(Monitor *m)
{
    for (Thread *t = vm.threads; t; t = t->next)
        if (t->state == TS_BLOCKED && t->blocked_on == m)
            sched_wake(t);
}

bool monitor_enter(Thread *t, Object *o)
{
    Monitor *m = monitor_of(o);
    if (!m->owner) {
        m->owner = t;
        m->count = 1;
        return true;
    }
    if (m->owner == t) {
        m->count++;
        return true;
    }
    t->state = TS_BLOCKED;
    t->blocked_on = m;
    return false;
}

bool monitor_exit(Thread *t, Object *o)
{
    Monitor *m = o->mon;
    if (!m || m->owner != t)
        return false;
    if (--m->count == 0) {
        m->owner = NULL;
        wake_blocked(m);
    }
    return true;
}

static void remove_waiter(Monitor *m, Thread *t)
{
    for (Thread **link = &m->waiters; *link; link = &(*link)->wait_next) {
        if (*link == t) {
            *link = t->wait_next;
            t->wait_next = NULL;
            return;
        }
    }
}

/* Object.wait(). Phase 0 releases the monitor and parks the thread; once it
 * is notified (or times out) the native runs again in phase 1 and
 * reacquires the monitor with its previous recursion count. */
int monitor_wait(Thread *t, Object *o, jlong ms)
{
    Monitor *m = o->mon;
    if (t->native_phase == 0) {
        if (!m || m->owner != t) {
            vm_throw_new(t, "java/lang/IllegalMonitorStateException", NULL);
            return NATIVE_OK;
        }
        if (ms < 0) {
            vm_throw_new(t, "java/lang/IllegalArgumentException", "timeout value is negative");
            return NATIVE_OK;
        }
        if (t->interrupted) {
            t->interrupted = false;
            vm_throw_new(t, "java/lang/InterruptedException", NULL);
            return NATIVE_OK;
        }
        t->saved_count = m->count;
        m->owner = NULL;
        m->count = 0;
        wake_blocked(m);
        t->wait_next = m->waiters;
        m->waiters = t;
        t->state = TS_WAITING;
        t->blocked_on = m;
        t->wake_time = ms > 0 ? pal_time_ms() + ms : 0;
        t->native_phase = 1;
        return NATIVE_BLOCK;
    }

    if (m->owner && m->owner != t) {
        t->state = TS_BLOCKED;
        t->blocked_on = m;
        return NATIVE_BLOCK;
    }
    m->owner = t;
    m->count = t->saved_count;
    t->native_phase = 0;
    t->blocked_on = NULL;
    if (t->interrupted) {
        t->interrupted = false;
        vm_throw_new(t, "java/lang/InterruptedException", NULL);
    }
    return NATIVE_OK;
}

void monitor_notify(Thread *t, Object *o, bool all)
{
    Monitor *m = o->mon;
    if (!m || m->owner != t) {
        vm_throw_new(t, "java/lang/IllegalMonitorStateException", NULL);
        return;
    }
    while (m->waiters) {
        /* The wait set is LIFO; notify the longest waiter first. */
        Thread **link = &m->waiters;
        while ((*link)->wait_next)
            link = &(*link)->wait_next;
        Thread *w = *link;
        *link = NULL;
        sched_wake(w);
        if (!all)
            break;
    }
}

/* Wakes everything waiting on o without owning its monitor (used when a
 * thread terminates, so join() returns). */
static void notify_all_internal(Object *o)
{
    Monitor *m = o ? o->mon : NULL;
    if (!m)
        return;
    while (m->waiters) {
        Thread *w = m->waiters;
        m->waiters = w->wait_next;
        w->wait_next = NULL;
        sched_wake(w);
    }
}

int thread_sleep(Thread *t, jlong ms)
{
    if (t->native_phase == 0) {
        if (ms < 0) {
            vm_throw_new(t, "java/lang/IllegalArgumentException", "timeout value is negative");
            return NATIVE_OK;
        }
        if (t->interrupted) {
            t->interrupted = false;
            vm_throw_new(t, "java/lang/InterruptedException", NULL);
            return NATIVE_OK;
        }
        t->native_phase = 1;
        if (ms > 0) {
            t->state = TS_SLEEPING;
            t->wake_time = pal_time_ms() + ms;
        }
        return NATIVE_BLOCK;   /* ms == 0 just yields */
    }
    t->native_phase = 0;
    if (t->interrupted) {
        t->interrupted = false;
        vm_throw_new(t, "java/lang/InterruptedException", NULL);
    }
    return NATIVE_OK;
}

void thread_interrupt(Thread *t)
{
    t->interrupted = true;
    if (t->state == TS_SLEEPING) {
        sched_wake(t);
    } else if (t->state == TS_WAITING) {
        remove_waiter(t->blocked_on, t);
        sched_wake(t);
    }
}

/* ------------------------------------------------------------------------ */
/* Scheduler                                                                */

static void reap(void)
{
    for (Thread **link = &vm.threads; *link;) {
        Thread *t = *link;
        if (t->state == TS_TERMINATED) {
            *link = t->next;
            vm.n_threads--;
            notify_all_internal(t->jthread);
            if (vm.current == t)
                vm.current = NULL;
            thread_free(t);
        } else {
            link = &t->next;
        }
    }
}

int vm_run(void)
{
    Thread *last = NULL;

    while (!vm.exit_requested) {
        if (vm_poll_hook)
            vm_poll_hook();

        jlong now = pal_time_ms();
        jlong next_wake = -1;
        bool  alive = false, event_waiters = false;
        for (Thread *t = vm.threads; t; t = t->next) {
            if (t->state == TS_TERMINATED)
                continue;
            if (!t->daemon)
                alive = true;
            if (t->state == TS_EVENT_WAIT)
                event_waiters = true;
            if (t->state != TS_RUNNABLE && t->state != TS_BLOCKED && t->wake_time) {
                if (now >= t->wake_time) {
                    if (t->state == TS_WAITING)
                        remove_waiter(t->blocked_on, t);
                    t->timed_out = true;
                    sched_wake(t);
                } else if (next_wake < 0 || t->wake_time < next_wake) {
                    next_wake = t->wake_time;
                }
            }
        }
        if (!alive)
            break;

        /* Round robin: the first runnable thread after the last one run. */
        Thread *run = NULL;
        Thread *start = last && last->state != TS_TERMINATED ? last->next : NULL;
        for (Thread *t = start; t && !run; t = t->next)
            if (t->state == TS_RUNNABLE)
                run = t;
        for (Thread *t = vm.threads; t && !run; t = t->next)
            if (t->state == TS_RUNNABLE)
                run = t;

        if (!run) {
            if (next_wake < 0 && !event_waiters) {
                nj_log("nanojava: all threads are blocked (deadlock)");
                return 1;
            }
            pal_wait(next_wake);
            continue;
        }

        vm.current = run;
        interp_run(run, TIME_SLICE);
        last = run;
        if (run->state == TS_TERMINATED) {
            last = NULL;
            reap();
        }
    }
    return vm.exit_code;
}

void vm_wake_event_waiters(void)
{
    for (Thread *t = vm.threads; t; t = t->next)
        if (t->state == TS_EVENT_WAIT)
            sched_wake(t);
}
