/*
 * natives_lcdui.c - natives for javax.microedition.lcdui Graphics, Image
 * and Font, and nanojava.Screen.
 *
 * Image is { int width, height; short[] pixels; byte[] alpha; boolean
 * mutable; } and Graphics keeps its state in plain fields (see the Java
 * sources); natives read those fields directly.
 */
#include "midp.h"
#include "gfx.h"
#include "../vm/vm.h"
#include "../util/png.h"

#include <stdlib.h>

typedef struct {
    Field *img_width, *img_height, *img_pixels, *img_alpha;
    Field *g_target, *g_tx, *g_ty, *g_cx1, *g_cy1, *g_cx2, *g_cy2, *g_pixel, *g_stroke;
    Field *g_font, *f_size, *f_style;
} Fields;

static Fields F;
static bool   fields_ready;

static Field *field(const char *cls, const char *name, const char *desc)
{
    Class *c = class_load(cls);
    Field *f = c ? class_find_field(c, name, desc) : NULL;
    if (!f)
        nj_fatal("lcdui field %s.%s missing", cls, name);
    return f;
}

static void init_fields(void)
{
    if (fields_ready)
        return;
    const char *I = "javax/microedition/lcdui/Image";
    const char *G = "javax/microedition/lcdui/Graphics";
    const char *FN = "javax/microedition/lcdui/Font";
    F.img_width = field(I, "width", "I");
    F.img_height = field(I, "height", "I");
    F.img_pixels = field(I, "pixels", "[S");
    F.img_alpha = field(I, "alpha", "[B");
    F.g_target = field(G, "target", "Ljavax/microedition/lcdui/Image;");
    F.g_tx = field(G, "tx", "I");
    F.g_ty = field(G, "ty", "I");
    F.g_cx1 = field(G, "cx1", "I");
    F.g_cy1 = field(G, "cy1", "I");
    F.g_cx2 = field(G, "cx2", "I");
    F.g_cy2 = field(G, "cy2", "I");
    F.g_pixel = field(G, "pixel", "I");
    F.g_stroke = field(G, "stroke", "I");
    F.g_font = field(G, "font", "Ljavax/microedition/lcdui/Font;");
    F.f_size = field(FN, "sizeIndex", "I");
    F.f_style = field(FN, "style", "I");
    fields_ready = true;
}

static void image_surface(Object *img, Surface *s)
{
    Object *px = GET_REF(img, F.img_pixels);
    Object *al = GET_REF(img, F.img_alpha);
    s->px = ARRAY_DATA(px, uint16_t);
    s->alpha = al ? ARRAY_DATA(al, uint8_t) : NULL;
    s->w = GET_INT(img, F.img_width);
    s->h = GET_INT(img, F.img_height);
}

/* Loads the drawing context of a Graphics object. */
typedef struct {
    Surface  s;
    Clip     c;
    int      tx, ty;
    uint16_t color;
    bool     dotted;
} Ctx;

static bool ctx_of(Thread *t, Object *g, Ctx *x)
{
    init_fields();
    Object *img = GET_REF(g, F.g_target);
    if (!img) {
        NPE(t);
        return false;
    }
    image_surface(img, &x->s);
    x->c.x1 = GET_INT(g, F.g_cx1);
    x->c.y1 = GET_INT(g, F.g_cy1);
    x->c.x2 = GET_INT(g, F.g_cx2);
    x->c.y2 = GET_INT(g, F.g_cy2);
    x->tx = GET_INT(g, F.g_tx);
    x->ty = GET_INT(g, F.g_ty);
    x->color = (uint16_t)GET_INT(g, F.g_pixel);
    x->dotted = GET_INT(g, F.g_stroke) != 0;
    return x->c.x1 < x->c.x2 && x->c.y1 < x->c.y2;
}

#define G_BEGIN() Ctx x; if (!ctx_of(t, args[0].ref, &x)) return NATIVE_OK
#define A(n)      (args[(n)].i)

/* ------------------------------------------------------------------------ */
/* Graphics                                                                 */

static int G_fillRect(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_fill_rect(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3), A(4), x.color);
    return NATIVE_OK;
}

static int G_drawRect(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_rect(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3), A(4), x.color, x.dotted);
    return NATIVE_OK;
}

