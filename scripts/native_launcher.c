#include <mach-o/dyld.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>
static void mkdirs(const char *path) {
    char buffer[PATH_MAX]; snprintf(buffer,sizeof(buffer),"%s",path);
    for(char *p=buffer+1;*p;p++)if(*p=='/'){*p=0;mkdir(buffer,0700);*p='/';}
    mkdir(buffer,0700);
}
int main(void) {
    char executable[PATH_MAX], resolved[PATH_MAX],game[PATH_MAX],mods[PATH_MAX],profile[PATH_MAX],logfile[PATH_MAX],java[PATH_MAX];
    uint32_t size=sizeof(executable);
    if(_NSGetExecutablePath(executable,&size)!=0||!realpath(executable,resolved))return 1;
    char *slash=strrchr(resolved,'/');if(!slash)return 1;*slash=0;
    slash=strrchr(resolved,'/');if(!slash)return 1;*slash=0;
    snprintf(game,sizeof(game),"%s/Resources/Game",resolved);
    snprintf(mods,sizeof(mods),"%s/Resources/Mods",resolved);
    const char *userHome=getenv("HOME");if(!userHome)return 1;
    snprintf(profile,sizeof(profile),"%s/Library/Application Support/ForestIslandNecesse",userHome);mkdirs(profile);
    snprintf(logfile,sizeof(logfile),"%s/runtime.log",profile);
    int fd=open(logfile,O_CREAT|O_WRONLY|O_APPEND,0600);if(fd>=0){dup2(fd,1);dup2(fd,2);close(fd);}
    snprintf(java,sizeof(java),"%s/jre/bin/java",game);
    if(chdir(game)!=0){perror("Island runtime folder");return 1;}
    puts("LOCALFOREST_NATIVE_APP kernel=Necesse-source-built-1.3.3.jar");fflush(stdout);
    char *args[]={"/usr/bin/arch","-x86_64",java,"-Dlocalforest.kernel=Necesse-source-built-1.3.3.jar","-Djdk.attach.allowAttachSelf=true","-Xms512m","-Xmx3g","-jar","Necesse-source-built-1.3.3.jar","-datadir",profile,"-mod",mods,"-islandprofile","-load","ForestIsland","-character","小岛农夫","-windowpos","100,70","-windowsize","1200,900",NULL};
    execv(args[0],args);perror("Island Java runtime");return 1;
}
