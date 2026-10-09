/*
 * interp.c - the bytecode interpreter, method invocation, class
 * initialization and exception dispatch.
 *
 * Java calls never recurse on the C stack: invoking a method pushes a Frame
 * and the loop continues with it. A caller's pc stays on its invoke
 * instruction while the callee runs (so exception ranges match), and the
 * callee's ret_skip says how far to advance it on return. Instructions that
 * have to wait (class initialization, monitor contention, blocking natives)
 * leave pc untouched and are executed again later.
 */
#include "vm.h"
#include "../pal/pal.h"

#include <math.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>

#define U2(p)  ((uint16_t)(((p)[0] << 8) | (p)[1]))
#define S2(p)  ((int16_t)U2(p))
#define S4(p)  ((int32_t)(((uint32_t)(p)[0] << 24) | ((uint32_t)(p)[1] << 16) | \
                          ((uint32_t)(p)[2] << 8) | (uint32_t)(p)[3]))

enum { INIT_READY = 1, INIT_PUSHED = 0, INIT_YIELD = 2, INIT_EXC = -1 };

/* ------------------------------------------------------------------------ */
/* Exceptions                                                               */

static Class *exception_class(const char *name)
{
    Class *c = class_load(name);
    if (!c && strcmp(name, "java/lang/Error") != 0)
        c = class_load("java/lang/Error");
    if (!c)
        nj_fatal("cannot load exception class %s", name);
    /* Exception classes are created without running Java code, so walk
     * the hierarchy and mark classes without static initializers ready. */
    for (Class *k = c; k && k->state == CLS_LINKED; k = k->super)
        if (!class_find_declared(k, "<clinit>", "()V"))
            k->state = CLS_INITIALIZED;
    return c;
}

static void fill_trace(Thread *t, Object *exc)
{
    int skip = 0;
    while (skip < t->depth) {
        Frame *f = &t->frames[t->depth - 1 - skip];
        if (f->m->name == S_init && (f->m->cls->flags & CF_THROWABLE))
            skip++;
        else
            break;
    }
    int    n = t->depth - skip;
    Array *a = heap_alloc_array(class_prim_array('J'), n * 2);
    if (!a)
        return;
    jlong *d = ARRAY_DATA(a, jlong);
    for (int i = 0; i < n; i++) {
        Frame *f = &t->frames[t->depth - 1 - skip - i];
        d[i * 2] = (jlong)(uintptr_t)f->m;
        d[i * 2 + 1] = f->m->code ? (jlong)(f->pc - f->m->code) : -1;
    }
    SET_REF(exc, vm.f_Throwable_trace, (Object *)a);
}

void vm_fill_stack_trace(Thread *t, Object *exc)
{
    fill_trace(t, exc);
}

void vm_throw(Thread *t, Object *exc)
{
    t->exception = exc;
}

void vm_throw_new(Thread *t, const char *cls, const char *msg)
{
    if (vm.opt.trace_exceptions) {
        if (t->depth > 0) {
            Frame *f = &t->frames[t->depth - 1];
            nj_log("[throw %s in %s.%s pc=%d: %s]", cls, f->m->cls->name, f->m->name,
                   f->m->code ? (int)(f->pc - f->m->code) : -1, msg ? msg : "");
        } else {
            nj_log("[throw %s: %s]", cls, msg ? msg : "");
        }
    }
    Class  *c = exception_class(cls);
    Object *e = heap_alloc_object(c);
    if (!e) {
        t->exception = vm.oom;
        return;
    }
    if (msg) {
        Object *s = str_new_utf8(t, msg);
        t->exception = NULL;   /* ignore OOM while building the message */
        if (s)
            SET_REF(e, vm.f_Throwable_message, s);
    }
    fill_trace(t, e);
    t->exception = e;
}

void vm_throw_newf(Thread *t, const char *cls, const char *fmt, ...)
{
    char    buf[256];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(buf, sizeof buf, fmt, ap);
    va_end(ap);
    vm_throw_new(t, cls, buf);
}

static int line_for(Method *m, jlong pc)
{
    int line = -1;
    for (int i = 0; i < m->n_lines; i++)
        if (m->lines[i].pc <= pc)
            line = m->lines[i].line;
    return line;
}

static void print_name(const char *internal)
{
    char buf[256];
    size_t i = 0;
    for (; internal[i] && i < sizeof buf - 1; i++)
        buf[i] = internal[i] == '/' ? '.' : internal[i];
    pal_console_write(buf, (int)i);
}

void vm_print_exception(Thread *t, Object *exc)
{
    (void)t;
    char buf[300];
    print_name(exc->cls->name);
    Object *msg = GET_REF(exc, vm.f_Throwable_message);
    if (msg) {
        pal_console_write(": ", 2);
        str_to_utf8(msg, buf, sizeof buf);
        pal_console_write(buf, (int)strlen(buf));
    }
    pal_console_write("\n", 1);

    Object *trace = GET_REF(exc, vm.f_Throwable_trace);
    if (!trace)
        return;
    jlong *d = ARRAY_DATA(trace, jlong);
    for (jint i = 0; i + 1 < ARRAY_LEN(trace); i += 2) {
        Method *m = (Method *)(uintptr_t)d[i];
        pal_console_write("\tat ", 4);
        print_name(m->cls->name);
        int n;
        if (m->access & ACC_NATIVE) {
            n = snprintf(buf, sizeof buf, ".%s(Native Method)\n", m->name);
        } else {
            int line = line_for(m, d[i + 1]);
            const char *src = m->cls->source_file ? m->cls->source_file : "Unknown Source";
            if (line >= 0)
                n = snprintf(buf, sizeof buf, ".%s(%s:%d)\n", m->name, src, line);
            else
                n = snprintf(buf, sizeof buf, ".%s(%s)\n", m->name, src);
        }
        pal_console_write(buf, n < (int)sizeof buf ? n : (int)sizeof buf - 1);
    }
}

bool vm_instanceof(Object *o, Class *c)
{
    return o && class_assignable(o->cls, c);
}

/* ------------------------------------------------------------------------ */
/* Resolution                                                               */

static const char *cp_utf8(Class *c, uint16_t i) { return c->cp[i].v.utf8; }

