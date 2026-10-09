/*
 * ui.c - the touch screen: a phone keypad on the right and, on the left, a
 * tab that opens the emulator menu (change game, controls, screen, volume).
 *
 * With the "fit" layout the whole touch screen is UI: soft keys, call/end,
 * a D-pad ring with OK, and the 3x4 number pad. With "span" the game uses
 * part of the touch screen and the rest gets a compact one-row keypad.
 */
#include <nds.h>

#include <stdio.h>
#include <string.h>

#include "nds_platform.h"
#include "draw.h"
#include "../../../src/midp/gfx.h"

#define W 256
#define H 192
#define TAB_W 18

/* Colors (24-bit, packed when drawn). */
#define C_BG        0x3A3F47
#define C_TAB       0x262A30
#define C_KEY       0xE9ECEF
#define C_KEY_DOWN  0xA9C4E6
#define C_EDGE      0x7D848D
#define C_TEXT      0x24282C
#define C_SUBTEXT   0x5A6068
#define C_RING      0xC9CED4
#define C_RING_EDGE 0x8E959E
#define C_GREEN     0x2EA043
#define C_RED       0xD03030
#define C_PANEL     0x20242A
#define C_ROW_SEL   0x2F6FC0
#define C_WHITE     0xFFFFFF
#define C_DIM       0x8A9099

typedef struct {
    int         x, y, w, h;
    int         code;
    const char *label;     /* main label (digit) */
    const char *sub;       /* small letters */
} Key;

#define MAX_KEYS 24

static Key  keys[MAX_KEYS];
static int  n_keys;
static int  ui_top;              /* first row of the UI on the touch screen */
static bool full;                /* full keypad (fit layout) */
static int  ring_cx, ring_cy, ring_r, ok_r;
static int  pressed_code;        /* key held through the touch screen */
static int  pressed_key = -1;    /* index in keys[], -1 for the ring */
static char soft_label[2][24] = {"", ""};

/* The touch screen is drawn here and copied to VRAM (see draw.h). */
static uint16_t buf[W * H];

static void flush_rows(int y1, int y2)
{
    draw_flush(BG_GFX_SUB, y1, y2);
}

static void flush(void)
{
    flush_rows(ui_top, H);
}

static int text_width(const char *t, int size)
{
    return draw_text_width(t, size, 0);
}

static void text(const char *t, int x, int y, int size, uint32_t rgb)
{
    draw_text(t, x, y, size, 0, rgb);
}

/* Centers text on cx, clipped to [x1, x2). */
static void text_centered(const char *t, int cx, int y, int size, uint32_t rgb, int x1, int x2)
{
    draw_text_in(t, cx - text_width(t, size) / 2, y, size, 0, rgb, x1, x2);
}

/* A phone handset: a thick arc with two ear pieces. */
static void handset(int cx, int cy, uint32_t rgb)
{
    for (int i = 0; i < 3; i++)
        draw_arc(cx - 12, cy - 6 + i, 24, 14, 20, 140, rgb);
    draw_rect(cx - 13, cy + 1, 5, 4, rgb);
    draw_rect(cx + 8, cy + 1, 5, 4, rgb);
}

/* ------------------------------------------------------------------------ */
/* Keypad                                                                   */

static void draw_key(int i, bool down)
{
    Key     *k = &keys[i];
    uint32_t face = down ? C_KEY_DOWN : C_KEY;
    draw_round_rect(k->x, k->y, k->w, k->h, 6, face, C_EDGE);
    switch (k->code) {
    case -6:
    case -7: {
        const char *l = soft_label[k->code == -6 ? 0 : 1];
        if (*l)
            text_centered(l, k->x + k->w / 2, k->y + (k->h - font_height(0)) / 2, 0, C_TEXT,
                          k->x + 3, k->x + k->w - 3);
        else
            draw_rect(k->x + k->w / 2 - 12, k->y + k->h / 2 - 1, 24, 3, C_TEXT);
        break;
    }
    case -10:
        handset(k->x + k->w / 2, k->y + k->h / 2 - 2, C_GREEN);
        break;
    case -11:
        handset(k->x + k->w / 2, k->y + k->h / 2 - 2, C_RED);
        break;
    default:
        if (k->sub && full) {
            text(k->label, k->x + 7, k->y + (k->h - font_height(2)) / 2, 2, C_TEXT);
            text(k->sub, k->x + 22, k->y + (k->h - font_height(0)) / 2 + 2, 0, C_SUBTEXT);
        } else {
            text_centered(k->label, k->x + k->w / 2, k->y + (k->h - font_height(full ? 2 : 0)) / 2,
                          full ? 2 : 0, C_TEXT, k->x, k->x + k->w);
        }
        break;
    }
}

