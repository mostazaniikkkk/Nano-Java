/*
 * natives_lang.c - natives for java.lang (CLDC 1.1) and the nanojava.*
 * support classes.
 */
#include "vm.h"
#include "../pal/pal.h"

#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#define THIS        (args[0].ref)
#define ARG_REF(n)  (args[(n)].ref)
#define ARG_INT(n)  (args[(n)].i)
#define ARG_LONG(n) slot_get_long(&args[(n)])

/* ------------------------------------------------------------------------ */
/* java.lang.Object                                                         */

static int Object_getClass(Thread *t, Slot *args, Slot *ret)
{
    ret->ref = class_mirror(THIS->cls);
    if (!ret->ref)
        vm_throw(t, vm.oom);
    return NATIVE_OK;
}

static int Object_hashCode(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    ret->i = (jint)((uintptr_t)THIS >> 3);
    return NATIVE_OK;
}

static int Object_clone(Thread *t, Slot *args, Slot *ret)
{
    Object *o = THIS;
    if (!(o->cls->flags & CF_ARRAY)) {
        vm_throw_new(t, "java/lang/InternalError", "clone of a non-array object");
        return NATIVE_OK;
    }
    Array *a = vm_new_array(t, o->cls, ARRAY_LEN(o));
    if (a)
        memcpy(ARRAY_DATA(a, uint8_t), ARRAY_DATA(o, uint8_t),
               (size_t)ARRAY_LEN(o) << o->cls->elem_shift);
    ret->ref = (Object *)a;
    return NATIVE_OK;
}

static int Object_wait(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    return monitor_wait(t, THIS, ARG_LONG(1));
}

static int Object_notify(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    monitor_notify(t, THIS, false);
    return NATIVE_OK;
}