static Class *resolve_class(Thread *t, Class *cur, uint16_t idx)
{
    CPEntry *e = &cur->cp[idx];
    if (e->resolved)
        return e->v.cls;
    Class *c = class_load(cp_utf8(cur, e->v.ref.a));
    if (!c) {
        vm_throw_new(t, "java/lang/NoClassDefFoundError", class_last_error());
        return NULL;
    }
    e->v.cls = c;
    e->resolved = 1;
    return c;
}

static void member_names(Class *cur, CPEntry *e, const char **name, const char **desc)
{
    CPEntry *nat = &cur->cp[e->v.ref.b];
    *name = cp_utf8(cur, nat->v.ref.a);
    *desc = cp_utf8(cur, nat->v.ref.b);
}

static Field *resolve_field(Thread *t, Class *cur, uint16_t idx)
{
    CPEntry *e = &cur->cp[idx];
    if (e->resolved)
        return e->v.field;
    Class *c = resolve_class(t, cur, e->v.ref.a);
    if (!c)
        return NULL;
    const char *name, *desc;
    member_names(cur, e, &name, &desc);
    Field *f = class_find_field(c, name, desc);
    if (!f) {
        vm_throw_newf(t, "java/lang/NoSuchFieldError", "%s.%s", c->name, name);
        return NULL;
    }
    e->v.field = f;
    e->resolved = 1;
    return f;
}

static Method *resolve_method(Thread *t, Class *cur, uint16_t idx)
{
    CPEntry *e = &cur->cp[idx];
    if (e->resolved)
        return e->v.method;
    Class *c = resolve_class(t, cur, e->v.ref.a);
    if (!c)
        return NULL;
    const char *name, *desc;
    member_names(cur, e, &name, &desc);
    Method *m = class_find_method(c, name, desc);
    if (!m) {
        vm_throw_newf(t, "java/lang/NoSuchMethodError", "%s.%s%s", c->name, name, desc);
        return NULL;
    }
    e->v.method = m;
    e->resolved = 1;
    return m;
}

static Object *resolve_string(Thread *t, Class *cur, uint16_t idx)
{
    CPEntry *e = &cur->cp[idx];
    if (e->resolved)
        return e->v.str;
    Object *s = str_literal(t, cp_utf8(cur, e->v.ref.a));
    if (!s)
        return NULL;
    e->v.str = s;
    e->resolved = 1;
    return s;
}

/* ------------------------------------------------------------------------ */
/* Frames and class initialization                                          */

bool interp_push_frame(Thread *t, Method *m, Slot *args)
{
    if (t->depth >= t->max_frames ||
        args + m->max_locals + m->max_stack > t->stack_limit) {
        vm_throw_new(t, "java/lang/StackOverflowError", NULL);
        return false;
    }
    Frame *f = &t->frames[t->depth++];
    f->m = m;
    f->pc = m->code;
    f->locals = args;
    f->sp = args + m->max_locals;
    f->lock = NULL;
    f->ret_skip = 0;
    return true;
}

static bool set_constant_values(Thread *t, Class *c)
{
    for (int i = 0; i < c->n_fields; i++) {
        Field *f = &c->fields[i];
        if (!(f->access & ACC_STATIC) || !f->const_index)
            continue;
        CPEntry *e = &c->cp[f->const_index];
        Slot    *s = &c->statics[f->offset];
        switch (e->tag) {
        case CONSTANT_Integer:
        case CONSTANT_Float:
            s->i = e->v.i;
            break;
        case CONSTANT_Long:
        case CONSTANT_Double:
            s[0].i = (jint)c->cp[f->const_index + 1].v.u;
            s[1].i = (jint)e->v.u;
            break;
        case CONSTANT_String:
            s->ref = resolve_string(t, c, f->const_index);
            if (!s->ref)
                return false;
            break;
        }
    }
    return true;
}

/* Initializes c if needed. Callers must have saved their frame first: a
 * <clinit> frame may be pushed on top of it. */
int interp_ensure_init(Thread *t, Class *c)
{
    switch (c->state) {
    case CLS_INITIALIZED:
        return INIT_READY;
    case CLS_ERROR:
        vm_throw_new(t, "java/lang/NoClassDefFoundError", c->name);
        return INIT_EXC;
    case CLS_INITIALIZING:
        return c->init_thread == t ? INIT_READY : INIT_YIELD;
    default:
        break;
    }
    if (c->super && c->super->state != CLS_INITIALIZED) {
        int r = interp_ensure_init(t, c->super);
        if (r != INIT_READY)
            return r;
    }
    c->state = CLS_INITIALIZING;
    c->init_thread = t;
    if (!set_constant_values(t, c)) {
        c->state = CLS_ERROR;
        return INIT_EXC;
    }
    Method *clinit = class_find_declared(c, "<clinit>", "()V");
    if (!clinit) {
        c->state = CLS_INITIALIZED;
        c->init_thread = NULL;
        return INIT_READY;
    }
    Slot *top = t->depth ? t->frames[t->depth - 1].sp : t->stack;
    if (!interp_push_frame(t, clinit, top)) {
        c->state = CLS_ERROR;
        return INIT_EXC;
    }
    return INIT_PUSHED;
}

static void finish_clinit(Method *m, bool ok)
{
    m->cls->state = ok ? CLS_INITIALIZED : CLS_ERROR;
    m->cls->init_thread = NULL;
}

/* ------------------------------------------------------------------------ */
/* Arithmetic helpers with Java semantics                                   */

static jint f2i(jfloat f)
{
    if (f != f) return 0;
    if (f >= 2147483647.0f) return INT32_MAX;
    if (f <= -2147483648.0f) return INT32_MIN;
    return (jint)f;
}
static jlong f2l(jfloat f)
{
    if (f != f) return 0;
    if (f >= 9223372036854775807.0f) return INT64_MAX;
    if (f <= -9223372036854775808.0f) return INT64_MIN;
    return (jlong)f;
}
static jint d2i(jdouble d)
{
    if (d != d) return 0;
    if (d >= 2147483647.0) return INT32_MAX;
    if (d <= -2147483648.0) return INT32_MIN;
    return (jint)d;
}
static jlong d2l(jdouble d)
{
    if (d != d) return 0;
    if (d >= 9223372036854775807.0) return INT64_MAX;
    if (d <= -9223372036854775808.0) return INT64_MIN;
    return (jlong)d;
}

