package nds;

public class Timers {
public static int TIMER_FREQ(int n) { return    (-0x2000000/(n)); }
public static int TIMER_FREQ_64(int n) { return  (-(0x2000000>>6)/(n)); }
public static int TIMER_FREQ_256(int n) { return (-(0x2000000>>8)/(n)); }
public static int TIMER_FREQ_1024(int n) { return (-(0x2000000>>10)/(n)); }
public static int TIMER0_DATA =    (0x04000100);
public static int TIMER1_DATA =    (0x04000104);
public static int TIMER2_DATA =    (0x04000108);
public static int TIMER3_DATA =    (0x0400010C);
public static int TIMER_DATA(int n) { return  ((0x04000100+((n)<<2))); }
public static int TIMER0_CR =   (0x04000102);
public static int TIMER1_CR =   (0x04000106);
public static int TIMER2_CR =   (0x0400010A);
public static int TIMER3_CR =   (0x0400010E);
public static int TIMER_CR(int n) { return ((0x04000102+((n)<<2))); }
public static int TIMER_ENABLE =    (1<<7);
public static int TIMER_IRQ_REQ =   (1<<6);
public static int TIMER_CASCADE =   (1<<2);
public static int TIMER_DIV_1 =     (0);
public static int TIMER_DIV_64 =    (1);
public static int TIMER_DIV_256 =   (2);
public static int TIMER_DIV_1024 =  (3);

}

