/*
 * png.h - PNG decoder (all color types and bit depths, Adam7 interlace).
 */
#ifndef NJ_PNG_H
#define NJ_PNG_H

#include <stdbool.h>
#include <stdint.h>

typedef struct PngInfo {
    int  width, height;
    bool has_alpha;    /* alpha channel or tRNS present */
} PngInfo;

/* Receives n decoded ARGB pixels for row y, at x0, x0 + step, ... */
typedef void (*PngRowFn)(void *ctx, int y, int x0, int step, const uint32_t *argb, int n);

bool png_is_png(const uint8_t *data, uint32_t len);
bool png_info(const uint8_t *data, uint32_t len, PngInfo *info);
bool png_decode(const uint8_t *data, uint32_t len, PngRowFn fn, void *ctx);

#endif
