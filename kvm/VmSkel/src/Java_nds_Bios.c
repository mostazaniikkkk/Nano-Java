#include <nds.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 
 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Bios_swiWaitForVBlank() { 
    swiWaitForVBlank();
    KNI_ReturnVoid(); 
} 