static int Object_notifyAll(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    monitor_notify(t, THIS, true);
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* java.lang.Class                                                          */

/* Initializes c on behalf of a native. Returns NATIVE_OK when c is ready
 * (or an exception is pending) and NATIVE_BLOCK when the calling invoke
 * has to run again once the initializer has finished. */
static int native_init(Thread *t, Class *c, bool *ready)
{
    *ready = false;
    switch (interp_ensure_init(t, c)) {
    case 1:  *ready = true; return NATIVE_OK;
    case -1: return NATIVE_OK;
    default: return NATIVE_BLOCK;
    }
}

static int Class_forName(Thread *t, Slot *args, Slot *ret)
{
    Object *name = ARG_REF(0);
    if (!name) {
        NPE(t);
        return NATIVE_OK;
    }
    char buf[256];
    str_to_utf8(name, buf, sizeof buf);
    for (char *p = buf; *p; p++)
        if (*p == '.')
            *p = '/';
    Class *c = class_load(buf);
    if (!c) {
        str_to_utf8(name, buf, sizeof buf);
        vm_throw_new(t, "java/lang/ClassNotFoundException", buf);
        return NATIVE_OK;
    }
    bool ready;
    int  r = native_init(t, c, &ready);
    if (!ready)
        return r;
    ret->ref = class_mirror(c);
    if (!ret->ref)
        vm_throw(t, vm.oom);
    return NATIVE_OK;
}

static int Class_newInstance(Thread *t, Slot *args, Slot *ret)
{
    Class *c = class_from_mirror(THIS);
    if (c->access & (ACC_ABSTRACT | ACC_INTERFACE)) {
        vm_throw_new(t, "java/lang/InstantiationException", c->name);
        return NATIVE_OK;
    }
    Method *init = class_find_declared(c, "<init>", "()V");
    if (!init) {
        vm_throw_new(t, "java/lang/InstantiationException", c->name);
        return NATIVE_OK;
    }
    if (!(init->access & ACC_PUBLIC) || !(c->access & ACC_PUBLIC)) {
        vm_throw_new(t, "java/lang/IllegalAccessException", c->name);
        return NATIVE_OK;
    }
    bool ready;
    int  r = native_init(t, c, &ready);
    if (!ready)
        return r;
    Object *o = vm_new_object(t, c);
    if (!o)
        return NATIVE_OK;
    ret->ref = o;
    t->tail_method = init;
    t->tail_args[0].ref = o;
    t->tail_nargs = 1;
    return NATIVE_INVOKE;
}

static int Class_isInstance(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    ret->i = vm_instanceof(ARG_REF(1), class_from_mirror(THIS));
    return NATIVE_OK;
}

static int Class_isAssignableFrom(Thread *t, Slot *args, Slot *ret)
{
    if (!ARG_REF(1)) {
        NPE(t);
        return NATIVE_OK;
    }
    ret->i = class_assignable(class_from_mirror(ARG_REF(1)), class_from_mirror(THIS));
    return NATIVE_OK;
}

static int Class_isInterface(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    ret->i = (class_from_mirror(THIS)->flags & CF_INTERFACE) != 0;
    return NATIVE_OK;
}

static int Class_isArray(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    ret->i = (class_from_mirror(THIS)->flags & CF_ARRAY) != 0;
    return NATIVE_OK;
}

static int Class_getName(Thread *t, Slot *args, Slot *ret)
{
    const char *n = class_from_mirror(THIS)->name;
    size_t      len = strlen(n);
    char       *buf = malloc(len + 1);
    for (size_t i = 0; i <= len; i++)
        buf[i] = n[i] == '/' ? '.' : n[i];
    ret->ref = str_new_utf8(t, buf);
    free(buf);
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* java.lang.String                                                         */

static int String_intern(Thread *t, Slot *args, Slot *ret)
{
    ret->ref = str_intern(t, THIS);
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* java.lang.System / Runtime                                               */

static int System_currentTimeMillis(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)args;
    slot_set_long(ret, pal_epoch_ms());
    return NATIVE_OK;
}

static int System_arraycopy(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    Object *src = ARG_REF(0), *dst = ARG_REF(2);
    jint    sp = ARG_INT(1), dp = ARG_INT(3), n = ARG_INT(4);
    if (!src || !dst) {
        NPE(t);
        return NATIVE_OK;
    }
    Class *sc = src->cls, *dc = dst->cls;
    if (!(sc->flags & CF_ARRAY) || !(dc->flags & CF_ARRAY)) {
        vm_throw_new(t, "java/lang/ArrayStoreException", "not an array");
        return NATIVE_OK;
    }
    bool sref = sc->component != NULL, dref = dc->component != NULL;
    if (sref != dref || (!sref && sc->elem_type != dc->elem_type)) {
        vm_throw_new(t, "java/lang/ArrayStoreException", "incompatible array types");
        return NATIVE_OK;
    }
    if (n < 0 || sp < 0 || dp < 0 || sp > ARRAY_LEN(src) - n || dp > ARRAY_LEN(dst) - n) {
        vm_throw_new(t, "java/lang/ArrayIndexOutOfBoundsException", NULL);
        return NATIVE_OK;
    }
    if (n == 0)
        return NATIVE_OK;
    int shift = sc->elem_shift;
    if (!sref || class_assignable(sc, dc)) {
        memmove(ARRAY_DATA(dst, uint8_t) + ((size_t)dp << shift),
                ARRAY_DATA(src, uint8_t) + ((size_t)sp << shift), (size_t)n << shift);
        return NATIVE_OK;
    }
    /* Reference arrays of unrelated types: check every element. */
    Object **s = ARRAY_DATA(src, Object *), **d = ARRAY_DATA(dst, Object *);
    for (jint i = 0; i < n; i++) {
        Object *e = s[sp + i];
        if (e && !class_assignable(e->cls, dc->component)) {
            vm_throw_new(t, "java/lang/ArrayStoreException", e->cls->name);
            return NATIVE_OK;
        }
        d[dp + i] = e;
    }
    return NATIVE_OK;
}

static int System_identityHashCode(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    ret->i = (jint)((uintptr_t)ARG_REF(0) >> 3);
    return NATIVE_OK;
}

static const char *const properties[][2] = {
    {"microedition.platform", "NanoJava"},
    {"microedition.configuration", "CLDC-1.1"},
    {"microedition.profiles", "MIDP-2.0"},
    {"microedition.encoding", "ISO-8859-1"},
    {"microedition.locale", "en-US"},
    {"microedition.commports", ""},
    {"microedition.hostname", "localhost"},
    {"microedition.media.version", "1.1"},
    {"supports.mixing", "false"},
    {"supports.audio.capture", "false"},
    {"supports.video.capture", "false"},
    {"supports.recording", "false"},
    {NULL, NULL}
};

static int System_getProperty0(Thread *t, Slot *args, Slot *ret)
{
    char key[128];
    if (!ARG_REF(0)) {
        NPE(t);
        return NATIVE_OK;
    }
    str_to_utf8(ARG_REF(0), key, sizeof key);
    for (int i = 0; properties[i][0]; i++) {
        if (strcmp(properties[i][0], key) == 0) {
            ret->ref = str_new_utf8(t, properties[i][1]);
            break;
        }
    }
    return NATIVE_OK;
}

static int Runtime_exit(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)ret;
    vm.exit_requested = true;
    vm.exit_code = ARG_INT(1);
    return NATIVE_BLOCK;
}

static int Runtime_freeMemory(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)args;
    slot_set_long(ret, (jlong)heap_free_bytes());
    return NATIVE_OK;
}

