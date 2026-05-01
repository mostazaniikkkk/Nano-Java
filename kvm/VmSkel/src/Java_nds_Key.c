#include <nds.h>
#include <nds/arm9/input.h>
#include <kni.h> 
#include <stdio.h> 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Key_scan() { 
    scanKeys();
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Key_held() { 
    KNI_ReturnInt(keysHeld()); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Key_down() { 
    KNI_ReturnInt(keysDown()); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Key_downRepeat() { 
    KNI_ReturnInt(keysDownRepeat()); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Key_up() { 
    KNI_ReturnInt(keysUp()); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Key_setRepeat() { 
    jint delay = KNI_GetParameterAsInt(1);
    jint repeat = KNI_GetParameterAsInt(2);

    keysSetRepeat(delay, repeat);

    KNI_ReturnVoid(); 
} 

 
