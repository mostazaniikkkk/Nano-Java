#include <nds.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 
 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Sprite_update() { 
    /* Declare handles */ 
    KNI_StartHandles(2); 
    KNI_DeclareHandle(objectHandle); 
    KNI_DeclareHandle(classHandle); 

    /* Get ?this? pointer */ 
    KNI_GetThisPointer(objectHandle); 

    /* Get instance?s class */ 
    KNI_GetObjectClass(objectHandle, classHandle); 

    /* Get field id and value */ 
    jfieldID fid = KNI_GetFieldID(classHandle, "entry", "B"); 
    jbyte entry = KNI_GetByteField(objectHandle, fid); 

    fid = KNI_GetFieldID(classHandle, "x", "C"); 
    jchar x = KNI_GetCharField(objectHandle, fid); 

    fid = KNI_GetFieldID(classHandle, "y", "C"); 
    jchar y = KNI_GetCharField(objectHandle, fid); 

    fid = KNI_GetFieldID(classHandle, "attr0", "C"); 
    jchar attr0 = KNI_GetCharField(objectHandle, fid); 

    fid = KNI_GetFieldID(classHandle, "attr1", "C"); 
    jchar attr1 = KNI_GetCharField(objectHandle, fid); 

    fid = KNI_GetFieldID(classHandle, "attr2", "C"); 
    jchar attr2 = KNI_GetCharField(objectHandle, fid); 

    attr0 = (attr0 & 0xFE00) | y; 
    attr1 = (attr1 & 0xFF00) | x; 

    OAM[entry*4    ] = attr0;
    OAM[entry*4 + 1] = attr1;
    OAM[entry*4 + 2] = attr2;

    KNI_EndHandles(); 
    KNI_ReturnVoid(); 
} 
