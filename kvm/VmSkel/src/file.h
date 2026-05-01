char* loadFile();

extern char fileName[256];
//extern char* platformProperties[MAX_PROPERTIES];
char* getPlatformProperty(char* key);
void freeJadBuffer(void);