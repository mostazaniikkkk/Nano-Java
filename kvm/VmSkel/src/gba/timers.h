/************************************\
* Timers.h by dovoto *
\************************************/

// Modified by Torlus (January 2004)

#ifndef TIMERS_H
#define TIMERS_H

#define REG_TM0D       *(u16*)0x4000100
#define REG_TM0CNT     *(u16*)0x4000102
#define REG_TM1D       *(u16*)0x4000104
#define REG_TM1CNT     *(u16*)0x4000106
#define REG_TM2D       *(u16*)0x4000108
#define REG_TM2CNT     *(u16*)0x400010A
#define REG_TM3D       *(u16*)0x400010C
#define REG_TM3CNT     *(u16*)0x400010E

#define TIME_FREQUENCY_SYSTEM 0x0
#define TIME_FREQUENCY_64 0x1
#define TIME_FREQUENCY_256 0x2
#define TIME_FREQUENCY_1024 0x3
#define TIME_OVERFLOW 0x4
#define TIME_ENABLE 0x80
#define TIME_IRQ_ENABLE 0x40

#endif
