/*
 * sym.c - symbol interning. Names and descriptors from class files are
 * interned once so the rest of the VM compares them by pointer.
 */
#include "vm.h"

#include <stdlib.h>

static const char **table;
static uint32_t     cap, count;

const char *S_init, *S_clinit, *S_V, *S_main, *S_run;

static uint32_t hash_bytes(const char *s, size_t len)
{
    uint32_t h = 2166136261u;
    for (size_t i = 0; i < len; i++)
        h = (h ^ (uint8_t)s[i]) * 16777619u;
    return h;
}

static void grow(void)
{
    uint32_t     ncap = cap ? cap * 2 : 1024;
    const char **nt = nj_alloc(ncap * sizeof *nt);
    for (uint32_t i = 0; i < cap; i++) {
        const char *s = table[i];
        if (!s)
            continue;
        uint32_t h = hash_bytes(s, strlen(s)) & (ncap - 1);
        while (nt[h])
            h = (h + 1) & (ncap - 1);
        nt[h] = s;
    }
    free(table);
    table = nt;
    cap = ncap;
}

const char *sym_intern_len(const char *s, size_t len)
{
    if (count * 2 >= cap)
        grow();
    uint32_t h = hash_bytes(s, len) & (cap - 1);
    for (;;) {
        const char *e = table[h];
        if (!e)
            break;
        if (strncmp(e, s, len) == 0 && e[len] == '\0')
            return e;
        h = (h + 1) & (cap - 1);
    }
    char *copy = nj_meta(len + 1);
    memcpy(copy, s, len);
    copy[len] = '\0';
    table[h] = copy;
    count++;
    return copy;
}

const char *sym_intern(const char *s)
{
    return sym_intern_len(s, strlen(s));
}

void sym_reset(void)
{
    free(table);
    table = NULL;
    cap = count = 0;
}

void sym_init(void)
{
    S_init = sym_intern("<init>");
    S_clinit = sym_intern("<clinit>");
    S_V = sym_intern("()V");
    S_main = sym_intern("main");
    S_run = sym_intern("run");
}
