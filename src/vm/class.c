/*
 * class.c - class loading, linking (field layout, vtables, native binding),
 * member lookup and subtype checks.
 *
 * CLDC has no user class loaders, so there is a single namespace. Class
 * bytes come from "sources" (the system class library first, then the
 * application jar), so applications cannot replace java.* classes.
 */
#include "vm.h"

#include <stdio.h>
#include <stdlib.h>

#define MAX_SOURCES 4
#define TABLE_SIZE  512

static ClassSourceFn sources[MAX_SOURCES];
static int           n_sources;
static Class        *table[TABLE_SIZE];
static char          last_error[160];

void class_add_source(ClassSourceFn fn)
{
    if (n_sources < MAX_SOURCES)
        sources[n_sources++] = fn;
}

const char *class_last_error(void) { return last_error; }

static void set_error(const char *fmt, const char *a, const char *b)
{
    snprintf(last_error, sizeof last_error, fmt, a, b ? b : "");
}

static unsigned bucket(const char *sym)
{
    return (unsigned)(((uintptr_t)sym >> 3) * 2654435761u) % TABLE_SIZE;
}

Class *class_find_loaded(const char *name)
{
    name = sym_intern(name);
    for (Class *c = table[bucket(name)]; c; c = c->hash_next)
        if (c->name == name)
            return c;
    return NULL;
}

static void table_add(Class *c)
{
    unsigned b = bucket(c->name);
    c->hash_next = table[b];
    table[b] = c;
}

/* Forgets every class (their metadata is freed with nj_meta_free_all). */
void class_reset(void)
{
    for (int i = 0; i < TABLE_SIZE; i++) {
        for (Class *c = table[i]; c; c = c->hash_next)
            free(c->statics);
        table[i] = NULL;
    }
    n_sources = 0;
}

void class_foreach(void (*fn)(Class *c, void *ctx), void *ctx)
{
    for (int i = 0; i < TABLE_SIZE; i++)
        for (Class *c = table[i]; c; c = c->hash_next)
            fn(c, ctx);
}

/* ------------------------------------------------------------------------ */
/* Arrays                                                                   */

static uint8_t elem_shift(char t)
{
    switch (t) {
    case 'Z': case 'B': return 0;
    case 'C': case 'S': return 1;
    case 'I': case 'F': return 2;
    case 'J': case 'D': return 3;
    default:  return sizeof(void *) == 8 ? 3 : 2;
    }
}

static Class *make_array_class(const char *name, char elem, Class *component)
{
    Class *c = nj_meta(sizeof(Class));
    memset(c, 0, sizeof *c);
    c->name = sym_intern(name);
    c->super = vm.c_Object;
    c->access = ACC_PUBLIC | ACC_FINAL | ACC_ABSTRACT;
    c->flags = CF_ARRAY;
    c->elem_type = elem;
    c->elem_shift = elem_shift(elem);
    c->component = component;
    c->vtable = vm.c_Object->vtable;
    c->vtable_len = vm.c_Object->vtable_len;
    c->state = CLS_INITIALIZED;
    table_add(c);
    return c;
}

Class *class_prim_array(char type)
{
    Class *c = vm.prim_arrays[(unsigned char)type];
    if (!c) {
        char name[3] = {'[', type, 0};
        c = class_find_loaded(name);
        if (!c)
            c = make_array_class(name, type, NULL);
        vm.prim_arrays[(unsigned char)type] = c;
    }
    return c;
}

Class *class_array_of(Class *elem)
{
    if (elem->array_class)
        return elem->array_class;
    size_t n = strlen(elem->name);
    char  *name = malloc(n + 4);
    if (elem->flags & CF_ARRAY)
        sprintf(name, "[%s", elem->name);
    else
        sprintf(name, "[L%s;", elem->name);
    Class *c = class_find_loaded(name);
    if (!c)
        c = make_array_class(name, (elem->flags & CF_ARRAY) ? '[' : 'L', elem);
    free(name);
    elem->array_class = c;
    return c;
}

static Class *load_array(const char *name)
{
    char t = name[1];
    if (t == '[') {
        Class *inner = class_load(name + 1);
        return inner ? class_array_of(inner) : NULL;
    }
    if (t == 'L') {
        size_t n = strlen(name);
        if (n < 4 || name[n - 1] != ';') {
            set_error("bad array class name %s%s", name, NULL);
            return NULL;
        }
        char *elem = malloc(n);
        memcpy(elem, name + 2, n - 3);
        elem[n - 3] = 0;
        Class *e = class_load(elem);
        free(elem);
        return e ? class_array_of(e) : NULL;
    }
    if (name[2] != 0 || !strchr("ZBCSIFJD", t)) {
        set_error("bad array class name %s%s", name, NULL);
        return NULL;
    }
    return class_prim_array(t);
}

/* ------------------------------------------------------------------------ */
/* Linking                                                                  */

