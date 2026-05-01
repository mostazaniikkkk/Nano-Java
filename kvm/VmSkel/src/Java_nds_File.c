#include <nds.h>
#include <nds/arm9/video.h>
#include <kni.h> 
#include <stdio.h> 
#include <fat.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

 
KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_File_exists() { 

    /* Allocate static buffer for the Unicode string */ 
    jchar buffer[256]; 
    char buffer2[256];
    jsize size; 
    int i; 
    FILE *fp;
 
    /* Declare handle */ 
    KNI_StartHandles(1); 
    KNI_DeclareHandle(stringHandle); 
 
    /* Read parameter #1 to stringHandle */ 
    KNI_GetParameterAsObject(1, stringHandle); 
 
    /* Get the length of the string */ 
    size = KNI_GetStringLength(stringHandle); 
 
    /* Copy the Java string to our own buffer (as Unicode) */ 
    KNI_GetStringRegion(stringHandle, 0, size, buffer); 
 
    /* Print the Unicode characters as 8-bit chars */ 
    for (i = 0; i < size; i++) { 
      buffer2[i] = buffer[i];
    } 
    buffer2[i] = 0;

    fp = fopen(buffer2, "r");

    KNI_EndHandles(); 

    if(fp==NULL) 
        KNI_ReturnBoolean(KNI_FALSE);

    fclose(fp);
    KNI_ReturnBoolean(KNI_TRUE);
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_File_size() { 
    /* Allocate static buffer for the Unicode string */ 
    jchar buffer[256]; 
    char buffer2[256];
    jsize size; 
    int i; 
    int sz;
    //struct stat st;
    FILE *fp;
 
    /* Declare handle */ 
    KNI_StartHandles(1); 
    KNI_DeclareHandle(stringHandle); 
 
    /* Read parameter #1 to stringHandle */ 
    KNI_GetParameterAsObject(1, stringHandle); 
 
    /* Get the length of the string */ 
    size = KNI_GetStringLength(stringHandle); 
 
    /* Copy the Java string to our own buffer (as Unicode) */ 
    KNI_GetStringRegion(stringHandle, 0, size, buffer); 
 
    /* Print the Unicode characters as 8-bit chars */ 
    for (i = 0; i < size; i++) { 
      buffer2[i] = buffer[i];
    } 
    buffer2[i] = 0;
/*
  //OLE: this doesn't work on my NDS, it allways returns incorrect number
  //so I changed it to the classic "seek" way.
    int ret = stat(buffer2, &st);

    if(ret == 0)
        sz = st.st_size;
    else
        sz = -1;
*/
    fp = fopen(buffer2, "rb");
    if (fp == NULL) {
           sz = -1;
    } else {
        fseek(fp, 0, SEEK_END);
        sz = ftell(fp);
        fclose(fp);	
    }
    KNI_EndHandles(); 
    KNI_ReturnInt(sz);
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_File_load() { 
    /* Allocate static buffer for the Unicode string */ 
    jchar buffer[256]; 
    char buffer2[256];
    jsize size; 
    int i; 
    FILE *fp;
 
    /* Declare handle */ 
    KNI_StartHandles(2); 
    KNI_DeclareHandle(stringHandle); 
    KNI_DeclareHandle(arrayHandle); 
 
    /* Read parameter #1 to stringHandle */ 
    KNI_GetParameterAsObject(1, stringHandle); 
 
    /* Get the length of the string */ 
    size = KNI_GetStringLength(stringHandle); 
 
    /* Copy the Java string to our own buffer (as Unicode) */ 
    KNI_GetStringRegion(stringHandle, 0, size, buffer); 
 
    /* Print the Unicode characters as 8-bit chars */ 
    for (i = 0; i < size; i++) { 
      buffer2[i] = buffer[i];
    } 
    buffer2[i] = 0;

    KNI_GetParameterAsObject(2, arrayHandle); 
    size = KNI_GetArrayLength(arrayHandle);

    fp = fopen(buffer2, "rb");
    if (fp != NULL) {
            fseek(fp, 0, SEEK_END);
            int len = ftell(fp);
            u8 *buf = (u8 *)malloc(len);
            if(!buf)
            {
                    fclose(fp);
                    KNI_ReturnInt(-2);
            }
            fseek(fp, 0, SEEK_SET);

            if(len > size)
                   len = size;
            if(size > len)
                    size = len;

            i = fread(buf, 1, len, fp);

            if(i < len)
                 len = i;

            fclose(fp);
            // KNI_SetRawArrayRegion(jarray arrayHandle, jsize offset, jsize n, const jbyte* srcBuffer);
            KNI_SetRawArrayRegion(arrayHandle, 0, size, (const jbyte*)buf);

            free(buf);
    } else {
           i = -1;
    }
    KNI_EndHandles(); 

    KNI_ReturnInt(i);
} 


KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_File_save() { 
    /* Allocate static buffer for the Unicode string */ 
    jchar buffer[256]; 
    char buffer2[256];
    jsize size; 
    int i; 
    int pos;
    FILE *fp;
 
    /* Declare handle */ 
    KNI_StartHandles(2); 
    KNI_DeclareHandle(stringHandle); 
    KNI_DeclareHandle(arrayHandle); 
 
    /* Read parameter #1 to stringHandle */
    KNI_GetParameterAsObject(1, stringHandle);
    /* Read parameter #2 to arrayHandle */
    KNI_GetParameterAsObject(2, arrayHandle);

    /* Get the length of the string */
    size = KNI_GetStringLength(stringHandle);

    /* Copy the Java string to our own buffer (as Unicode) */
    KNI_GetStringRegion(stringHandle, 0, size, buffer);

    /* Print the Unicode characters as 8-bit chars */
    for (i = 0; i < size; i++) {
      buffer2[i] = buffer[i];
    }
    buffer2[i] = 0;

    /* Copy the Java array into a native buffer, then write to file */
    size = KNI_GetArrayLength(arrayHandle);
    {
        jbyte *dataBuffer = (jbyte*)malloc(size);
        KNI_GetRawArrayRegion(arrayHandle, 0, size, dataBuffer);

        fp = fopen(buffer2, "wb");
        pos = 0;
        i = 1;
        while (pos < size && i > 0) {
            i = fwrite(dataBuffer + pos, 1, size-pos, fp);
            pos += i;
        }
        fclose(fp);
        free(dataBuffer);
    }

    KNI_EndHandles();
    KNI_ReturnInt(i);
} 

KNIEXPORT KNI_RETURNTYPE_VOID Java_nds_File_loadToVRAM() { 
    /* Allocate static buffer for the Unicode string */ 
    jchar buffer[256]; 
    char buffer2[256];
    jsize size; 
    int i; 
    FILE *fp;
    int len;
 
    /* Declare handle */ 
    KNI_StartHandles(1); 
    KNI_DeclareHandle(stringHandle); 
 
    /* Read parameter #1 to stringHandle */ 
    KNI_GetParameterAsObject(1, stringHandle); 
 
    /* Get the length of the string */ 
    size = KNI_GetStringLength(stringHandle); 
 
    /* Copy the Java string to our own buffer (as Unicode) */ 
    KNI_GetStringRegion(stringHandle, 0, size, buffer); 
 
    /* Print the Unicode characters as 8-bit chars */ 
    for (i = 0; i < size; i++) { 
      buffer2[i] = buffer[i];
    } 

    fp = fopen(buffer2, "rb");

    if(!fp) {
        KNI_ReturnInt(-2);
    }

    fseek(fp, 0, SEEK_END);
    len = ftell(fp);
    u16 *buf = (u16 *)malloc(len);
    if(!buf)
    {
        fclose(fp);
        KNI_ReturnInt(-2);
    }
    fseek(fp, 0, SEEK_SET);
    fread(buf, 2, len/2, fp);

    u16 *ptr = (u16 *)KNI_GetParameterAsInt(2);
    for(i=0; i<len/2; i++) {
        ptr[i] = buf[i];
    }

    fclose(fp);

    free(buf);

    KNI_EndHandles(); 

    KNI_ReturnInt(len);
} 

