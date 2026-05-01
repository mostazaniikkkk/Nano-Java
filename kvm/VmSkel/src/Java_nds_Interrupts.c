#include <nds.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Interrupts_irqInit() { 
    irqInit();
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Interrupts_irqSet() { 
    irqSet(IRQ_VBLANK, 0);
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Interrupts_irqClear() { 
    irqClear(IRQ_VBLANK);
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Interrupts_irqEnable() { 
    irqEnable(IRQ_VBLANK);
    KNI_ReturnVoid(); 
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_Interrupts_irqDisable() { 
    irqDisable(IRQ_VBLANK);
    KNI_ReturnVoid(); 
} 

 