static bool is_ref_desc(const char *d) { return d[0] == 'L' || d[0] == '['; }
static int  desc_slots(const char *d)  { return (d[0] == 'J' || d[0] == 'D') ? 2 : 1; }

static bool link_class(Class *c)
{
    const char **names = (const char **)c->interfaces;

    if (names[0]) {
        Class *s = class_load(names[0]);
        if (!s)
            return false;
        if (s->flags & CF_INTERFACE) {
            set_error("class %s has interface %s as superclass", c->name, s->name);
            return false;
        }
        c->super = s;
    } else if (c != vm.c_Object && strcmp(c->name, "java/lang/Object") != 0) {
        set_error("class %s has no superclass%s", c->name, NULL);
        return false;
    }

    Class **ifaces = NULL;
    if (c->n_interfaces) {
        ifaces = nj_meta(sizeof(Class *) * c->n_interfaces);
        for (int i = 0; i < c->n_interfaces; i++) {
            ifaces[i] = class_load(names[i + 1]);
            if (!ifaces[i])
                return false;
        }
    }
    c->interfaces = ifaces;

    Class *s = c->super;
    if (s)
        c->flags |= s->flags & (CF_WEAKREF | CF_THROWABLE);
    if (strcmp(c->name, "java/lang/ref/WeakReference") == 0)
        c->flags |= CF_WEAKREF;
    if (strcmp(c->name, "java/lang/Throwable") == 0)
        c->flags |= CF_THROWABLE;

    /* Field layout. */
    int inst = s ? s->inst_slots : 0;
    int stat = 0;
    int nrefs = s ? s->n_ref_slots : 0;
    for (int i = 0; i < c->n_fields; i++) {
        Field *f = &c->fields[i];
        if (f->access & ACC_STATIC) {
            f->offset = (uint16_t)stat;
            stat += desc_slots(f->desc);
        } else {
            f->offset = (uint16_t)inst;
            inst += desc_slots(f->desc);
            if (is_ref_desc(f->desc))
                nrefs++;
        }
    }
    if (strcmp(c->name, "java/lang/Class") == 0) {
        c->flags |= CF_MIRROR;
        inst++;   /* hidden Class* slot */
    }
    if (inst > 0xFFFF || stat > 0xFFFF) {
        set_error("class %s is too large%s", c->name, NULL);
        return false;
    }
    c->inst_slots = (uint16_t)inst;
    c->n_static_slots = (uint16_t)stat;
    if (stat)
        c->statics = nj_alloc(sizeof(Slot) * stat);
    if (nrefs) {
        c->ref_slots = nj_meta(sizeof(uint16_t) * nrefs);
        int k = 0;
        if (s && s->n_ref_slots) {
            memcpy(c->ref_slots, s->ref_slots, sizeof(uint16_t) * s->n_ref_slots);
            k = s->n_ref_slots;
        }
        for (int i = 0; i < c->n_fields; i++) {
            Field *f = &c->fields[i];
            if (!(f->access & ACC_STATIC) && is_ref_desc(f->desc))
                c->ref_slots[k++] = f->offset;
        }
        c->n_ref_slots = (uint16_t)nrefs;
    }

    /* Virtual method table. */
    if (!(c->flags & CF_INTERFACE)) {
        int cap = (s ? s->vtable_len : 0) + c->n_methods;
        Method **vt = nj_meta(sizeof(Method *) * (cap ? cap : 1));
        int n = 0;
        if (s) {
            memcpy(vt, s->vtable, sizeof(Method *) * s->vtable_len);
            n = s->vtable_len;
        }
        for (int i = 0; i < c->n_methods; i++) {
            Method *m = &c->methods[i];
            if ((m->access & (ACC_STATIC | ACC_PRIVATE)) || m->name == S_init ||
                m->name == S_clinit)
                continue;
            int slot = -1;
            for (int j = 0; j < (s ? s->vtable_len : 0); j++) {
                if (vt[j]->name == m->name && vt[j]->desc == m->desc) {
                    slot = j;
                    break;
                }
            }
            if (slot < 0)
                slot = n++;
            vt[slot] = m;
            m->vtable_index = (uint16_t)slot;
        }
        c->vtable = vt;
        c->vtable_len = (uint16_t)n;
    }

    /* Natives. */
    for (int i = 0; i < c->n_methods; i++) {
        Method *m = &c->methods[i];
        if (m->access & ACC_NATIVE) {
            m->native = native_lookup(c->name, m->name, m->desc);
            m->max_locals = m->arg_slots;
        }
    }

    c->state = CLS_LINKED;
    return true;
}

