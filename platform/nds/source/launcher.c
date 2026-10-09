/*
 * launcher.c - the game list. The top screen shows the selected game (icon,
 * name, vendor, version, description, screen size); the touch screen lists
 * the MIDlets (.jar / .jad) found in /nanojava, /java, /games and the root
 * of the SD card. Details and icons come from each jar's manifest (and the
 * .jad, if there is one) and are read only for the rows being shown.
 *
 * Controls: D-pad or touch to select, A or a second tap to start, L/R to
 * change the phone screen size, B to go back from the MIDlet chooser.
 */
#include <nds.h>

#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>

#include "nds_platform.h"
#include "draw.h"
#include "../../../src/util/png.h"
#include "../../../src/util/zip.h"

#define MAX_FILES   256
#define MAX_MIDLETS 8
#define ICON        32      /* stored icon size limit */
#define HEADER_H    20
#define ROW_H       34
#define ROWS        ((DRAW_H - HEADER_H) / ROW_H)

#define C_BG_TOP    0x1C2B4C
#define C_BG_BOTTOM 0x0C1222
#define C_CARD      0x22304E
#define C_CARD_EDGE 0x3E5684
#define C_TILE      0x2E4068
#define C_LIST_BG   0x161B24
#define C_HEADER    0x0F131A
#define C_ROW_SEL   0x2F6FC0
#define C_WHITE     0xFFFFFF
#define C_SUBTEXT   0xA4B3CF
#define C_FAINT     0x6E7C96
#define C_ACCENT    0xFFC94A

static const int sizes[][2] = {
    {240, 320}, {176, 208}, {176, 220}, {128, 160}, {128, 128}, {208, 208},
    {240, 266}, {256, 192}, {320, 240}
};
#define N_SIZES (int)(sizeof sizes / sizeof sizes[0])

static const char *const dirs[] = {"/nanojava", "/java", "/games", "/"};

typedef struct {
    char     *name;
    char     *cls;
    char     *icon_path;
} MidletEntry;

typedef struct {
    char        *path;           /* the .jar or .jad picked */
    const char  *file;           /* file name part of path */
    bool         loaded;
    bool         broken;         /* not a readable jar */
    char        *name, *vendor, *version, *description;
    char        *icon_path;      /* MIDlet-Icon */
    uint32_t    *icon;           /* ARGB, at most ICON x ICON */
    int          icon_w, icon_h;
    MidletEntry  midlets[MAX_MIDLETS];
    int          n_midlets;
    int          screen_w, screen_h;
} Game;

static Game     games[MAX_FILES];
static int      n_games;
static uint16_t top_buf[DRAW_W * DRAW_H];
static uint16_t bottom_buf[DRAW_W * DRAW_H];

/* ------------------------------------------------------------------------ */
/* Reading games                                                            */

static bool has_ext(const char *name, const char *ext)
{
    size_t n = strlen(name), e = strlen(ext);
    return n > e && strcasecmp(name + n - e, ext) == 0;
}

static char *dup_trim(const char *s, size_t n)
{
    while (n && (*s == ' ' || *s == '\t'))
        s++, n--;
    while (n && (s[n - 1] == ' ' || s[n - 1] == '\t' || s[n - 1] == '\r'))
        n--;
    char *r = malloc(n + 1);
    memcpy(r, s, n);
    r[n] = 0;
    return r;
}

/* Calls fn for each "Key: value" in a manifest or .jad. Manifest lines
 * continue on the next line when it starts with a space. */
static void parse_props(const char *text, bool manifest,
                        void (*fn)(Game *g, const char *key, const char *val), Game *g)
{
    char  key[64];
    char *val = NULL;
    size_t val_len = 0;
    const char *p = text;
    key[0] = 0;
    while (*p) {
        const char *end = p + strcspn(p, "\r\n");
        size_t      len = (size_t)(end - p);
        if (manifest && *p == ' ' && key[0]) {
            val = realloc(val, val_len + len);
            memcpy(val + val_len, p + 1, len - 1);
            val_len += len - 1;
        } else {
            if (key[0] && val) {
                char *v = dup_trim(val, val_len);
                fn(g, key, v);
                free(v);
            }
            key[0] = 0;
            const char *colon = memchr(p, ':', len);
            if (colon && colon - p < (int)sizeof key) {
                memcpy(key, p, (size_t)(colon - p));
                key[colon - p] = 0;
                val_len = len - (size_t)(colon - p) - 1;
                val = realloc(val, val_len + 1);
                memcpy(val, colon + 1, val_len);
            }
        }
        p = end;
        while (*p == '\r' || *p == '\n')
            p++;
    }
    if (key[0] && val) {
        char *v = dup_trim(val, val_len);
        fn(g, key, v);
        free(v);
    }
    free(val);
}

