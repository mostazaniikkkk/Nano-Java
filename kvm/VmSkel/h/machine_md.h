//Torlus - based on VmUnix machine_md.h file
// Adapted for NDS/devkitARM: removed Unix-only headers

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <time.h>
#include <math.h>
#include <setjmp.h>

/* nds.h and fat.h are included only in the NDS-specific .c files
 * that actually need them (nds_runtime.c, nds_main.c, Java_nds_*.c).
 * Including them here would pull the full libnds into every KVM
 * compilation unit via global.h → machine_md.h. */

//Torlus
//Ole: 2mbytes of the heap  2048
#define DEFAULTHEAPSIZE 2048*1024 
#define JAR_FILES_USE_STDIO 1

#define INLINECACHESIZE 512
#define VERIFYCONSTANTPOOLINTEGRITY 1
#define LPISLOCAL 1
#define FPISLOCAL 1
#define CPISLOCAL 1


#define MAX_PROPERTIES 64

typedef long long long64;   
typedef unsigned long long ulong64;
typedef long long jlong;

#ifndef Calendar_md
unsigned long * Calendar_md(void);
#endif

/* By default we are assume that if we are not using a i386 machine the 
 * target platform is big-endian.
 * Solaris requires 8-byte alignment of longs and doubles */

#if !defined(i386) && !defined(__arm__)
#define BIG_ENDIAN 1
#define NEED_LONG_ALIGNMENT 1
#define NEED_DOUBLE_ALIGNMENT 1
#else
#undef BIG_ENDIAN
#undef LITTLE_ENDIAN
#undef NEED_LONG_ALIGNMENT 
#define LITTLE_ENDIAN 1
#define NEED_LONG_ALIGNMENT 0 
#endif

/* Override the sleep function defined in main.h */
#define SLEEP_FOR(delta) nds_sleep(delta)
void nds_sleep(long ms);

#define InitializeVM()
#define FinalizeVM()

#define freeHeap(heap) free(heap)
#define RandomNumber_md() rand()

void InitializeWindowSystem();
void FinalizeWindowSystem(void);

void* allocateVirtualMemory_md(long size);
void  freeVirtualMemory_md(void *address, long size);

void  protectVirtualMemory_md(void *address, long size, int protection);

#define GetAndStoreNextKVMEvent(x,y)
#define _NOT_IMPLEMENTED_GetAndStoreNextKVMEvent