static int Runtime_totalMemory(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)args;
    slot_set_long(ret, (jlong)heap_total_bytes());
    return NATIVE_OK;
}

static int Runtime_gc(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)args; (void)ret;
    heap_gc();
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* java.lang.Thread                                                         */

static int Thread_currentThread(Thread *t, Slot *args, Slot *ret)
{
    (void)args;
    ret->ref = t->jthread;
    return NATIVE_OK;
}

static int Thread_yield(Thread *t, Slot *args, Slot *ret)
{
    (void)args; (void)ret;
    return thread_sleep(t, 0);
}

static int Thread_sleep(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    return thread_sleep(t, ARG_LONG(0));
}

static int Thread_start0(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    Object *jt = THIS;
    Method *run = class_find_virtual(jt->cls, S_run, S_V);
    if (!run || !run->code) {
        vm_throw_new(t, "java/lang/InternalError", "Thread.run() not found");
        return NATIVE_OK;
    }
    Slot a;
    a.ref = jt;
    Thread *nt = thread_create(jt, run, &a, 1);
    if (!nt) {
        vm_throw(t, vm.oom);
        return NATIVE_OK;
    }
    nt->priority = GET_INT(jt, vm.f_Thread_priority);
    return NATIVE_OK;
}

static int Thread_isAlive(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    Thread *other = thread_find(THIS);
    ret->i = other && other->state != TS_TERMINATED;
    return NATIVE_OK;
}

static int Thread_join(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    Thread *other = thread_find(THIS);
    t->native_phase = 0;
    if (t->interrupted) {
        t->interrupted = false;
        vm_throw_new(t, "java/lang/InterruptedException", NULL);
        return NATIVE_OK;
    }
    if (!other || other->state == TS_TERMINATED || other == t)
        return NATIVE_OK;
    /* Park in the thread object's wait set; termination wakes it. */
    Object *jt = THIS;
    if (!jt->mon)
        jt->mon = nj_alloc(sizeof(Monitor));
    t->wait_next = jt->mon->waiters;
    jt->mon->waiters = t;
    t->state = TS_WAITING;
    t->blocked_on = jt->mon;
    t->wake_time = 0;
    return NATIVE_BLOCK;
}

static int Thread_interrupt(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)ret;
    Thread *other = thread_find(THIS);
    if (other)
        thread_interrupt(other);
    return NATIVE_OK;
}

static int Thread_activeCount(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)args;
    ret->i = vm.n_threads;
    return NATIVE_OK;
}

static int Thread_setPriority0(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)ret;
    Thread *other = thread_find(THIS);
    if (other)
        other->priority = ARG_INT(1);
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* java.lang.Throwable                                                      */

void vm_fill_stack_trace(Thread *t, Object *exc);

static int Throwable_fillInStackTrace(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    vm_fill_stack_trace(t, THIS);
    return NATIVE_OK;
}

