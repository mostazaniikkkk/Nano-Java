/*
 * nds_runtime.c
 * NDS/devkitARM platform-specific runtime for KVM.
 * Based on Torlus's simu_runtime.c and Sun's VmUnix/runtime_md.c.
 */

#include <global.h>
#include <stdlib.h>
#include <time.h>
#include <nds.h>
#include <fat.h>

#define MAXCALENDARFLDS 15

#define YEAR          1
#define MONTH         2
#define DAY_OF_MONTH  5
#define HOUR         10
#define MINUTE       12
#define SECOND       13
#define MILLISECOND  14

static unsigned long date[MAXCALENDARFLDS];

/* Timer 0 is used as a millisecond tick counter.
 * Timer 1 cascades from Timer 0 to extend the range.
 * Both are started in InitializeNativeCode(). */
static volatile bool timerInitialized = false;

void AlertUser(const char *message)
{
    printf("ALERT: %s\n", message != NULL ? message : "(null)");
    /* Spin so the message stays visible before VM_EXIT clears everything */
    volatile int i;
    for (i = 0; i < 10000000; i++) {}
}

cell *allocateHeap(long *sizeptr, void **realresultptr)
{
    void *space = malloc(*sizeptr + sizeof(cell) - 1);
    *realresultptr = space;
    return (void *)(((long)space + (sizeof(cell) - 1)) & ~(sizeof(cell) - 1));
}

/* NDS has no virtual memory — stubs satisfy the KVM interface. */
void *allocateVirtualMemory_md(long size)       { return malloc(size); }
void  freeVirtualMemory_md(void *addr, long sz) { (void)sz; free(addr); }
void  protectVirtualMemory_md(void *addr, long size, int protection)
{
    (void)addr; (void)size; (void)protection;
}

void InitializeFloatingPoint(void) {}

void InitializeNativeCode(void)
{
    /* Timer 0: count up at BUS_CLOCK/1 (33.5 MHz / 1024 ≈ 32 kHz).
     * We use TIMER_FREQ_1024 so one tick ≈ 30.5 µs; overflow every ~2 s.
     * Timer 1 cascades to count overflows, giving ~18 hours before wrap. */
    if (!timerInitialized) {
        timerStart(0, ClockDivider_1024, 0, NULL);
        timerStart(1, ClockDivider_1, 0, NULL);  /* cascade via hardware */
        timerInitialized = true;
    }

    /* Initialize libfat for SD/flash access (used by file.c / GBFS). */
    (void)fatInitDefault();
}

void FinalizeNativeCode(void)
{
    timerStop(0);
    timerStop(1);
}

void InitializeWindowSystem(void)  {}
void FinalizeWindowSystem(void)    {}

/*
 * nds_sleep: busy-wait for the requested number of milliseconds.
 * Replaces gba_sleep() referenced by the SLEEP_FOR macro in machine_md.h.
 */
void nds_sleep(long ms)
{
    /* swiDelay counts in ~8-cycle units on ARM9; calibrate as needed. */
    while (ms-- > 0)
        swiDelay(8378);  /* approx 1 ms at ~67 MHz ARM9 */
}

/*
 * CurrentTime_md: milliseconds elapsed since VM start.
 * Reads the cascaded 16-bit timers 0+1 for a 32-bit tick value.
 */
ulong64 CurrentTime_md(void)
{
    /* Each Timer-0 tick at ClockDivider_1024 is 1024/33513982 s ≈ 30.5 µs.
     * Multiply by 1000/32 (≈ 30.5 µs * 32 ≈ 1 ms) for milliseconds. */
    u32 ticks = (timerElapsed(1) << 16) | timerElapsed(0);
    return (ulong64)(ticks * 1000ULL / (BUS_CLOCK / 1024));
}

/*
 * Calendar_md: wall-clock date/time fields.
 * Returns an array indexed by the KVM calendar constants.
 * Requires libfat + hardware RTC (via libnds).
 */
unsigned long *Calendar_md(void)
{
    time_t now;
    struct tm *t;

    time(&now);
    t = localtime(&now);

    date[YEAR]         = t->tm_year + 1900;
    date[MONTH]        = t->tm_mon;
    date[DAY_OF_MONTH] = t->tm_mday;
    date[HOUR]         = t->tm_hour;
    date[MINUTE]       = t->tm_min;
    date[SECOND]       = t->tm_sec;
    date[MILLISECOND]  = 0;

    return date;
}
