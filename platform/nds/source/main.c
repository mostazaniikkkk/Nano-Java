/*
 * main.c - Nano Java for Nintendo DS: pick a MIDlet, start the VM.
 *
 * A homebrew loader may pass the .jar or .jad path as argv[1]; otherwise
 * the launcher lists the MIDlets found on the SD card.
 */
#include <nds.h>
#include <fat.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "nds_platform.h"

/* Canvas size for ROMs with an embedded MIDlet (make nds-embed SCREEN=WxH). */
#ifndef NJ_EMBED_W
#define NJ_EMBED_W 240
#define NJ_EMBED_H 320
#endif
#include "../../../src/pal/pal.h"

static bool ends_with(const char *s, const char *suffix)
{
    size_t a = strlen(s), b = strlen(suffix);
    return a >= b && strcasecmp(s + a - b, suffix) == 0;
}

static uint8_t *read_file(const char *path, uint32_t *len)
{
    FILE *f = fopen(path, "rb");
    if (!f)
        return NULL;
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fseek(f, 0, SEEK_SET);
    uint8_t *buf = malloc((size_t)size + 1);
    if (buf && fread(buf, 1, (size_t)size, f) != (size_t)size) {
        free(buf);
        buf = NULL;
    }
    fclose(f);
    if (buf) {
        buf[size] = 0;
        *len = (uint32_t)size;
    }
    return buf;
}

/* For a .jad, registers its properties and returns the jar it points to. */
static char *jar_for_jad(const char *jad_path)
{
    uint32_t len;
    uint8_t *jad = read_file(jad_path, &len);
    if (!jad)
        return NULL;
    nds_set_jad(jad, len);
    char *jar = nds_jad_jar_path(jad_path, (const char *)jad);
    free(jad);
    return jar;
}

/* Shows an error under the log and waits for A. */
static void error_screen(const char *msg)
{
    nds_console_start();
    consoleClear();
    printf("\x1b[31mThe MIDlet stopped\x1b[39m\n\n");
    nds_show_log(14);
    printf("\n%s\n\nPress A to go back.", msg);
    for (;;) {
        swiWaitForVBlank();
        scanKeys();
        if (keysDown() & KEY_A)
            return;
    }
}

int main(int argc, char **argv)
{
    int stack_base;
    heap_set_stack_base(&stack_base);
    defaultExceptionHandler();
    nds_console_start();
    printf("Nano Java\n\n");

    nds_storage_ok = fatInitDefault();
    if (nds_has_embedded_app()) {
        /* Test builds carry their MIDlet in the ROM (make nds-embed). */
        for (;;) {
            GameSettings s;
            settings_defaults(&s);
            s.w = NJ_EMBED_W;
            s.h = NJ_EMBED_H;
            int code = nds_run_vm(NULL, NULL, &s);
            if (nds_exit_action == EXIT_RESTART_GAME)
                continue;
            if (code != 0)
                pal_fatal("The MIDlet ended with an error.");
            return 0;
        }
    }
    if (!nds_storage_ok)
        pal_fatal("Cannot access the SD card.");

    char *path = argc > 1 && argv[1] && argv[1][0] ? strdup(argv[1]) : NULL;
    char *midlet = NULL;
    for (;;) {
        if (!path)
            path = nds_launcher_pick(&midlet);
        char *jar = path;
        if (ends_with(path, ".jad")) {
            jar = jar_for_jad(path);
            if (!jar) {
                error_screen("Cannot read the .jad file.");
                free(path);
                free(midlet);
                path = midlet = NULL;
                continue;
            }
        }

        GameSettings s;
        settings_load(jar, &s);
        int code = nds_run_vm(jar, midlet, &s);
        if (jar != path)
            free(jar);
        if (nds_exit_action == EXIT_RESTART_GAME)
            continue;
        if (nds_exit_action != EXIT_CHANGE_GAME && code != 0)
            error_screen("The MIDlet ended with an error.");
        free(path);
        free(midlet);
        path = midlet = NULL;
    }
}
