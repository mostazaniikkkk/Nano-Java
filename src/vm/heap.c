/*
 * heap.c - the Java heap: a single arena managed with segregated free lists
 * and a non-moving mark & sweep collector.
 *
 * Object fields are scanned precisely (each class knows its reference
 * slots); thread stacks, static fields and the C stack are scanned
 * conservatively. To make conservative scanning safe, a bitmap records which
 * 8-byte granules start a live object, so an arbitrary word is only treated
 * as a reference if it points exactly at the start of an allocated object.
 */
#include "vm.h"

#include <setjmp.h>
#include <stdlib.h>

#define GRANULE     8
#define NBINS       64          /* exact-size lists for chunks up to 512 B */
#define MIN_CHUNK   ((sizeof(Object) + GRANULE - 1) & ~(size_t)(GRANULE - 1))
#define MARK_STACK  2048
#define MAX_ROOTS   32

static uint8_t  *heap_start, *heap_end;
static uint32_t *start_bits;
static Object   *bins[NBINS + 1];
static Object   *large;
static size_t    used_bytes;
static void     *c_stack_base;

static Object  **extra_roots[MAX_ROOTS];
static int       n_extra_roots;

static Object   *mark_stack[MARK_STACK];
static int       mark_sp;
static bool      mark_overflow;

static Object  **weak_list;
static int       weak_count, weak_cap;

#define CHUNK_SIZE(o)  ((o)->info & OBJ_INFO_SIZE)

/* Free chunks reuse the monitor field as their free-list link. */
static inline Object *next_free(const Object *o)       { return (Object *)(void *)o->mon; }
static inline void    set_next_free(Object *o, Object *n) { o->mon = (Monitor *)(void *)n; }

static inline size_t granule_index(const void *p)
{
    return (size_t)((const uint8_t *)p - heap_start) / GRANULE;
}
static inline void bit_set(const void *p)
{
    size_t i = granule_index(p);
    start_bits[i >> 5] |= 1u << (i & 31);
}
static inline void bit_clear(const void *p)
{
    size_t i = granule_index(p);
    start_bits[i >> 5] &= ~(1u << (i & 31));
}
static inline bool bit_test(const void *p)
{
    size_t i = granule_index(p);
    return (start_bits[i >> 5] >> (i & 31)) & 1;
}

void heap_init(size_t size)
{
    size &= ~(size_t)(GRANULE - 1);
    heap_start = malloc(size);
    if (!heap_start)
        nj_fatal("cannot allocate a %u byte heap", (unsigned)size);
    heap_end = heap_start + size;
    start_bits = nj_alloc((size / GRANULE + 31) / 32 * 4);

    Object *all = (Object *)heap_start;
    all->cls = NULL;
    all->info = (uint32_t)size;
    set_next_free(all, NULL);
    large = all;
}

void heap_shutdown(void)
{
    /* Free the monitors attached to live objects, then the arena. */
    for (uint8_t *p = heap_start; p && p < heap_end; p += CHUNK_SIZE((Object *)p)) {
        Object *o = (Object *)p;
        if (o->cls && o->mon)
            monitor_free(o->mon);
    }
    free(heap_start);
    free(start_bits);
    free(weak_list);
    heap_start = heap_end = NULL;
    start_bits = NULL;
    weak_list = NULL;
    weak_count = weak_cap = 0;
    memset(bins, 0, sizeof bins);
    large = NULL;
    used_bytes = 0;
    n_extra_roots = 0;
}

void heap_set_stack_base(void *base) { c_stack_base = base; }

void heap_add_root(Object **root)
{
    if (n_extra_roots == MAX_ROOTS)
        nj_fatal("too many heap roots");
    extra_roots[n_extra_roots++] = root;
}

size_t heap_total_bytes(void) { return (size_t)(heap_end - heap_start); }
size_t heap_free_bytes(void)  { return heap_total_bytes() - used_bytes; }

bool heap_is_object(const void *p)
{
    if ((const uint8_t *)p < heap_start || (const uint8_t *)p >= heap_end)
        return false;
    if (((uintptr_t)p & (GRANULE - 1)) != 0)
        return false;
    return bit_test(p) && ((const Object *)p)->cls != NULL;
}

/* ------------------------------------------------------------------------ */
/* Free lists                                                               */

static void free_insert(Object *o, size_t size)
{
    o->cls = NULL;
    o->info = (uint32_t)size;
    size_t g = size / GRANULE;
    if (g <= NBINS) {
        set_next_free(o, bins[g]);
        bins[g] = o;
    } else {
        set_next_free(o, large);
        large = o;
    }
}

