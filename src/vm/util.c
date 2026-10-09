/*
 * util.c - allocation and logging helpers.
 */
#include "vm.h"
#include "../pal/pal.h"

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

void *nj_alloc(size_t n)
{
    void *p = calloc(1, n ? n : 1);
    if (!p)
        nj_fatal("out of native memory (%u bytes)", (unsigned)n);
    return p;
}

/* Metadata (classes, methods, symbols) lives as long as the VM, so it is
 * carved out of large blocks to avoid per-allocation malloc overhead. All
 * blocks are chained so nj_meta_free_all() can release them at shutdown. */
#define META_BLOCK 16384

typedef union MetaBlock {
    union MetaBlock *next;
    double           align;
} MetaBlock;

static MetaBlock *meta_blocks;
static char      *meta_cur;
static size_t     meta_left;

static void *meta_block(size_t n)
{
    MetaBlock *b = nj_alloc(sizeof(MetaBlock) + n);
    b->next = meta_blocks;
    meta_blocks = b;
    return b + 1;
}

void nj_meta_free_all(void)
{
    while (meta_blocks) {
        MetaBlock *next = meta_blocks->next;
        free(meta_blocks);
        meta_blocks = next;
    }
    meta_cur = NULL;
    meta_left = 0;
}

void *nj_meta(size_t n)
{
    n = (n + 7) & ~(size_t)7;
    if (n > META_BLOCK / 4)
        return meta_block(n);
    if (n > meta_left) {
        meta_cur = meta_block(META_BLOCK);
        meta_left = META_BLOCK;
    }
    void *p = meta_cur;
    meta_cur += n;
    meta_left -= n;
    return p;
}

void nj_log(const char *fmt, ...)
{
    char buf[512];
    va_list ap;
    va_start(ap, fmt);
    int n = vsnprintf(buf, sizeof buf - 1, fmt, ap);
    va_end(ap);
    if (n < 0)
        return;
    if (n > (int)sizeof buf - 2)
        n = sizeof buf - 2;
    buf[n++] = '\n';
    pal_console_write(buf, n);
}

void nj_fatal(const char *fmt, ...)
{
    char buf[512];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, sizeof buf, fmt, ap);
    va_end(ap);
    nj_log("FATAL: %s", buf);
    pal_fatal(buf);
}
