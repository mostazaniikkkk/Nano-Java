// File Loading Code
// Adapted From WinterMute's fatlibtest
#include <global.h>
#include <nds.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/stat.h>

// #include "fat/gba_nds_fat.h"

char fileName[256];
int numFiles = 0;
int scrollPos = 0;
int scrollDelay = 0;

char* keyVal="hello";

int choosingfile=2;
char* jadBuffer; 
char jadEntry[256];
struct fileEntry {
	struct fileEntry *next;
	struct fileEntry *prev;
	int type;
	char name[256];
};

struct fileEntry *fileList = NULL;
struct fileEntry *fileLast = NULL;

char mapName[256] = "";

typedef enum {FT_NONE,FT_FILE, FT_DIR, FT_JAD} FILE_TYPE;



char* getPlatformProperty(char* key) {
	int entryLength;
	char* buf;

    if (jadBuffer == NULL) {
        return NULL;
    }
    /* JamGetProp requires USE_JAM=1; without JAM, property lookup is unsupported */
#if USE_JAM
    buf = JamGetProp(jadBuffer, key, &entryLength);
    if (buf != NULL) {
        strncpy(jadEntry, buf, entryLength);
        jadEntry[entryLength] = 0;
        return jadEntry;
    }
#endif
    (void)buf;
    return NULL;

}

void parseJad(char* name) {
	long fileSize;
	FILE* file = fopen(name,"rb");
//	printf("parse jad:%s \n", name);

	if (file == NULL) {
		printf("open failed:%s \n", name);
		jadBuffer = NULL;
	} else {
		int read;
		int pos;

		fseek(file, 0, SEEK_END);
		fileSize = ftell(file) ;
		fseek(file, 0, SEEK_SET);
		jadBuffer = (char*)malloc(fileSize+1);
		pos = 0;
		while (pos< fileSize) {
//			printf("size: %i read:%i \n", fileSize, pos);
			read = fread(jadBuffer+pos, 1, fileSize-pos, file);
			pos += read;
		}
		jadBuffer[fileSize] = 0;
		fclose(file);
	}

}

void initJadBuffer(void) {
    jadBuffer = NULL;
}
void freeJadBuffer(void) {
	if (jadBuffer != NULL) {
		free(jadBuffer);
	}
	jadBuffer = NULL;
}

//---------------------------------------------------------------------------------
void addFile(int type, char* fileName) {
//---------------------------------------------------------------------------------

	int len = strlen(fileName);

	if ( type == FT_DIR ||
		 !strcasecmp(".class",&fileName[len-6]) ||
		 !strcasecmp(".jad",&fileName[len-4]) 
		
	) {

		if ( NULL == fileLast ) {
			fileLast = (struct fileEntry *)malloc( sizeof(struct fileEntry));
			fileLast->prev = NULL;
		} else {
			fileLast->next = (struct fileEntry *)malloc( sizeof(struct fileEntry));
			fileLast->next->prev = fileLast;
			fileLast = fileLast->next;
		}
		if (!strcasecmp(".jad",&fileName[len-4]))  {
			type = FT_JAD;
		}

		sprintf(fileLast->name,"%s",fileName);
		fileLast->type = type;
		fileLast->next = NULL;

		if ( NULL == fileList ) fileList = fileLast;
		
		numFiles++;

	}
}

//---------------------------------------------------------------------------------
void freeFileList() {
//---------------------------------------------------------------------------------

	if ( NULL != fileList ) {
		
		while ( NULL != fileList->prev ) fileList = fileList->prev;

		while ( NULL != fileList ) {
			struct fileEntry *temp = fileList->next;
			free(fileList);
			fileList = temp;
		}
	}
	numFiles = 0;
	fileList = NULL;
	fileLast = NULL;
}

//---------------------------------------------------------------------------------
void showFileList() {
//---------------------------------------------------------------------------------
	if ( NULL == fileList ) return;

	struct fileEntry *entry = fileList;
	
	char *position = "\x1b[00;00H";

	char dispname[29];
	
	int line = 6;

	while ( NULL != entry && line < 21) {
		sprintf(position,"\x1b[%d;%dH",line,4);

		if ( entry->type == FT_DIR ) {
			strncpy(dispname,"[",29);
			if(strlen(entry->name) <= 26) {
				strncat(dispname,entry->name,26);
				strlcat(dispname,"]",29);
			}
			else {
				strlcat(dispname,entry->name,29);
			}
			printf("%s%s\x1b[K",position,dispname);
		} else {
			strlcpy(dispname,entry->name,29);
			printf("%s%s\x1b[K",position,dispname);
		}			
		line++;
		
		entry = entry->next;
	}
	// printf("\x1b[22;0HnumFiles: %d    ",numFiles);
	
}

DIR* dir;

//---------------------------------------------------------------------------------
void getFileList() {
//---------------------------------------------------------------------------------
	printf("\x1b[6;0H\x1b[0J");
	freeFileList();
	
	int type;
	struct stat st;
	struct dirent *ent;

	while ((ent = readdir(dir)) != NULL) {
		strncpy(fileName, ent->d_name, sizeof(fileName) - 1);
		fileName[sizeof(fileName) - 1] = '\0';
		if (stat(fileName, &st) == 0 && (st.st_mode & S_IFDIR))
			type = FT_DIR;
		else
			type = FT_FILE;
		addFile(type, fileName);
	}

	closedir(dir);
}

char *cursorPos = "\x1b[0;0H  ";
int cursorLine = 6;

