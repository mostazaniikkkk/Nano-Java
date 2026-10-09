/*
 * nds_platform.h - shared declarations of the Nintendo DS platform layer.
 */
#ifndef NJ_NDS_PLATFORM_H
#define NJ_NDS_PLATFORM_H

#include <stdbool.h>
#include <stdint.h>

/* ---- Per-game settings (settings.c) ---- */

enum { BTN_A, BTN_B, BTN_X, BTN_Y, BTN_L, BTN_R, BTN_START, BTN_SELECT,
       BTN_UP, BTN_DOWN, BTN_LEFT, BTN_RIGHT, NUM_BUTTONS };

enum { LAYOUT_FIT, LAYOUT_SPAN, LAYOUT_TOUCH, NUM_LAYOUTS };

/* Pseudo key code that opens the emulator menu instead of reaching Java. */
#define KEY_MENU 0x10000

typedef struct PhoneKey {
    const char *id;      /* name in the .ini */
    const char *label;   /* name shown in the menu */
    int         code;    /* MIDP key code (0 = none) */
} PhoneKey;

typedef struct GameSettings {
    int  w, h;                /* phone screen size */
    int  layout;              /* LAYOUT_FIT, LAYOUT_SPAN or LAYOUT_TOUCH */
    int  volume;              /* 0-100 */
    bool show_fps;
    int  keys[NUM_BUTTONS];   /* MIDP key code per physical button */
} GameSettings;

extern const char *const button_names[NUM_BUTTONS];
extern const char *const layout_names[NUM_LAYOUTS];   /* "fit", ... */
extern const PhoneKey    phone_keys[];
extern const int         num_phone_keys;

const PhoneKey *phone_key_by_code(int code);
void settings_defaults(GameSettings *s);
void settings_load(const char *jar, GameSettings *s);   /* jar may be NULL */
void settings_save(const char *jar, const GameSettings *s);

/* ---- Platform (pal_nds.c) ---- */

/* Set once the SD card is mounted; without it nothing is saved. */
extern bool nds_storage_ok;

/* Text console on the bottom screen, used by the launcher and for errors.
 * While it is active, console output is also printed there. */
void nds_console_start(void);

/* Prints the tail of the log on the console. */
void nds_show_log(int lines);

/* Starts a game's video and input with these settings. jar is the game's
 * path, for saving settings from the menu (NULL for embedded games). */
void nds_session_start(GameSettings *s, const char *jar);

/* audio_nds.c: overall J2ME volume (0-100), from the settings. */
void nds_audio_master(int volume);

/* What the emulator menu asked for when the VM stopped. */
enum { EXIT_NONE, EXIT_RESTART_GAME, EXIT_CHANGE_GAME };
extern int nds_exit_action;

/* ---- Touch screen UI (ui.c) ---- */

/* Draws the keypad on the bottom screen from row top down (0 = the whole
 * screen gets the full phone keypad; otherwise a compact strip). */
void ui_init(int top);
/* Handles a touch starting (down) or ending at (x, y). Returns the MIDP
 * key code pressed or released on the keypad, KEY_MENU when the menu tab
 * was tapped, or 0. *on_ui tells whether the touch hit the UI at all. */
int  ui_touch(bool down, int x, int y, bool *on_ui);
void ui_soft_labels(const char *left, const char *right);
/* Touch layout: the game is on the touch screen and the top screen shows
 * the button map, the soft key commands and the FPS. */
void ui_info_init(const GameSettings *s);
void ui_show_fps(int fps, int busy_percent);
/* Runs the emulator menu until it is closed (the game is paused meanwhile).
 * Returns an EXIT_* action. */
int  ui_menu_run(GameSettings *s, const char *jar, bool can_change_game);

/* ---- Launcher (launcher.c) ---- */

/* Lets the user pick a .jar/.jad; returns its malloc'd path. When the
 * jar holds several MIDlets, *midlet_class gets the chosen one's class
 * (malloc'd), otherwise NULL (the first MIDlet runs). */
char *nds_launcher_pick(char **midlet_class);

/* The jar a .jad points to (MIDlet-Jar-URL, next to the .jad), or the
 * .jad's name with a .jar extension. Returns a malloc'd path. */
char *nds_jad_jar_path(const char *jad_path, const char *jad_text);

/* ---- VM glue (vm_start.c, kept apart from libnds code because calico's
 * Thread type clashes with the VM's) ---- */

void heap_set_stack_base(void *base);   /* from the VM's heap.c */
void nds_set_jad(const uint8_t *data, uint32_t len);
/* Runs the MIDlet in jar_path, or the one embedded in the ROM if NULL, and
 * shuts the VM down afterwards; returns the exit status. */
int  nds_run_vm(const char *jar_path, const char *midlet_class, GameSettings *s);
bool nds_has_embedded_app(void);
void nds_stop_vm(void);

#endif
