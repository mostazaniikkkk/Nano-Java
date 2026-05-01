/*
 * nds_main.c
 * NDS entry point for the Nano Java / KVM MIDP runtime.
 *
 * DevkitARM programs use a standard main(); libnds handles the CRT startup.
 * The MIDlet class name is read from "midlet.conf" on the FAT filesystem,
 * or can be baked in at compile time via -DPSTROS_MIDLET_CLASS="com/foo/Bar".
 *
 * Build chain:
 *   arm-none-eabi-gcc ... nds_main.c  ->  nanojava.nds
 */

#include <nds.h>
#include <fat.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* KVM portable startup (VmCommon/src/StartJVM.c) */
extern int StartJVM(int argc, char *argv[]);

/* Set by loaderFile.c; must point to the class search path before StartJVM */
extern char *UserClassPath;

/* Heap size requested by the VM (global.c); every platform main must set this */
extern long RequestedHeapSize;

/* Default MIDlet class if not overridden at compile time */
#ifndef PSTROS_MIDLET_CLASS
#define PSTROS_MIDLET_CLASS "HelloMIDlet"
#endif

#define CONF_FILE "fat:/midlet.conf"
#define MAX_CLASSNAME 256
#define HEAP_SIZE_STR  "2097152"   /* 2 MB */

/*
 * Read midlet.conf. Two formats:
 *   Single line:  HelloMIDlet          (class on FAT root)
 *   Two lines:    sonic/app.jar        (JAR path relative to fat:/)
 *                 GloftSOUN            (MIDlet class name)
 *
 * On return, buf holds the class name and jarPath (if non-NULL) holds
 * "fat:/<first-line>" when the first line ends in ".jar".
 * Returns 1 on success, 0 if file missing/empty.
 */
static int read_midlet_conf(char *buf, size_t bufsz, char *jarPath, size_t jarSz)
{
    FILE *f = fopen(CONF_FILE, "r");
    if (!f) return 0;

    char line1[256] = {0};
    char line2[256] = {0};
    if (!fgets(line1, sizeof(line1), f)) { fclose(f); return 0; }
    fgets(line2, sizeof(line2), f);
    fclose(f);

    /* Strip trailing newline from both */
    size_t n1 = strlen(line1);
    while (n1 > 0 && (line1[n1-1] == '\n' || line1[n1-1] == '\r')) line1[--n1] = '\0';
    size_t n2 = strlen(line2);
    while (n2 > 0 && (line2[n2-1] == '\n' || line2[n2-1] == '\r')) line2[--n2] = '\0';

    /* Detect JAR: first line ends with ".jar" */
    if (n1 > 4 && strcmp(line1 + n1 - 4, ".jar") == 0) {
        /* Use relative path — chdir("fat:/") already done, and ":" is PATH_SEPARATOR */
        snprintf(jarPath, jarSz, "%s", line1);
        strncpy(buf, line2, bufsz - 1);
        buf[bufsz - 1] = '\0';
        return n2 > 0;
    }

    /* Two-line format: line1=classpath dir, line2="main:ClassName" or MIDlet class */
    if (n2 > 0) {
        snprintf(jarPath, jarSz, "%s", line1);  /* reuse jarPath as classpath dir */
        strncpy(buf, line2, bufsz - 1);
        buf[bufsz - 1] = '\0';
        return 1;
    }

    /* Single line: plain MIDlet class name, classpath = "." */
    strncpy(buf, line1, bufsz - 1);
    buf[bufsz - 1] = '\0';
    return n1 > 0;
}

int main(void)
{
    char midletClass[MAX_CLASSNAME] = PSTROS_MIDLET_CLASS;
    char jarPath[MAX_CLASSNAME + 8] = {0};
    static char classPathBuf[MAX_CLASSNAME + 8];

    /* Basic NDS hardware init */
    defaultExceptionHandler();
    consoleDemoInit();   /* sub-screen text console for debug output */

    printf("Nano Java - KVM starting\n");

    if (!fatInitDefault()) {
        printf("FAT init failed - using built-in class\n");
    } else {
        chdir("fat:/");
        read_midlet_conf(midletClass, sizeof(midletClass), jarPath, sizeof(jarPath));
    }

    /* Every KVM platform must set this before StartJVM; default is 0 which
     * gives a zero-byte heap and crashes silently in collector.c */
    RequestedHeapSize = 2048 * 1024;  /* matches DEFAULTHEAPSIZE in machine_md.h */

    /* If a JAR was specified, load classes from it; otherwise use FAT root */
    if (jarPath[0] != '\0') {
        snprintf(classPathBuf, sizeof(classPathBuf), "%s", jarPath);
        printf("JAR: fat:/%s\n", classPathBuf);
    } else {
        snprintf(classPathBuf, sizeof(classPathBuf), ".");
    }
    UserClassPath = classPathBuf;

    char *argv[3];
    int argc;

    /* "main:ClassName" → run that class directly via StartJVM (no MainApp) */
    if (strncmp(midletClass, "main:", 5) == 0) {
        char *mainClass = midletClass + 5;
        printf("main(): %s\n", mainClass);
        argv[0] = mainClass;
        argv[1] = NULL;
        argc = 1;
    } else {
        /* MIDlet mode: launch via MainApp with -C<ClassName> */
        char launchArg[MAX_CLASSNAME + 2];
        launchArg[0] = '-'; launchArg[1] = 'C';
        strncpy(launchArg + 2, midletClass, MAX_CLASSNAME - 1);
        launchArg[MAX_CLASSNAME + 1] = '\0';
        printf("MIDlet: %s\n", midletClass);
        argv[0] = "nds/pstros/MainApp";
        argv[1] = launchArg;
        argv[2] = NULL;
        argc = 2;
    }

    int result = StartJVM(argc, argv);

    /* KVM exited — show result and wait for button press before halting */
    printf("\nVM exited: %d\n", result);
    printf("Press START to exit.\n");

    while (1) {
        swiWaitForVBlank();
        scanKeys();
        if (keysDown() & KEY_START) break;
    }

    return result;
}
