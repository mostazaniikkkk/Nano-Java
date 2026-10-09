/*
 * vm_start.c - creates and runs the VM for the chosen MIDlet.
 */
#include <stdio.h>
#include <stdlib.h>

#include "nds_platform.h"
#include "classlib_bin.h"
#include "../../../src/vm/vm.h"
#include "../../../src/util/zip.h"
#include "../../../src/midp/midp.h"

/* Native memory kept outside the Java heap for class metadata, thread
 * stacks and image decoding. */
#define NATIVE_RESERVE (1u << 20)

/* Present only in test builds made with `make nds-embed JAR=...`. */
extern const uint8_t app_bin[] __attribute__((weak));
extern const uint8_t app_bin_end[] __attribute__((weak));

bool nds_has_embedded_app(void)
{
    return app_bin != NULL && app_bin_end != NULL;
}

/* libnds (nds/system.h, which cannot be included here). */
uint8_t *getHeapEnd(void);
uint8_t *getHeapLimit(void);

/* Memory malloc can still get from the system: the space between the
 * current end of the heap and its limit (4 MB on DS, 16 MB on DSi). */
static size_t free_memory(void)
{
    return (size_t)(getHeapLimit() - getHeapEnd());
}

void nds_set_jad(const uint8_t *data, uint32_t len)
{
    res_set_jad(data, len);
}

void nds_stop_vm(void)
{
    vm.exit_requested = true;
}

int nds_run_vm(const char *jar_path, const char *midlet_class, GameSettings *s)
{
    size_t free_mem = free_memory();
    if (free_mem < NATIVE_RESERVE + (1u << 20))
        pal_fatal("Not enough memory.");
    VMOptions opt = {free_mem - NATIVE_RESERVE, 4096, 256, false, false, false};
    vm_init(&opt);
    midp_init();

    Zip *classlib = zip_open_mem(classlib_bin, (uint32_t)classlib_bin_size);
    if (!classlib)
        pal_fatal("The built-in class library is damaged.");
    res_set_system(classlib);
    Zip *app = jar_path ? zip_open_file(jar_path)
                        : zip_open_mem(app_bin, (uint32_t)(app_bin_end - app_bin));
    if (!app)
        pal_fatal("Cannot open the .jar file.");
    res_add_app_zip(app);

    printf("Starting %s\n", jar_path ? jar_path : "embedded MIDlet");
    printf("Java heap: %u KB\n", (unsigned)(opt.heap_size >> 10));
    nds_session_start(s, jar_path);

    const char *boot_args[] = {"-midlet", midlet_class ? midlet_class : ""};
    if (!vm_boot(boot_args, 2))
        pal_fatal("The VM failed to start.");
    vm_poll_hook = midp_poll;
    int code = vm_run();
    if (vm.uncaught)
        code = 1;
    vm_shutdown();
    midp_shutdown();
    return code;
}
