/*
 * font_data.h - built-in bitmap fonts (see tools/genfont.py).
 */
#ifndef NJ_FONT_DATA_H
#define NJ_FONT_DATA_H

#include <stdint.h>

#define FONT_FIRST 32
#define FONT_LAST  255

typedef struct FontData {
    uint8_t        height;        /* line height in pixels */
    uint8_t        baseline;      /* pixels from the top to the baseline */
    uint8_t        bytes_per_row;
    const uint8_t *widths;        /* advance per glyph */
    const uint8_t *bits;          /* glyph rows, LSB = leftmost pixel */
} FontData;

extern const FontData font_data[3];

#endif
