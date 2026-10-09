/*
 * pal_nds.c - Nintendo DS platform layer.
 *
 * Two screen layouts (per game, see settings.c):
 *  - fit: the phone screen is on the top screen, scaled down by the 2D
 *    hardware when it is taller than 192 rows; the touch screen holds the
 *    phone keypad and the emulator menu (ui.c).
 *  - span: the phone screen is shown 1:1 across both screens (a 256x384
 *    area) and the rest of the touch screen gets a compact keypad.
 *  - touch: the phone screen is on the touch screen (scaled like in fit),
 *    so touch-driven games can be played; the top screen shows information.
 * Physical buttons are mapped to phone keys per game.
 */
#include <nds.h>
#include <fat.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>

#include "nds_platform.h"
#include "../../../src/pal/pal.h"
#include "../../../src/midp/gfx.h"

#define VW 256          /* virtual screen in span layout: both screens */
#define VH 384
#define SCREEN_H 192

/* ------------------------------------------------------------------------ */
/* Log                                                                      */

#define LOG_SIZE 8192

static char log_buf[LOG_SIZE];
static int  log_len;
static bool console_on;

void pal_console_write(const char *s, int len)
{
    for (int i = 0; i < len; i++) {
        if (log_len == LOG_SIZE) {
            memmove(log_buf, log_buf + LOG_SIZE / 2, LOG_SIZE / 2);
            log_len = LOG_SIZE / 2;
        }
        log_buf[log_len++] = s[i];
    }
    if (console_on)
        fwrite(s, 1, (size_t)len, stdout);
}

void nds_console_start(void)
{
    videoSetModeSub(MODE_0_2D);
    vramSetBankC(VRAM_C_SUB_BG);
    consoleDemoInit();
    console_on = true;
}

void nds_show_log(int lines)
{
    int start = log_len;
    while (start > 0 && lines > 0) {
        start--;
        if (log_buf[start] == '\n' && start != log_len - 1)
            lines--;
    }
    if (start > 0)
        start++;
    fwrite(log_buf + start, 1, (size_t)(log_len - start), stdout);
}

void pal_fatal(const char *msg)
{
    nds_console_start();
    consoleClear();
    printf("\x1b[31mNano Java stopped\x1b[39m\n\n");
    nds_show_log(14);
    printf("\n%s\n\nPress START to exit.", msg);
    for (;;) {
        swiWaitForVBlank();
        scanKeys();
        if (keysDown() & KEY_START)
            exit(1);
    }
}

/* ------------------------------------------------------------------------ */
/* Time                                                                     */

static uint32_t last_ticks;
static uint64_t total_ticks;
static bool     timing;
static int64_t  paused_ms;   /* time spent in the emulator menu */

static int64_t raw_time_ms(void)
{
    if (!timing) {
        cpuStartTiming(0);
        timing = true;
    }
    uint32_t now = cpuGetTiming();
    total_ticks += (uint32_t)(now - last_ticks);
    last_ticks = now;
    return (int64_t)(total_ticks * 1000 / BUS_CLOCK);
}

/* The VM's clock stops while the menu is open, so games see no jump. */
int64_t pal_time_ms(void)
{
    return raw_time_ms() - paused_ms;
}

int64_t pal_epoch_ms(void)
{
    static int64_t base = -1;
    if (base < 0)
        base = (int64_t)time(NULL) * 1000 - raw_time_ms();
    return base + raw_time_ms();
}

static int64_t idle_ms;   /* time spent waiting, for the FPS display */

void pal_wait(int64_t until_ms)
{
    int64_t now = pal_time_ms();
    if (until_ms < 0 || until_ms - now >= 17)
        swiWaitForVBlank();
    idle_ms += pal_time_ms() - now;
}

/* ------------------------------------------------------------------------ */
/* Video                                                                    */

static GameSettings *settings;
static const char   *game_jar;
int                  nds_exit_action;

static int  canvas_w = 240, canvas_h = 320;
static int  ox, oy;                 /* canvas position on its screen(s) */
static bool span;                   /* canvas spans both screens */
static bool soft_scaled;            /* scaled in software (too big for VRAM) */
static bool on_sub;                 /* fit/touch: the canvas is on the touch screen */
static int  aff_sx = 256, aff_sy = 256, aff_x0, aff_y0;   /* hardware scaling */
static int  out_w, out_h;           /* displayed size of a soft-scaled canvas */
static int *xmap, *ymap;            /* displayed pixel -> canvas pixel */