static int G_drawLine(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_line(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3) + x.tx, A(4) + x.ty, x.color, x.dotted);
    return NATIVE_OK;
}

static int G_drawRoundRect(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_round_rect(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3), A(4), A(5), A(6), x.color, false);
    return NATIVE_OK;
}

static int G_fillRoundRect(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_round_rect(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3), A(4), A(5), A(6), x.color, true);
    return NATIVE_OK;
}

static int G_drawArc(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_arc(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3), A(4), A(5), A(6), x.color, false);
    return NATIVE_OK;
}

static int G_fillArc(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_arc(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3), A(4), A(5), A(6), x.color, true);
    return NATIVE_OK;
}

static int G_fillTriangle(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    gfx_fill_triangle(&x.s, &x.c, A(1) + x.tx, A(2) + x.ty, A(3) + x.tx, A(4) + x.ty,
                      A(5) + x.tx, A(6) + x.ty, x.color);
    return NATIVE_OK;
}

static void font_of(Object *g, int *size, int *style)
{
    Object *f = GET_REF(g, F.g_font);
    *size = f ? GET_INT(f, F.f_size) : 1;
    *style = f ? GET_INT(f, F.f_style) : 0;
}

/* drawString0(String s, int off, int len, int x, int y): top-left */
static int G_drawString0(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    Object *s = args[1].ref;
    int     size, style;
    font_of(args[0].ref, &size, &style);
    font_draw(&x.s, &x.c, size, style, str_chars(s) + A(2), A(3), A(4) + x.tx, A(5) + x.ty, x.color);
    return NATIVE_OK;
}

/* drawChars0(char[] c, int off, int len, int x, int y): top-left */
static int G_drawChars0(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    Object *c = args[1].ref;
    int     size, style;
    font_of(args[0].ref, &size, &style);
    font_draw(&x.s, &x.c, size, style, ARRAY_DATA(c, uint16_t) + A(2), A(3), A(4) + x.tx,
              A(5) + x.ty, x.color);
    return NATIVE_OK;
}

/* drawRegion0(Image src, int sx, int sy, int w, int h, int transform,
 *             int x, int y): destination top-left */
static int G_drawRegion0(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    Surface src;
    image_surface(args[1].ref, &src);
    int sx = A(2), sy = A(3), w = A(4), h = A(5);
    if (sx < 0 || sy < 0 || w < 0 || h < 0 || sx + w > src.w || sy + h > src.h) {
        vm_throw_new(t, "java/lang/IllegalArgumentException", "region exceeds image bounds");
        return NATIVE_OK;
    }
    gfx_blit(&x.s, &x.c, &src, sx, sy, w, h, A(6), A(7) + x.tx, A(8) + x.ty);
    return NATIVE_OK;
}

/* copyArea0(int sx, int sy, int w, int h, int dx, int dy): untranslated
 * absolute source, translated destination handled in Java. */
static int G_copyArea0(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    int sx = A(1) + x.tx, sy = A(2) + x.ty, w = A(3), h = A(4);
    if (sx < 0 || sy < 0 || w < 0 || h < 0 || sx + w > x.s.w || sy + h > x.s.h) {
        vm_throw_new(t, "java/lang/IllegalArgumentException", "copyArea out of bounds");
        return NATIVE_OK;
    }
    gfx_blit(&x.s, &x.c, &x.s, sx, sy, w, h, TRANS_NONE, A(5) + x.tx, A(6) + x.ty);
    return NATIVE_OK;
}

/* drawRGB0(int[] rgb, int offset, int scan, int x, int y, int w, int h,
 *          boolean alpha) */