static int Throwable_printStackTrace0(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    vm_print_exception(t, THIS);
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* java.lang.Math, Float, Double                                            */

#define MATH1(name, expr) \
    static int Math_##name(Thread *t, Slot *args, Slot *ret) \
    { (void)t; jdouble x = slot_get_double(args); slot_set_double(ret, (expr)); return NATIVE_OK; }

MATH1(sin, sin(x))
MATH1(cos, cos(x))
MATH1(tan, tan(x))
MATH1(sqrt, sqrt(x))
MATH1(ceil, ceil(x))
MATH1(floor, floor(x))

static int Float_floatToIntBits(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    jfloat f = args[0].f;
    ret->i = f != f ? 0x7fc00000 : args[0].i;
    return NATIVE_OK;
}

static int Float_intBitsToFloat(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    ret->i = args[0].i;
    return NATIVE_OK;
}

static int Double_doubleToLongBits(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    jdouble d = slot_get_double(args);
    if (d != d)
        slot_set_long(ret, 0x7ff8000000000000LL);
    else
        slot_set_long(ret, slot_get_long(args));
    return NATIVE_OK;
}

static int Double_longBitsToDouble(Thread *t, Slot *args, Slot *ret)
{
    (void)t;
    slot_set_long(ret, slot_get_long(args));
    return NATIVE_OK;
}

/* Formats like Java's Double.toString/Float.toString: the shortest digit
 * string that reads back as the same value, in plain notation for
 * 1e-3 <= |v| < 1e7 and computerized scientific notation otherwise. */
static void java_format(double v, bool is_float, char *out)
{
    if (v != v) {
        strcpy(out, "NaN");
        return;
    }
    if (isinf(v)) {
        strcpy(out, v > 0 ? "Infinity" : "-Infinity");
        return;
    }
    if (v == 0) {
        strcpy(out, signbit(v) ? "-0.0" : "0.0");
        return;
    }

    char buf[40];
    int  maxp = is_float ? 9 : 17;
    /* Java prints at least two digits for subnormal doubles (4.9E-324). */
    int minp = !is_float && fabs(v) < 2.2250738585072014e-308 ? 2 : 1;
    for (int p = minp; p <= maxp; p++) {
        snprintf(buf, sizeof buf, "%.*e", p - 1, v);
        if (is_float ? (strtof(buf, NULL) == (float)v) : (strtod(buf, NULL) == v))
            break;
    }

    /* buf is "[-]d.ddde[+-]xx": split into sign, digits and exponent. */
    char *p = buf;
    char *o = out;
    if (*p == '-')
        *o++ = *p++;
    char digits[24];
    int  nd = 0;
    for (; *p && *p != 'e'; p++)
        if (*p >= '0' && *p <= '9')
            digits[nd++] = *p;
    while (nd > 1 && digits[nd - 1] == '0')
        nd--;
    digits[nd] = 0;
    int exp = atoi(p + 1);

    double a = fabs(v);
    if (a >= 1e-3 && a < 1e7) {
        if (exp >= 0) {
            for (int i = 0; i <= exp; i++)
                *o++ = i < nd ? digits[i] : '0';
            *o++ = '.';
            if (exp + 1 < nd) {
                for (int i = exp + 1; i < nd; i++)
                    *o++ = digits[i];
            } else {
                *o++ = '0';
            }
        } else {
            *o++ = '0';
            *o++ = '.';
            for (int i = 0; i < -exp - 1; i++)
                *o++ = '0';
            for (int i = 0; i < nd; i++)
                *o++ = digits[i];
        }
        *o = 0;
    } else {
        *o++ = digits[0];
        *o++ = '.';
        if (nd > 1) {
            for (int i = 1; i < nd; i++)
                *o++ = digits[i];
        } else {
            *o++ = '0';
        }
        sprintf(o, "E%d", exp);
    }
}

static int Double_toString(Thread *t, Slot *args, Slot *ret)
{
    char buf[48];
    java_format(slot_get_double(args), false, buf);
    ret->ref = str_new_utf8(t, buf);
    return NATIVE_OK;
}

static int Float_toString(Thread *t, Slot *args, Slot *ret)
{
    char buf[48];
    java_format(args[0].f, true, buf);
    ret->ref = str_new_utf8(t, buf);
    return NATIVE_OK;
}

static bool parse_java_double(Object *s, double *out)
{
    char buf[128];
    if (!s || str_length(s) >= (jint)sizeof buf)
        return false;
    str_to_utf8(s, buf, sizeof buf);
    char *b = buf;
    while (*b && (unsigned char)*b <= ' ')
        b++;
    char *e = b + strlen(b);
    while (e > b && (unsigned char)e[-1] <= ' ')
        e--;
    if (e > b && strchr("fFdD", e[-1]))
        e--;
    *e = 0;
    if (!*b)
        return false;
    for (char *c = b; *c; c++)
        if (*c == 'x' || *c == 'X')
            return false;
    char *end;
    *out = strtod(b, &end);
    return *end == 0;
}

static int Double_parseDouble(Thread *t, Slot *args, Slot *ret)
{
    double d;
    if (!ARG_REF(0)) {
        NPE(t);
        return NATIVE_OK;
    }
    if (!parse_java_double(ARG_REF(0), &d)) {
        char buf[128];
        vm_throw_new(t, "java/lang/NumberFormatException", str_to_utf8(ARG_REF(0), buf, sizeof buf));
        return NATIVE_OK;
    }
    slot_set_double(ret, d);
    return NATIVE_OK;
}

static int Float_parseFloat(Thread *t, Slot *args, Slot *ret)
{
    double d;
    if (!ARG_REF(0)) {
        NPE(t);
        return NATIVE_OK;
    }
    if (!parse_java_double(ARG_REF(0), &d)) {
        char buf[128];
        vm_throw_new(t, "java/lang/NumberFormatException", str_to_utf8(ARG_REF(0), buf, sizeof buf));
        return NATIVE_OK;
    }
    ret->f = (jfloat)d;
    return NATIVE_OK;
}

/* ------------------------------------------------------------------------ */
/* nanojava.*                                                               */

static int Console_write(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)ret;
    char c = (char)ARG_INT(1);
    pal_console_write(&c, 1);
    return NATIVE_OK;
}