static void fill(uint16_t *fb, int x, int y, int w, int h, uint16_t c)
{
    for (int yy = y; yy < y + h; yy++)
        for (int xx = x; xx < x + w; xx++)
            fb[yy * 256 + xx] = c;
}

/* Row vy of the canvas area in span layout: both screens stacked. */
static uint16_t *row_ptr(int vy)
{
    return vy < SCREEN_H ? BG_GFX + vy * 256 : BG_GFX_SUB + (vy - SCREEN_H) * 256;
}

/* Sets a screen's BG3 transform: screen pixel (x, y) shows bitmap pixel
 * ((x - x0) * sx, (y - y0) * sy), with steps in 8.8 fixed point. */
static void set_affine(bool sub, int sx, int sy, int x0, int y0)
{
    if (sub) {
        REG_BG3PA_SUB = (int16_t)sx;
        REG_BG3PB_SUB = 0;
        REG_BG3PC_SUB = 0;
        REG_BG3PD_SUB = (int16_t)sy;
        REG_BG3X_SUB = -x0 * sx;
        REG_BG3Y_SUB = -y0 * sy;
    } else {
        REG_BG3PA = (int16_t)sx;
        REG_BG3PB = 0;
        REG_BG3PC = 0;
        REG_BG3PD = (int16_t)sy;
        REG_BG3X = -x0 * sx;
        REG_BG3Y = -y0 * sy;
    }
}

/* The bitmap that holds the canvas in fit and touch layouts. */
static uint16_t *game_fb(void)
{
    return on_sub ? BG_GFX_SUB : BG_GFX;
}

/* Draws the UI that goes with the current layout. */
static void layout_ui(void);

/* Bottom row of the canvas on the touch screen in span layout (0 if the
 * canvas fits on the top screen); the keypad goes below it. */
static int span_ui_top(void)
{
    int bottom_used = oy + canvas_h - SCREEN_H;
    return bottom_used > 0 ? bottom_used + 2 : 0;
}

