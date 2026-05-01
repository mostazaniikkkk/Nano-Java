/* This is a generated file.  Do not modify.
 * Generated on Wed Dec 31 16:31:22 CET 2003
 */


#include <global.h>

#if !ROMIZING
extern void Java_java_lang_Object_getClass(void);
extern void Java_java_lang_Object_hashCode(void);
extern void Java_java_lang_Object_notify(void);
extern void Java_java_lang_Object_notifyAll(void);
extern void Java_java_lang_Object_wait(void);
extern void Java_java_lang_Throwable_printStackTrace0(void);
extern void Java_java_lang_System_currentTimeMillis(void);
extern void Java_java_lang_System_arraycopy(void);
extern void Java_java_lang_System_identityHashCode(void);
extern void Java_java_lang_System_getProperty0(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_open(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_close(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_size(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_read(void);
extern void Java_com_sun_cldc_io_ResourceInputStream_readBytes(void);
extern void Java_java_lang_Double_doubleToLongBits(void);
extern void Java_java_lang_Double_longBitsToDouble(void);
extern void Java_java_lang_Float_floatToIntBits(void);
extern void Java_java_lang_Float_intBitsToFloat(void);
extern void Java_java_lang_Class_forName(void);
extern void Java_java_lang_Class_newInstance(void);
extern void Java_java_lang_Class_isInstance(void);
extern void Java_java_lang_Class_isAssignableFrom(void);
extern void Java_java_lang_Class_isInterface(void);
extern void Java_java_lang_Class_isArray(void);
extern void Java_java_lang_Class_getName(void);
extern void Java_java_lang_StringBuffer_append__Ljava_lang_String_2(void);
extern void Java_java_lang_StringBuffer_append__I(void);
extern void Java_java_lang_StringBuffer_toString(void);
extern void Java_java_lang_Thread_currentThread(void);
extern void Java_java_lang_Thread_yield(void);
extern void Java_java_lang_Thread_sleep(void);
extern void Java_java_lang_Thread_start(void);
extern void Java_java_lang_Thread_isAlive(void);
extern void Java_java_lang_Thread_activeCount(void);
extern void Java_java_lang_Thread_setPriority0(void);
extern void Java_java_lang_Thread_interrupt0(void);
extern void Java_com_sun_cldc_io_Waiter_waitForIO(void);
extern void Java_java_lang_Runtime_exitInternal(void);
extern void Java_java_lang_Runtime_freeMemory(void);
extern void Java_java_lang_Runtime_totalMemory(void);
extern void Java_java_lang_Runtime_gc(void);
extern void Java_java_lang_Math_sin(void);
extern void Java_java_lang_Math_cos(void);
extern void Java_java_lang_Math_tan(void);
extern void Java_java_lang_Math_sqrt(void);
extern void Java_java_lang_Math_ceil(void);
extern void Java_java_lang_Math_floor(void);
extern void Java_com_sun_cldc_io_ConsoleOutputStream_write(void);
extern void Java_java_lang_String_charAt(void);
extern void Java_java_lang_String_equals(void);
extern void Java_java_lang_String_indexOf__I(void);
extern void Java_java_lang_String_indexOf__II(void);
extern void Java_java_lang_String_intern(void);
extern void Java_java_lang_ref_WeakReference_initializeWeakReference(void);
extern void Java_nds_pstros_video_NDSFont_getSystemFontData(void);


const NativeImplementationType java_lang_Object_natives[] = {
    { "getClass",            NULL, Java_java_lang_Object_getClass},
    { "hashCode",            NULL, Java_java_lang_Object_hashCode},
    { "notify",              NULL, Java_java_lang_Object_notify},
    { "notifyAll",           NULL, Java_java_lang_Object_notifyAll},
    { "wait",                NULL, Java_java_lang_Object_wait},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Throwable_natives[] = {
    { "printStackTrace0",    NULL, Java_java_lang_Throwable_printStackTrace0},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_System_natives[] = {
    { "currentTimeMillis",   NULL, Java_java_lang_System_currentTimeMillis},
    { "arraycopy",           NULL, Java_java_lang_System_arraycopy},
    { "identityHashCode",    NULL, Java_java_lang_System_identityHashCode},
    { "getProperty0",        NULL, Java_java_lang_System_getProperty0},
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

const NativeImplementationType java_lang_Double_natives[] = {
    { "doubleToLongBits",    NULL, Java_java_lang_Double_doubleToLongBits},
    { "longBitsToDouble",    NULL, Java_java_lang_Double_longBitsToDouble},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Float_natives[] = {
    { "floatToIntBits",      NULL, Java_java_lang_Float_floatToIntBits},
    { "intBitsToFloat",      NULL, Java_java_lang_Float_intBitsToFloat},
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

const NativeImplementationType java_lang_StringBuffer_natives[] = {
    { "append",              "(Ljava/lang/String;)Ljava/lang/StringBuffer;", Java_java_lang_StringBuffer_append__Ljava_lang_String_2},
    { "append",              "(I)Ljava/lang/StringBuffer;", Java_java_lang_StringBuffer_append__I},
    { "toString",            NULL, Java_java_lang_StringBuffer_toString},
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

const NativeImplementationType com_sun_cldc_io_Waiter_natives[] = {
    { "waitForIO",           NULL, Java_com_sun_cldc_io_Waiter_waitForIO},
    NATIVE_END_OF_LIST
};

const NativeImplementationType java_lang_Runtime_natives[] = {
    { "exitInternal",        NULL, Java_java_lang_Runtime_exitInternal},
    { "freeMemory",          NULL, Java_java_lang_Runtime_freeMemory},
    { "totalMemory",         NULL, Java_java_lang_Runtime_totalMemory},
    { "gc",                  NULL, Java_java_lang_Runtime_gc},
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

const NativeImplementationType com_sun_cldc_io_ConsoleOutputStream_natives[] = {
    { "write",               NULL, Java_com_sun_cldc_io_ConsoleOutputStream_write},
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

const NativeImplementationType java_lang_ref_WeakReference_natives[] = {
    { "initializeWeakReference", NULL, Java_java_lang_ref_WeakReference_initializeWeakReference},
    NATIVE_END_OF_LIST
};

const NativeImplementationType nds_pstros_video_NDSFont_natives[] = {
    { "getSystemFontData",   NULL, Java_nds_pstros_video_NDSFont_getSystemFontData},
    NATIVE_END_OF_LIST
};

const ClassNativeImplementationType nativeImplementations[] = {
    { "java/lang",                "Object",                   java_lang_Object_natives },
    { "java/lang",                "Throwable",                java_lang_Throwable_natives },
    { "java/lang",                "System",                   java_lang_System_natives },
    { "com/sun/cldc/io",          "ResourceInputStream",      com_sun_cldc_io_ResourceInputStream_natives },
    { "java/lang",                "Double",                   java_lang_Double_natives },
    { "java/lang",                "Float",                    java_lang_Float_natives },
    { "java/lang",                "Class",                    java_lang_Class_natives },
    { "java/lang",                "StringBuffer",             java_lang_StringBuffer_natives },
    { "java/lang",                "Thread",                   java_lang_Thread_natives },
    { "com/sun/cldc/io",          "Waiter",                   com_sun_cldc_io_Waiter_natives },
    { "java/lang",                "Runtime",                  java_lang_Runtime_natives },
    { "java/lang",                "Math",                     java_lang_Math_natives },
    { "com/sun/cldc/io",          "ConsoleOutputStream",      com_sun_cldc_io_ConsoleOutputStream_natives },
    { "java/lang",                "String",                   java_lang_String_natives },
    { "java/lang/ref",            "WeakReference",            java_lang_ref_WeakReference_natives },
    { "nds/pstros/video",         "NDSFont",                  nds_pstros_video_NDSFont_natives },
NATIVE_END_OF_LIST
};
#endif
