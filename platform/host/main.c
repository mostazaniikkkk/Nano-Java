/*
 * main.c - command line front end of the host build.
 *
 *   nanojava [options] game.jar [MIDletClass]
 *   nanojava [options] -cp <dir|jar> -main <Class> [args...]
 */
#include "host.h"
#include "../../src/vm/vm.h"
#include "../../src/util/zip.h"
#include "../../src/midp/midp.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int64_t     run_ms = -1;
static int         runs = 1;
static const char *screenshot;
static const char *audio_out;
static int64_t     shot_every = -1;     /* --shot-every: periodic screenshots */
static int64_t     next_shot;
static int         shot_index;

static void usage(void)
{
    fprintf(stderr,
        "usage: nanojava [options] game.jar [MIDletClass]\n"
        "       nanojava [options] -cp <dir|jar> -main <Class> [args...]\n"
        "options:\n"
        "  --classlib PATH    class library jar (default: $NANOJAVA_CLASSLIB\n"
        "                     or build/classlib.jar)\n"
        "  --heap MB          Java heap size (default 8)\n"
        "  --screen WxH       canvas size (default 240x320)\n"
        "  --data DIR         record store directory (default ./rms)\n"
        "  --run-ms N         exit after N milliseconds\n"
        "  --keys SCRIPT      scripted input: ms:key[:up],... (MIDP key codes)\n"
        "  --screenshot FILE  save the last frame as a PPM image on exit\n"
        "  --audio-out FILE   record the sound to a WAV file\n"
        "  --shot-every MS    also save FILE-0.ppm, FILE-1.ppm, ... every MS ms\n"
        "  --trace-classes    log every class load\n");
    exit(2);
}

static void parse_keys(const char *s)
{
    while (*s && host.n_script < MAX_SCRIPT) {
        char *end;
        long  at = strtol(s, &end, 10);
        if (*end != ':')
            usage();
        long key = strtol(end + 1, &end, 10);
        int  type = EV_KEY_DOWN;
        if (strncmp(end, ":up", 3) == 0) {
            type = EV_KEY_UP;
            end += 3;
        }
        ScriptEvent *e = &host.script[host.n_script++];
        e->at_ms = at;
        e->ev.type = type;
        e->ev.a = (int)key;
        e->ev.b = 0;
        s = *end == ',' ? end + 1 : end;
        if (*end && *end != ',')
            usage();
    }
}

static Zip *open_zip_or_die(const char *path)
{
    Zip *z = zip_open_file(path);
    if (!z) {
        fprintf(stderr, "nanojava: cannot open %s\n", path);
        exit(2);
    }
    return z;
}

static bool ends_with(const char *s, const char *suffix)
{
    size_t a = strlen(s), b = strlen(suffix);
    return a >= b && strcmp(s + a - b, suffix) == 0;
}

static const char *jad_text;   /* contents of the .jad, if one was given */

/* Reads a .jad and returns the jar next to it (named by MIDlet-Jar-URL, or
 * the .jad's name with a .jar extension). */
static const char *load_jad_path(const char *path)
{
    FILE *f = fopen(path, "rb");
    if (!f) {
        fprintf(stderr, "nanojava: cannot open %s\n", path);
        exit(2);
    }
    static char buf[65536];
    size_t n = fread(buf, 1, sizeof buf - 1, f);
    fclose(f);
    buf[n] = 0;
    jad_text = buf;

    static char jar[1024];
    snprintf(jar, sizeof jar, "%s", path);
    char *slash = strrchr(jar, '/');
    char *base = slash ? slash + 1 : jar;
    const char *url = strstr(buf, "MIDlet-Jar-URL:");
    if (url) {
        url += strlen("MIDlet-Jar-URL:");
        while (*url == ' ' || *url == '\t')
            url++;
        size_t len = strcspn(url, "\r\n");
        const char *name = url;
        for (const char *p = url; p < url + len; p++)
            if (*p == '/')
                name = p + 1;
        len -= (size_t)(name - url);
        if (len > 0 && (size_t)(base - jar) + len < sizeof jar) {
            memcpy(base, name, len);
            base[len] = 0;
            FILE *j = fopen(jar, "rb");
            if (j) {
                fclose(j);
                return jar;
            }
        }
    }
    snprintf(jar, sizeof jar, "%.*s.jar", (int)(strlen(path) - 4), path);
    return jar;
}

static void poll_hook(void)
{
    midp_poll();
    host_audio_update();
    if (shot_every > 0 && screenshot && pal_time_ms() >= next_shot) {
        char path[512];
        snprintf(path, sizeof path, "%.*s-%d.ppm", (int)strcspn(screenshot, "."), screenshot,
                 shot_index++);
        host_write_screenshot(path);
        next_shot += shot_every;
    }
    if (run_ms >= 0 && pal_time_ms() >= run_ms)
        vm.exit_requested = true;
}