static Object *take(size_t size)
{
    size_t g = size / GRANULE;
    if (g <= NBINS && bins[g]) {
        Object *o = bins[g];
        bins[g] = next_free(o);
        return o;
    }
    /* First fit in the large list, splitting the remainder. */
    Object *prev = NULL;
    for (Object *o = large; o; prev = o, o = next_free(o)) {
        size_t have = CHUNK_SIZE(o);
        if (have < size)
            continue;
        if (prev)
            set_next_free(prev, next_free(o));
        else
            large = next_free(o);
        size_t rest = have - size;
        if (rest >= MIN_CHUNK) {
            free_insert((Object *)((uint8_t *)o + size), rest);
        } else {
            size = have;
        }
        o->info = (uint32_t)size;
        return o;
    }
    /* Small request with an empty bin: carve from a bigger small bin. */
    for (size_t b = g + 1; b <= NBINS; b++) {
        Object *o = bins[b];
        if (!o)
            continue;
        bins[b] = next_free(o);
        size_t rest = b * GRANULE - size;
        if (rest >= MIN_CHUNK)
            free_insert((Object *)((uint8_t *)o + size), rest);
        else
            size = b * GRANULE;
        o->info = (uint32_t)size;
        return o;
    }
    return NULL;
}

static Object *alloc_chunk(size_t size)
{
    if (size < MIN_CHUNK)
        size = MIN_CHUNK;
    size = (size + GRANULE - 1) & ~(size_t)(GRANULE - 1);
    if (size > OBJ_INFO_SIZE)
        return NULL;
    Object *o = take(size);
    if (!o) {
        heap_gc();
        o = take(size);
        if (!o)
            return NULL;
    }
    size = CHUNK_SIZE(o);
    memset(o, 0, size);
    o->info = (uint32_t)size;
    bit_set(o);
    used_bytes += size;
    return o;
}

Object *heap_alloc_object(Class *c)
{
    Object *o = alloc_chunk(sizeof(Object) + (size_t)c->inst_slots * sizeof(Slot));
    if (o)
        o->cls = c;
    return o;
}

Array *heap_alloc_array(Class *c, jint len)
{
    if (len < 0)
        return NULL;
    uint64_t bytes = ARRAY_DATA_OFFSET + ((uint64_t)len << c->elem_shift);
    if (bytes > (uint64_t)heap_total_bytes())
        return NULL;
    Array *a = (Array *)alloc_chunk((size_t)bytes);
    if (a) {
        a->hdr.cls = c;
        a->length = len;
    }
    return a;
}

/* ------------------------------------------------------------------------ */
/* Mark                                                                     */

static inline bool is_marked(const Object *o) { return o->info & OBJ_INFO_MARK; }

static void mark(Object *o)
{
    if (!o || is_marked(o))
        return;
    o->info |= OBJ_INFO_MARK;
    if (mark_sp < MARK_STACK)
        mark_stack[mark_sp++] = o;
    else
        mark_overflow = true;
}

static void mark_maybe(uintptr_t w)
{
    if (heap_is_object((const void *)w))
        mark((Object *)w);
}

/* Conservative scanning reads whole stack frames, including the padding
 * AddressSanitizer poisons, so it is excluded from instrumentation. */
#if defined(__GNUC__)
#define NO_ASAN __attribute__((no_sanitize_address))
#else
#define NO_ASAN
#endif

NO_ASAN static void scan_range(const void *lo, const void *hi)
{
    uintptr_t a = ((uintptr_t)lo + sizeof(uintptr_t) - 1) & ~(uintptr_t)(sizeof(uintptr_t) - 1);
    for (const uintptr_t *p = (const uintptr_t *)a; (const void *)(p + 1) <= hi; p++)
        mark_maybe(*p);
}

static void add_weak(Object *o)
{
    if (weak_count == weak_cap) {
        weak_cap = weak_cap ? weak_cap * 2 : 32;
        weak_list = realloc(weak_list, sizeof(Object *) * weak_cap);
        if (!weak_list)
            nj_fatal("out of memory in GC");
    }
    weak_list[weak_count++] = o;
}

static void scan_children(Object *o)
{
    Class *c = o->cls;
    if (c->flags & CF_ARRAY) {
        if (c->elem_type == 'L' || c->elem_type == '[') {
            Object **e = ARRAY_DATA(o, Object *);
            for (jint i = 0, n = ARRAY_LEN(o); i < n; i++)
                mark(e[i]);
        }
        return;
    }
    Slot *f = FIELDS(o);
    if (c->flags & CF_WEAKREF) {
        uint16_t skip = vm.f_Weak_referent->offset;
        for (int i = 0; i < c->n_ref_slots; i++)
            if (c->ref_slots[i] != skip)
                mark(f[c->ref_slots[i]].ref);
        add_weak(o);
        return;
    }
    for (int i = 0; i < c->n_ref_slots; i++)
        mark(f[c->ref_slots[i]].ref);
}

