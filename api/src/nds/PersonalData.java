package nds;

public class PersonalData {
    public static native void populate();

    //----


    public static int theme;                                     //!<    The user's theme color (0-15).
    public static int birthMonth;                        //!<    The user's birth month (1-12).
    public static int birthDay;                          //!<    The user's birth day (1-31).


    public static int[] name;                         //!<    The user's name in UTF-16 format.
    public static int nameLen;                          //!<    The length of the user's name in characters.

    public static int[] message;                      //!<    The user's message.
    public static int messageLen;                       //!<    The length of the user's message in characters.

    public static int alarmHour;                         //!<    What hour the alarm clock is set to (0-23).
    public static int alarmMinute;                       //!<    What minute the alarm clock is set to (0-59).
    //0x027FFCD3  alarm minute

    //0x027FFCD4  ??

    public static int calX1;                            //!<    Touchscreen calibration: first X touch
    public static int calY1;                            //!<    Touchscreen calibration: first Y touch
    public static int calX1px;                           //!<    Touchscreen calibration: first X touch pixel
    public static int calY1px;                           //!<    Touchscreen calibration: first X touch pixel

    public static int calX2;                            //!<    Touchscreen calibration: second X touch
    public static int calY2;                            //!<    Touchscreen calibration: second Y touch
    public static int calX2px;                           //!<    Touchscreen calibration: second X touch pixel
    public static int calY2px;                           //!<    Touchscreen calibration: second Y touch pixel

    public static int language                   ;    //!<    User's language.
    public static int gbaScreen                  ;    //!<    GBA screen selection (lower screen if set, otherwise upper screen).
    public static int defaultBrightness  ;    //!<    Brightness level at power on, dslite.
    public static int autoMode                   ;    //!<    The DS should boot from the DS cart or GBA cart automatically if one is inserted.
    public static int settingsLost           ;    //!<    User Settings Lost (0=Normal, 1=Prompt/Settings Lost)

    public static int   rtcOffset;

}