static int run_once(const VMOptions *opt, const char *classlib, const char *cp,
                    const char *main_class, const char *jar, const char *midlet,
                    int argc, char **argv, int first_arg)
{
    vm_init(opt);
    midp_init();
    res_set_system(open_zip_or_die(classlib));
    if (cp) {
        if (ends_with(cp, ".jar") || ends_with(cp, ".zip"))
            res_add_app_zip(open_zip_or_die(cp));
        else
            res_add_app_dir(cp);
    }

    const char *boot_args[64];
    int         n = 0;
    if (main_class) {
        boot_args[n++] = "-main";
        boot_args[n++] = main_class;
        for (int i = first_arg; i < argc && n < 64; i++)
            boot_args[n++] = argv[i];
    } else {
        if (jad_text)
            res_set_jad((const uint8_t *)jad_text, (uint32_t)strlen(jad_text));
        Zip *z = open_zip_or_die(jar);
        res_add_app_zip(z);
        boot_args[n++] = "-midlet";
        boot_args[n++] = midlet ? midlet : "";
    }

    if (!vm_boot(boot_args, n)) {
        fprintf(stderr, "nanojava: VM boot failed\n");
        return 1;
    }
    vm_poll_hook = poll_hook;
    return vm_run();
}

int main(int argc, char **argv)
{
    int stack_base;
    heap_set_stack_base(&stack_base);

    VMOptions opt = {8u << 20, 16384, 512, false, false, false};
    const char *classlib = getenv("NANOJAVA_CLASSLIB");
    if (!classlib)
        classlib = "build/classlib.jar";
    const char *cp = NULL, *main_class = NULL, *jar = NULL, *midlet = NULL;
    int         first_arg = argc;

    host.screen_w = 240;
    host.screen_h = 320;
    host.data_dir = "rms";

    for (int i = 1; i < argc; i++) {
        const char *a = argv[i];
        bool        has = i + 1 < argc;
        if (strcmp(a, "--classlib") == 0 && has) {
            classlib = argv[++i];
        } else if (strcmp(a, "--heap") == 0 && has) {
            opt.heap_size = (size_t)atoi(argv[++i]) << 20;
        } else if (strcmp(a, "--screen") == 0 && has) {
            if (sscanf(argv[++i], "%dx%d", &host.screen_w, &host.screen_h) != 2)
                usage();
        } else if (strcmp(a, "--data") == 0 && has) {
            host.data_dir = argv[++i];
        } else if (strcmp(a, "--run-ms") == 0 && has) {
            run_ms = atol(argv[++i]);
        } else if (strcmp(a, "--keys") == 0 && has) {
            parse_keys(argv[++i]);
        } else if (strcmp(a, "--screenshot") == 0 && has) {
            screenshot = argv[++i];
        } else if (strcmp(a, "--runs") == 0 && has) {
            runs = atoi(argv[++i]);
        } else if (strcmp(a, "--audio-out") == 0 && has) {
            audio_out = argv[++i];
        } else if (strcmp(a, "--shot-every") == 0 && has) {
            shot_every = atol(argv[++i]);
            next_shot = shot_every;
        } else if (strcmp(a, "--trace-classes") == 0) {
            opt.trace_classes = true;
        } else if (strcmp(a, "--trace-exceptions") == 0) {
            opt.trace_exceptions = true;
        } else if (strcmp(a, "--trace-audio") == 0) {
            opt.trace_audio = true;
        } else if (strcmp(a, "-cp") == 0 && has) {
            cp = argv[++i];
        } else if (strcmp(a, "-main") == 0 && has) {
            main_class = argv[++i];
            first_arg = i + 1;
            break;
        } else if (a[0] == '-') {
            usage();
        } else if (!jar) {
            jar = a;
        } else if (!midlet) {
            midlet = a;
        } else {
            usage();
        }
    }
    if (!main_class && !jar)
        usage();

    /* --runs N starts the program N times in one process, shutting the VM
     * down in between (exercises vm_shutdown, as the DS game switcher). */
    int code = 0;
    if (audio_out && !host_audio_open(audio_out)) {
        fprintf(stderr, "nanojava: cannot write %s\n", audio_out);
        return 2;
    }
    if (ends_with(jar ? jar : "", ".jad"))
        jar = load_jad_path(jar);
    for (int run = 0; run < runs; run++) {
        if (run > 0) {
            vm_shutdown();
            midp_shutdown();
            run_ms += pal_time_ms();
        }
        code = run_once(&opt, classlib, cp, main_class, jar, midlet, argc, argv, first_arg);
    }
    if (getenv("NANOJAVA_STATS")) {
        int64_t ms = pal_time_ms();
        fprintf(stderr, "nanojava: %d frames in %lld ms (%.1f fps), %lld ms idle\n",
                host.frames_presented, (long long)ms,
                ms > 0 ? host.frames_presented * 1000.0 / ms : 0.0, (long long)host.idle_ms);
    }
    host_audio_close();
    if (screenshot && !host_write_screenshot(screenshot))
        fprintf(stderr, "nanojava: no frame to save\n");
    if (code == 0 && vm.uncaught)
        code = 1;
    return code;
}