static void drain(void)
{
    for (;;) {
        while (mark_sp > 0)
            scan_children(mark_stack[--mark_sp]);
        if (!mark_overflow)
            return;
        /* The mark stack overflowed: rescan marked objects to find the
         * children that were dropped. */
        mark_overflow = false;
        for (uint8_t *p = heap_start; p < heap_end; p += CHUNK_SIZE((Object *)p)) {
            Object *o = (Object *)p;
            if (o->cls && is_marked(o)) {
                scan_children(o);
                while (mark_sp > 0)
                    scan_children(mark_stack[--mark_sp]);
            }
        }
    }
}

static void mark_class(Class *c, void *ctx)
{
    (void)ctx;
    mark(c->mirror);
    if (c->statics)
        scan_range(c->statics, c->statics + c->n_static_slots);
    for (int i = 1; i < c->cp_count; i++)
        if (c->cp[i].tag == CONSTANT_String && c->cp[i].resolved)
            mark(c->cp[i].v.str);
}

static void mark_thread(Thread *t)
{
    mark(t->jthread);
    mark(t->exception);
    for (int i = 0; i < t->tail_nargs; i++)
        mark_maybe(t->tail_args[i].raw);
    for (int i = 0; i < t->depth; i++)
        mark(t->frames[i].lock);
    if (t->depth > 0) {
        Frame *f = &t->frames[t->depth - 1];
        Slot  *top = f->locals + f->m->max_locals + f->m->max_stack;
        if (top > t->stack_limit)
            top = t->stack_limit;
        scan_range(t->stack, top);
    }
}

#if defined(__GNUC__)
__attribute__((noinline))
#endif
NO_ASAN static void scan_c_stack(void)
{
    jmp_buf regs;
    setjmp(regs);
    scan_range(&regs, (const char *)&regs + sizeof regs);
    void *here = &regs;
    if (c_stack_base > here)
        scan_range(here, c_stack_base);
    else
        scan_range(c_stack_base, here);
}

/* ------------------------------------------------------------------------ */
/* Sweep                                                                    */

static void sweep(void)
{
    for (int i = 0; i <= NBINS; i++)
        bins[i] = NULL;
    large = NULL;
    used_bytes = 0;

    uint8_t *run = NULL;
    for (uint8_t *p = heap_start; p < heap_end;) {
        Object *o = (Object *)p;
        size_t  size = CHUNK_SIZE(o);
        if (o->cls && is_marked(o)) {
            o->info &= ~OBJ_INFO_MARK;
            used_bytes += size;
            if (run) {
                free_insert((Object *)run, (size_t)(p - run));
                run = NULL;
            }
        } else {
            if (o->cls) {
                if (o->mon)
                    monitor_free(o->mon);
                bit_clear(o);
            }
            if (!run)
                run = p;
        }
        p += size;
    }
    if (run)
        free_insert((Object *)run, (size_t)(heap_end - run));
}

void heap_gc(void)
{
    mark_sp = 0;
    mark_overflow = false;
    weak_count = 0;

    for (Thread *t = vm.threads; t; t = t->next)
        mark_thread(t);
    class_foreach(mark_class, NULL);
    str_mark_roots(mark);
    for (int i = 0; i < n_extra_roots; i++)
        mark(*extra_roots[i]);
    mark(vm.oom);
    scan_c_stack();
    drain();

    for (int i = 0; i < weak_count; i++) {
        Slot *ref = &FIELDS(weak_list[i])[vm.f_Weak_referent->offset];
        if (ref->ref && !is_marked(ref->ref))
            ref->ref = NULL;
    }

    sweep();
}

/* ------------------------------------------------------------------------ */
/* Throwing wrappers                                                        */

Object *vm_new_object(Thread *t, Class *c)
{
    Object *o = heap_alloc_object(c);
    if (!o)
        vm_throw(t, vm.oom);
    return o;
}

Array *vm_new_array(Thread *t, Class *c, jint len)
{
    if (len < 0) {
        vm_throw_newf(t, "java/lang/NegativeArraySizeException", "%d", (int)len);
        return NULL;
    }
    Array *a = heap_alloc_array(c, len);
    if (!a)
        vm_throw(t, vm.oom);
    return a;
}

Array *vm_new_prim_array(Thread *t, char type, jint len)
{
    return vm_new_array(t, class_prim_array(type), len);
}
