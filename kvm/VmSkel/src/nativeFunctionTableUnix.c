/* This is a generated file.  Do not modify.
 * Generated on Sat Jul 26 14:34:00 CEST 2008
 */


#include <global.h>

#if !ROMIZING
extern void Java_java_lang_Object_getClass(void);
extern void Java_java_lang_Object_hashCode(void);
extern void Java_java_lang_Object_notify(void);
extern void Java_java_lang_Object_notifyAll(void);
extern void Java_java_lang_Object_wait(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_open(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_close(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_size(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_read(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_readBytes(void);
extern void Java_java_lang_Throwable_printStackTrace0(void);
extern void Java_java_lang_Thread_currentThread(void);
extern void Java_java_lang_Thread_yield(void);
extern void Java_java_lang_Thread_sleep(void);
extern void Java_java_lang_Thread_start(void);
extern void Java_java_lang_Thread_isAlive(void);
extern void Java_java_lang_Thread_activeCount(void);
extern void Java_java_lang_Thread_setPriority0(void);
extern void Java_java_lang_Thread_interrupt0(void);
extern void Java_java_lang_Class_forName(void);
extern void Java_java_lang_Class_newInstance(void);
extern void Java_java_lang_Class_isInstance(void);
extern void Java_java_lang_Class_isAssignableFrom(void);
extern void Java_java_lang_Class_isInterface(void);
extern void Java_java_lang_Class_isArray(void);
extern void Java_java_lang_Class_getName(void);
extern void Java_java_lang_System_currentTimeMillis(void);
extern void Java_java_lang_System_arraycopy(void);
extern void Java_java_lang_System_identityHashCode(void);
extern void Java_java_lang_System_getProperty0(void);
extern void Java_java_lang_String_charAt(void);
extern void Java_java_lang_String_equals(void);
extern void Java_java_lang_String_indexOf__I(void);
extern void Java_java_lang_String_indexOf__II(void);
extern void Java_java_lang_String_intern(void);
extern void Java_com_sun_cldc_io_Waiter_waitForIO(void);
extern void Java_java_lang_Double_doubleToLongBits(void);
extern void Java_java_lang_Double_longBitsToDouble(void);
extern void Java_nds_Key_scan(void);
extern void Java_nds_Key_held(void);
extern void Java_nds_Key_down(void);
extern void Java_nds_Key_downRepeat(void);
extern void Java_nds_Key_setRepeat(void);
extern void Java_nds_Key_up(void);
extern void Java_com_sun_cldc_io_ConsoleOutputStream_write(void);
extern void Java_nds_Sprite_update(void);
extern void Java_nds_Console_cls(void);
extern void Java_nds_Console_cll(void);
extern void Java_nds_Console_setpos(void);
extern void Java_nds_Console_up(void);
extern void Java_nds_Console_down(void);
extern void Java_nds_Console_left(void);
extern void Java_nds_Console_right(void);
extern void Java_nds_Console_erase(void);
extern void Java_java_lang_StringBuffer_append__Ljava_lang_String_2(void);
extern void Java_java_lang_StringBuffer_append__I(void);
extern void Java_java_lang_StringBuffer_toString(void);
extern void Java_nds_Interrupts_irqInit(void);
extern void Java_nds_Interrupts_irqSet(void);
extern void Java_nds_Interrupts_irqClear(void);
extern void Java_nds_Interrupts_irqEnable(void);
extern void Java_nds_Interrupts_irqDisable(void);
extern void Java_nds_Video_lcdSwap(void);
extern void Java_nds_Video_initVideo(void);
extern void Java_nds_Video_blit(void);
extern void Java_nds_Video_blitGray(void);
extern void Java_nds_Video_blitRGB(void);
extern void Java_nds_Video_blitRGB4444(void);
extern void Java_nds_Video_blitRGB8888(void);
extern void Java_nds_Video_getRGB(void);
extern void Java_nds_Video_fillRect(void);
extern void Java_nds_Video_drawLine(void);
extern void Java_nds_Video_fillTriangle(void);
extern void Java_nds_Video_drawArc(void);
extern void Java_nds_Video_fillArc(void);
extern void Java_nds_Video_decodePngImage(void);
extern void Java_nds_Video_vramSetMainBanks(void);
extern void Java_nds_Video_vramRestoreMainBanks(void);
extern void Java_nds_Video_vramSetBankA(void);
extern void Java_nds_Video_vramSetBankB(void);
extern void Java_nds_Video_vramSetBankC(void);
extern void Java_nds_Video_vramSetBankD(void);
extern void Java_nds_Video_vramSetBankE(void);
extern void Java_nds_Video_vramSetBankF(void);
extern void Java_nds_Video_vramSetBankG(void);
extern void Java_nds_Video_vramSetBankH(void);
extern void Java_nds_Video_vramSetBankI(void);
extern void Java_java_lang_ref_WeakReference_initializeWeakReference(void);
extern void Java_java_lang_Float_floatToIntBits(void);
extern void Java_java_lang_Float_intBitsToFloat(void);
extern void Java_nds_Bios_swiWaitForVBlank(void);
extern void Java_nds_TouchPosition_update(void);
extern void Java_java_lang_Runtime_exitInternal(void);
extern void Java_java_lang_Runtime_freeMemory(void);
extern void Java_java_lang_Runtime_totalMemory(void);
extern void Java_java_lang_Runtime_gc(void);
extern void Java_nds_File_exists(void);
extern void Java_nds_File_size(void);
extern void Java_nds_File_load(void);
extern void Java_nds_File_save(void);
extern void Java_nds_File_loadToVRAM(void);
extern void Java_java_lang_Math_sin(void);
extern void Java_java_lang_Math_cos(void);
extern void Java_java_lang_Math_tan(void);
extern void Java_java_lang_Math_sqrt(void);
extern void Java_java_lang_Math_ceil(void);
extern void Java_java_lang_Math_floor(void);
extern void Java_nds_PersonalData_populate(void);
extern void Java_nds_Memory_R32(void);
extern void Java_nds_Memory_W32(void);
extern void Java_nds_Memory_R16(void);
extern void Java_nds_Memory_W16(void);
extern void Java_nds_Memory_R8(void);
extern void Java_nds_Memory_W8(void);
extern void Java_nds_Memory_WArrShort(void);


const NativeImplementationType java_lang_Object_natives[] = {
    { "getClass",            NULL, Java_java_lang_Object_getClass},
    { "hashCode",            NULL, Java_java_lang_Object_hashCode},
    { "notify",              NULL, Java_java_lang_Object_notify},
    { "notifyAll",           NULL, Java_java_lang_Object_notifyAll},
    { "wait",                NULL, Java_java_lang_Object_wait},
    NATIVE_END_OF_LIST
};

const NativeImplementationType com_sun_cldc_io_ResourceInputStream_natives[] = {
    { "open",                NULL, Java_com_sun_cldc_io_ResourceInputStream_open},
    { "close",               NULL, Java_com_sun_cldc_io_ResourceInputStream_close},
    { "size",                NULL, Java_com_sun_cldc_io_ResourceInputStream_size},
    { "read",                NULL, Java_com_sun_cldc_io_ResourceInputStream_read},
    { "readBytes",           NULL, Java_com_sun_cldc_io_ResourceInputStream_readBytes},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Throwable_natives[] = {
    { "printStackTrace0",    NULL, Java_java_lang_Throwable_printStackTrace0},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Thread_natives[] = {
    { "currentThread",       NULL, Java_java_lang_Thread_currentThread},
    { "yield",               NULL, Java_java_lang_Thread_yield},
    { "sleep",               NULL, Java_java_lang_Thread_sleep},
    { "start",               NULL, Java_java_lang_Thread_start},
    { "isAlive",             NULL, Java_java_lang_Thread_isAlive},
    { "activeCount",         NULL, Java_java_lang_Thread_activeCount},
    { "setPriority0",        NULL, Java_java_lang_Thread_setPriority0},
    { "interrupt0",          NULL, Java_java_lang_Thread_interrupt0},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Class_natives[] = {
    { "forName",             NULL, Java_java_lang_Class_forName},
    { "newInstance",         NULL, Java_java_lang_Class_newInstance},
    { "isInstance",          NULL, Java_java_lang_Class_isInstance},
    { "isAssignableFrom",    NULL, Java_java_lang_Class_isAssignableFrom},
    { "isInterface",         NULL, Java_java_lang_Class_isInterface},
    { "isArray",             NULL, Java_java_lang_Class_isArray},
    { "getName",             NULL, Java_java_lang_Class_getName},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_System_natives[] = {
    { "currentTimeMillis",   NULL, Java_java_lang_System_currentTimeMillis},
    { "arraycopy",           NULL, Java_java_lang_System_arraycopy},
    { "identityHashCode",    NULL, Java_java_lang_System_identityHashCode},
    { "getProperty0",        NULL, Java_java_lang_System_getProperty0},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_String_natives[] = {
    { "charAt",              NULL, Java_java_lang_String_charAt},
    { "equals",              NULL, Java_java_lang_String_equals},
    { "indexOf",             "(I)I", Java_java_lang_String_indexOf__I},
    { "indexOf",             "(II)I", Java_java_lang_String_indexOf__II},
    { "intern",              NULL, Java_java_lang_String_intern},
    NATIVE_END_OF_LIST
};

const NativeImplementationType com_sun_cldc_io_Waiter_natives[] = {
    { "waitForIO",           NULL, Java_com_sun_cldc_io_Waiter_waitForIO},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Double_natives[] = {
    { "doubleToLongBits",    NULL, Java_java_lang_Double_doubleToLongBits},
    { "longBitsToDouble",    NULL, Java_java_lang_Double_longBitsToDouble},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Key_natives[] = {
    { "scan",                NULL, Java_nds_Key_scan},
    { "held",                NULL, Java_nds_Key_held},
    { "down",                NULL, Java_nds_Key_down},
    { "downRepeat",          NULL, Java_nds_Key_downRepeat},
    { "setRepeat",           NULL, Java_nds_Key_setRepeat},
    { "up",                  NULL, Java_nds_Key_up},
    NATIVE_END_OF_LIST
};

const NativeImplementationType com_sun_cldc_io_ConsoleOutputStream_natives[] = {
    { "write",               NULL, Java_com_sun_cldc_io_ConsoleOutputStream_write},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Sprite_natives[] = {
    { "update",              NULL, Java_nds_Sprite_update},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Console_natives[] = {
    { "cls",                 NULL, Java_nds_Console_cls},
    { "cll",                 NULL, Java_nds_Console_cll},
    { "setpos",              NULL, Java_nds_Console_setpos},
    { "up",                  NULL, Java_nds_Console_up},
    { "down",                NULL, Java_nds_Console_down},
    { "left",                NULL, Java_nds_Console_left},
    { "right",               NULL, Java_nds_Console_right},
    { "erase",               NULL, Java_nds_Console_erase},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_StringBuffer_natives[] = {
    { "append",              "(Ljava/lang/String;)Ljava/lang/StringBuffer;", Java_java_lang_StringBuffer_append__Ljava_lang_String_2},
    { "append",              "(I)Ljava/lang/StringBuffer;", Java_java_lang_StringBuffer_append__I},
    { "toString",            NULL, Java_java_lang_StringBuffer_toString},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Interrupts_natives[] = {
    { "irqInit",             NULL, Java_nds_Interrupts_irqInit},
    { "irqSet",              NULL, Java_nds_Interrupts_irqSet},
    { "irqClear",            NULL, Java_nds_Interrupts_irqClear},
    { "irqEnable",           NULL, Java_nds_Interrupts_irqEnable},
    { "irqDisable",          NULL, Java_nds_Interrupts_irqDisable},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Video_natives[] = {
    { "lcdSwap",             NULL, Java_nds_Video_lcdSwap},
    { "initVideo",           NULL, Java_nds_Video_initVideo},
    { "blit",                NULL, Java_nds_Video_blit},
    { "blitGray",            NULL, Java_nds_Video_blitGray},
    { "blitRGB",             NULL, Java_nds_Video_blitRGB},
    { "blitRGB4444",         NULL, Java_nds_Video_blitRGB4444},
    { "blitRGB8888",         NULL, Java_nds_Video_blitRGB8888},
    { "getRGB",              NULL, Java_nds_Video_getRGB},
    { "fillRect",            NULL, Java_nds_Video_fillRect},
    { "drawLine",            NULL, Java_nds_Video_drawLine},
    { "fillTriangle",        NULL, Java_nds_Video_fillTriangle},
    { "drawArc",             NULL, Java_nds_Video_drawArc},
    { "fillArc",             NULL, Java_nds_Video_fillArc},
    { "decodePngImage",      NULL, Java_nds_Video_decodePngImage},
    { "vramSetMainBanks",    NULL, Java_nds_Video_vramSetMainBanks},
    { "vramRestoreMainBanks", NULL, Java_nds_Video_vramRestoreMainBanks},
    { "vramSetBankA",        NULL, Java_nds_Video_vramSetBankA},
    { "vramSetBankB",        NULL, Java_nds_Video_vramSetBankB},
    { "vramSetBankC",        NULL, Java_nds_Video_vramSetBankC},
    { "vramSetBankD",        NULL, Java_nds_Video_vramSetBankD},
    { "vramSetBankE",        NULL, Java_nds_Video_vramSetBankE},
    { "vramSetBankF",        NULL, Java_nds_Video_vramSetBankF},
    { "vramSetBankG",        NULL, Java_nds_Video_vramSetBankG},
    { "vramSetBankH",        NULL, Java_nds_Video_vramSetBankH},
    { "vramSetBankI",        NULL, Java_nds_Video_vramSetBankI},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_ref_WeakReference_natives[] = {
    { "initializeWeakReference", NULL, Java_java_lang_ref_WeakReference_initializeWeakReference},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Float_natives[] = {
    { "floatToIntBits",      NULL, Java_java_lang_Float_floatToIntBits},
    { "intBitsToFloat",      NULL, Java_java_lang_Float_intBitsToFloat},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Bios_natives[] = {
    { "swiWaitForVBlank",    NULL, Java_nds_Bios_swiWaitForVBlank},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_TouchPosition_natives[] = {
    { "update",              NULL, Java_nds_TouchPosition_update},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Runtime_natives[] = {
    { "exitInternal",        NULL, Java_java_lang_Runtime_exitInternal},
    { "freeMemory",          NULL, Java_java_lang_Runtime_freeMemory},
    { "totalMemory",         NULL, Java_java_lang_Runtime_totalMemory},
    { "gc",                  NULL, Java_java_lang_Runtime_gc},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_File_natives[] = {
    { "exists",              NULL, Java_nds_File_exists},
    { "size",                NULL, Java_nds_File_size},
    { "load",                NULL, Java_nds_File_load},
    { "save",                NULL, Java_nds_File_save},
    { "loadToVRAM",          NULL, Java_nds_File_loadToVRAM},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Math_natives[] = {
    { "sin",                 NULL, Java_java_lang_Math_sin},
    { "cos",                 NULL, Java_java_lang_Math_cos},
    { "tan",                 NULL, Java_java_lang_Math_tan},
    { "sqrt",                NULL, Java_java_lang_Math_sqrt},
    { "ceil",                NULL, Java_java_lang_Math_ceil},
    { "floor",               NULL, Java_java_lang_Math_floor},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_PersonalData_natives[] = {
    { "populate",            NULL, Java_nds_PersonalData_populate},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_Memory_natives[] = {
    { "R32",                 NULL, Java_nds_Memory_R32},
    { "W32",                 NULL, Java_nds_Memory_W32},
    { "R16",                 NULL, Java_nds_Memory_R16},
    { "W16",                 NULL, Java_nds_Memory_W16},
    { "R8",                  NULL, Java_nds_Memory_R8},
    { "W8",                  NULL, Java_nds_Memory_W8},
    { "WArrShort",           NULL, Java_nds_Memory_WArrShort},
    NATIVE_END_OF_LIST
};

const ClassNativeImplementationType nativeImplementations[] = {
    { "java/lang",                "Object",                   java_lang_Object_natives },
    { "com/sun/cldc/io",          "ResourceInputStream",      com_sun_cldc_io_ResourceInputStream_natives },
    { "java/lang",                "Throwable",                java_lang_Throwable_natives },
    { "java/lang",                "Thread",                   java_lang_Thread_natives },
    { "java/lang",                "Class",                    java_lang_Class_natives },
    { "java/lang",                "System",                   java_lang_System_natives },
    { "java/lang",                "String",                   java_lang_String_natives },
    { "com/sun/cldc/io",          "Waiter",                   com_sun_cldc_io_Waiter_natives },
    { "java/lang",                "Double",                   java_lang_Double_natives },
    { "nds",                      "Key",                      nds_Key_natives },
    { "com/sun/cldc/io",          "ConsoleOutputStream",      com_sun_cldc_io_ConsoleOutputStream_natives },
    { "nds",                      "Sprite",                   nds_Sprite_natives },
    { "nds",                      "Console",                  nds_Console_natives },
    { "java/lang",                "StringBuffer",             java_lang_StringBuffer_natives },
    { "nds",                      "Interrupts",               nds_Interrupts_natives },
    { "nds",                      "Video",                    nds_Video_natives },
    { "java/lang/ref",            "WeakReference",            java_lang_ref_WeakReference_natives },
    { "java/lang",                "Float",                    java_lang_Float_natives },
    { "nds",                      "Bios",                     nds_Bios_natives },
    { "nds",                      "TouchPosition",            nds_TouchPosition_natives },
    { "java/lang",                "Runtime",                  java_lang_Runtime_natives },
    { "nds",                      "File",                     nds_File_natives },
    { "java/lang",                "Math",                     java_lang_Math_natives },
    { "nds",                      "PersonalData",             nds_PersonalData_natives },
    { "nds",                      "Memory",                   nds_Memory_natives },
NATIVE_END_OF_LIST
};
#endif