static void draw_ring(int dir_down)
{
    draw_disc(ring_cx, ring_cy, ring_r, C_RING, C_RING_EDGE);
    int r = ring_r, a = 5;
    uint32_t c[4];
    for (int i = 0; i < 4; i++)
        c[i] = dir_down == -1 - i ? C_ROW_SEL : C_TEXT;
    draw_triangle(ring_cx, ring_cy - r + 4, ring_cx - a, ring_cy - r + 4 + a, ring_cx + a, ring_cy - r + 4 + a, c[0]);
    draw_triangle(ring_cx, ring_cy + r - 4, ring_cx - a, ring_cy + r - 4 - a, ring_cx + a, ring_cy + r - 4 - a, c[1]);
    draw_triangle(ring_cx - r + 4, ring_cy, ring_cx - r + 4 + a, ring_cy - a, ring_cx - r + 4 + a, ring_cy + a, c[2]);
    draw_triangle(ring_cx + r - 4, ring_cy, ring_cx + r - 4 - a, ring_cy - a, ring_cx + r - 4 - a, ring_cy + a, c[3]);
    draw_disc(ring_cx, ring_cy, ok_r, dir_down == -5 ? C_KEY_DOWN : C_KEY, C_RING_EDGE);
    text_centered("OK", ring_cx, ring_cy - font_height(0) / 2, 0, C_SUBTEXT, 0, W);
}

static void draw_tab(void)
{
    draw_rect(0, ui_top, TAB_W, H - ui_top, C_TAB);
    draw_menu_icon(4, ui_top + 6, C_WHITE);
}

static Key *add_key(int x, int y, int w, int h, int code, const char *label, const char *sub)
{
    Key *k = &keys[n_keys++];
    *k = (Key){x, y, w, h, code, label, sub};
    return k;
}

static void layout_full(void)
{
    static const char *const digits[12] = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    static const char *const letters[12] = {"oo", "abc", "def", "ghi", "jkl", "mno",
                                             "pqrs", "tuv", "wxyz", "+", "_", ""};
    static const int codes[12] = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'};
    int x0 = TAB_W + 4, x1 = W - 4;
    int side = 62;

    add_key(x0, 4, side, 24, -6, NULL, NULL);
    add_key(x1 - side, 4, side, 24, -7, NULL, NULL);
    add_key(x0, 34, side, 26, -10, NULL, NULL);
    add_key(x1 - side, 34, side, 26, -11, NULL, NULL);
    ring_cx = (x0 + x1) / 2;
    ring_cy = 33;
    ring_r = 30;
    ok_r = 12;

    int gy = 68, gh = (H - 2 - gy) / 4;
    int gw = (x1 - x0 + 4) / 3;
    for (int i = 0; i < 12; i++) {
        int col = i % 3, row = i / 3;
        add_key(x0 + col * gw, gy + row * gh, gw - 4, gh - 3, codes[i], digits[i], letters[i]);
    }
}

static void layout_compact(void)
{
    static const char *const digits[12] = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
    static const int codes[12] = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'};
    int avail = H - ui_top;
    ring_r = 0;
    if (avail < 36)
        return;
    int kh = avail / 2 > 24 ? 24 : avail / 2;
    int y0 = ui_top + (avail - 2 * kh) / 2;
    int x0 = TAB_W + 2, kw = (W - x0) / 12;
    for (int i = 0; i < 12; i++)
        add_key(x0 + i * kw, y0, kw - 2, kh - 2, codes[i], digits[i], NULL);
    int half = (W - x0) / 2;
    add_key(x0, y0 + kh, half - 2, kh - 2, -6, NULL, NULL);
    add_key(x0 + half, y0 + kh, half - 2, kh - 2, -7, NULL, NULL);
}

static void redraw(void)
{
    draw_rect(0, ui_top, W, H - ui_top, C_BG);
    draw_tab();
    if (ring_r)
        draw_ring(0);
    for (int i = 0; i < n_keys; i++)
        draw_key(i, false);
}

void ui_init(int top)
{
    draw_target(buf);
    ui_top = top;
    full = top == 0;
    n_keys = 0;
    pressed_code = 0;
    pressed_key = -1;
    if (H - top < 14)
        return;
    if (full)
        layout_full();
    else
        layout_compact();
    redraw();
    flush();
}

