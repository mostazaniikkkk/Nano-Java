/*
 * host.h - shared state of the headless host platform.
 */
#ifndef NJ_HOST_H
#define NJ_HOST_H

#include "../../src/pal/pal.h"

#define MAX_SCRIPT 256

typedef struct ScriptEvent {
    int64_t at_ms;
    NjEvent ev;
} ScriptEvent;

typedef struct HostConfig {
    int         screen_w, screen_h;
    const char *data_dir;
    ScriptEvent script[MAX_SCRIPT];
    int         n_script;
    int         frames_presented;
    int64_t     idle_ms;          /* time spent waiting with nothing to run */
} HostConfig;

extern HostConfig host;

bool host_write_screenshot(const char *path);

/* audio_host.c */
bool host_audio_open(const char *path);   /* record the mix to a WAV file */
void host_audio_update(void);             /* advance the synthesizer */
void host_audio_close(void);

#endif
