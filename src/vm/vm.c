/*
 * vm.c - VM start-up: core classes, well-known fields and the boot thread.
 */
#include "vm.h"
#include "../pal/pal.h"

VM vm;

void natives_lang_init(void);

bool vm_init(const VMOptions *opt)
{
    memset(&vm, 0, sizeof vm);
    vm.opt = *opt;
    vm.start_ms = pal_time_ms();
    sym_init();
    heap_init(opt->heap_size);
    natives_lang_init();
    return true;
}

void sym_reset(void);
void str_reset(void);
void class_reset(void);
void heap_shutdown(void);
void native_reset(void);
void res_reset(void);
void interp_reset(void);
void nj_meta_free_all(void);

/* Releases everything the VM allocated so vm_init() can start a new one
 * in the same process (used by the DS menu to switch games). */
void vm_shutdown(void)
{
    Thread *t = vm.threads;
    while (t) {
        Thread *next = t->next;
        thread_free(t);
        t = next;
    }
    heap_shutdown();
    class_reset();
    str_reset();
    sym_reset();
    native_reset();
    res_reset();
    interp_reset();
    nj_meta_free_all();
    vm_poll_hook = NULL;
    memset(&vm, 0, sizeof vm);
}

static Class *core_class(const char *name)
{
    Class *c = class_load(name);
    if (!c)
        nj_fatal("cannot load core class %s (%s)", name, class_last_error());
    return c;
}

static Field *core_field(Class *c, const char *name, const char *desc)
{
    Field *f = class_find_field(c, name, desc);
    if (!f)
        nj_fatal("core field %s.%s missing", c->name, name);
    return f;
}

static bool load_core(void)
{
    vm.c_Object = core_class("java/lang/Object");
    vm.c_Class = core_class("java/lang/Class");
    vm.c_String = core_class("java/lang/String");
    vm.c_Thread = core_class("java/lang/Thread");
    vm.c_Throwable = core_class("java/lang/Throwable");

    vm.f_String_value = core_field(vm.c_String, "value", "[C");
    vm.f_String_offset = core_field(vm.c_String, "offset", "I");
    vm.f_String_count = core_field(vm.c_String, "count", "I");
    vm.f_Throwable_message = core_field(vm.c_Throwable, "detailMessage", "Ljava/lang/String;");
    vm.f_Throwable_trace = core_field(vm.c_Throwable, "trace", "Ljava/lang/Object;");
    vm.f_Thread_priority = core_field(vm.c_Thread, "priority", "I");
    vm.f_Thread_target = core_field(vm.c_Thread, "target", "Ljava/lang/Runnable;");
    vm.f_Thread_name = core_field(vm.c_Thread, "name", "Ljava/lang/String;");

    Class *weak = core_class("java/lang/ref/WeakReference");
    vm.f_Weak_referent = core_field(weak, "referent", "Ljava/lang/Object;");

    /* These are used by C code before any Java code could initialize
     * them; they must not need static initializers. */
    vm.c_Object->state = CLS_INITIALIZED;
    vm.c_String->state = CLS_INITIALIZED;

    Class *oom = core_class("java/lang/OutOfMemoryError");
    for (Class *k = oom; k && k->state == CLS_LINKED; k = k->super)
        k->state = CLS_INITIALIZED;
    vm.oom = heap_alloc_object(oom);
    return vm.oom != NULL;
}

bool vm_boot(const char *const *args, int nargs)
{
    if (!load_core())
        return false;

    Class  *boot = core_class("nanojava/Boot");
    Method *main = class_find_declared(boot, "main", "([Ljava/lang/String;)V");
    if (!main)
        nj_fatal("nanojava.Boot.main missing");

    Thread tmp;   /* a throwaway context for allocation helpers */
    memset(&tmp, 0, sizeof tmp);

    Object *jthread = heap_alloc_object(vm.c_Thread);
    Array  *argv = heap_alloc_array(class_array_of(vm.c_String), nargs);
    Object *name = str_new_utf8(&tmp, "main");
    if (!jthread || !argv || !name)
        return false;
    SET_REF(jthread, vm.f_Thread_name, name);
    SET_INT(jthread, vm.f_Thread_priority, 5);
    for (int i = 0; i < nargs; i++) {
        Object *s = str_new_utf8(&tmp, args[i]);
        if (!s)
            return false;
        ARRAY_DATA(argv, Object *)[i] = s;
    }

    Slot a;
    a.ref = (Object *)argv;
    Thread *t = thread_create(jthread, main, &a, 1);
    if (!t)
        return false;
    vm.current = t;
    if (interp_ensure_init(t, boot) < 0)
        return false;
    return true;
}