static void video_layout(void)
{
    int      w = canvas_w, h = canvas_h;
    uint16_t bg = gfx_pack(0x101018);

    videoSetMode(MODE_5_2D);
    videoSetModeSub(MODE_5_2D);
    vramSetBankA(VRAM_A_MAIN_BG);
    vramSetBankC(VRAM_C_SUB_BG);
    bgInit(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    bgInitSub(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    BG_PALETTE[0] = bg;
    BG_PALETTE_SUB[0] = bg;
    fill(BG_GFX, 0, 0, 256, 256, bg);
    fill(BG_GFX_SUB, 0, 0, 256, SCREEN_H, bg);
    free(xmap);
    free(ymap);
    xmap = ymap = NULL;
    soft_scaled = false;
    set_affine(false, 256, 256, 0, 0);
    set_affine(true, 256, 256, 0, 0);

    span = settings->layout == LAYOUT_SPAN && w <= VW && h <= VH;
    on_sub = settings->layout == LAYOUT_TOUCH;
    if (span) {
        ox = (VW - w) / 2;
        oy = h <= SCREEN_H ? (SCREEN_H - h) / 2 : 0;
        layout_ui();
        return;
    }

    if (w <= 256 && h <= 256) {
        /* The canvas is copied 1:1 into VRAM and the 2D engine scales it to
         * the 256x192 screen. Slightly tall canvases (176x208) are only
         * squeezed vertically; others keep their aspect ratio. */
        int sx = 256, sy = 256;   /* bitmap pixels per screen pixel, 8.8 */
        if (h > SCREEN_H || w > 256) {
            sy = (h * 256 + SCREEN_H - 1) / SCREEN_H;
            sx = (w * 256 + 255) / 256;
            if (sx < 256)
                sx = 256;
            if (sy * 100 > sx * 110)
                sx = sy;   /* more than 10% squeeze: keep the aspect */
        }
        int shown_w = w * 256 / sx, shown_h = h * 256 / sy;
        ox = 0;
        oy = 0;
        aff_sx = sx;
        aff_sy = sy;
        aff_x0 = (256 - shown_w) / 2;
        aff_y0 = (SCREEN_H - shown_h) / 2;
        set_affine(on_sub, sx, sy, aff_x0, aff_y0);
    } else {
        /* Too big for the bitmap: scale in software (nearest neighbour). */
        soft_scaled = true;
        if ((int64_t)w * SCREEN_H > (int64_t)h * 256) {
            out_w = 256;
            out_h = h * 256 / w;
        } else {
            out_h = SCREEN_H;
            out_w = w * SCREEN_H / h;
        }
        xmap = malloc(sizeof(int) * out_w);
        ymap = malloc(sizeof(int) * out_h);
        for (int i = 0; i < out_w; i++)
            xmap[i] = i * w / out_w;
        for (int i = 0; i < out_h; i++)
            ymap[i] = i * h / out_h;
        ox = (256 - out_w) / 2;
        oy = (SCREEN_H - out_h) / 2;
    }
    layout_ui();
}

static void layout_ui(void)
{
    if (span) {
        ui_init(span_ui_top());
    } else if (on_sub) {
        ui_init(SCREEN_H);   /* no keypad: the game has the touch screen */
        ui_info_init(settings);
    } else {
        ui_init(0);
    }
}

void nds_session_start(GameSettings *s, const char *jar)
{
    console_on = false;
    settings = s;
    game_jar = jar;
    canvas_w = s->w;
    canvas_h = s->h;
    nds_exit_action = EXIT_NONE;
    nds_audio_master(s->volume);
    video_layout();
}

void pal_soft_labels(const char *left, const char *right)
{
    ui_soft_labels(left, right);
}

void pal_screen_size(int *w, int *h)
{
    *w = canvas_w;
    *h = canvas_h;
}

static void copy_row(uint16_t *dst, const uint16_t *src, int w)
{
    if (((uintptr_t)dst & 3) == 0 && ((uintptr_t)src & 3) == 0 && (w & 1) == 0) {
        uint32_t       *d32 = (uint32_t *)dst;
        const uint32_t *s32 = (const uint32_t *)src;
        for (int i = 0; i < w / 2; i++)
            d32[i] = s32[i];
    } else {
        for (int i = 0; i < w; i++)
            dst[i] = src[i];
    }
}

static void update_fps(void)
{
    static int64_t window_start;
    static int     frames;
    frames++;
    int64_t now = pal_time_ms();
    if (now - window_start < 1000)
        return;
    if (settings->show_fps) {
        int busy = 100 - (int)(idle_ms * 100 / (now - window_start));
        ui_show_fps(frames, busy < 0 ? 0 : busy);
    }
    frames = 0;
    idle_ms = 0;
    window_start = now;
}

void pal_present(const uint16_t *px, int w, int h)
{
    if (w != canvas_w || h != canvas_h)
        return;
    update_fps();
    if (span) {
        for (int y = 0; y < h; y++)
            copy_row(row_ptr(oy + y) + ox, px + y * w, w);
    } else if (!soft_scaled) {
        uint16_t *fb = game_fb();
        for (int y = 0; y < h; y++)
            copy_row(fb + y * 256, px + y * w, w);
    } else {
        uint16_t *fb = game_fb();
        for (int y = 0; y < out_h; y++) {
            uint16_t       *dst = fb + (oy + y) * 256 + ox;
            const uint16_t *src = px + ymap[y] * w;
            for (int x = 0; x < out_w; x++)
                dst[x] = src[xmap[x]];
        }
    }
}

/* ------------------------------------------------------------------------ */
/* Input                                                                    */

#define QUEUE 32
static NjEvent queue[QUEUE];
static int     q_head, q_tail;

static void push(int type, int a, int b)
{
    int next = (q_tail + 1) % QUEUE;
    if (next == q_head)
        return;
    queue[q_tail] = (NjEvent){type, a, b};
    q_tail = next;
}

static const uint32_t button_masks[NUM_BUTTONS] = {
    KEY_A, KEY_B, KEY_X, KEY_Y, KEY_L, KEY_R, KEY_START, KEY_SELECT,
    KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT
};

static bool touching;
static bool touch_on_canvas;
static int  last_tx, last_ty;

/* Converts a touch-screen point to canvas coordinates, in the layouts
 * that show the canvas on the touch screen (span and touch). */
static bool touch_to_canvas(int tx, int ty, int *cx, int *cy)
{
    if (on_sub && !span) {
        int vx, vy;
        if (soft_scaled) {
            vx = tx - ox;
            vy = ty - oy;
            if (vx < 0 || vy < 0 || vx >= out_w || vy >= out_h)
                return false;
            vx = xmap[vx];
            vy = ymap[vy];
        } else {
            vx = (tx - aff_x0) * aff_sx / 256;
            vy = (ty - aff_y0) * aff_sy / 256;
        }
        if (tx < aff_x0 || ty < aff_y0 || vx < 0 || vy < 0 || vx >= canvas_w || vy >= canvas_h)
            return false;
        *cx = vx;
        *cy = vy;
        return true;
    }
    if (!span)
        return false;
    int vx = tx - ox, vy = ty + SCREEN_H - oy;
    if (vx < 0 || vy < 0 || vx >= canvas_w || vy >= canvas_h)
        return false;
    *cx = vx;
    *cy = vy;
    return true;
}

static void open_menu(void)
{
    int64_t      start = raw_time_ms();
    GameSettings before = *settings;
    /* In the touch layout the touch screen shows the game scaled; the menu
     * is drawn 1:1. */
    bool scaled_sub = on_sub && !span && !soft_scaled;
    if (scaled_sub)
        set_affine(true, 256, 256, 0, 0);
    int action = ui_menu_run(settings, game_jar, nds_storage_ok && game_jar != NULL);
    if (scaled_sub)
        set_affine(true, aff_sx, aff_sy, aff_x0, aff_y0);
    paused_ms += raw_time_ms() - start;
    if (action != EXIT_NONE) {
        nds_exit_action = action;
        nds_stop_vm();
        return;
    }
    if (before.volume != settings->volume)
        nds_audio_master(settings->volume);
    if (before.layout != settings->layout)
        video_layout();   /* the game's next frame fills the screen again */
    else
        layout_ui();
    /* A touch that closed the menu must not reach the game: treat it as
     * already handled until the stylus is lifted. */
    touching = (keysHeld() & KEY_TOUCH) != 0;
    touch_on_canvas = false;
    /* Let go of keys that may have been held when the menu opened. */
    for (int b = 0; b < NUM_BUTTONS; b++)
        if (before.keys[b] && before.keys[b] != KEY_MENU)
            push(EV_KEY_UP, before.keys[b], 0);
}

static void poll_hardware(void)
{
    scanKeys();
    uint32_t down = keysDown(), up = keysUp(), held = keysHeld();
    /* START+SELECT always opens the menu, whatever the button map says. */
    if ((held & (KEY_START | KEY_SELECT)) == (KEY_START | KEY_SELECT) &&
        (down & (KEY_START | KEY_SELECT))) {
        open_menu();
        return;
    }
    for (int b = 0; b < NUM_BUTTONS; b++) {
        int code = settings->keys[b];
        if (!code)
            continue;
        if (down & button_masks[b]) {
            if (code == KEY_MENU) {
                open_menu();
                return;
            }
            push(EV_KEY_DOWN, code, 0);
        }
        if ((up & button_masks[b]) && code != KEY_MENU)
            push(EV_KEY_UP, code, 0);
    }

    if (held & KEY_TOUCH) {
        touchPosition tp;
        touchRead(&tp);
        int cx, cy;
        if (!touching) {
            bool on_ui;
            int  code = ui_touch(true, tp.px, tp.py, &on_ui);
            touching = true;
            touch_on_canvas = false;
            if (code == KEY_MENU) {
                touching = false;
                open_menu();
                return;
            }
            if (code) {
                push(EV_KEY_DOWN, code, 0);
            } else if (!on_ui && touch_to_canvas(tp.px, tp.py, &cx, &cy)) {
                touch_on_canvas = true;
                push(EV_POINTER_DOWN, cx, cy);
            }
        } else if (touch_on_canvas && (tp.px != last_tx || tp.py != last_ty)) {
            if (touch_to_canvas(tp.px, tp.py, &cx, &cy))
                push(EV_POINTER_DRAG, cx, cy);
        }
        last_tx = tp.px;
        last_ty = tp.py;
    } else if (touching) {
        bool on_ui;
        int  code = ui_touch(false, last_tx, last_ty, &on_ui);
        int  cx, cy;
        touching = false;
        if (code)
            push(EV_KEY_UP, code, 0);
        else if (touch_on_canvas && touch_to_canvas(last_tx, last_ty, &cx, &cy))
            push(EV_POINTER_UP, cx, cy);
    }
}

bool pal_poll_event(NjEvent *ev)
{
    if (q_head == q_tail && settings)
        poll_hardware();
    if (q_head == q_tail)
        return false;
    *ev = queue[q_head];
    q_head = (q_head + 1) % QUEUE;
    return true;
}

/* ------------------------------------------------------------------------ */
/* Storage                                                                  */

bool nds_storage_ok;

const char *pal_data_dir(void)
{
    if (!nds_storage_ok)
        return NULL;
    mkdir("/nanojava", 0777);
    mkdir("/nanojava/rms", 0777);
    return "/nanojava/rms";
}
