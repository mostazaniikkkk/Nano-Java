/*
 * zip.h - read-only ZIP/JAR archive access.
 */
#ifndef NJ_ZIP_H
#define NJ_ZIP_H

#include <stdint.h>
#include <stdio.h>

typedef struct ZipEntry {
    const char      *name;
    uint32_t         hash;
    uint16_t         method;
    uint32_t         csize, usize;
    uint32_t         local_off;
    struct ZipEntry *next;
} ZipEntry;

typedef struct Zip Zip;

/* Opens an archive kept in memory (not copied; must outlive the Zip). */
Zip *zip_open_mem(const uint8_t *data, uint32_t size);
/* Opens an archive file; entries are read from disk on demand. */
Zip *zip_open_file(const char *path);
void zip_close(Zip *z);

ZipEntry *zip_find(Zip *z, const char *name);
uint32_t  zip_count(Zip *z);
ZipEntry *zip_entry(Zip *z, uint32_t index);

/* Returns a malloc'd buffer with the uncompressed entry (plus a trailing
 * NUL byte, not counted in *len), or NULL on error. */
uint8_t *zip_read(Zip *z, ZipEntry *e, uint32_t *len);

/* Convenience: find + read. */
uint8_t *zip_read_name(Zip *z, const char *name, uint32_t *len);

#endif
