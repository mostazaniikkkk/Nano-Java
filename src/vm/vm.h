/*
 * Nano Java - a J2ME (CLDC 1.1 / MIDP 2.0) runtime.
 *
 * vm.h - core VM types and the internal API shared by the VM modules.
 *
 * Conventions:
 *  - Every value on the operand stack, in locals and in object fields is a
 *    Slot. long/double take two consecutive slots (low word first).
 *  - Names and descriptors are interned symbols (see sym.c), so they can be
 *    compared by pointer.
 *  - Class metadata lives outside the Java heap and is never freed.
 */
#ifndef NJ_VM_H
#define NJ_VM_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>
#include <string.h>

typedef int8_t   jbyte;
typedef uint8_t  jboolean;
typedef uint16_t jchar;
typedef int16_t  jshort;
typedef int32_t  jint;
typedef int64_t  jlong;
typedef float    jfloat;
typedef double   jdouble;

typedef struct Object  Object;
typedef struct Array   Array;
typedef struct Class   Class;
typedef struct Field   Field;
typedef struct Method  Method;
typedef struct Thread  Thread;
typedef struct Monitor Monitor;
typedef struct Frame   Frame;

typedef union Slot {
    jint      i;
    jfloat    f;
    Object   *ref;
    uintptr_t raw;
} Slot;

static inline jlong slot_get_long(const Slot *s)
{
    return (jlong)(((uint64_t)(uint32_t)s[1].i << 32) | (uint32_t)s[0].i);
}
static inline void slot_set_long(Slot *s, jlong v)
{
    s[0].i = (jint)(uint32_t)v;
    s[1].i = (jint)(uint32_t)((uint64_t)v >> 32);
}
static inline jdouble slot_get_double(const Slot *s)
{
    jlong l = slot_get_long(s);
    jdouble d;
    memcpy(&d, &l, sizeof d);
    return d;
}
static inline void slot_set_double(Slot *s, jdouble d)
{
    jlong l;
    memcpy(&l, &d, sizeof l);
    slot_set_long(s, l);
}

/* ------------------------------------------------------------------------ */
/* Objects                                                                  */

/* Every heap chunk starts with this header. For free chunks cls is NULL and
 * mon doubles as the free-list link. */
struct Object {
    Class    *cls;
    uint32_t  info;   /* chunk size in bytes | GC bits */
    Monitor  *mon;
};

struct Array {
    Object hdr;
    jint   length;
};

#define OBJ_INFO_MARK   0x80000000u
#define OBJ_INFO_SIZE   0x3FFFFFF8u

#define FIELDS(o)           ((Slot *)((char *)(o) + sizeof(Object)))
#define ARRAY_DATA_OFFSET   ((sizeof(Array) + 7) & ~(size_t)7)
#define ARRAY_DATA(a, T)    ((T *)((char *)(a) + ARRAY_DATA_OFFSET))
#define ARRAY_LEN(a)        (((Array *)(a))->length)

/* ------------------------------------------------------------------------ */
/* Class files                                                              */

enum {
    CONSTANT_Utf8 = 1, CONSTANT_Integer = 3, CONSTANT_Float = 4,
    CONSTANT_Long = 5, CONSTANT_Double = 6, CONSTANT_Class = 7,
    CONSTANT_String = 8, CONSTANT_Fieldref = 9, CONSTANT_Methodref = 10,
    CONSTANT_InterfaceMethodref = 11, CONSTANT_NameAndType = 12
};

enum {
    ACC_PUBLIC = 0x0001, ACC_PRIVATE = 0x0002, ACC_PROTECTED = 0x0004,
    ACC_STATIC = 0x0008, ACC_FINAL = 0x0010, ACC_SYNCHRONIZED = 0x0020,
    ACC_SUPER = 0x0020, ACC_VOLATILE = 0x0040, ACC_TRANSIENT = 0x0080,
    ACC_NATIVE = 0x0100, ACC_INTERFACE = 0x0200, ACC_ABSTRACT = 0x0400
};