static void set_str(char **field, const char *v)
{
    free(*field);
    *field = *v ? strdup(v) : NULL;
}

static void on_prop(Game *g, const char *key, const char *val)
{
    if (strcmp(key, "MIDlet-Name") == 0) {
        set_str(&g->name, val);
    } else if (strcmp(key, "MIDlet-Vendor") == 0) {
        set_str(&g->vendor, val);
    } else if (strcmp(key, "MIDlet-Version") == 0) {
        set_str(&g->version, val);
    } else if (strcmp(key, "MIDlet-Description") == 0) {
        set_str(&g->description, val);
    } else if (strcmp(key, "MIDlet-Icon") == 0) {
        set_str(&g->icon_path, val);
    } else if (strncmp(key, "MIDlet-", 7) == 0 && key[7] >= '1' && key[7] <= '9' && !key[8]) {
        /* "MIDlet-n: Name, /icon.png, com.example.Main" */
        int i = key[7] - '1';
        if (i >= MAX_MIDLETS)
            return;
        const char *c1 = strchr(val, ',');
        const char *c2 = c1 ? strchr(c1 + 1, ',') : NULL;
        if (!c2)
            return;
        MidletEntry *m = &g->midlets[i];
        free(m->name);
        free(m->icon_path);
        free(m->cls);
        m->name = dup_trim(val, (size_t)(c1 - val));
        m->icon_path = dup_trim(c1 + 1, (size_t)(c2 - c1 - 1));
        m->cls = dup_trim(c2 + 1, strlen(c2 + 1));
        if (i + 1 > g->n_midlets)
            g->n_midlets = i + 1;
    }
}

typedef struct {
    uint32_t *px;
    int       w;
} IconCtx;

static void icon_row(void *vctx, int y, int x0, int step, const uint32_t *argb, int n)
{
    IconCtx *c = vctx;
    for (int i = 0; i < n; i++)
        c->px[y * c->w + x0 + i * step] = argb[i];
}

/* Decodes a PNG icon, shrinking it to at most ICON x ICON. */
static void load_icon(Game *g, Zip *z, const char *path)
{
    if (!path || !*path)
        return;
    uint32_t len;
    uint8_t *data = zip_read_name(z, path[0] == '/' ? path + 1 : path, &len);
    PngInfo  info;
    if (!data || !png_info(data, len, &info) || info.width * info.height > 256 * 256) {
        free(data);
        return;
    }
    uint32_t *full = calloc((size_t)info.width * info.height, 4);
    IconCtx   ctx = {full, info.width};
    if (full && png_decode(data, len, icon_row, &ctx)) {
        int w = info.width, h = info.height;
        int dw = w, dh = h;
        if (dw > ICON || dh > ICON) {
            if (w >= h) {
                dw = ICON;
                dh = h * ICON / w;
            } else {
                dh = ICON;
                dw = w * ICON / h;
            }
        }
        g->icon = malloc(sizeof(uint32_t) * dw * dh);
        for (int y = 0; y < dh; y++)
            for (int x = 0; x < dw; x++)
                g->icon[y * dw + x] = full[(y * h / dh) * w + x * w / dw];
        g->icon_w = dw;
        g->icon_h = dh;
    }
    free(full);
    free(data);
}

static char *read_text_file(const char *path)
{
    FILE *f = fopen(path, "rb");
    if (!f)
        return NULL;
    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fseek(f, 0, SEEK_SET);
    char *buf = size >= 0 && size < 65536 ? malloc((size_t)size + 1) : NULL;
    if (buf && fread(buf, 1, (size_t)size, f) != (size_t)size) {
        free(buf);
        buf = NULL;
    }
    fclose(f);
    if (buf)
        buf[size] = 0;
    return buf;
}