static Array *multi_array(Thread *t, Class *c, int dims, const jint *counts)
{
    Array *a = vm_new_array(t, c, counts[0]);
    if (!a || dims == 1)
        return a;
    Object **e = ARRAY_DATA(a, Object *);
    for (jint i = 0; i < counts[0]; i++) {
        e[i] = (Object *)multi_array(t, c->component, dims - 1, counts + 1);
        if (!e[i])
            return NULL;
    }
    return a;
}

/* ------------------------------------------------------------------------ */
/* The interpreter loop                                                     */

#ifdef NJ_OPSTATS
uint64_t nj_opstats[256];
#endif

/* Inline cache for interface calls, keyed by call site. */
static struct { const uint8_t *pc; Class *cls; Method *m; } icache[256];

void interp_reset(void)
{
    memset(icache, 0, sizeof icache);
}

static Method *icache_lookup(const uint8_t *pc, Class *cls, Method *im)
{
    unsigned h = (unsigned)(((uintptr_t)pc >> 1) & 255);
    if (icache[h].pc == pc && icache[h].cls == cls)
        return icache[h].m;
    Method *m = class_find_virtual(cls, im->name, im->desc);
    if (!m)
        m = class_find_method(cls, im->name, im->desc);
    icache[h].pc = pc;
    icache[h].cls = cls;
    icache[h].m = m;
    return m;
}

NJ_HOT void interp_run(Thread *t, int budget)
{
    Frame         *f;
    Method        *m;
    const uint8_t *pc;
    Slot          *sp, *lv;
    CPEntry       *cp;
    Class         *cls;

#define LOAD_FRAME() do { f = &t->frames[t->depth - 1]; m = f->m; pc = f->pc; \
        sp = f->sp; lv = f->locals; cls = m->cls; cp = cls->cp; } while (0)
#define SAVE_FRAME() do { f->pc = pc; f->sp = sp; } while (0)

#define PUSH_I(v)   ((sp++)->i = (v))
#define PUSH_F(v)   ((sp++)->f = (v))
#define PUSH_R(v)   ((sp++)->ref = (v))
#define PUSH_L(v)   do { slot_set_long(sp, (v)); sp += 2; } while (0)
#define PUSH_D(v)   do { slot_set_double(sp, (v)); sp += 2; } while (0)
#define POP_I()     ((--sp)->i)
#define POP_F()     ((--sp)->f)
#define POP_R()     ((--sp)->ref)
#define POP_L()     (sp -= 2, slot_get_long(sp))
#define POP_D()     (sp -= 2, slot_get_double(sp))

#define THROW(cls_, msg_) do { SAVE_FRAME(); vm_throw_new(t, cls_, msg_); goto exception; } while (0)
#define CHECK_EXC()       do { if (t->exception) goto exception; } while (0)
#define NULL_CHECK(o)     do { if (!(o)) THROW("java/lang/NullPointerException", NULL); } while (0)
/* Time slices are counted in backward branches and calls, which bounds
 * every loop and recursion without a check on each instruction. */
#define BRANCH(off)       { int off_ = (off); pc += off_;         if (off_ <= 0 && --budget < 0) { SAVE_FRAME(); return; }         goto dispatch; }

#define ARRAY_CHECK(a, idx) do { \
        NULL_CHECK(a); \
        if ((uint32_t)(idx) >= (uint32_t)ARRAY_LEN(a)) { \
            SAVE_FRAME(); \
            vm_throw_newf(t, "java/lang/ArrayIndexOutOfBoundsException", "%d", (int)(idx)); \
            goto exception; \
        } } while (0)

#define ALOAD(T, PUSH) do { jint i_ = POP_I(); Object *a_ = POP_R(); \
        ARRAY_CHECK(a_, i_); PUSH(ARRAY_DATA(a_, T)[i_]); pc++; } while (0)
#define ASTORE(T, POP) do { T v_ = (T)POP(); jint i_ = POP_I(); Object *a_ = POP_R(); \
        ARRAY_CHECK(a_, i_); ARRAY_DATA(a_, T)[i_] = v_; pc++; } while (0)

/* Resolved constant pool entries are read inline; only the first execution
 * of an instruction calls the resolver. */
#define RESOLVE(var, member, fn) do {         CPEntry *e_ = &cp[U2(pc + 1)];         if (e_->resolved) {             var = e_->v.member;         } else {             SAVE_FRAME();             var = fn(t, cls, U2(pc + 1));             if (!var) goto exception;         } } while (0)

