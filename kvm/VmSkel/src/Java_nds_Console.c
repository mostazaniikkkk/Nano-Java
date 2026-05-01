#include <nds.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 


KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_cls() { 
    printf("\x1b[2J");
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_cll() { 
    printf("\x1b[2J");
    printf("\x1b[0;0H");
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_setpos() { 
    int x = KNI_GetParameterAsInt(1);
    int y = KNI_GetParameterAsInt(2);
    printf("\x1b[%d;%dH", y, x);
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_up() { 
    printf("\x1b[10A");
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_down() { 
    printf("\x1b[19B");
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_left() { 
    printf("\x1b[28D");
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_right() { 
    printf("\x1b[5C");
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Console_erase() { 
    printf("\x1b[2J");
    KNI_ReturnVoid(); 
} 

 
