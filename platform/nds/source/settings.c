/*
 * settings.c - per-game settings, kept next to the jar in <name>.ini:
 *
 *   screen=176x208     phone screen size
 *   layout=fit         fit (top screen, scaled if needed) or span (both)
 *   volume=80          J2ME volume, 0-100
 *   fps=0              show frames per second
 *   key.A=fire         physical button -> phone key (see phone_keys)
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#include "nds_platform.h"

const char *const button_names[NUM_BUTTONS] = {
    "A", "B", "X", "Y", "L", "R", "Start", "Select", "Up", "Down", "Left", "Right"
};

const PhoneKey phone_keys[] = {
    {"none", "-", 0},
    {"up", "Up", -1}, {"down", "Down", -2}, {"left", "Left", -3}, {"right", "Right", -4},
    {"fire", "Fire", -5}, {"soft1", "Soft L", -6}, {"soft2", "Soft R", -7},
    {"call", "Call", -10}, {"end", "End", -11},
    {"0", "0", '0'}, {"1", "1", '1'}, {"2", "2", '2'}, {"3", "3", '3'}, {"4", "4", '4'},
    {"5", "5", '5'}, {"6", "6", '6'}, {"7", "7", '7'}, {"8", "8", '8'}, {"9", "9", '9'},
    {"star", "*", '*'}, {"pound", "#", '#'},
    {"menu", "Menu", KEY_MENU},
};
const int num_phone_keys = sizeof phone_keys / sizeof phone_keys[0];

static const int default_keys[NUM_BUTTONS] = {
    -5,         /* A: fire */
    -7,         /* B: right soft key (usually "back") */
    '5',        /* X */
    '0',        /* Y */
    '*',        /* L */
    '#',        /* R */
    -6,         /* Start: left soft key (usually "menu"/"ok") */
    KEY_MENU,   /* Select: emulator menu */
    -1, -2, -3, -4
};

const PhoneKey *phone_key_by_code(int code)
{
    for (int i = 0; i < num_phone_keys; i++)
        if (phone_keys[i].code == code)
            return &phone_keys[i];
    return &phone_keys[0];
}

static const PhoneKey *phone_key_by_id(const char *id)
{
    for (int i = 0; i < num_phone_keys; i++)
        if (strcasecmp(phone_keys[i].id, id) == 0)
            return &phone_keys[i];
    return NULL;
}

void settings_defaults(GameSettings *s)
{
    s->w = 240;
    s->h = 320;
    s->layout = LAYOUT_FIT;
    s->volume = 80;
    s->show_fps = false;
    memcpy(s->keys, default_keys, sizeof s->keys);
}

static void ini_path(const char *jar, char *out, size_t size)
{
    snprintf(out, size, "%s", jar);
    char *dot = strrchr(out, '.');
    char *slash = strrchr(out, '/');
    if (dot && (!slash || dot > slash) && (size_t)(dot - out) + 5 <= size)
        strcpy(dot, ".ini");
}

void settings_load(const char *jar, GameSettings *s)
{
    char path[512], line[128];
    settings_defaults(s);
    if (!jar)
        return;
    ini_path(jar, path, sizeof path);
    FILE *f = fopen(path, "r");
    if (!f)
        return;
    while (fgets(line, sizeof line, f)) {
        line[strcspn(line, "\r\n")] = 0;
        char *eq = strchr(line, '=');
        if (!eq)
            continue;
        *eq = 0;
        const char *key = line, *val = eq + 1;
        int w, h;
        if (strcmp(key, "screen") == 0 && sscanf(val, "%dx%d", &w, &h) == 2 &&
            w >= 64 && h >= 64 && w <= 640 && h <= 640) {
            s->w = w;
            s->h = h;
        } else if (strcmp(key, "layout") == 0) {
            s->layout = strcmp(val, "span") == 0 ? LAYOUT_SPAN : LAYOUT_FIT;
        } else if (strcmp(key, "volume") == 0) {
            int v = atoi(val);
            s->volume = v < 0 ? 0 : v > 100 ? 100 : v;
        } else if (strcmp(key, "fps") == 0) {
            s->show_fps = atoi(val) != 0;
        } else if (strncmp(key, "key.", 4) == 0) {
            const PhoneKey *k = phone_key_by_id(val);
            for (int b = 0; b < NUM_BUTTONS && k; b++)
                if (strcasecmp(key + 4, button_names[b]) == 0)
                    s->keys[b] = k->code;
        }
    }
    fclose(f);
}

void settings_save(const char *jar, const GameSettings *s)
{
    char path[512];
    if (!jar || !nds_storage_ok)
        return;
    ini_path(jar, path, sizeof path);
    FILE *f = fopen(path, "w");
    if (!f)
        return;
    fprintf(f, "screen=%dx%d\nlayout=%s\nvolume=%d\nfps=%d\n", s->w, s->h,
            s->layout == LAYOUT_SPAN ? "span" : "fit", s->volume, s->show_fps ? 1 : 0);
    for (int b = 0; b < NUM_BUTTONS; b++)
        fprintf(f, "key.%s=%s\n", button_names[b], phone_key_by_code(s->keys[b])->id);
    fclose(f);
}