void ui_soft_labels(const char *left, const char *right)
{
    draw_target(buf);
    const char *l[2] = {left ? left : "", right ? right : ""};
    for (int s = 0; s < 2; s++) {
        if (strcmp(soft_label[s], l[s]) == 0)
            continue;
        snprintf(soft_label[s], sizeof soft_label[s], "%s", l[s]);
        for (int i = 0; i < n_keys; i++)
            if (keys[i].code == (s ? -7 : -6))
                draw_key(i, pressed_key == i);
        flush();
    }
}

void ui_show_fps(int fps, int busy)
{
    draw_target(buf);
    char txt[8];
    if (H - ui_top < 40)
        return;
    draw_rect(0, H - 26, TAB_W, 26, C_TAB);
    snprintf(txt, sizeof txt, "%d", fps);
    text_centered(txt, TAB_W / 2, H - 25, 0, 0x60FF60, 0, TAB_W);
    snprintf(txt, sizeof txt, "%d", busy);
    text_centered(txt, TAB_W / 2, H - 13, 0, 0xFFC040, 0, TAB_W);
    flush_rows(H - 26, H);
}

static int ring_code(int x, int y)
{
    int dx = x - ring_cx, dy = y - ring_cy;
    int d2 = dx * dx + dy * dy;
    if (!ring_r || d2 > ring_r * ring_r)
        return 0;
    if (d2 <= ok_r * ok_r)
        return -5;
    if (dy * dy >= dx * dx)
        return dy < 0 ? -1 : -2;
    return dx < 0 ? -3 : -4;
}

int ui_touch(bool down, int x, int y, bool *on_ui)
{
    draw_target(buf);
    *on_ui = y >= ui_top && H - ui_top >= 14;
    if (!down) {
        int code = pressed_code;
        if (pressed_key >= 0)
            draw_key(pressed_key, false);
        else if (code && ring_r)
            draw_ring(0);
        flush();
        pressed_code = 0;
        pressed_key = -1;
        return code;
    }
    if (!*on_ui)
        return 0;
    if (x < TAB_W)
        return KEY_MENU;
    for (int i = 0; i < n_keys; i++) {
        Key *k = &keys[i];
        if (x >= k->x && x < k->x + k->w && y >= k->y && y < k->y + k->h) {
            pressed_key = i;
            pressed_code = k->code;
            draw_key(i, true);
            flush();
            return k->code;
        }
    }
    int code = ring_code(x, y);
    if (code) {
        pressed_key = -1;
        pressed_code = code;
        draw_ring(code);
        flush();
    }
    return code;
}

/* ------------------------------------------------------------------------ */
/* Emulator menu                                                            */

enum { M_RESUME, M_RESTART, M_CHANGE, M_CONTROLS, M_SCREEN, M_LAYOUT, M_VOLUME, M_FPS, M_COUNT };

#define PANEL_W  168
#define ROW_H    20
#define HEADER_H 22

static const int screen_sizes[][2] = {
    {240, 320}, {176, 208}, {176, 220}, {128, 160}, {128, 128}, {208, 208},
    {240, 266}, {256, 192}, {320, 240}
};
#define N_SIZES (int)(sizeof screen_sizes / sizeof screen_sizes[0])

/* Darkens what is on screen right of x, behind the open panel. */
static void dim_from(int x)
{
    draw_dim(x, 0, W - x, H);
}

static void header(const char *title, int width)
{
    draw_rect(0, 0, width, HEADER_H, C_TAB);
    draw_menu_icon(5, 6, C_WHITE);
    text(title, 22, (HEADER_H - font_height(1)) / 2, 1, C_WHITE);
}

static void value_row(int y, bool sel, const char *name, const char *value, bool enabled, int width)
{
    draw_rect(0, y, width, ROW_H, sel ? C_ROW_SEL : C_PANEL);
    uint32_t fg = enabled ? C_WHITE : C_DIM;
    text(name, 8, y + (ROW_H - font_height(1)) / 2, 1, fg);
    if (value) {
        int vw = text_width(value, 1);
        int vx = width - 14 - vw;
        text(value, vx, y + (ROW_H - font_height(1)) / 2, 1, fg);
        draw_triangle(vx - 9, y + ROW_H / 2, vx - 4, y + ROW_H / 2 - 4, vx - 4, y + ROW_H / 2 + 4, C_DIM);
        draw_triangle(width - 5, y + ROW_H / 2, width - 10, y + ROW_H / 2 - 4, width - 10, y + ROW_H / 2 + 4, C_DIM);
    }
}

