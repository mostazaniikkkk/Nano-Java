#include <nds.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 
 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_R32() { 
    int adr = KNI_GetParameterAsInt(1);
    u32 *ptr = (u32 *)adr;
    KNI_ReturnInt(*ptr);
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_W32() { 
    int adr = KNI_GetParameterAsInt(1);
    u32 data = KNI_GetParameterAsInt(2);
    u32 *ptr = (u32 *)adr;
    *ptr = data;
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_R16() { 
    int adr = KNI_GetParameterAsInt(1);
    u16 *ptr = (u16 *)adr;
    KNI_ReturnInt(*ptr);
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_W16() { 
    int adr = KNI_GetParameterAsInt(1);
    u16 data = KNI_GetParameterAsInt(2);
    u16 *ptr = (u16 *)adr;
    *ptr = data;
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_R8() { 
    int adr = KNI_GetParameterAsInt(1);
    u8 *ptr = (u8 *)adr;
    KNI_ReturnInt(*ptr);
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_W8() { 
    int adr = KNI_GetParameterAsInt(1);
    u8 data = KNI_GetParameterAsInt(2);
    u8 *ptr = (u8 *)adr;
    *ptr = data;
}


KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_WArrShort() { 
    jsize arrSize;
    KNI_StartHandles(1); 
    KNI_DeclareHandle(arrHandle); 

    int adr = KNI_GetParameterAsInt(1);

    KNI_GetParameterAsObject(2, arrHandle);
    arrSize = KNI_GetArrayLength(arrHandle);
    jbyte *ptr = (jbyte *)adr;

    KNI_GetRawArrayRegion(arrHandle, 0, arrSize * 2, ptr);
    //iprintf("\x1b[19B %i",arrSize );


    KNI_EndHandles(); 
    KNI_ReturnVoid(); 


}


KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Memory_setClasspath() { 
    char buffer[64];
    jsize arrSize;
    KNI_StartHandles(1); 
    KNI_DeclareHandle(arrHandle); 

    KNI_GetParameterAsObject(1, arrHandle);
    arrSize = KNI_GetArrayLength(arrHandle);

    KNI_EndHandles(); 
    KNI_ReturnVoid(); 


}