static int G_drawRGB0(Thread *t, Slot *args, Slot *ret)
{
    G_BEGIN();
    Object *rgb = args[1].ref;
    int     off = A(2), scan = A(3), w = A(6), h = A(7);
    if (!rgb) {
        NPE(t);
        return NATIVE_OK;
    }
    if (w <= 0 || h <= 0)
        return NATIVE_OK;
    int64_t first = (int64_t)off + (scan < 0 ? (int64_t)(h - 1) * scan : 0);
    int64_t last = (int64_t)off + (scan < 0 ? 0 : (int64_t)(h - 1) * scan) + w - 1;
    if (first < 0 || last >= ARRAY_LEN(rgb)) {
        vm_throw_new(t, "java/lang/ArrayIndexOutOfBoundsException", NULL);
        return NATIVE_OK;
    }
    gfx_draw_rgb(&x.s, &x.c, ARRAY_DATA(rgb, int32_t), off, scan, A(4) + x.tx, A(5) + x.ty,
                 w, h, A(8) != 0);
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* Image                                                                    */

static bool alloc_pixels(Thread *t, Object *img, int w, int h, bool alpha)
{
    init_fields();
    Array *px = vm_new_prim_array(t, 'S', w * h);
    if (!px)
        return false;
    SET_REF(img, F.img_pixels, (Object *)px);
    SET_INT(img, F.img_width, w);
    SET_INT(img, F.img_height, h);
    SET_REF(img, F.img_alpha, NULL);
    if (alpha) {
        Array *al = vm_new_prim_array(t, 'B', w * h);
        if (!al)
            return false;
        SET_REF(img, F.img_alpha, (Object *)al);
    }
    return true;
}

typedef struct {
    uint16_t *px;
    uint8_t  *alpha;
    int       w;
    bool      translucent;
} DecodeCtx;

static void png_row(void *vctx, int y, int x0, int step, const uint32_t *argb, int n)
{
    DecodeCtx *d = vctx;
    for (int i = 0; i < n; i++) {
        int      idx = y * d->w + x0 + i * step;
        uint32_t p = argb[i];
        d->px[idx] = gfx_pack(p);
        if (d->alpha) {
            d->alpha[idx] = (uint8_t)(p >> 24);
            if ((p >> 24) != 255)
                d->translucent = true;
        }
    }
}

/* static boolean decode(Image img, byte[] data, int off, int len) */
static int Image_decode(Thread *t, Slot *args, Slot *ret)
{
    Object *img = args[0].ref, *data = args[1].ref;
    int     off = A(2), len = A(3);
    if (!data) {
        NPE(t);
        return NATIVE_OK;
    }
    if (off < 0 || len < 0 || off > ARRAY_LEN(data) - len) {
        vm_throw_new(t, "java/lang/ArrayIndexOutOfBoundsException", NULL);
        return NATIVE_OK;
    }
    const uint8_t *bytes = ARRAY_DATA(data, uint8_t) + off;
    PngInfo        info;
    if (!png_info(bytes, (uint32_t)len, &info))
        return NATIVE_OK;
    if (!alloc_pixels(t, img, info.width, info.height, info.has_alpha))
        return NATIVE_OK;
    DecodeCtx d;
    d.px = ARRAY_DATA(GET_REF(img, F.img_pixels), uint16_t);
    d.alpha = info.has_alpha ? ARRAY_DATA(GET_REF(img, F.img_alpha), uint8_t) : NULL;
    d.w = info.width;
    d.translucent = false;
    if (!png_decode(bytes, (uint32_t)len, png_row, &d))
        return NATIVE_OK;
    if (info.has_alpha && !d.translucent)
        SET_REF(img, F.img_alpha, NULL);   /* tRNS present but unused */
    ret->i = 1;
    return NATIVE_OK;
}

/* static void initRGB(Image img, int[] rgb, int w, int h, boolean alpha) */
static int Image_initRGB(Thread *t, Slot *args, Slot *ret)
{
    Object *img = args[0].ref, *rgb = args[1].ref;
    int     w = A(2), h = A(3);
    bool    alpha = A(4) != 0;
    if (!rgb) {
        NPE(t);
        return NATIVE_OK;
    }
    if ((int64_t)w * h > ARRAY_LEN(rgb)) {
        vm_throw_new(t, "java/lang/ArrayIndexOutOfBoundsException", NULL);
        return NATIVE_OK;
    }
    if (!alloc_pixels(t, img, w, h, alpha))
        return NATIVE_OK;
    uint16_t      *px = ARRAY_DATA(GET_REF(img, F.img_pixels), uint16_t);
    uint8_t       *al = alpha ? ARRAY_DATA(GET_REF(img, F.img_alpha), uint8_t) : NULL;
    const int32_t *src = ARRAY_DATA(rgb, int32_t);
    for (int i = 0; i < w * h; i++) {
        px[i] = gfx_pack((uint32_t)src[i]);
        if (al)
            al[i] = (uint8_t)((uint32_t)src[i] >> 24);
    }
    return NATIVE_OK;
}

/* static void initBlank(Image img, int w, int h): white, opaque */
static int Image_initBlank(Thread *t, Slot *args, Slot *ret)
{
    Object *img = args[0].ref;
    if (!alloc_pixels(t, img, A(1), A(2), false))
        return NATIVE_OK;
    uint16_t *px = ARRAY_DATA(GET_REF(img, F.img_pixels), uint16_t);
    for (int i = 0; i < A(1) * A(2); i++)
        px[i] = 0xFFFF;
    return NATIVE_OK;
}

/* static void initRegion(Image dst, Image src, int x, int y, int w, int h,
 *                        int transform) */
static int Image_initRegion(Thread *t, Slot *args, Slot *ret)
{
    Object *dst = args[0].ref, *srcimg = args[1].ref;
    int     x = A(2), y = A(3), w = A(4), h = A(5), tr = A(6);
    init_fields();
    bool    has_alpha = GET_REF(srcimg, F.img_alpha) != NULL;
    int     dw = tr >= 4 ? h : w, dh = tr >= 4 ? w : h;
    if (!alloc_pixels(t, dst, dw, dh, has_alpha))
        return NATIVE_OK;
    Surface src, out;
    image_surface(srcimg, &src);
    image_surface(dst, &out);
    Clip all = {0, 0, dw, dh};
    /* Copy without blending: alpha is carried over separately. */
    Surface opaque = src;
    opaque.alpha = NULL;
    gfx_blit(&out, &all, &opaque, x, y, w, h, tr, 0, 0);
    if (has_alpha) {
        /* Transform the alpha plane by blitting it as a fake surface of
         * 16-bit values. */
        uint16_t *a16 = malloc(sizeof(uint16_t) * (size_t)src.w * src.h);
        uint16_t *o16 = malloc(sizeof(uint16_t) * (size_t)dw * dh);
        if (a16 && o16) {
            for (int i = 0; i < src.w * src.h; i++)
                a16[i] = src.alpha[i];
            Surface as = {a16, NULL, src.w, src.h}, ao = {o16, NULL, dw, dh};
            gfx_blit(&ao, &all, &as, x, y, w, h, tr, 0, 0);
            uint8_t *al = ARRAY_DATA(GET_REF(dst, F.img_alpha), uint8_t);
            for (int i = 0; i < dw * dh; i++)
                al[i] = (uint8_t)o16[i];
        }
        free(a16);
        free(o16);
    }
    return NATIVE_OK;
}

/* getRGB(int[] out, int offset, int scan, int x, int y, int w, int h) */
static int Image_getRGB(Thread *t, Slot *args, Slot *ret)
{
    init_fields();
    Object *img = args[0].ref, *out = args[1].ref;
    int     off = A(2), scan = A(3), x = A(4), y = A(5), w = A(6), h = A(7);
    if (!out) {
        NPE(t);
        return NATIVE_OK;
    }
    Surface s;
    image_surface(img, &s);
    if (x < 0 || y < 0 || w < 0 || h < 0 || x + w > s.w || y + h > s.h) {
        vm_throw_new(t, "java/lang/IllegalArgumentException", "area exceeds image bounds");
        return NATIVE_OK;
    }
    if (w == 0 || h == 0)
        return NATIVE_OK;
    if ((scan >= 0 && scan < w) || (scan < 0 && -scan < w)) {
        vm_throw_new(t, "java/lang/IllegalArgumentException", "scanlength");
        return NATIVE_OK;
    }
    int64_t first = (int64_t)off + (scan < 0 ? (int64_t)(h - 1) * scan : 0);
    int64_t last = (int64_t)off + (scan < 0 ? 0 : (int64_t)(h - 1) * scan) + w - 1;
    if (off < 0 || first < 0 || last >= ARRAY_LEN(out)) {
        vm_throw_new(t, "java/lang/ArrayIndexOutOfBoundsException", NULL);
        return NATIVE_OK;
    }
    int32_t *o = ARRAY_DATA(out, int32_t);
    for (int yy = 0; yy < h; yy++) {
        for (int xx = 0; xx < w; xx++) {
            int      si = (y + yy) * s.w + x + xx;
            uint32_t a = s.alpha ? s.alpha[si] : 255;
            o[off + yy * scan + xx] = (int32_t)((a << 24) | gfx_unpack(s.px[si]));
        }
    }
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* Font                                                                     */

static int Font_charWidth0(Thread *t, Slot *args, Slot *ret)
{
    ret->i = font_char_width(A(0), A(1), A(2));
    return NATIVE_OK;
}

static int Font_stringWidth0(Thread *t, Slot *args, Slot *ret)
{
    Object *s = args[2].ref;
    ret->i = font_chars_width(A(0), A(1), str_chars(s) + A(3), A(4));
    return NATIVE_OK;
}

static int Font_charsWidth0(Thread *t, Slot *args, Slot *ret)
{
    Object *c = args[2].ref;
    ret->i = font_chars_width(A(0), A(1), ARRAY_DATA(c, uint16_t) + A(3), A(4));
    return NATIVE_OK;
}

static int Font_height0(Thread *t, Slot *args, Slot *ret)
{
    ret->i = font_height(A(0));
    return NATIVE_OK;
}

static int Font_baseline0(Thread *t, Slot *args, Slot *ret)
{
    ret->i = font_baseline(A(0));
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* nanojava.Screen                                                          */

static int Screen_width(Thread *t, Slot *args, Slot *ret)
{
    int w, h;
    pal_screen_size(&w, &h);
    ret->i = w;
    return NATIVE_OK;
}

static int Screen_height(Thread *t, Slot *args, Slot *ret)
{
    int w, h;
    pal_screen_size(&w, &h);
    ret->i = h;
    return NATIVE_OK;
}

/* static void present(Image img) */
static int Screen_present(Thread *t, Slot *args, Slot *ret)
{
    init_fields();
    Surface s;
    if (!args[0].ref) {
        NPE(t);
        return NATIVE_OK;
    }
    image_surface(args[0].ref, &s);
    pal_present(s.px, s.w, s.h);
    return NATIVE_OK;
}

/* static void softLabels(String left, String right) */
static int Screen_softLabels(Thread *t, Slot *args, Slot *ret)
{
    char l[32], r[32];
    pal_soft_labels(args[0].ref ? str_to_utf8(args[0].ref, l, sizeof l) : NULL,
                    args[1].ref ? str_to_utf8(args[1].ref, r, sizeof r) : NULL);
    return NATIVE_OK;
}

#define G "javax/microedition/lcdui/Graphics"
#define IMG "javax/microedition/lcdui/Image"
#define FONT "javax/microedition/lcdui/Font"

static const NativeEntry lcdui_natives[] = {
    {G, "fillRect", NULL, G_fillRect},
    {G, "drawRect", NULL, G_drawRect},
    {G, "drawLine", NULL, G_drawLine},
    {G, "drawRoundRect", NULL, G_drawRoundRect},
    {G, "fillRoundRect", NULL, G_fillRoundRect},
    {G, "drawArc", NULL, G_drawArc},
    {G, "fillArc", NULL, G_fillArc},
    {G, "fillTriangle", NULL, G_fillTriangle},
    {G, "drawString0", NULL, G_drawString0},
    {G, "drawChars0", NULL, G_drawChars0},
    {G, "drawRegion0", NULL, G_drawRegion0},
    {G, "copyArea0", NULL, G_copyArea0},
    {G, "drawRGB0", NULL, G_drawRGB0},
    {IMG, "decode", NULL, Image_decode},
    {IMG, "initRGB", NULL, Image_initRGB},
    {IMG, "initBlank", NULL, Image_initBlank},
    {IMG, "initRegion", NULL, Image_initRegion},
    {IMG, "getRGB", NULL, Image_getRGB},
    {FONT, "charWidth0", NULL, Font_charWidth0},
    {FONT, "stringWidth0", NULL, Font_stringWidth0},
    {FONT, "charsWidth0", NULL, Font_charsWidth0},
    {FONT, "height0", NULL, Font_height0},
    {FONT, "baseline0", NULL, Font_baseline0},
    {"nanojava/Screen", "width", NULL, Screen_width},
    {"nanojava/Screen", "height", NULL, Screen_height},
    {"nanojava/Screen", "present", NULL, Screen_present},
    {"nanojava/Screen", "softLabels", NULL, Screen_softLabels},
    {NULL, NULL, NULL, NULL}
};

void midp_lcdui_init(void)
{
    fields_ready = false;
    native_register(lcdui_natives);
}
