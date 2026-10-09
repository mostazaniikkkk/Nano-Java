/*
 * events.c - input event queue shared by the platform and the Java event
 * thread (nanojava.Events).
 */
#include "midp.h"
#include "../vm/vm.h"

#define QUEUE_SIZE 64

static NjEvent queue[QUEUE_SIZE];
static int     head, tail;
static bool    wakeup;

static bool queue_put(const NjEvent *ev)
{
    int next = (tail + 1) % QUEUE_SIZE;
    if (next == head)
        return false;
    queue[tail] = *ev;
    tail = next;
    return true;
}

static bool queue_get(NjEvent *ev)
{
    if (head == tail)
        return false;
    *ev = queue[head];
    head = (head + 1) % QUEUE_SIZE;
    return true;
}

void midp_poll(void)
{
    NjEvent ev;
    bool    any = false;
    while (pal_poll_event(&ev)) {
        if (ev.type == EV_QUIT) {
            vm.exit_requested = true;
            return;
        }
        if (queue_put(&ev))
            any = true;
    }
    if (any)
        vm_wake_event_waiters();
}

/* static int next(int[] out, int timeoutMs)
 * Returns the event type (filling out[0..1]) or 0 on timeout/wakeup.
 * timeoutMs < 0 waits forever, 0 polls. */
static int Events_next(Thread *t, Slot *args, Slot *ret)
{
    Object *out = args[0].ref;
    jint    timeout = args[1].i;
    NjEvent ev;

    if (!out || ARRAY_LEN(out) < 2) {
        NPE(t);
        return NATIVE_OK;
    }
    if (queue_get(&ev)) {
        t->native_phase = 0;
        ARRAY_DATA(out, jint)[0] = ev.a;
        ARRAY_DATA(out, jint)[1] = ev.b;
        ret->i = ev.type;
        return NATIVE_OK;
    }
    if (t->native_phase == 1 || timeout == 0 || wakeup) {
        t->native_phase = 0;
        wakeup = false;
        ret->i = 0;
        return NATIVE_OK;
    }
    t->native_phase = 1;
    t->state = TS_EVENT_WAIT;
    t->wake_time = timeout > 0 ? pal_time_ms() + timeout : 0;
    return NATIVE_BLOCK;
}

/* static void wakeup(): makes a pending or future next() return 0. */
static int Events_wakeup(Thread *t, Slot *args, Slot *ret)
{
    wakeup = true;
    vm_wake_event_waiters();
    return NATIVE_OK;
}

static const NativeEntry event_natives[] = {
    {"nanojava/Events", "next", NULL, Events_next},
    {"nanojava/Events", "wakeup", NULL, Events_wakeup},
    {NULL, NULL, NULL, NULL}
};

void midp_events_init(void)
{
    head = tail = 0;
    wakeup = false;
    native_register(event_natives);
}
