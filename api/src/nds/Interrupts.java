package nds;


public class Interrupts {
    public static int IRQ_VBLANK                      =       1<<0;         /*!< vertical blank interrupt mask */
    public static int IRQ_HBLANK                      =       1<<1;         /*!< horizontal blank interrupt mask */
    public static int IRQ_VCOUNT                      =       1<<2;         /*!< vcount match interrupt mask */
    public static int IRQ_TIMER0                      =       1<<3;         /*!< timer 0 interrupt mask */
    public static int IRQ_TIMER1                      =       1<<4;         /*!< timer 1 interrupt mask */
    public static int IRQ_TIMER2                      =       1<<5;         /*!< timer 2 interrupt mask */
    public static int IRQ_TIMER3                      =       1<<6;         /*!< timer 3 interrupt mask */
    public static int IRQ_NETWORK                     =       1<<7;         /*!< serial interrupt mask */
    public static int IRQ_DMA0                        =       1<<8;         /*!< DMA 0 interrupt mask */
    public static int IRQ_DMA1                        =       1<<9;         /*!< DMA 1 interrupt mask */
    public static int IRQ_DMA2                        =       1<<10;        /*!< DMA 2 interrupt mask */
    public static int IRQ_DMA3                        =       1<<11;        /*!< DMA 3 interrupt mask */
    public static int IRQ_KEYS                        =       1<<12;        /*!< Keypad interrupt mask */
    public static int IRQ_CART                        =       1<<13;        /*!< GBA cartridge interrupt mask */
    public static int IRQ_IPC_SYNC            =       1<<16;        /*!< IPC sync interrupt mask */
    public static int IRQ_FIFO_EMPTY          =       1<<17;        /*!< Send FIFO empty interrupt mask */
    public static int IRQ_FIFO_NOT_EMPTY      =       1<<18;        /*!< Receive FIFO empty interrupt mask */
    public static int IRQ_CARD                        =       1<<19;        /*!< interrupt mask */
    public static int IRQ_CARD_LINE           =       1<<20;        /*!< interrupt mask */
    public static int IRQ_GEOMETRY_FIFO       =       1<<21;        /*!< geometry FIFO interrupt mask */
    public static int IRQ_LID                         =       1<<22;        /*!< interrupt mask */
    public static int IRQ_SPI                         =       1<<23;        /*!< SPI interrupt mask */
    public static int IRQ_WIFI                        =       1<<24;        /*!< WIFI interrupt mask (ARM7)*/
    public static int IRQ_ALL                         =       (~0);


    public static native void irqInit();
    public static native void irqSet(int irqMask, int handler); // how to do this??
    public static native void irqClear(int irqMask);
    public static native void irqEnable(int irqMask);
    public static native void irqDisable(int irqMask);

}