static int Console_writeBytes(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    Object *b = ARG_REF(1);
    jint    off = ARG_INT(2), len = ARG_INT(3);
    if (!b) {
        NPE(t);
        return NATIVE_OK;
    }
    if (off < 0 || len < 0 || off > ARRAY_LEN(b) - len) {
        vm_throw_new(t, "java/lang/IndexOutOfBoundsException", NULL);
        return NATIVE_OK;
    }
    pal_console_write(ARRAY_DATA(b, char) + off, len);
    return NATIVE_OK;
}

static int Resources_read(Thread *t, Slot *args, Slot *ret)
{
    char path[256];
    if (!ARG_REF(0)) {
        NPE(t);
        return NATIVE_OK;
    }
    str_to_utf8(ARG_REF(0), path, sizeof path);
    uint32_t len;
    uint8_t *data = res_read_app(path, &len);
    if (!data)
        return NATIVE_OK;
    Array *a = vm_new_prim_array(t, 'B', (jint)len);
    if (a)
        memcpy(ARRAY_DATA(a, uint8_t), data, len);
    free(data);
    ret->ref = (Object *)a;
    return NATIVE_OK;
}

static int AppProperties_jad(Thread *t, Slot *args, Slot *ret)
{
    uint32_t       len;
    const uint8_t *data = res_jad(&len);
    if (!data)
        return NATIVE_OK;
    Array *a = vm_new_prim_array(t, 'B', (jint)len);
    if (a)
        memcpy(ARRAY_DATA(a, uint8_t), data, len);
    ret->ref = (Object *)a;
    return NATIVE_OK;
}

static int Boot_invokeMain(Thread *t, Slot *args, Slot *ret)
{
    (void)ret;
    Class  *c = class_from_mirror(ARG_REF(0));
    Method *m = class_find_declared(c, "main", "([Ljava/lang/String;)V");
    if (!m || !(m->access & ACC_STATIC)) {
        vm_throw_new(t, "java/lang/NoSuchMethodError", "main(String[])");
        return NATIVE_OK;
    }
    bool ready;
    int  r = native_init(t, c, &ready);
    if (!ready)
        return r;
    t->tail_method = m;
    t->tail_args[0] = args[1];
    t->tail_nargs = 1;
    return NATIVE_INVOKE;
}

static int Boot_exit(Thread *t, Slot *args, Slot *ret)
{
    (void)t; (void)ret;
    vm.exit_requested = true;
    vm.exit_code = ARG_INT(0);
    return NATIVE_BLOCK;
}