static void draw_main_menu(const GameSettings *s, int sel, bool can_change)
{
    char buf[32];
    header("Nano Java", PANEL_W);
    for (int i = 0; i < M_COUNT; i++) {
        int y = HEADER_H + i * ROW_H;
        switch (i) {
        case M_RESUME:   value_row(y, sel == i, "Resume", NULL, true, PANEL_W); break;
        case M_RESTART:  value_row(y, sel == i, "Restart game", NULL, true, PANEL_W); break;
        case M_CHANGE:   value_row(y, sel == i, "Change game", NULL, can_change, PANEL_W); break;
        case M_CONTROLS: value_row(y, sel == i, "Controls", NULL, true, PANEL_W); break;
        case M_SCREEN:
            snprintf(buf, sizeof buf, "%dx%d", s->w, s->h);
            value_row(y, sel == i, "Screen", buf, true, PANEL_W);
            break;
        case M_LAYOUT:
            value_row(y, sel == i, "Layout", s->layout == LAYOUT_FIT ? "Fit" : "Span", true, PANEL_W);
            break;
        case M_VOLUME:
            snprintf(buf, sizeof buf, "%d%%", s->volume);
            value_row(y, sel == i, "Volume", buf, true, PANEL_W);
            break;
        case M_FPS:
            value_row(y, sel == i, "Show FPS", s->show_fps ? "On" : "Off", true, PANEL_W);
            break;
        }
    }
    int y = HEADER_H + M_COUNT * ROW_H;
    draw_rect(0, y, PANEL_W, H - y, C_PANEL);
    flush_rows(0, H);
}

static void draw_controls(const GameSettings *s, int sel)
{
    draw_rect(0, 0, W, H, C_PANEL);
    header("Controls", W);
    draw_round_rect(W - 108, 3, 50, HEADER_H - 6, 4, C_PANEL, C_DIM);
    text_centered("Default", W - 83, 4 + (HEADER_H - 8 - font_height(0)) / 2, 0, C_WHITE, 0, W);
    draw_round_rect(W - 54, 3, 50, HEADER_H - 6, 4, C_ROW_SEL, C_DIM);
    text_centered("Back", W - 29, 4 + (HEADER_H - 8 - font_height(0)) / 2, 0, C_WHITE, 0, W);
    int rh = (H - HEADER_H) / NUM_BUTTONS;
    for (int b = 0; b < NUM_BUTTONS; b++) {
        int y = HEADER_H + b * rh;
        draw_rect(0, y, W, rh, sel == b ? C_ROW_SEL : C_PANEL);
        text(button_names[b], 10, y + (rh - font_height(0)) / 2, 0, C_WHITE);
        const char *v = phone_key_by_code(s->keys[b])->label;
        int vx = 170 - text_width(v, 0) / 2;
        text(v, vx, y + (rh - font_height(0)) / 2, 0, C_WHITE);
        draw_triangle(110, y + rh / 2, 116, y + rh / 2 - 4, 116, y + rh / 2 + 4, C_DIM);
        draw_triangle(230, y + rh / 2, 224, y + rh / 2 - 4, 224, y + rh / 2 + 4, C_DIM);
    }
    flush_rows(0, H);
}

static int key_index(int code)
{
    for (int i = 0; i < num_phone_keys; i++)
        if (phone_keys[i].code == code)
            return i;
    return 0;
}

static void cycle_key(GameSettings *s, int b, int dir)
{
    int i = (key_index(s->keys[b]) + dir + num_phone_keys) % num_phone_keys;
    s->keys[b] = phone_keys[i].code;
}

static void cycle_size(GameSettings *s, int dir)
{
    int i = 0;
    while (i < N_SIZES && (screen_sizes[i][0] != s->w || screen_sizes[i][1] != s->h))
        i++;
    i = i == N_SIZES ? 0 : (i + dir + N_SIZES) % N_SIZES;
    s->w = screen_sizes[i][0];
    s->h = screen_sizes[i][1];
}

/* Waits for the next button press or tap; returns pressed buttons and sets
 * the tap position in tx, ty (or -1). */