Class *class_load(const char *name)
{
    Class *c = class_find_loaded(name);
    if (c) {
        if (c->state == CLS_LOADED) {
            set_error("class circularity: %s%s", name, NULL);
            return NULL;
        }
        if (c->state == CLS_ERROR) {
            set_error("class %s failed to link earlier%s", name, NULL);
            return NULL;
        }
        return c;
    }
    if (name[0] == '[')
        return load_array(name);

    size_t   n = strlen(name);
    char    *path = malloc(n + 7);
    memcpy(path, name, n);
    memcpy(path + n, ".class", 7);
    uint8_t *data = NULL;
    uint32_t len = 0;
    for (int i = 0; i < n_sources && !data; i++)
        data = sources[i](path, &len);
    free(path);
    if (!data) {
        set_error("%s%s", name, NULL);
        return NULL;
    }

    const char *err;
    c = classfile_parse(data, len, &err);
    free(data);
    if (!c) {
        set_error("%s: %s", name, err);
        return NULL;
    }
    if (strcmp(c->name, sym_intern(name)) != 0) {
        set_error("%s: wrong name in class file (%s)", name, c->name);
        return NULL;
    }
    if (vm.opt.trace_classes)
        nj_log("[load %s]", c->name);
    table_add(c);
    if (!link_class(c)) {
        c->state = CLS_ERROR;
        return NULL;
    }
    return c;
}

/* ------------------------------------------------------------------------ */
/* Lookup                                                                   */

static Field *find_field_rec(Class *c, const char *name, const char *desc)
{
    for (; c; c = c->super) {
        for (int i = 0; i < c->n_fields; i++) {
            Field *f = &c->fields[i];
            if (f->name == name && (!desc || f->desc == desc))
                return f;
        }
        for (int i = 0; i < c->n_interfaces; i++) {
            Field *f = find_field_rec(c->interfaces[i], name, desc);
            if (f)
                return f;
        }
    }
    return NULL;
}

Field *class_find_field(Class *c, const char *name, const char *desc)
{
    return find_field_rec(c, sym_intern(name), desc ? sym_intern(desc) : NULL);
}

Method *class_find_declared(Class *c, const char *name, const char *desc)
{
    name = sym_intern(name);
    desc = sym_intern(desc);
    for (int i = 0; i < c->n_methods; i++)
        if (c->methods[i].name == name && c->methods[i].desc == desc)
            return &c->methods[i];
    return NULL;
}

static Method *find_in_interfaces(Class *c, const char *name, const char *desc)
{
    for (; c; c = c->super) {
        for (int i = 0; i < c->n_interfaces; i++) {
            Class *ic = c->interfaces[i];
            for (int j = 0; j < ic->n_methods; j++)
                if (ic->methods[j].name == name && ic->methods[j].desc == desc)
                    return &ic->methods[j];
            Method *m = find_in_interfaces(ic, name, desc);
            if (m)
                return m;
        }
    }
    return NULL;
}

Method *class_find_method(Class *c, const char *name, const char *desc)
{
    name = sym_intern(name);
    desc = sym_intern(desc);
    for (Class *k = c; k; k = k->super)
        for (int i = 0; i < k->n_methods; i++)
            if (k->methods[i].name == name && k->methods[i].desc == desc)
                return &k->methods[i];
    return find_in_interfaces(c, name, desc);
}

Method *class_find_virtual(Class *c, const char *name, const char *desc)
{
    for (int i = c->vtable_len - 1; i >= 0; i--) {
        Method *m = c->vtable[i];
        if (m->name == name && m->desc == desc)
            return m;
    }
    return NULL;
}

/* ------------------------------------------------------------------------ */
/* Subtyping                                                                */

bool class_is_subclass(const Class *c, const Class *of)
{
    for (; c; c = c->super)
        if (c == of)
            return true;
    return false;
}

static bool implements(Class *c, Class *iface)
{
    for (; c; c = c->super) {
        for (int i = 0; i < c->n_interfaces; i++) {
            Class *ic = c->interfaces[i];
            if (ic == iface || implements(ic, iface))
                return true;
        }
    }
    return false;
}

bool class_assignable(Class *from, Class *to)
{
    if (from == to || to == vm.c_Object)
        return true;
    if (from->flags & CF_ARRAY) {
        if (!(to->flags & CF_ARRAY))
            return false;
        if (!from->component || !to->component)
            return from->elem_type == to->elem_type && !from->component && !to->component;
        return class_assignable(from->component, to->component);
    }
    if (to->flags & CF_INTERFACE)
        return implements(from, to);
    if (to->flags & CF_ARRAY)
        return false;
    return class_is_subclass(from, to);
}

/* ------------------------------------------------------------------------ */
/* Mirrors                                                                  */

Object *class_mirror(Class *c)
{
    if (!c->mirror) {
        Object *m = heap_alloc_object(vm.c_Class);
        if (!m)
            return NULL;
        FIELDS(m)[vm.c_Class->inst_slots - 1].raw = (uintptr_t)c;
        c->mirror = m;
    }
    return c->mirror;
}

Class *class_from_mirror(Object *mirror)
{
    return (Class *)FIELDS(mirror)[vm.c_Class->inst_slots - 1].raw;
}