char *nds_jad_jar_path(const char *jad_path, const char *jad_text)
{
    char *jar = malloc(strlen(jad_path) + 256);
    strcpy(jar, jad_path);
    char       *slash = strrchr(jar, '/');
    const char *url = jad_text ? strstr(jad_text, "MIDlet-Jar-URL:") : NULL;
    if (url && slash) {
        url += strlen("MIDlet-Jar-URL:");
        while (*url == ' ' || *url == '\t')
            url++;
        size_t      n = strcspn(url, "\r\n");
        const char *base = url;
        for (const char *p = url; p < url + n; p++)
            if (*p == '/')
                base = p + 1;
        n -= (size_t)(base - url);
        if (n > 0 && n < 200) {
            memcpy(slash + 1, base, n);
            slash[1 + n] = 0;
            FILE *f = fopen(jar, "rb");
            if (f) {
                fclose(f);
                return jar;
            }
        }
    }
    strcpy(jar, jad_path);
    strcpy(jar + strlen(jar) - 4, ".jar");
    return jar;
}

static void load_game(Game *g)
{
    if (g->loaded)
        return;
    g->loaded = true;

    char *jad = NULL, *jar = g->path;
    if (has_ext(g->path, ".jad")) {
        jad = read_text_file(g->path);
        jar = nds_jad_jar_path(g->path, jad);
    }
    Zip *z = zip_open_file(jar);
    if (z) {
        uint32_t len;
        char    *mf = (char *)zip_read_name(z, "META-INF/MANIFEST.MF", &len);
        if (mf) {
            parse_props(mf, true, on_prop, g);
            free(mf);
        }
    } else {
        g->broken = true;
    }
    if (jad)
        parse_props(jad, false, on_prop, g);   /* the .jad wins */
    if (z) {
        /* MIDlet-Icon is preferred; otherwise the first MIDlet's icon. */
        const char *icon = g->icon_path;
        if ((!icon || !*icon) && g->n_midlets > 0)
            icon = g->midlets[0].icon_path;
        load_icon(g, z, icon);
        zip_close(z);
    }
    if (jar != g->path)
        free(jar);
    free(jad);

    GameSettings s;
    settings_load(g->path, &s);
    g->screen_w = s.w;
    g->screen_h = s.h;
}

static void free_game(Game *g)
{
    free(g->path);
    free(g->name);
    free(g->vendor);
    free(g->version);
    free(g->description);
    free(g->icon_path);
    free(g->icon);
    for (int i = 0; i < MAX_MIDLETS; i++) {
        free(g->midlets[i].name);
        free(g->midlets[i].cls);
        free(g->midlets[i].icon_path);
    }
    memset(g, 0, sizeof *g);
}

static int compare_games(const void *a, const void *b)
{
    return strcasecmp(((const Game *)a)->file, ((const Game *)b)->file);
}

static void scan(void)
{
    n_games = 0;
    for (size_t d = 0; d < sizeof dirs / sizeof dirs[0]; d++) {
        DIR *dir = opendir(dirs[d]);
        if (!dir)
            continue;
        struct dirent *e;
        while ((e = readdir(dir)) && n_games < MAX_FILES) {
            const char *n = e->d_name;
            if (n[0] == '.' || !(has_ext(n, ".jar") || has_ext(n, ".jad")))
                continue;
            char *path = malloc(strlen(dirs[d]) + strlen(n) + 2);
            sprintf(path, "%s%s%s", dirs[d], strcmp(dirs[d], "/") ? "/" : "", n);
            if (has_ext(n, ".jar")) {
                /* A jar with a .jad next to it is listed once, as the .jad. */
                char *jad = strdup(path);
                strcpy(jad + strlen(jad) - 4, ".jad");
                FILE *f = fopen(jad, "rb");
                free(jad);
                if (f) {
                    fclose(f);
                    free(path);
                    continue;
                }
            }
            Game *g = &games[n_games++];
            memset(g, 0, sizeof *g);
            g->path = path;
            const char *slash = strrchr(path, '/');
            g->file = slash ? slash + 1 : path;
        }
        closedir(dir);
    }
    /* Sorting moves the structs, not the path strings file points into. */
    qsort(games, (size_t)n_games, sizeof games[0], compare_games);
}