typedef struct CPEntry {
    uint8_t tag;
    uint8_t resolved;
    union {
        jint        i;
        jfloat      f;
        uint32_t    u;
        const char *utf8;
        struct { uint16_t a, b; } ref;   /* class/nat index, name/desc index */
        Class      *cls;
        Field      *field;
        Method     *method;
        Object     *str;
    } v;
} CPEntry;

typedef struct ExcEntry {
    uint16_t start, end, handler, catch_type;
} ExcEntry;

typedef struct LineEntry {
    uint16_t pc, line;
} LineEntry;

/* Native method: args point at the receiver/first argument. A native writes
 * its result into ret[0] (ret[0..1] for long/double) and returns one of the
 * NATIVE_* codes. */
typedef int (*NativeFn)(Thread *t, Slot *args, Slot *ret);

enum {
    NATIVE_OK = 0,   /* returned normally (or threw: t->exception set) */
    NATIVE_BLOCK,    /* thread blocked: the invoke is re-executed later */
    NATIVE_INVOKE    /* push result, then call t->tail_method (see interp.c) */
};

struct Method {
    Class          *cls;
    const char     *name;
    const char     *desc;
    uint16_t        access;
    uint16_t        max_stack;
    uint16_t        max_locals;
    uint16_t        arg_slots;    /* including 'this' */
    uint16_t        vtable_index;
    char            ret_type;     /* descriptor char of the return type */
    uint8_t         ret_slots;
    uint8_t        *code;
    uint32_t        code_len;
    ExcEntry       *exc;
    uint16_t        n_exc;
    uint16_t        n_lines;
    LineEntry      *lines;
    NativeFn        native;
};

struct Field {
    Class      *cls;
    const char *name;
    const char *desc;
    uint16_t    access;
    uint16_t    offset;       /* slot index into FIELDS(obj) or cls->statics */
    uint16_t    const_index;  /* ConstantValue attribute, 0 if none */
};

enum {
    CLS_LOADED, CLS_LINKED, CLS_INITIALIZING, CLS_INITIALIZED, CLS_ERROR
};

enum {
    CF_ARRAY     = 1 << 0,
    CF_INTERFACE = 1 << 1,
    CF_WEAKREF   = 1 << 2,   /* java/lang/ref/WeakReference or subclass */
    CF_MIRROR    = 1 << 3,   /* java/lang/Class: hidden Class* slot */
    CF_THROWABLE = 1 << 4
};

struct Class {
    const char  *name;          /* internal form: java/lang/String, [I */
    Class       *super;
    Class      **interfaces;
    uint16_t     n_interfaces;
    uint16_t     access;
    uint8_t      state;
    uint8_t      flags;
    char         elem_type;     /* arrays: descriptor char of the element */
    uint8_t      elem_shift;    /* arrays: log2(element size) */
    Class       *component;     /* arrays: element class (NULL for primitive) */

    CPEntry     *cp;
    uint16_t     cp_count;
    Field       *fields;
    uint16_t     n_fields;
    Method      *methods;
    uint16_t     n_methods;
    Method     **vtable;
    uint16_t     vtable_len;

    Slot        *statics;
    uint16_t     n_static_slots;
    uint16_t     inst_slots;    /* instance size in slots, super included */
    uint16_t    *ref_slots;     /* instance slots holding references */
    uint16_t     n_ref_slots;

    Object      *mirror;        /* java.lang.Class instance, lazily made */
    Thread      *init_thread;
    const char  *source_file;
    Class       *array_class;   /* cached class of "[this" */
    Class       *hash_next;
};

/* ------------------------------------------------------------------------ */
/* Threads                                                                  */

struct Frame {
    Method        *m;
    const uint8_t *pc;        /* caller frames: the invoke being executed */
    Slot          *locals;
    Slot          *sp;
    Object        *lock;      /* monitor held by a synchronized method */
    uint8_t        ret_skip;  /* bytes to advance the caller's pc on return */
};

enum {
    TS_RUNNABLE, TS_SLEEPING, TS_WAITING, TS_BLOCKED, TS_EVENT_WAIT,
    TS_TERMINATED
};

struct Thread {
    Object   *jthread;
    Slot     *stack;
    Slot     *stack_limit;
    Frame    *frames;
    int       max_frames;
    int       depth;

