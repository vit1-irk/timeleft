#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <time.h>
#include <sys/stat.h>
#include <string.h>
#include <jni.h>

#define BREAKS_MAX_INDEX 19 // максимальное количество перемен: 20

#include "file-functions.c"

time_t current;
struct tm* current_usable;
int start_lessons_offset;
int current_offset;

static char __configPath[200];
char configfile[]=".local/share/timetable.cfg";
char configfile_fallback[]=".timetable.cfg";
char configtext_default[]="14:0:28800\n6:40:10\n15\n15\n10\n10\n10\n10\n10";

int start_lessons[] = { 14, 0 };
int breaks_in_minutes[BREAKS_MAX_INDEX+1] = { 15, 15, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
int default_break=10;
int lesson_length=40;
int count_lessons=6;
static int count_timekeys;
static time_t* timekeys;
static char textstring[1000];
static char buf[100];

int localtime_offset=8*60*60;
static char diffoutput[9];
int i, a, isbreak, numbreak;

int currentPosition(time_t* timekeys, int count_timekeys, int number) {
	int i;

	for (i=0; i<count_timekeys; i++) {
		if (number < timekeys[i]) return i-1;
	}
	
	return -2;
}

char* timeAppearance(time_t diff) {
	struct tm* usable=gmtime(&diff);
	char* p=diffoutput;

	snprintf(p, 9, "%02d:%02d:%02d", (*usable).tm_hour, (*usable).tm_min, (*usable).tm_sec);

	return diffoutput;
}

void printTimeTable(time_t *timekeys, int count_timekeys) {
	int i, br, res;
	strcpy(buf, "");
	strcpy(textstring, "");

	for (i=0; i<count_timekeys; i++) {
		br=i%2;
		res=i/2+1;

		if (br==0) {
			sprintf(buf, "%d-", res); // Для уроков
			strcat(textstring, buf);
		} else {
			if (i==count_timekeys-1) {
				strcat(textstring, "Конец занятий-");
			} else {
				sprintf(buf, "%d-", res); // Для перемен
				strcat(textstring, buf);
			}
		}
		sprintf (buf, "%s\n", timeAppearance(timekeys[i]));
		strcat(textstring, buf);
	}
}

void getcfg() {
	char* __homedir="/sdcard/";
	int i;

	if (__homedir==NULL) {
 		strcpy(__configPath, configfile_fallback); // если у юзера отсутствует домашний каталог, то держим все данные прямо здесь, рядом
  	} else {
   		strcpy(__configPath, __homedir);
		strcat(__configPath, "/"); // на всякий случай добавляем слэш

		if (!dir_exists(__configPath)) {
			printf("Пытаемся создать каталог для конфига\n");
			mkdir_p(__configPath, 0666);
		}

		strcat(__configPath, configfile);
	}
	
	FILE *f;

	if (access(__configPath, R_OK) == -1) {
		printf("Конфиг не существует, пытаемся скопировать дефолтный...\n");
		
		f=fopen(__configPath, "w");
		if (!f) {
			printf("Не получается записать дефолтный конфиг! =(\n");
		} else {
			fputs(configtext_default, f);
			fclose(f);
		}
	}

	char* config=file_get_contents(__configPath);
	
	if (config!=NULL) {
		struct list config_lines=split(config, "\n");

		if (config_lines.length<2) {
			printf ("Формат конфига неверный, пропускаем.\n");
		} else {
			sscanf(config_lines.index[0], "%d:%d:%d", &start_lessons[0], &start_lessons[1], &localtime_offset);
			sscanf(config_lines.index[1], "%d:%d:%d", &count_lessons, &lesson_length, &default_break);
		}

		for (i=2; i<config_lines.length; i++) {
			if ((i-2)>BREAKS_MAX_INDEX) break;
			sscanf(config_lines.index[i], "%d", &breaks_in_minutes[i-2]);
		}
	} else {
		printf("Невозможно прочесть конфиг, используются значения по-умолчанию.\n");
	}
}

JNIEXPORT void JNICALL Java_vit01_timeleft_MainActivity_resources_1init() {
	getcfg();
	// дневное timestamp-смещение первого урока
	start_lessons_offset=start_lessons[0]*3600+start_lessons[1]*60;

	count_timekeys=count_lessons*2;
	timekeys=(time_t*)malloc(count_timekeys*sizeof(time_t));

	for (i=0; i<count_timekeys; i++) {
		timekeys[i]=start_lessons_offset;
		timekeys[i]+=lesson_length*(i/2)*60;

		isbreak=i%2;
		
		numbreak=i/2; // номер текущей перемены

		for (a=0; a<numbreak; a++) {
			if (a>=BREAKS_MAX_INDEX) {
				timekeys[i]+=default_break*60;
			} else {
				timekeys[i]+=breaks_in_minutes[a]*60;
			}
		}

		if (isbreak) { // если эта метка - перемена, то прибавляем время 1 урока, чтобы уравняться
			timekeys[i]+=lesson_length*60;
		}
	}
}

int saveConfig() {
	FILE *f=fopen(__configPath, "w");
	if (!f) return -1;

	fprintf(f, "%d:%d:%d\n", start_lessons[0], start_lessons[1], localtime_offset);
	fprintf(f, "%d:%d:%d", count_lessons, lesson_length, default_break);

	int count_breaks=count_timekeys/2;
	for(i=0;i<count_breaks;i++) {
		fprintf(f, "\n%d", breaks_in_minutes[i]);
	}
	fclose(f);

	return 0;
}


void current_update(time_t *timekeys, int count_timekeys) {
	time_t curr_timestamp=time(NULL)+localtime_offset; // текущее время + смещение по Иркутску
	//time_t curr_timestamp=57000;
	current_usable=gmtime(&curr_timestamp);
	// дневное timestamp-смещение момента "сейчас"
	current=(*current_usable).tm_hour*3600+(*current_usable).tm_min*60+(*current_usable).tm_sec;

	// смотрим, где мы находимся относительно временных меток уроков или перемен
	int currPos=currentPosition(timekeys, count_timekeys, current);
	int diff=0;

	strcpy(textstring, "");
	strcat(textstring, "Время: ");
	strcat(textstring, timeAppearance(current));
	strcat(textstring, "\nСейчас: ");

	isbreak=currPos%2;
	numbreak=currPos/2+1;
	
	if (currPos==-1) {
		diff=timekeys[0]-current;

		strcat(textstring, "время до уроков\n");
		strcat(textstring, "До начала уроков осталось: ");
		strcat(textstring, timeAppearance(diff));
		strcat(textstring, "\n");
		
	} else {
		if (currPos==-2) strcat(textstring, "время после уроков\n");
		else {
			if (isbreak) {
				diff=timekeys[currPos+1]-current;
				sprintf(buf, "%d\n", numbreak);

				strcat(textstring, "перемена ");
				strcat(textstring, buf);

				strcat(textstring, "До урока осталось: ");
				strcat(textstring, timeAppearance(diff));
				strcat(textstring, "\n");
			}
			else {
				sprintf(buf, "%d\n", numbreak);
				strcat(textstring, "урок ");
				strcat(textstring, buf);

				if (count_timekeys-currPos!=2) {
					diff=timekeys[currPos+1]-current;
					strcat(textstring, "До перемены осталось: ");
					strcat(textstring, timeAppearance(diff));
					strcat(textstring, "\n");
				}
			}
			diff=timekeys[count_timekeys-1]-current;
			strcat(textstring, "\nДо конца уроков осталось: ");
			strcat(textstring, timeAppearance(diff));
			diff=current-timekeys[0];
			strcat(textstring, "\nС начала уроков прошло: ");
			strcat(textstring, timeAppearance(diff));
		}
	}
}

JNIEXPORT jstring JNICALL Java_vit01_timeleft_MainActivity_update_1wrapper(JNIEnv * env, jobject jObj) {
	current_update(timekeys, count_timekeys);
	return (*env)->NewStringUTF(env, textstring);
}
JNIEXPORT jstring JNICALL Java_vit01_timeleft_MainActivity_getTimetable(JNIEnv * env, jobject jObj) {
	printTimeTable(timekeys, count_timekeys);
	return (*env)->NewStringUTF(env, textstring);
}
JNIEXPORT void JNICALL Java_vit01_timeleft_MainActivity_setConfig_1fromstring(JNIEnv * env, jobject jObj,
																			jstring config_java
) {
	// здесь будут некие костыли

	const char *config = (*env)->GetStringUTFChars(env, config_java, 0);

	if (config != NULL) {
		struct list config_lines = split((char *) config, "\n");

		if (config_lines.length < 2) {
			printf("Формат конфига неверный, пропускаем.\n");
		} else {
			sscanf(config_lines.index[0], "%d:%d:%d", &start_lessons[0], &start_lessons[1],
				   &localtime_offset);
			sscanf(config_lines.index[1], "%d:%d:%d", &count_lessons, &lesson_length,
				   &default_break);
		}

		for (i = 2; i < config_lines.length; i++) {
			if ((i - 2) > BREAKS_MAX_INDEX) break;
			sscanf(config_lines.index[i], "%d", &breaks_in_minutes[i - 2]);
		}
	}
	saveConfig();
}
// void main(int argc, char** argv) {
//	getcfg(); //получаем конфигурацию
//	resources_init();

	// считаем, сколько будет временных меток

	//while(1) {
	//	current_update(timekeys, count_timekeys);
	//	usleep(1*1000000);
	//}
//}