/* ------------------------------------------------------------------------ */
/* Drawing                                                                  */

static const char *title_of(const Game *g)
{
    if (g->name)
        return g->name;
    if (g->n_midlets && g->midlets[0].name)
        return g->midlets[0].name;
    return g->file;
}

static void draw_icon(const Game *g, int x, int y, int box)
{
    if (!g->icon) {
        /* A generic "J" tile for games without an icon. */
        draw_round_rect(x, y, box, box, box / 5, 0x3A6EA5, 0x5C8FC8);
        draw_text_centered("J", x + box / 2, y + (box - draw_text_height(2)) / 2, 2, BOLD, C_WHITE);
        return;
    }
    int scale = box / (g->icon_w > g->icon_h ? g->icon_w : g->icon_h);
    if (scale < 1)
        scale = 1;
    int dw = g->icon_w * scale, dh = g->icon_h * scale;
    draw_argb(g->icon, g->icon_w, g->icon_h, x + (box - dw) / 2, y + (box - dh) / 2, dw, dh);
}

static void pill(int x, int y, const char *key, const char *label)
{
    int kw = draw_text_width(key, 0, BOLD) + 8;
    int lw = draw_text_width(label, 0, 0);
    draw_round_rect(x, y, kw + lw + 8, 16, 7, 0x2A3A5E, C_CARD_EDGE);
    draw_round_rect(x + 2, y + 2, kw, 12, 5, C_ACCENT, C_ACCENT);
    draw_text(key, x + 6, y + 2, 0, BOLD, 0x1A1A1A);
    draw_text(label, x + kw + 5, y + 2, 0, 0, C_WHITE);
}

static void draw_top(const Game *g)
{
    draw_target(top_buf);
    draw_vgradient(0, 0, DRAW_W, DRAW_H, C_BG_TOP, C_BG_BOTTOM);
    draw_text("Nano Java", 8, 4, 1, BOLD, C_WHITE);
    draw_text("J2ME for DS", 8 + draw_text_width("Nano Java ", 1, BOLD), 6, 0, 0, C_FAINT);

    if (!g) {
        draw_round_rect(12, 40, 232, 100, 8, C_CARD, C_CARD_EDGE);
        draw_text_centered("No games found", 128, 58, 2, BOLD, C_WHITE);
        draw_text_wrapped("Copy your .jar (and .jad) files to the /nanojava folder of "
                          "the SD card.", 28, 86, 200, 1, 0, C_SUBTEXT, 3);
        draw_flush(BG_GFX, 0, DRAW_H);
        return;
    }

    draw_round_rect(10, 24, 236, 124, 8, C_CARD, C_CARD_EDGE);
    draw_round_rect(20, 34, 68, 68, 10, C_TILE, C_CARD_EDGE);
    draw_icon(g, 22, 36, 64);

    int tx = 98, tw = 140, y = 34;
    int lines = draw_text_wrapped(title_of(g), tx, y, tw, 2, BOLD, C_WHITE, 2);
    y += lines * draw_text_height(2) + 2;
    if (g->vendor) {
        draw_text_wrapped(g->vendor, tx, y, tw, 1, 0, C_SUBTEXT, 1);
        y += draw_text_height(1) + 1;
    }
    if (g->version) {
        char v[40];
        snprintf(v, sizeof v, "Version %s", g->version);
        draw_text_in(v, tx, y, 0, 0, C_FAINT, tx, tx + tw);
    }
    const char *desc = g->broken ? "This file cannot be read." : g->description;
    if (desc)
        draw_text_wrapped(desc, 22, 108, 214, 0, 0, g->broken ? 0xFF8080 : C_SUBTEXT, 3);
    else
        draw_text_in(g->file, 22, 108, 0, 0, C_FAINT, 22, 236);

    char size[32];
    snprintf(size, sizeof size, "Screen %dx%d", g->screen_w, g->screen_h);
    pill(10, 160, "L/R", size);
    pill(176, 160, "A", "Play");
    draw_flush(BG_GFX, 0, DRAW_H);
}