    uint8_t   state;
    bool      interrupted;
    bool      daemon;
    bool      timed_out;
    int       native_phase;   /* for restartable natives */
    jint      saved_count;    /* monitor recursion saved across wait() */
    jlong     wake_time;      /* ms, for timed sleeps/waits; 0 = forever */
    Monitor  *blocked_on;     /* monitor entered or waited on */
    Object   *exception;      /* pending exception */

    Method   *tail_method;    /* NATIVE_INVOKE */
    int       tail_nargs;
    Slot      tail_args[4];

    Thread   *next;           /* all threads */
    Thread   *wait_next;      /* monitor wait set */
    int       priority;
};

struct Monitor {
    Thread *owner;
    jint    count;
    Thread *waiters;          /* wait set, linked through wait_next */
};

/* ------------------------------------------------------------------------ */
/* VM state                                                                 */

typedef struct VMOptions {
    size_t heap_size;
    int    stack_slots;       /* operand stack + locals per thread */
    int    max_frames;
    bool   trace_classes;
    bool   trace_exceptions;  /* log exceptions created by the VM */
    bool   trace_audio;       /* log nanojava.Audio calls */
} VMOptions;

typedef struct VM {
    VMOptions opt;

    Class *c_Object, *c_Class, *c_String, *c_Thread, *c_Throwable;
    Class *c_Cloneable;
    Class *prim_arrays[128];  /* "[I" etc, indexed by element char */

    Field *f_String_value, *f_String_offset, *f_String_count;
    Field *f_Throwable_message, *f_Throwable_trace;
    Field *f_Thread_priority, *f_Thread_target, *f_Thread_name;
    Field *f_Weak_referent;

    Thread *threads;
    Thread *current;
    int     n_threads;

    bool    exit_requested;
    int     exit_code;
    bool    uncaught;         /* a thread died from an uncaught exception */
    Object *oom;              /* preallocated OutOfMemoryError */

    jlong   start_ms;
} VM;

extern VM vm;

/* ------------------------------------------------------------------------ */
/* Module API                                                               */

/* util */
void       *nj_alloc(size_t n);            /* zeroed malloc, fatal on OOM */
void       *nj_meta(size_t n);             /* permanent metadata allocation */
void        nj_log(const char *fmt, ...);
void        nj_fatal(const char *fmt, ...);

/* sym.c */
const char *sym_intern(const char *s);
const char *sym_intern_len(const char *s, size_t len);
void        sym_init(void);
extern const char *S_init, *S_clinit, *S_V, *S_main, *S_run;

/* classfile.c */
Class *classfile_parse(const uint8_t *data, uint32_t len, const char **err);
int    desc_arg_slots(const char *desc);   /* without 'this' */

/* class.c */
typedef uint8_t *(*ClassSourceFn)(const char *path, uint32_t *len);
void    class_add_source(ClassSourceFn fn);
Class  *class_load(const char *name);      /* NULL: exception pending */
Class  *class_find_loaded(const char *name);
Class  *class_array_of(Class *elem);
Class  *class_prim_array(char type);
Field  *class_find_field(Class *c, const char *name, const char *desc);
Method *class_find_method(Class *c, const char *name, const char *desc);
Method *class_find_declared(Class *c, const char *name, const char *desc);
Method *class_find_virtual(Class *c, const char *name, const char *desc);
bool    class_is_subclass(const Class *c, const Class *of);
bool    class_assignable(Class *from, Class *to);
Object *class_mirror(Class *c);
Class  *class_from_mirror(Object *mirror);
void    class_foreach(void (*fn)(Class *c, void *ctx), void *ctx);
const char *class_last_error(void);

/* heap.c */
void    heap_init(size_t size);
Object *heap_alloc_object(Class *c);           /* NULL on OOM */
Array  *heap_alloc_array(Class *c, jint len);  /* NULL on OOM */
bool    heap_is_object(const void *p);
void    heap_gc(void);
size_t  heap_free_bytes(void);
size_t  heap_total_bytes(void);
void    heap_set_stack_base(void *base);
void    heap_add_root(Object **root);

