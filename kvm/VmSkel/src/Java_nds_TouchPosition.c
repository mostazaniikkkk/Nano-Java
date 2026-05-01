#include <nds.h>
#include <nds/arm9/input.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 

static touchPosition pos;
 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_TouchPosition_update() { 
    touchRead(&pos);

    /* Declare handles */ 
    KNI_StartHandles(2); 
    KNI_DeclareHandle(objectHandle); 
    KNI_DeclareHandle(classHandle); 

    /* Get .this. pointer */ 
    KNI_GetThisPointer(objectHandle); 

    /* Get instance.s class */ 
    KNI_GetObjectClass(objectHandle, classHandle); 

    /* Get field id and value */ 
    jfieldID fid;
    jint value;

    fid = KNI_GetFieldID(classHandle, "x", "I");
    KNI_SetIntField(objectHandle, fid, pos.px);
    fid = KNI_GetFieldID(classHandle, "y", "I");
    KNI_SetIntField(objectHandle, fid, pos.py);
    fid = KNI_GetFieldID(classHandle, "px", "I"); 
    KNI_SetIntField(objectHandle, fid, pos.px); 
    fid = KNI_GetFieldID(classHandle, "py", "I"); 
    KNI_SetIntField(objectHandle, fid, pos.py); 
    fid = KNI_GetFieldID(classHandle, "z1", "I"); 
    KNI_SetIntField(objectHandle, fid, pos.z1); 
    fid = KNI_GetFieldID(classHandle, "z2", "I"); 
    KNI_SetIntField(objectHandle, fid, pos.z2); 

    KNI_EndHandles(); 
    KNI_ReturnVoid(); 
} 