static void draw_header(const char *title, const char *right, bool back)
{
    draw_rect(0, 0, DRAW_W, HEADER_H, C_HEADER);
    int x = 8;
    if (back) {
        draw_round_rect(4, 3, 40, 14, 5, 0x2A3A5E, C_CARD_EDGE);
        draw_text_centered("Back", 24, 4, 0, 0, C_WHITE);
        x = 52;
    }
    draw_text(title, x, (HEADER_H - draw_text_height(1)) / 2, 1, BOLD, C_WHITE);
    if (right)
        draw_text(right, DRAW_W - 8 - draw_text_width(right, 0, 0), 5, 0, 0, C_FAINT);
}

static void draw_scrollbar(int top, int count)
{
    if (count <= ROWS)
        return;
    int track = DRAW_H - HEADER_H - 4;
    int h = track * ROWS / count;
    if (h < 8)
        h = 8;
    int y = HEADER_H + 2 + (track - h) * top / (count - ROWS);
    draw_round_rect(DRAW_W - 5, y, 3, h, 1, C_FAINT, C_FAINT);
}

static void draw_game_list(int sel, int top)
{
    char count[16];
    draw_target(bottom_buf);
    draw_vgradient(0, 0, DRAW_W, DRAW_H, C_LIST_BG, C_BG_BOTTOM);
    snprintf(count, sizeof count, "%d", n_games);
    draw_header("Games", count, false);
    for (int r = 0; r < ROWS && top + r < n_games; r++) {
        Game *g = &games[top + r];
        load_game(g);
        int y = HEADER_H + r * ROW_H;
        if (top + r == sel)
            draw_round_rect(3, y + 2, DRAW_W - 12, ROW_H - 3, 6, C_ROW_SEL, 0x5A92DA);
        draw_icon(g, 8, y + 5, 24);
        draw_text_in(title_of(g), 40, y + 4, 1, BOLD, C_WHITE, 40, DRAW_W - 12);
        const char *sub = g->broken ? "Cannot be read" : g->vendor ? g->vendor : g->file;
        draw_text_in(sub, 40, y + 19, 0, 0, top + r == sel ? 0xDDE6F5 : C_SUBTEXT, 40, DRAW_W - 12);
    }
    draw_scrollbar(top, n_games);
    draw_flush(BG_GFX_SUB, 0, DRAW_H);
}

static void draw_midlet_list(const Game *g, int sel)
{
    draw_target(bottom_buf);
    draw_vgradient(0, 0, DRAW_W, DRAW_H, C_LIST_BG, C_BG_BOTTOM);
    draw_header("Choose", NULL, true);
    for (int i = 0; i < g->n_midlets && i < ROWS; i++) {
        int y = HEADER_H + i * ROW_H;
        if (i == sel)
            draw_round_rect(3, y + 2, DRAW_W - 6, ROW_H - 3, 6, C_ROW_SEL, 0x5A92DA);
        const char *name = g->midlets[i].name ? g->midlets[i].name : g->midlets[i].cls;
        draw_text_in(name ? name : "?", 12, y + 10, 1, BOLD, C_WHITE, 12, DRAW_W - 8);
    }
    draw_flush(BG_GFX_SUB, 0, DRAW_H);
}

/* ------------------------------------------------------------------------ */
/* Input                                                                    */

