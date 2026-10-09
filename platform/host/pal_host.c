/*
 * pal_host.c - headless desktop platform (POSIX). Used for development and
 * automated tests: input comes from a scripted key sequence and frames can
 * be saved as PPM screenshots.
 */
#define _POSIX_C_SOURCE 200809L
#include "host.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>

HostConfig host;

static uint16_t *frame;
static int       frame_w, frame_h;
static int       script_pos;
static int64_t   t0 = -1;

static int64_t now_raw(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

int64_t pal_time_ms(void)
{
    if (t0 < 0)
        t0 = now_raw();
    return now_raw() - t0;
}

int64_t pal_epoch_ms(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

void pal_console_write(const char *s, int len)
{
    fwrite(s, 1, (size_t)len, stdout);
    fflush(stdout);
}

bool pal_poll_event(NjEvent *ev)
{
    if (script_pos < host.n_script && host.script[script_pos].at_ms <= pal_time_ms()) {
        *ev = host.script[script_pos].ev;
        script_pos++;
        return true;
    }
    return false;
}

void pal_wait(int64_t until_ms)
{
    int64_t now = pal_time_ms();
    int64_t limit = now + 20;
    if (script_pos < host.n_script && host.script[script_pos].at_ms < limit)
        limit = host.script[script_pos].at_ms;
    if (until_ms >= 0 && until_ms < limit)
        limit = until_ms;
    if (limit <= now)
        return;
    host.idle_ms += limit - now;
    struct timespec ts;
    ts.tv_sec = (limit - now) / 1000;
    ts.tv_nsec = (long)((limit - now) % 1000) * 1000000L;
    nanosleep(&ts, NULL);
}

void pal_screen_size(int *w, int *h)
{
    *w = host.screen_w;
    *h = host.screen_h;
}

void pal_present(const uint16_t *px, int w, int h)
{
    if (w != frame_w || h != frame_h) {
        free(frame);
        frame = malloc(sizeof(uint16_t) * (size_t)w * h);
        frame_w = w;
        frame_h = h;
    }
    memcpy(frame, px, sizeof(uint16_t) * (size_t)w * h);
    host.frames_presented++;
}

void pal_soft_labels(const char *left, const char *right)
{
    (void)left;
    (void)right;
}

const char *pal_data_dir(void)
{
    mkdir(host.data_dir, 0755);
    return host.data_dir;
}

bool host_write_screenshot(const char *path)
{
    if (!frame)
        return false;
    FILE *f = fopen(path, "wb");
    if (!f)
        return false;
    fprintf(f, "P6\n%d %d\n255\n", frame_w, frame_h);
    for (int i = 0; i < frame_w * frame_h; i++) {
        uint16_t c = frame[i];
        uint8_t  rgb[3] = {
            (uint8_t)(((c & 31) << 3) | ((c & 31) >> 2)),
            (uint8_t)((((c >> 5) & 31) << 3) | (((c >> 5) & 31) >> 2)),
            (uint8_t)((((c >> 10) & 31) << 3) | (((c >> 10) & 31) >> 2)),
        };
        fwrite(rgb, 1, 3, f);
    }
    fclose(f);
    return true;
}

void pal_fatal(const char *msg)
{
    (void)msg;
    exit(1);
}
