/*
 * zip.c - ZIP central directory reader. Many J2ME jars were produced by
 * obfuscators and hand-rolled packers, so the reader trusts only the central
 * directory and skips anything it does not understand.
 */
#include "zip.h"
#include "inflate.h"

#include <stdlib.h>
#include <string.h>

struct Zip {
    const uint8_t *mem;      /* in-memory archive, or NULL */
    FILE          *file;
    uint32_t       size;
    ZipEntry      *entries;
    uint32_t       n;
    ZipEntry     **buckets;
    uint32_t       nbuckets;
    char          *names;
};

static uint32_t name_hash(const char *s)
{
    uint32_t h = 2166136261u;
    while (*s)
        h = (h ^ (uint8_t)*s++) * 16777619u;
    return h;
}

static int read_at(Zip *z, uint32_t off, void *dst, uint32_t len)
{
    if (off > z->size || len > z->size - off)
        return -1;
    if (z->mem) {
        memcpy(dst, z->mem + off, len);
        return 0;
    }
    if (fseek(z->file, (long)off, SEEK_SET) != 0)
        return -1;
    return fread(dst, 1, len, z->file) == len ? 0 : -1;
}

static uint16_t le16(const uint8_t *p) { return (uint16_t)(p[0] | (p[1] << 8)); }
static uint32_t le32(const uint8_t *p)
{
    return p[0] | (p[1] << 8) | (p[2] << 16) | ((uint32_t)p[3] << 24);
}

static int parse(Zip *z)
{
    /* Locate the end-of-central-directory record. */
    uint32_t tail = z->size < 65557 ? z->size : 65557;
    uint8_t *buf = malloc(tail);
    if (!buf || read_at(z, z->size - tail, buf, tail) != 0) {
        free(buf);
        return -1;
    }
    int eocd = -1;
    for (int i = (int)tail - 22; i >= 0; i--) {
        if (le32(buf + i) == 0x06054b50) {
            eocd = i;
            break;
        }
    }
    if (eocd < 0) {
        free(buf);
        return -1;
    }
    uint32_t count = le16(buf + eocd + 10);
    uint32_t cd_size = le32(buf + eocd + 12);
    uint32_t cd_off = le32(buf + eocd + 16);
    free(buf);

    uint8_t *cd = malloc(cd_size ? cd_size : 1);
    if (!cd || read_at(z, cd_off, cd, cd_size) != 0) {
        free(cd);
        return -1;
    }

    z->entries = calloc(count ? count : 1, sizeof(ZipEntry));
    z->names = malloc(cd_size + 1);
    z->nbuckets = 16;
    while (z->nbuckets < count)
        z->nbuckets <<= 1;
    z->buckets = calloc(z->nbuckets, sizeof(ZipEntry *));
    if (!z->entries || !z->names || !z->buckets) {
        free(cd);
        return -1;
    }

    char    *names = z->names;
    uint32_t p = 0;
    for (uint32_t i = 0; i < count; i++) {
        if (p + 46 > cd_size || le32(cd + p) != 0x02014b50)
            break;
        uint16_t nlen = le16(cd + p + 28);
        uint16_t xlen = le16(cd + p + 30);
        uint16_t clen = le16(cd + p + 32);
        if (p + 46 + nlen > cd_size)
            break;
        ZipEntry *e = &z->entries[z->n];
        e->method = le16(cd + p + 10);
        e->csize = le32(cd + p + 20);
        e->usize = le32(cd + p + 24);
        e->local_off = le32(cd + p + 42);
        memcpy(names, cd + p + 46, nlen);
        names[nlen] = '\0';
        e->name = names;
        names += nlen + 1;
        e->hash = name_hash(e->name);
        uint32_t b = e->hash & (z->nbuckets - 1);
        e->next = z->buckets[b];
        z->buckets[b] = e;
        z->n++;
        p += 46 + nlen + xlen + clen;
    }
    free(cd);
    return 0;
}

static Zip *finish_open(Zip *z)
{
    if (parse(z) != 0) {
        zip_close(z);
        return NULL;
    }
    return z;
}

Zip *zip_open_mem(const uint8_t *data, uint32_t size)
{
    Zip *z = calloc(1, sizeof *z);
    if (!z)
        return NULL;
    z->mem = data;
    z->size = size;
    return finish_open(z);
}

Zip *zip_open_file(const char *path)
{
    FILE *f = fopen(path, "rb");
    if (!f)
        return NULL;
    Zip *z = calloc(1, sizeof *z);
    if (!z) {
        fclose(f);
        return NULL;
    }
    z->file = f;
    fseek(f, 0, SEEK_END);
    z->size = (uint32_t)ftell(f);
    return finish_open(z);
}

void zip_close(Zip *z)
{
    if (!z)
        return;
    if (z->file)
        fclose(z->file);
    free(z->entries);
    free(z->names);
    free(z->buckets);
    free(z);
}

ZipEntry *zip_find(Zip *z, const char *name)
{
    if (name[0] == '/')
        name++;
    uint32_t h = name_hash(name);
    for (ZipEntry *e = z->buckets[h & (z->nbuckets - 1)]; e; e = e->next)
        if (e->hash == h && strcmp(e->name, name) == 0)
            return e;
    return NULL;
}

uint32_t zip_count(Zip *z) { return z->n; }

ZipEntry *zip_entry(Zip *z, uint32_t index)
{
    return index < z->n ? &z->entries[index] : NULL;
}

uint8_t *zip_read(Zip *z, ZipEntry *e, uint32_t *len)
{
    uint8_t hdr[30];
    if (read_at(z, e->local_off, hdr, 30) != 0 || le32(hdr) != 0x04034b50)
        return NULL;
    uint32_t data_off = e->local_off + 30 + le16(hdr + 26) + le16(hdr + 28);

    uint8_t *out = malloc(e->usize + 1);
    if (!out)
        return NULL;
    if (e->method == 0) {
        if (read_at(z, data_off, out, e->usize) != 0)
            goto fail;
    } else if (e->method == 8) {
        uint8_t *in = malloc(e->csize ? e->csize : 1);
        if (!in)
            goto fail;
        if (read_at(z, data_off, in, e->csize) != 0 ||
            nj_inflate(out, e->usize, in, e->csize) != (int32_t)e->usize) {
            free(in);
            goto fail;
        }
        free(in);
    } else {
        goto fail;
    }
    out[e->usize] = 0;
    if (len)
        *len = e->usize;
    return out;
fail:
    free(out);
    return NULL;
}

uint8_t *zip_read_name(Zip *z, const char *name, uint32_t *len)
{
    ZipEntry *e = zip_find(z, name);
    return e ? zip_read(z, e, len) : NULL;
}
