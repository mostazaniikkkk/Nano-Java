/*
 * native.c - registry of native method implementations. Each module
 * registers a NULL-terminated table; methods are bound when their class is
 * linked.
 */
#include "vm.h"

#include <string.h>

#define MAX_TABLES 32

static const NativeEntry *tables[MAX_TABLES];
static int                n_tables;

void native_register(const NativeEntry *table)
{
    if (n_tables == MAX_TABLES)
        nj_fatal("too many native tables");
    tables[n_tables++] = table;
}

void native_reset(void)
{
    n_tables = 0;
}

NativeFn native_lookup(const char *cls, const char *name, const char *desc)
{
    for (int i = 0; i < n_tables; i++) {
        for (const NativeEntry *e = tables[i]; e->cls; e++) {
            if (strcmp(e->cls, cls) == 0 && strcmp(e->name, name) == 0 &&
                (!e->desc || strcmp(e->desc, desc) == 0))
                return e->fn;
        }
    }
    return NULL;
}
