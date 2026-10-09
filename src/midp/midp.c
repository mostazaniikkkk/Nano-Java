/*
 * midp.c - MIDP module initialization.
 */
#include "midp.h"
#include "../vm/vm.h"

void midp_events_init(void);
void midp_lcdui_init(void);
void midp_storage_init(void);
void midp_audio_init(void);
void midp_audio_shutdown(void);

void midp_init(void)
{
    midp_events_init();
    midp_lcdui_init();
    midp_storage_init();
    midp_audio_init();
}

void midp_shutdown(void)
{
    midp_audio_shutdown();
}
