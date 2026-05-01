//Torlus - intermediate work file

#include <global.h>
#include <stdlib.h>

#define MAXCALENDARFLDS 15

#define YEAR 1
#define MONTH 2
#define DAY_OF_MONTH 5
#define HOUR 10
#define MINUTE 12
#define SECOND 13
#define MILLISECOND 14

static unsigned long date[MAXCALENDARFLDS];

void AlertUser(const char* message)
{
    fprintf(stderr, "ALERT: %s\n", message);
}

cell *allocateHeap(long *sizeptr, void **realresultptr) { 
    void *space = malloc(*sizeptr + sizeof(cell) - 1);
    *realresultptr = space;
    return (void *) ((((long)space) + (sizeof(cell) - 1)) & ~(sizeof(cell) - 1));
}

void *
allocateVirtualMemory_md(long size) {
}

void 
freeVirtualMemory_md(void *address, long size) { 
}

void  
protectVirtualMemory_md(void *address, long size, int protection) {
}

static void signal_handler(int sig) {
}

void InitializeFloatingPoint() {
}

void InitializeNativeCode() {
}

void FinalizeNativeCode() {
}

/*=========================================================================
 * FUNCTION:      CurrentTime_md()
 * TYPE:          machine-specific implementation of native function
 * OVERVIEW:      Returns the current time. 
 * INTERFACE:
 *   parameters:  none
 *   returns:     current time, in milliseconds since startup
 *=======================================================================*/

ulong64
CurrentTime_md(void)
{
    long long result;
    return result;
}

/*=========================================================================
 * FUNCTION:      Calendar_md()
 * TYPE:          machine-specific implementation of native function
 * OVERVIEW:      Initializes the calendar fields, which represent the 
 *                Calendar related attributes of a date. 
 * INTERFACE:
 *   parameters:  none
 *   returns:     none
 * AUTHOR:        Tasneem Sayeed
 *=======================================================================*/

unsigned long *
Calendar_md(void)
{
    return date;
}
