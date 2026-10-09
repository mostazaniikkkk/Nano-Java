/*
 * inflate.h - raw DEFLATE (RFC 1951) decoder used for JAR entries and PNG.
 */
#ifndef NJ_INFLATE_H
#define NJ_INFLATE_H

#include <stdint.h>

/* Decompresses a raw deflate stream into dst. Returns the number of bytes
 * written, or -1 on corrupt input or if dst is too small. */
int32_t nj_inflate(uint8_t *dst, uint32_t dst_len,
                   const uint8_t *src, uint32_t src_len);

/* Same for a zlib-wrapped stream (2-byte header, adler32 trailer). */
int32_t nj_zlib_inflate(uint8_t *dst, uint32_t dst_len,
                        const uint8_t *src, uint32_t src_len);

#endif
