/*
 * midp.h - MIDP support: input event queue and the natives behind
 * javax.microedition.* and nanojava.*.
 */
#ifndef NJ_MIDP_H
#define NJ_MIDP_H

#include "../pal/pal.h"

/* Registers all MIDP natives; call after vm_init() and before vm_boot(). */
void midp_init(void);

/* Stops sound and frees what the MIDP natives allocated; call after
 * vm_shutdown(). */
void midp_shutdown(void);

/* Moves platform input into the VM's event queue and wakes the event
 * thread. Called by the scheduler between time slices. */
void midp_poll(void);

#endif