static const NativeEntry lang_natives[] = {
    {"java/lang/Object", "getClass", NULL, Object_getClass},
    {"java/lang/Object", "hashCode", NULL, Object_hashCode},
    {"java/lang/Object", "clone", NULL, Object_clone},
    {"java/lang/Object", "wait", "(J)V", Object_wait},
    {"java/lang/Object", "notify", NULL, Object_notify},
    {"java/lang/Object", "notifyAll", NULL, Object_notifyAll},

    {"java/lang/Class", "forName", NULL, Class_forName},
    {"java/lang/Class", "newInstance", NULL, Class_newInstance},
    {"java/lang/Class", "isInstance", NULL, Class_isInstance},
    {"java/lang/Class", "isAssignableFrom", NULL, Class_isAssignableFrom},
    {"java/lang/Class", "isInterface", NULL, Class_isInterface},
    {"java/lang/Class", "isArray", NULL, Class_isArray},
    {"java/lang/Class", "getName", NULL, Class_getName},

    {"java/lang/String", "intern", NULL, String_intern},

    {"java/lang/System", "currentTimeMillis", NULL, System_currentTimeMillis},
    {"java/lang/System", "arraycopy", NULL, System_arraycopy},
    {"java/lang/System", "identityHashCode", NULL, System_identityHashCode},
    {"java/lang/System", "getProperty0", NULL, System_getProperty0},

    {"java/lang/Runtime", "exit", NULL, Runtime_exit},
    {"java/lang/Runtime", "freeMemory", NULL, Runtime_freeMemory},
    {"java/lang/Runtime", "totalMemory", NULL, Runtime_totalMemory},
    {"java/lang/Runtime", "gc", NULL, Runtime_gc},

    {"java/lang/Thread", "currentThread", NULL, Thread_currentThread},
    {"java/lang/Thread", "yield", NULL, Thread_yield},
    {"java/lang/Thread", "sleep", NULL, Thread_sleep},
    {"java/lang/Thread", "start0", NULL, Thread_start0},
    {"java/lang/Thread", "isAlive", NULL, Thread_isAlive},
    {"java/lang/Thread", "join", NULL, Thread_join},
    {"java/lang/Thread", "interrupt0", NULL, Thread_interrupt},
    {"java/lang/Thread", "activeCount", NULL, Thread_activeCount},
    {"java/lang/Thread", "setPriority0", NULL, Thread_setPriority0},

    {"java/lang/Throwable", "fillInStackTrace", NULL, Throwable_fillInStackTrace},
    {"java/lang/Throwable", "printStackTrace0", NULL, Throwable_printStackTrace0},

    {"java/lang/Math", "sin", NULL, Math_sin},
    {"java/lang/Math", "cos", NULL, Math_cos},
    {"java/lang/Math", "tan", NULL, Math_tan},
    {"java/lang/Math", "sqrt", NULL, Math_sqrt},
    {"java/lang/Math", "ceil", NULL, Math_ceil},
    {"java/lang/Math", "floor", NULL, Math_floor},

    {"java/lang/Float", "floatToIntBits", NULL, Float_floatToIntBits},
    {"java/lang/Float", "intBitsToFloat", NULL, Float_intBitsToFloat},
    {"java/lang/Float", "toString", "(F)Ljava/lang/String;", Float_toString},
    {"java/lang/Float", "parseFloat", NULL, Float_parseFloat},
    {"java/lang/Double", "doubleToLongBits", NULL, Double_doubleToLongBits},
    {"java/lang/Double", "longBitsToDouble", NULL, Double_longBitsToDouble},
    {"java/lang/Double", "toString", "(D)Ljava/lang/String;", Double_toString},
    {"java/lang/Double", "parseDouble", NULL, Double_parseDouble},

    {"nanojava/ConsoleOutputStream", "write", "(I)V", Console_write},
    {"nanojava/ConsoleOutputStream", "writeBytes", NULL, Console_writeBytes},
    {"nanojava/Resources", "read", NULL, Resources_read},
    {"nanojava/AppProperties", "jad", NULL, AppProperties_jad},
    {"nanojava/Boot", "invokeMain", NULL, Boot_invokeMain},
    {"nanojava/Boot", "exit", NULL, Boot_exit},
    {NULL, NULL, NULL, NULL}
};

void natives_lang_init(void)
{
    native_register(lang_natives);
}