/* Allocation helpers that throw OutOfMemoryError / NegativeArraySize. */
Object *vm_new_object(Thread *t, Class *c);
Array  *vm_new_array(Thread *t, Class *c, jint len);
Array  *vm_new_prim_array(Thread *t, char type, jint len);

/* string.c */
Object *str_new_utf8(Thread *t, const char *s);
Object *str_new_utf8_len(Thread *t, const char *s, size_t len);
Object *str_new_chars(Thread *t, const jchar *s, jint len);
Object *str_intern(Thread *t, Object *s);
Object *str_literal(Thread *t, const char *utf8);
jint    str_length(Object *s);
const jchar *str_chars(Object *s);
char   *str_to_utf8(Object *s, char *buf, size_t size); /* NUL terminated */
char   *str_dup_utf8(Object *s);                       /* malloc'd */
void    str_mark_roots(void (*mark)(Object *));

/* thread.c */
Thread *thread_create(Object *jthread, Method *entry, Slot *args, int nargs);
Thread *thread_find(Object *jthread);
void    thread_free(Thread *t);
bool    monitor_enter(Thread *t, Object *o);   /* false: must block */
bool    monitor_exit(Thread *t, Object *o);    /* false: not owner */
void    monitor_free(Monitor *m);
int     monitor_wait(Thread *t, Object *o, jlong ms);  /* NATIVE_* code */
void    monitor_notify(Thread *t, Object *o, bool all);
int     thread_sleep(Thread *t, jlong ms);              /* NATIVE_* code */
void    thread_interrupt(Thread *t);
int     vm_run(void);                          /* scheduler; exit code */
extern void (*vm_poll_hook)(void);             /* called between time slices */
void    vm_wake_event_waiters(void);
void    sched_wake(Thread *t);

/* interp.c. On the DS the interpreter loop runs from ITCM, the ARM9's
 * zero-wait instruction memory. */
#if defined(NJ_NDS)
#define NJ_HOT __attribute__((section(".itcm"), long_call))
#else
#define NJ_HOT
#endif
NJ_HOT void interp_run(Thread *t, int budget);
bool    interp_push_frame(Thread *t, Method *m, Slot *args);
int     interp_ensure_init(Thread *t, Class *c);  /* 1 ready, 0 retry, -1 exc */
void    vm_throw(Thread *t, Object *exc);
void    vm_throw_new(Thread *t, const char *cls, const char *msg);
void    vm_throw_newf(Thread *t, const char *cls, const char *fmt, ...);
void    vm_print_exception(Thread *t, Object *exc);
bool    vm_instanceof(Object *o, Class *c);

/* vm.c */
bool    vm_init(const VMOptions *opt);
bool    vm_boot(const char *const *args, int nargs);  /* nanojava.Boot.main */
void    vm_shutdown(void);                            /* free everything */

/* resource.c - class and resource lookup in the class library and app */
struct Zip;
void     res_set_system(struct Zip *z);
void     res_add_app_zip(struct Zip *z);
void     res_add_app_dir(const char *dir);
uint8_t *res_read_app(const char *path, uint32_t *len);   /* malloc'd */
void     res_set_jad(const uint8_t *data, uint32_t len);   /* copied */
const uint8_t *res_jad(uint32_t *len);                     /* NULL if none */

/* native.c */
typedef struct NativeEntry {
    const char *cls;
    const char *name;
    const char *desc;    /* NULL matches any descriptor */
    NativeFn    fn;
} NativeEntry;
void     native_register(const NativeEntry *table);
NativeFn native_lookup(const char *cls, const char *name, const char *desc);

/* Field access by slot (works for instance FIELDS() and statics). */
#define GET_REF(o, f)    (FIELDS(o)[(f)->offset].ref)
#define GET_INT(o, f)    (FIELDS(o)[(f)->offset].i)
#define SET_REF(o, f, v) (FIELDS(o)[(f)->offset].ref = (v))
#define SET_INT(o, f, v) (FIELDS(o)[(f)->offset].i = (v))

/* Exception helpers used throughout natives. */
#define NPE(t)  vm_throw_new((t), "java/lang/NullPointerException", NULL)

#endif