void scrollFile() {
	if ( NULL == fileList ) return;

	char *position = "\x1b[00;00H";
	int fileNum = cursorLine -6;
	struct fileEntry *entry = fileList; 
	while ( fileNum > 0 ) {
		entry = entry->next;
		fileNum--;
	}

	char dispname[29];

	sprintf(position,"\x1b[%d;%dH",cursorLine,4);

	if ( entry->type == FT_DIR ) {
			if(strlen((entry->name)+scrollPos) > 25) {
			strncpy(dispname,"[",29);
			if(strlen((entry->name)+scrollPos) <= 26) {
				strncat(dispname,(entry->name)+scrollPos,26);
				strlcat(dispname,"]",29);
			}
			else {
				strlcat(dispname,(entry->name)+scrollPos,29);
			}
			printf("%s%s\x1b[K",position,dispname);
			if(++scrollPos > ((int)strlen(entry->name)-26)) {
					scrollPos = 0;
				}
			}
		} else {
			if(strlen((entry->name)+scrollPos) > 27) {
				strlcpy(dispname,(entry->name)+scrollPos,29);
				printf("%s%s\x1b[K",position,dispname);
				if(++scrollPos > ((int)strlen(entry->name)-28)) {
					scrollPos = 0;
				}
			}
	}
	
}

//---------------------------------------------------------------------------------
void updateCursor() {
//---------------------------------------------------------------------------------
	printf("%s  ", cursorPos);
	sprintf(cursorPos,"\x1b[%d;%dH",cursorLine,1);
	printf("%s->", cursorPos);

}

char* loadFile() {
  int keysPressed, keysReleased, keysDownNonRepeat;
  printf("\x1b[4;10HLoad File");
  dir = opendir(".");
  getFileList();
  showFileList();
  // printf("numFiles: %d\n",numFiles);	
	cursorLine = 6;
	int cursorFile = 0, j = 0;
	updateCursor();

	while(1) {

		swiWaitForVBlank();
		scanKeys();
		
		keysDownNonRepeat = keysDown();
		keysPressed = keysDownRepeat();
		keysReleased = keysUp();
		
		if ( keysPressed & KEY_B ) {
			chdir("..");
			dir = opendir(".");
			getFileList();
			showFileList();
			cursorLine=6;
			cursorFile=0;
			updateCursor();
		}
		
		if ( keysPressed & KEY_A ) {
			// choosingfile = 2;

			int fileNum = cursorLine -6;
			struct fileEntry *file = fileList; 
			while ( fileNum > 0 ) {
				file = file->next;
				fileNum--;
			}
			jadBuffer = NULL;
			if ( file->type == FT_DIR ) {
				chdir(file->name);
				dir = opendir(".");
				getFileList();
				showFileList();
				cursorLine=6;
				cursorFile=0;
				updateCursor();
			}  else
			if (file->type == FT_JAD) {
                char* value;
				parseJad(file->name);
				value = getPlatformProperty("MIDlet-Jar-URL");
				if (value == NULL) {
                    return NULL;
                } else {
    				strcpy(fileName,value);
                    freeFileList();
                    return fileName;
                }
				break;
			}
			else {
				int propertyIndex = 0;
				printf("\x1b[6;0H\x1b[0J");
				
				printf("Loading %s ... ",file->name);
				strcpy(fileName,file->name);
				freeFileList();
				return fileName;
								
				break;

			}

		} 

		if ( keysDownNonRepeat & KEY_SELECT) {
			if(choosingfile == 2) {
				return NULL;
			}
		}
		
		if ( keysPressed & KEY_UP ) {
			scrollPos = 0; scrollFile();
			if ( cursorLine == 6  && NULL != fileList->prev  ) {
				fileList = fileList->prev;
				showFileList();
				cursorFile--;
			}
			if ( cursorLine > 6 ) cursorLine--;
			updateCursor();
			scrollPos = 0;
		}
		
		if ( keysPressed & KEY_L ) {
			scrollPos = 0; scrollFile();
			
			if ( cursorLine > 6 ) {
				for(j = 0; (cursorLine > 6) && (j < 5); j++) {
					cursorLine--;
				}
			}
			else if ( NULL != fileList->prev ) {
				for(j = 0; (NULL != fileList->prev) && (j < 5); j++) {	
					fileList = fileList->prev;
					showFileList();
					cursorFile--;
				}
			}
			updateCursor();
			
			scrollPos = 0;
		}

		if ( keysPressed & KEY_DOWN ) {
			scrollPos = 0; scrollFile();
			if ( cursorLine == 20  && NULL != fileList->next && cursorFile < (numFiles  - 15) ) {
				fileList = fileList->next;
				showFileList();
				cursorFile++;
			}

			if ( cursorLine < 20 && (numFiles + 5) > cursorLine ) cursorLine++;
			updateCursor();
			scrollPos = 0;
		}

		if ( keysPressed & KEY_R ) {
			scrollPos = 0; scrollFile();
			
			if ( ((cursorLine) < 20) && ((numFiles+5) > cursorLine) ) {
				for(j = 0; (cursorLine < 20) && ((numFiles+5) > cursorLine) && (j < 5); j++) {
					cursorLine++;
				}
			}
			else if ( /*((cursorLine+5) > 20) &&*/ (NULL != fileList->next) /*&& (cursorFile < (numFiles - 15))*/ ) {
				for(j = 0; (NULL != fileList->next) && (cursorFile < (numFiles - 15)) && (j < 5); j++) {	
					fileList = fileList->next;
					showFileList();
					cursorFile++;
				}
			}
			updateCursor();
			
			scrollPos = 0;
		}

		scrollDelay++;
		if( ((scrollPos < 2) && (scrollDelay == 30)) 
			|| ((scrollPos >= 2) && (scrollDelay == 10)) ) {
			scrollFile();
			scrollDelay = 0;
		}

	}
}