static uint32_t next_input(int *tx, int *ty)
{
    for (;;) {
        swiWaitForVBlank();
        scanKeys();
        uint32_t down = keysDown();
        uint32_t rep = keysDownRepeat() & (KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT);
        *tx = *ty = -1;
        if (down & KEY_TOUCH) {
            touchPosition tp;
            touchRead(&tp);
            *tx = tp.px;
            *ty = tp.py;
        }
        uint32_t k = (down | rep) & ~KEY_TOUCH;
        if (k || *tx >= 0)
            return k;
    }
}

static void run_controls(GameSettings *s)
{
    int sel = 0;
    for (;;) {
        draw_controls(s, sel);
        int      tx, ty;
        uint32_t k = next_input(&tx, &ty);
        int      dir = 0;
        if (tx >= 0) {
            if (ty < HEADER_H) {
                if (tx >= W - 54)
                    return;
                if (tx >= W - 108) {
                    GameSettings d;
                    settings_defaults(&d);
                    memcpy(s->keys, d.keys, sizeof s->keys);
                }
                continue;
            }
            sel = (ty - HEADER_H) / ((H - HEADER_H) / NUM_BUTTONS);
            if (sel >= NUM_BUTTONS)
                sel = NUM_BUTTONS - 1;
            dir = tx < 140 ? -1 : 1;
        }
        if (k & KEY_UP)
            sel = (sel + NUM_BUTTONS - 1) % NUM_BUTTONS;
        if (k & KEY_DOWN)
            sel = (sel + 1) % NUM_BUTTONS;
        if (k & KEY_LEFT)
            dir = -1;
        if (k & (KEY_RIGHT | KEY_A))
            dir = 1;
        if (k & KEY_B)
            return;
        if (dir)
            cycle_key(s, sel, dir);
    }
}

int ui_menu_run(GameSettings *s, const char *jar, bool can_change)
{
    draw_target(buf);
    GameSettings before = *s;
    int  sel = M_RESUME;
    int  action = EXIT_NONE;
    bool open = true;

    /* Release a key held on the keypad so it does not stay pressed. */
    pressed_code = 0;
    pressed_key = -1;
    const uint32_t *vram = (const uint32_t *)BG_GFX_SUB;
    for (int i = 0; i < ui_top * W / 2; i++)
        ((uint32_t *)buf)[i] = vram[i];
    dim_from(PANEL_W);
    while (open) {
        draw_main_menu(s, sel, can_change);
        int      tx, ty;
        uint32_t k = next_input(&tx, &ty);
        int      dir = 0;
        bool     activate = false;
        if (tx >= 0) {
            if (tx >= PANEL_W || ty < HEADER_H) {
                break;   /* tap outside the menu (or on its tab) closes it */
            }
            int row = (ty - HEADER_H) / ROW_H;
            if (row >= M_COUNT)
                continue;
            sel = row;
            activate = true;
            dir = tx < PANEL_W / 2 ? -1 : 1;
        }
        if (k & KEY_UP)
            sel = (sel + M_COUNT - 1) % M_COUNT;
        if (k & KEY_DOWN)
            sel = (sel + 1) % M_COUNT;
        if (k & KEY_LEFT)
            dir = -1, activate = sel >= M_SCREEN;
        if (k & KEY_RIGHT)
            dir = 1, activate = sel >= M_SCREEN;
        if (k & KEY_A)
            dir = 1, activate = true;
        if (k & (KEY_B | KEY_SELECT))
            break;
        if (!activate)
            continue;

        switch (sel) {
        case M_RESUME:
            open = false;
            break;
        case M_RESTART:
            action = EXIT_RESTART_GAME;
            open = false;
            break;
        case M_CHANGE:
            if (can_change) {
                action = EXIT_CHANGE_GAME;
                open = false;
            }
            break;
        case M_CONTROLS:
            run_controls(s);
            redraw();
            dim_from(PANEL_W);
            break;
        case M_SCREEN:
            cycle_size(s, dir);
            break;
        case M_LAYOUT:
            s->layout = s->layout == LAYOUT_FIT ? LAYOUT_SPAN : LAYOUT_FIT;
            break;
        case M_VOLUME:
            s->volume += dir * 10;
            s->volume = s->volume < 0 ? 0 : s->volume > 100 ? 100 : s->volume;
            break;
        case M_FPS:
            s->show_fps = !s->show_fps;
            break;
        }
    }

    if (memcmp(&before, s, sizeof before) != 0)
        settings_save(jar, s);
    /* A new screen size only takes effect when the game starts again. */
    if (action == EXIT_NONE && (before.w != s->w || before.h != s->h))
        action = EXIT_RESTART_GAME;
    return action;
}