/* Re-executes the current instruction after a class initializer. */
#define ENSURE_INIT(c) do { \
        if ((c)->state != CLS_INITIALIZED) { \
            SAVE_FRAME(); \
            int r_ = interp_ensure_init(t, (c)); \
            if (r_ == INIT_EXC) goto exception; \
            if (r_ == INIT_PUSHED) { LOAD_FRAME(); goto dispatch; } \
            if (r_ == INIT_YIELD) return; \
        } } while (0)

    if (t->depth == 0)
        return;
    LOAD_FRAME();
    if (t->exception)
        goto exception;

    for (;;) {
    dispatch:
#ifdef NJ_OPSTATS
        nj_opstats[*pc]++;
#endif
        switch (*pc) {
        case 0x00: /* nop */ pc++; break;
        case 0x01: PUSH_R(NULL); pc++; break;
        case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07: case 0x08:
            PUSH_I(*pc - 0x03); pc++; break;                        /* iconst_<n> */
        case 0x09: case 0x0a: PUSH_L(*pc - 0x09); pc++; break;     /* lconst */
        case 0x0b: case 0x0c: case 0x0d: PUSH_F((jfloat)(*pc - 0x0b)); pc++; break;
        case 0x0e: case 0x0f: PUSH_D((jdouble)(*pc - 0x0e)); pc++; break;
        case 0x10: PUSH_I((jbyte)pc[1]); pc += 2; break;            /* bipush */
        case 0x11: PUSH_I(S2(pc + 1)); pc += 3; break;              /* sipush */

        case 0x12: case 0x13: {                                     /* ldc, ldc_w */
            uint16_t idx = *pc == 0x12 ? pc[1] : U2(pc + 1);
            CPEntry *e = &cp[idx];
            if (e->tag == CONSTANT_String) {
                SAVE_FRAME();
                Object *s = resolve_string(t, cls, idx);
                if (!s) goto exception;
                PUSH_R(s);
            } else if (e->tag == CONSTANT_Class) {
                SAVE_FRAME();
                Class *c = resolve_class(t, cls, idx);
                if (!c) goto exception;
                Object *mir = class_mirror(c);
                if (!mir) { vm_throw(t, vm.oom); goto exception; }
                PUSH_R(mir);
            } else {
                PUSH_I(e->v.i);
            }
            pc += *pc == 0x12 ? 2 : 3;
            break;
        }
        case 0x14: {                                                /* ldc2_w */
            uint16_t idx = U2(pc + 1);
            sp[0].i = (jint)cp[idx + 1].v.u;
            sp[1].i = (jint)cp[idx].v.u;
            sp += 2;
            pc += 3;
            break;
        }

        /* Loads */
        case 0x15: case 0x17: case 0x19: *sp++ = lv[pc[1]]; pc += 2; break;
        case 0x16: case 0x18: sp[0] = lv[pc[1]]; sp[1] = lv[pc[1] + 1]; sp += 2; pc += 2; break;
        case 0x1a: case 0x1b: case 0x1c: case 0x1d: *sp++ = lv[*pc - 0x1a]; pc++; break;
        case 0x1e: case 0x1f: case 0x20: case 0x21: {
            int n = *pc - 0x1e; sp[0] = lv[n]; sp[1] = lv[n + 1]; sp += 2; pc++; break;
        }
        case 0x22: case 0x23: case 0x24: case 0x25: *sp++ = lv[*pc - 0x22]; pc++; break;
        case 0x26: case 0x27: case 0x28: case 0x29: {
            int n = *pc - 0x26; sp[0] = lv[n]; sp[1] = lv[n + 1]; sp += 2; pc++; break;
        }
        case 0x2a: case 0x2b: case 0x2c: case 0x2d: *sp++ = lv[*pc - 0x2a]; pc++; break;

        case 0x2e: ALOAD(jint, PUSH_I); break;                      /* iaload */
        case 0x2f: ALOAD(jlong, PUSH_L); break;                     /* laload */
        case 0x30: ALOAD(jfloat, PUSH_F); break;                    /* faload */
        case 0x31: ALOAD(jdouble, PUSH_D); break;                   /* daload */
        case 0x32: ALOAD(Object *, PUSH_R); break;                  /* aaload */
        case 0x33: ALOAD(jbyte, PUSH_I); break;                     /* baload */
        case 0x34: ALOAD(jchar, PUSH_I); break;                     /* caload */
        case 0x35: ALOAD(jshort, PUSH_I); break;                    /* saload */

        /* Stores */
        case 0x36: case 0x38: case 0x3a: lv[pc[1]] = *--sp; pc += 2; break;
        case 0x37: case 0x39: sp -= 2; lv[pc[1]] = sp[0]; lv[pc[1] + 1] = sp[1]; pc += 2; break;
        case 0x3b: case 0x3c: case 0x3d: case 0x3e: lv[*pc - 0x3b] = *--sp; pc++; break;
        case 0x3f: case 0x40: case 0x41: case 0x42: {
            int n = *pc - 0x3f; sp -= 2; lv[n] = sp[0]; lv[n + 1] = sp[1]; pc++; break;
        }
        case 0x43: case 0x44: case 0x45: case 0x46: lv[*pc - 0x43] = *--sp; pc++; break;
        case 0x47: case 0x48: case 0x49: case 0x4a: {
            int n = *pc - 0x47; sp -= 2; lv[n] = sp[0]; lv[n + 1] = sp[1]; pc++; break;
        }
        case 0x4b: case 0x4c: case 0x4d: case 0x4e: lv[*pc - 0x4b] = *--sp; pc++; break;

        case 0x4f: ASTORE(jint, POP_I); break;                      /* iastore */
        case 0x50: ASTORE(jlong, POP_L); break;                     /* lastore */
        case 0x51: ASTORE(jfloat, POP_F); break;                    /* fastore */
        case 0x52: ASTORE(jdouble, POP_D); break;                   /* dastore */
        case 0x53: {                                                /* aastore */
            Object *v = POP_R();
            jint    i = POP_I();
            Object *a = POP_R();
            ARRAY_CHECK(a, i);
            if (v && !class_assignable(v->cls, a->cls->component)) {
                SAVE_FRAME();
                vm_throw_new(t, "java/lang/ArrayStoreException", v->cls->name);
                goto exception;
            }
            ARRAY_DATA(a, Object *)[i] = v;
            pc++;
            break;
        }
        case 0x54: {                                                /* bastore */
            jint v = POP_I(); jint i = POP_I(); Object *a = POP_R();
            ARRAY_CHECK(a, i);
            ARRAY_DATA(a, jbyte)[i] = (jbyte)(a->cls->elem_type == 'Z' ? (v & 1) : v);
            pc++;
            break;
        }
        case 0x55: ASTORE(jchar, POP_I); break;                     /* castore */
        case 0x56: ASTORE(jshort, POP_I); break;                    /* sastore */

        /* Stack */
        case 0x57: sp--; pc++; break;                               /* pop */
        case 0x58: sp -= 2; pc++; break;                            /* pop2 */
        case 0x59: sp[0] = sp[-1]; sp++; pc++; break;               /* dup */
        case 0x5a: {                                                /* dup_x1 */
            Slot a = sp[-1], b = sp[-2];
            sp[-2] = a; sp[-1] = b; sp[0] = a; sp++; pc++; break;
        }
        case 0x5b: {                                                /* dup_x2 */
            Slot a = sp[-1], b = sp[-2], c = sp[-3];
            sp[-3] = a; sp[-2] = c; sp[-1] = b; sp[0] = a; sp++; pc++; break;
        }
        case 0x5c: sp[0] = sp[-2]; sp[1] = sp[-1]; sp += 2; pc++; break;   /* dup2 */
        case 0x5d: {                                                /* dup2_x1 */
            Slot a = sp[-1], b = sp[-2], c = sp[-3];
            sp[-3] = b; sp[-2] = a; sp[-1] = c; sp[0] = b; sp[1] = a; sp += 2; pc++; break;
        }
        case 0x5e: {                                                /* dup2_x2 */
            Slot a = sp[-1], b = sp[-2], c = sp[-3], d = sp[-4];
            sp[-4] = b; sp[-3] = a; sp[-2] = d; sp[-1] = c; sp[0] = b; sp[1] = a;
            sp += 2; pc++; break;
        }
        case 0x5f: { Slot a = sp[-1]; sp[-1] = sp[-2]; sp[-2] = a; pc++; break; }  /* swap */

        /* Integer arithmetic */
        case 0x60: { jint b = POP_I(); sp[-1].i = (jint)((uint32_t)sp[-1].i + (uint32_t)b); pc++; break; }
        case 0x64: { jint b = POP_I(); sp[-1].i = (jint)((uint32_t)sp[-1].i - (uint32_t)b); pc++; break; }
        case 0x68: { jint b = POP_I(); sp[-1].i = (jint)((uint32_t)sp[-1].i * (uint32_t)b); pc++; break; }
        case 0x6c: {                                                /* idiv */
            jint b = POP_I();
            if (b == 0) { sp++; THROW("java/lang/ArithmeticException", "/ by zero"); }
            jint a = sp[-1].i;
            sp[-1].i = (a == INT32_MIN && b == -1) ? a : a / b;
            pc++;
            break;
        }
        case 0x70: {                                                /* irem */
            jint b = POP_I();
            if (b == 0) { sp++; THROW("java/lang/ArithmeticException", "/ by zero"); }
            sp[-1].i = b == -1 ? 0 : sp[-1].i % b;
            pc++;
            break;
        }
        case 0x74: sp[-1].i = (jint)(0u - (uint32_t)sp[-1].i); pc++; break;
        case 0x78: { jint b = POP_I(); sp[-1].i = (jint)((uint32_t)sp[-1].i << (b & 31)); pc++; break; }
        case 0x7a: { jint b = POP_I(); sp[-1].i = sp[-1].i >> (b & 31); pc++; break; }
        case 0x7c: { jint b = POP_I(); sp[-1].i = (jint)((uint32_t)sp[-1].i >> (b & 31)); pc++; break; }
        case 0x7e: { jint b = POP_I(); sp[-1].i &= b; pc++; break; }
        case 0x80: { jint b = POP_I(); sp[-1].i |= b; pc++; break; }
        case 0x82: { jint b = POP_I(); sp[-1].i ^= b; pc++; break; }

        /* Long arithmetic */
        case 0x61: { jlong b = POP_L(), a = POP_L(); PUSH_L((jlong)((uint64_t)a + (uint64_t)b)); pc++; break; }
        case 0x65: { jlong b = POP_L(), a = POP_L(); PUSH_L((jlong)((uint64_t)a - (uint64_t)b)); pc++; break; }
        case 0x69: { jlong b = POP_L(), a = POP_L(); PUSH_L((jlong)((uint64_t)a * (uint64_t)b)); pc++; break; }
        case 0x6d: {                                                /* ldiv */
            jlong b = POP_L(), a = POP_L();
            if (b == 0) { sp += 4; THROW("java/lang/ArithmeticException", "/ by zero"); }
            PUSH_L((a == INT64_MIN && b == -1) ? a : a / b);
            pc++;
            break;
        }
        case 0x71: {                                                /* lrem */
            jlong b = POP_L(), a = POP_L();
            if (b == 0) { sp += 4; THROW("java/lang/ArithmeticException", "/ by zero"); }
            PUSH_L(b == -1 ? 0 : a % b);
            pc++;
            break;
        }
        case 0x75: { jlong a = POP_L(); PUSH_L((jlong)(0u - (uint64_t)a)); pc++; break; }
        case 0x79: { jint s = POP_I(); jlong a = POP_L(); PUSH_L((jlong)((uint64_t)a << (s & 63))); pc++; break; }
        case 0x7b: { jint s = POP_I(); jlong a = POP_L(); PUSH_L(a >> (s & 63)); pc++; break; }
        case 0x7d: { jint s = POP_I(); jlong a = POP_L(); PUSH_L((jlong)((uint64_t)a >> (s & 63))); pc++; break; }
        case 0x7f: { jlong b = POP_L(), a = POP_L(); PUSH_L(a & b); pc++; break; }
        case 0x81: { jlong b = POP_L(), a = POP_L(); PUSH_L(a | b); pc++; break; }
        case 0x83: { jlong b = POP_L(), a = POP_L(); PUSH_L(a ^ b); pc++; break; }

        /* Float / double arithmetic */
        case 0x62: { jfloat b = POP_F(); sp[-1].f += b; pc++; break; }
        case 0x66: { jfloat b = POP_F(); sp[-1].f -= b; pc++; break; }
        case 0x6a: { jfloat b = POP_F(); sp[-1].f *= b; pc++; break; }
        case 0x6e: { jfloat b = POP_F(); sp[-1].f /= b; pc++; break; }
        case 0x72: { jfloat b = POP_F(); sp[-1].f = fmodf(sp[-1].f, b); pc++; break; }
        case 0x76: sp[-1].f = -sp[-1].f; pc++; break;
        case 0x63: { jdouble b = POP_D(), a = POP_D(); PUSH_D(a + b); pc++; break; }
        case 0x67: { jdouble b = POP_D(), a = POP_D(); PUSH_D(a - b); pc++; break; }
        case 0x6b: { jdouble b = POP_D(), a = POP_D(); PUSH_D(a * b); pc++; break; }
        case 0x6f: { jdouble b = POP_D(), a = POP_D(); PUSH_D(a / b); pc++; break; }
        case 0x73: { jdouble b = POP_D(), a = POP_D(); PUSH_D(fmod(a, b)); pc++; break; }
        case 0x77: { jdouble a = POP_D(); PUSH_D(-a); pc++; break; }

        case 0x84: {                                                /* iinc */
            lv[pc[1]].i = (jint)((uint32_t)lv[pc[1]].i + (uint32_t)(jint)(jbyte)pc[2]);
            pc += 3;
            break;
        }

        /* Conversions */
        case 0x85: { jint a = POP_I(); PUSH_L(a); pc++; break; }               /* i2l */
        case 0x86: sp[-1].f = (jfloat)sp[-1].i; pc++; break;                   /* i2f */
        case 0x87: { jint a = POP_I(); PUSH_D(a); pc++; break; }               /* i2d */
        case 0x88: { jlong a = POP_L(); PUSH_I((jint)a); pc++; break; }        /* l2i */
        case 0x89: { jlong a = POP_L(); PUSH_F((jfloat)a); pc++; break; }      /* l2f */
        case 0x8a: { jlong a = POP_L(); PUSH_D((jdouble)a); pc++; break; }     /* l2d */
        case 0x8b: sp[-1].i = f2i(sp[-1].f); pc++; break;                      /* f2i */
        case 0x8c: { jfloat a = POP_F(); PUSH_L(f2l(a)); pc++; break; }        /* f2l */
        case 0x8d: { jfloat a = POP_F(); PUSH_D(a); pc++; break; }             /* f2d */
        case 0x8e: { jdouble a = POP_D(); PUSH_I(d2i(a)); pc++; break; }       /* d2i */
        case 0x8f: { jdouble a = POP_D(); PUSH_L(d2l(a)); pc++; break; }       /* d2l */
        case 0x90: { jdouble a = POP_D(); PUSH_F((jfloat)a); pc++; break; }    /* d2f */
        case 0x91: sp[-1].i = (jbyte)sp[-1].i; pc++; break;                    /* i2b */
        case 0x92: sp[-1].i = (jchar)sp[-1].i; pc++; break;                    /* i2c */
        case 0x93: sp[-1].i = (jshort)sp[-1].i; pc++; break;                   /* i2s */

        /* Comparisons */
        case 0x94: { jlong b = POP_L(), a = POP_L(); PUSH_I(a < b ? -1 : a > b); pc++; break; }
        case 0x95: case 0x96: {                                     /* fcmpl, fcmpg */
            jfloat b = POP_F(), a = POP_F();
            PUSH_I(a < b ? -1 : a > b ? 1 : a == b ? 0 : (*pc == 0x95 ? -1 : 1));
            pc++;
            break;
        }
        case 0x97: case 0x98: {                                     /* dcmpl, dcmpg */
            jdouble b = POP_D(), a = POP_D();
            PUSH_I(a < b ? -1 : a > b ? 1 : a == b ? 0 : (*pc == 0x97 ? -1 : 1));
            pc++;
            break;
        }

        /* Branches */
        case 0x99: if (POP_I() == 0) BRANCH(S2(pc + 1)); pc += 3; break;
        case 0x9a: if (POP_I() != 0) BRANCH(S2(pc + 1)); pc += 3; break;
        case 0x9b: if (POP_I() < 0)  BRANCH(S2(pc + 1)); pc += 3; break;
        case 0x9c: if (POP_I() >= 0) BRANCH(S2(pc + 1)); pc += 3; break;
        case 0x9d: if (POP_I() > 0)  BRANCH(S2(pc + 1)); pc += 3; break;
        case 0x9e: if (POP_I() <= 0) BRANCH(S2(pc + 1)); pc += 3; break;
        case 0x9f: { jint b = POP_I(), a = POP_I(); if (a == b) BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa0: { jint b = POP_I(), a = POP_I(); if (a != b) BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa1: { jint b = POP_I(), a = POP_I(); if (a < b)  BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa2: { jint b = POP_I(), a = POP_I(); if (a >= b) BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa3: { jint b = POP_I(), a = POP_I(); if (a > b)  BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa4: { jint b = POP_I(), a = POP_I(); if (a <= b) BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa5: { Object *b = POP_R(), *a = POP_R(); if (a == b) BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa6: { Object *b = POP_R(), *a = POP_R(); if (a != b) BRANCH(S2(pc + 1)); pc += 3; break; }
        case 0xa7: BRANCH(S2(pc + 1));                              /* goto */
        case 0xa8:                                                  /* jsr */
            PUSH_I((jint)(pc + 3 - m->code));
            BRANCH(S2(pc + 1));
        case 0xa9: pc = m->code + lv[pc[1]].i; break;               /* ret */
        case 0xc6: if (POP_R() == NULL) BRANCH(S2(pc + 1)); pc += 3; break;   /* ifnull */
        case 0xc7: if (POP_R() != NULL) BRANCH(S2(pc + 1)); pc += 3; break;   /* ifnonnull */
        case 0xc8: BRANCH(S4(pc + 1));                              /* goto_w */
        case 0xc9:                                                  /* jsr_w */
            PUSH_I((jint)(pc + 5 - m->code));
            BRANCH(S4(pc + 1));

        case 0xaa: {                                                /* tableswitch */
            const uint8_t *p = m->code + ((pc + 1 - m->code + 3) & ~3);
            jint def = S4(p), low = S4(p + 4), high = S4(p + 8);
            jint idx = POP_I();
            if (idx < low || idx > high)
                BRANCH(def);
            BRANCH(S4(p + 12 + (uint32_t)(idx - low) * 4));
        }
        case 0xab: {                                                /* lookupswitch */
            const uint8_t *p = m->code + ((pc + 1 - m->code + 3) & ~3);
            jint def = S4(p), n = S4(p + 4);
            jint key = POP_I();
            jint lo = 0, hi = n - 1, target = def;
            while (lo <= hi) {
                jint mid = (lo + hi) >> 1;
                jint k = S4(p + 8 + mid * 8);
                if (k == key) {
                    target = S4(p + 12 + mid * 8);
                    break;
                }
                if (k < key) lo = mid + 1; else hi = mid - 1;
            }
            BRANCH(target);
        }

        /* Returns */
        case 0xac: case 0xad: case 0xae: case 0xaf: case 0xb0: case 0xb1: {
            int n = m->ret_slots;
            if (f->lock && !monitor_exit(t, f->lock))
                THROW("java/lang/IllegalMonitorStateException", NULL);
            if (m->name == S_clinit)
                finish_clinit(m, true);
            Slot *dst = f->locals;
            for (int i = 0; i < n; i++)
                dst[i] = sp[i - n];
            int skip = f->ret_skip;
            if (--t->depth == 0) {
                t->state = TS_TERMINATED;
                return;
            }
            LOAD_FRAME();
            sp = dst + n;
            pc += skip;
            break;
        }

        /* Fields */
        case 0xb2: case 0xb3: {                                     /* get/putstatic */
            Field *fl;
            RESOLVE(fl, field, resolve_field);
            ENSURE_INIT(fl->cls);
            Slot *s = &fl->cls->statics[fl->offset];
            bool wide = fl->desc[0] == 'J' || fl->desc[0] == 'D';
            if (*pc == 0xb2) {
                *sp++ = s[0];
                if (wide) *sp++ = s[1];
            } else {
                if (wide) { sp -= 2; s[0] = sp[0]; s[1] = sp[1]; }
                else s[0] = *--sp;
            }
            pc += 3;
            break;
        }
        case 0xb4: {                                                /* getfield */
            Field *fl;
            RESOLVE(fl, field, resolve_field);
            Object *o = sp[-1].ref;
            NULL_CHECK(o);
            Slot *s = &FIELDS(o)[fl->offset];
            if (fl->desc[0] == 'J' || fl->desc[0] == 'D') {
                sp[-1] = s[0];
                *sp++ = s[1];
            } else {
                sp[-1] = s[0];
            }
            pc += 3;
            break;
        }
        case 0xb5: {                                                /* putfield */
            Field *fl;
            RESOLVE(fl, field, resolve_field);
            if (fl->desc[0] == 'J' || fl->desc[0] == 'D') {
                Object *o = sp[-3].ref;
                NULL_CHECK(o);
                Slot *s = &FIELDS(o)[fl->offset];
                s[0] = sp[-2];
                s[1] = sp[-1];
                sp -= 3;
            } else {
                Object *o = sp[-2].ref;
                NULL_CHECK(o);
                FIELDS(o)[fl->offset] = sp[-1];
                sp -= 2;
            }
            pc += 3;
            break;
        }

        /* Invocation */
        case 0xb6: case 0xb7: case 0xb8: case 0xb9: {
            uint8_t op = *pc;
            int     skip = op == 0xb9 ? 5 : 3;
            Method *rm;
            RESOLVE(rm, method, resolve_method);
            SAVE_FRAME();
            Method *tm = rm;
            Slot   *args = sp - rm->arg_slots;

            if (op == 0xb8) {                                       /* invokestatic */
                ENSURE_INIT(rm->cls);
            } else {
                Object *recv = args[0].ref;
                NULL_CHECK(recv);
                if (op == 0xb6) {                                   /* invokevirtual */
                    if (rm->cls->flags & CF_INTERFACE)
                        tm = icache_lookup(pc, recv->cls, rm);
                    else if (!(rm->access & ACC_PRIVATE) && rm->vtable_index < recv->cls->vtable_len)
                        tm = recv->cls->vtable[rm->vtable_index];
                } else if (op == 0xb9) {                            /* invokeinterface */
                    tm = icache_lookup(pc, recv->cls, rm);
                } else {                                            /* invokespecial */
                    if (rm->name != S_init && !(rm->access & ACC_PRIVATE) &&
                        (cls->access & ACC_SUPER) && cls->super &&
                        rm->cls != cls && class_is_subclass(cls->super, rm->cls))
                        tm = class_find_method(cls->super, rm->name, rm->desc);
                }
                if (!tm) {
                    SAVE_FRAME();
                    vm_throw_newf(t, "java/lang/IncompatibleClassChangeError", "%s.%s%s",
                                  recv->cls->name, rm->name, rm->desc);
                    goto exception;
                }
            }

        invoke:
            if (tm->access & ACC_NATIVE) {
                if (!tm->native) {
                    SAVE_FRAME();
                    vm_throw_newf(t, "java/lang/UnsatisfiedLinkError", "%s.%s%s",
                                  tm->cls->name, tm->name, tm->desc);
                    goto exception;
                }
                Slot ret[2];
                ret[0].raw = ret[1].raw = 0;
                int depth0 = t->depth;
                int r = tm->native(t, args, ret);
                if (t->exception) {
                    t->native_phase = 0;
                    if (t->depth != depth0)
                        LOAD_FRAME();
                    goto exception;
                }
                if (r == NATIVE_OK) {
                    sp = args;
                    for (int i = 0; i < tm->ret_slots; i++)
                        *sp++ = ret[i];
                    pc += skip;
                    break;
                }
                if (r == NATIVE_BLOCK) {
                    if (t->depth != depth0) {
                        LOAD_FRAME();
                        break;
                    }
                    return;
                }
                /* NATIVE_INVOKE: push the result, then call tail_method with
                 * tail_args; when it returns the invoke completes. */
                sp = args;
                for (int i = 0; i < tm->ret_slots; i++)
                    *sp++ = ret[i];
                for (int i = 0; i < t->tail_nargs; i++)
                    *sp++ = t->tail_args[i];
                tm = t->tail_method;
                t->tail_nargs = 0;
                args = sp - tm->arg_slots;
                SAVE_FRAME();
                /* The native has initialized tail_method's class; only
                 * bytecode methods may be tail-invoked. */
                if (tm->access & ACC_NATIVE) {
                    vm_throw_new(t, "java/lang/InternalError", "native tail call");
                    goto exception;
                }
                goto invoke;
            }

            if (!tm->code) {
                SAVE_FRAME();
                vm_throw_newf(t, "java/lang/AbstractMethodError", "%s.%s", tm->cls->name, tm->name);
                goto exception;
            }
            Object *lock = NULL;
            if (tm->access & ACC_SYNCHRONIZED) {
                lock = (tm->access & ACC_STATIC) ? class_mirror(tm->cls) : args[0].ref;
                if (!lock) { vm_throw(t, vm.oom); goto exception; }
                if (!monitor_enter(t, lock))
                    return;   /* blocked; the invoke runs again later */
            }
            f->sp = args;
            if (!interp_push_frame(t, tm, args)) {
                f->sp = sp;
                if (lock)
                    monitor_exit(t, lock);
                goto exception;
            }
            t->frames[t->depth - 1].lock = lock;
            t->frames[t->depth - 1].ret_skip = (uint8_t)skip;
            LOAD_FRAME();
            if (--budget < 0) {
                SAVE_FRAME();
                return;
            }
            break;
        }

        /* Objects */
        case 0xbb: {                                                /* new */
            SAVE_FRAME();
            Class *c = resolve_class(t, cls, U2(pc + 1));
            if (!c) goto exception;
            if (c->access & (ACC_ABSTRACT | ACC_INTERFACE))
                THROW("java/lang/InstantiationError", c->name);
            ENSURE_INIT(c);
            Object *o = vm_new_object(t, c);
            if (!o) goto exception;
            PUSH_R(o);
            pc += 3;
            break;
        }
        case 0xbc: {                                                /* newarray */
            static const char types[12] = {0, 0, 0, 0, 'Z', 'C', 'F', 'D', 'B', 'S', 'I', 'J'};
            uint8_t at = pc[1];
            if (at < 4 || at > 11)
                THROW("java/lang/VerifyError", "bad newarray type");
            SAVE_FRAME();
            Array *a = vm_new_prim_array(t, types[at], sp[-1].i);
            if (!a) goto exception;
            sp[-1].ref = (Object *)a;
            pc += 2;
            break;
        }
        case 0xbd: {                                                /* anewarray */
            SAVE_FRAME();
            Class *c = resolve_class(t, cls, U2(pc + 1));
            if (!c) goto exception;
            Array *a = vm_new_array(t, class_array_of(c), sp[-1].i);
            if (!a) goto exception;
            sp[-1].ref = (Object *)a;
            pc += 3;
            break;
        }
        case 0xbe: {                                                /* arraylength */
            Object *a = sp[-1].ref;
            NULL_CHECK(a);
            sp[-1].i = ARRAY_LEN(a);
            pc++;
            break;
        }
        case 0xbf: {                                                /* athrow */
            Object *e = sp[-1].ref;
            NULL_CHECK(e);
            SAVE_FRAME();
            t->exception = e;
            goto exception;
        }
        case 0xc0: case 0xc1: {                                     /* checkcast, instanceof */
            SAVE_FRAME();
            Class *c = resolve_class(t, cls, U2(pc + 1));
            if (!c) goto exception;
            Object *o = sp[-1].ref;
            if (*pc == 0xc0) {
                if (o && !class_assignable(o->cls, c)) {
                    char msg[200];
                    snprintf(msg, sizeof msg, "%s cannot be cast to %s", o->cls->name, c->name);
                    THROW("java/lang/ClassCastException", msg);
                }
            } else {
                sp[-1].i = o && class_assignable(o->cls, c);
            }
            pc += 3;
            break;
        }
        case 0xc2: {                                                /* monitorenter */
            Object *o = sp[-1].ref;
            NULL_CHECK(o);
            if (!monitor_enter(t, o)) {
                SAVE_FRAME();
                return;
            }
            sp--;
            pc++;
            break;
        }
        case 0xc3: {                                                /* monitorexit */
            Object *o = POP_R();
            NULL_CHECK(o);
            if (!monitor_exit(t, o))
                THROW("java/lang/IllegalMonitorStateException", NULL);
            pc++;
            break;
        }
        case 0xc4: {                                                /* wide */
            uint16_t idx = U2(pc + 2);
            switch (pc[1]) {
            case 0x15: case 0x17: case 0x19: *sp++ = lv[idx]; break;
            case 0x16: case 0x18: sp[0] = lv[idx]; sp[1] = lv[idx + 1]; sp += 2; break;
            case 0x36: case 0x38: case 0x3a: lv[idx] = *--sp; break;
            case 0x37: case 0x39: sp -= 2; lv[idx] = sp[0]; lv[idx + 1] = sp[1]; break;
            case 0xa9: pc = m->code + lv[idx].i; goto dispatch;
            case 0x84:
                lv[idx].i = (jint)((uint32_t)lv[idx].i + (uint32_t)(jint)S2(pc + 4));
                pc += 6;
                goto dispatch;
            default:
                THROW("java/lang/VerifyError", "bad wide opcode");
            }
            pc += 4;
            break;
        }
        case 0xc5: {                                                /* multianewarray */
            SAVE_FRAME();
            Class *c = resolve_class(t, cls, U2(pc + 1));
            if (!c) goto exception;
            int  dims = pc[3];
            jint counts[256];
            for (int i = 0; i < dims; i++) {
                counts[i] = sp[i - dims].i;
                if (counts[i] < 0)
                    THROW("java/lang/NegativeArraySizeException", NULL);
            }
            Array *a = multi_array(t, c, dims, counts);
            if (!a) goto exception;
            sp -= dims;
            PUSH_R((Object *)a);
            pc += 4;
            break;
        }

        default:
            SAVE_FRAME();
            vm_throw_newf(t, "java/lang/VerifyError", "illegal opcode 0x%02x in %s.%s",
                          *pc, cls->name, m->name);
            goto exception;
        }
        continue;

    exception: {
            /* Unwind to the nearest matching handler. f->pc is the
             * faulting instruction in every frame. */
            Object *exc = t->exception;
            for (;;) {
                f = &t->frames[t->depth - 1];
                m = f->m;
                uint32_t off = (uint32_t)(f->pc - m->code);
                int handler = -1;
                for (int i = 0; i < m->n_exc && handler < 0; i++) {
                    ExcEntry *e = &m->exc[i];
                    if (off < e->start || off >= e->end)
                        continue;
                    if (e->catch_type == 0) {
                        handler = e->handler;
                    } else {
                        t->exception = NULL;
                        Class *c = resolve_class(t, m->cls, e->catch_type);
                        t->exception = NULL;
                        if (c && class_assignable(exc->cls, c))
                            handler = e->handler;
                    }
                }
                if (handler >= 0) {
                    t->exception = NULL;
                    f->pc = m->code + handler;
                    f->sp = f->locals + m->max_locals;
                    f->sp->ref = exc;
                    f->sp++;
                    break;
                }
                if (f->lock)
                    monitor_exit(t, f->lock);
                if (m->name == S_clinit)
                    finish_clinit(m, false);
                if (--t->depth == 0) {
                    t->exception = NULL;
                    vm.uncaught = true;
                    nj_log("Uncaught exception in thread:");
                    vm_print_exception(t, exc);
                    t->state = TS_TERMINATED;
                    return;
                }
            }
            LOAD_FRAME();
        }
    }
}