static void video_init(void)
{
    videoSetMode(MODE_5_2D);
    videoSetModeSub(MODE_5_2D);
    vramSetBankA(VRAM_A_MAIN_BG);
    vramSetBankC(VRAM_C_SUB_BG);
    bgInit(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    bgInitSub(3, BgType_Bmp16, BgSize_B16_256x256, 0, 0);
    /* A game may have left a scaling transform on the top screen. */
    REG_BG3PA = 256;
    REG_BG3PB = 0;
    REG_BG3PC = 0;
    REG_BG3PD = 256;
    REG_BG3X = 0;
    REG_BG3Y = 0;
}

static uint32_t wait_input(int *tx, int *ty)
{
    for (;;) {
        swiWaitForVBlank();
        scanKeys();
        uint32_t k = keysDown() | (keysDownRepeat() & (KEY_UP | KEY_DOWN | KEY_LEFT | KEY_RIGHT));
        *tx = *ty = -1;
        if (k & KEY_TOUCH) {
            touchPosition tp;
            touchRead(&tp);
            *tx = tp.px;
            *ty = tp.py;
        }
        if (k)
            return k;
    }
}

static void cycle_size(Game *g, int dir)
{
    int i = 0;
    while (i < N_SIZES && (sizes[i][0] != g->screen_w || sizes[i][1] != g->screen_h))
        i++;
    i = i == N_SIZES ? 0 : (i + dir + N_SIZES) % N_SIZES;
    g->screen_w = sizes[i][0];
    g->screen_h = sizes[i][1];
    GameSettings s;
    settings_load(g->path, &s);
    s.w = g->screen_w;
    s.h = g->screen_h;
    settings_save(g->path, &s);
}

/* Lets the user pick one of a suite's MIDlets; -1 = back. */
static int choose_midlet(Game *g)
{
    int sel = 0;
    for (;;) {
        draw_midlet_list(g, sel);
        int      tx, ty;
        uint32_t k = wait_input(&tx, &ty);
        if (tx >= 0) {
            if (ty < HEADER_H) {
                if (tx < 48)
                    return -1;
                continue;
            }
            int row = (ty - HEADER_H) / ROW_H;
            if (row < g->n_midlets && row < ROWS) {
                if (row == sel)
                    return sel;
                sel = row;
            }
        }
        if (k & KEY_UP && sel > 0)
            sel--;
        if (k & KEY_DOWN && sel < g->n_midlets - 1 && sel < ROWS - 1)
            sel++;
        if (k & KEY_A)
            return sel;
        if (k & KEY_B)
            return -1;
    }
}

char *nds_launcher_pick(char **midlet_class)
{
    *midlet_class = NULL;
    video_init();
    scan();
    if (n_games == 0) {
        draw_top(NULL);
        draw_target(bottom_buf);
        draw_vgradient(0, 0, DRAW_W, DRAW_H, C_LIST_BG, C_BG_BOTTOM);
        draw_header("Games", "0", false);
        draw_flush(BG_GFX_SUB, 0, DRAW_H);
        for (;;)
            swiWaitForVBlank();
    }

    static int sel, top;   /* remembered across games */
    if (sel >= n_games)
        sel = top = 0;
    char *chosen = NULL;
    while (!chosen) {
        if (sel < top)
            top = sel;
        if (sel >= top + ROWS)
            top = sel - ROWS + 1;
        draw_game_list(sel, top);
        Game *g = &games[sel];
        load_game(g);
        draw_top(g);

        int      tx, ty;
        uint32_t k = wait_input(&tx, &ty);
        bool     start = (k & (KEY_A | KEY_START)) != 0;
        if (tx >= 0 && ty >= HEADER_H) {
            int row = top + (ty - HEADER_H) / ROW_H;
            if (row < n_games) {
                if (row == sel)
                    start = true;
                sel = row;
            }
        }
        if (k & KEY_UP && sel > 0)
            sel--;
        if (k & KEY_DOWN && sel < n_games - 1)
            sel++;
        if (k & KEY_LEFT)
            sel = sel >= ROWS ? sel - ROWS : 0;
        if (k & KEY_RIGHT)
            sel = sel + ROWS < n_games ? sel + ROWS : n_games - 1;
        if (k & (KEY_L | KEY_R))
            cycle_size(&games[sel], (k & KEY_R) ? 1 : -1);
        if (!start)
            continue;

        g = &games[sel];
        if (g->broken)
            continue;
        if (g->n_midlets > 1) {
            int m = choose_midlet(g);
            if (m < 0)
                continue;
            if (g->midlets[m].cls)
                *midlet_class = strdup(g->midlets[m].cls);
        }
        chosen = strdup(g->path);
    }
    for (int i = 0; i < n_games; i++)
        free_game(&games[i]);
    n_games = 0;
    return chosen;
}
