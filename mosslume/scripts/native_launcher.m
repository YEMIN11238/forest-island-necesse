#import <AppKit/AppKit.h>
#include <mach-o/dyld.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void alertFailure(const char *message, const char *logfile) {
    @autoreleasepool {
        [NSApplication sharedApplication];
        [NSApp setActivationPolicy:NSApplicationActivationPolicyRegular];
        [NSApp activateIgnoringOtherApps:YES];
        NSAlert *alert = [[NSAlert alloc] init];
        alert.messageText = @"Mosslume 未能启动";
        NSString *detail = [NSString stringWithUTF8String:message ?: "Unknown launch error"];
        if (logfile && *logfile) detail = [detail stringByAppendingFormat:@"\n\n启动日志：%s", logfile];
        alert.informativeText = detail;
        alert.alertStyle = NSAlertStyleCritical;
        [alert addButtonWithTitle:@"好"];
        [alert runModal];
    }
}

static int mkdirs(const char *path) {
    char buffer[PATH_MAX];
    if (snprintf(buffer, sizeof buffer, "%s", path) >= sizeof buffer) return -1;
    for (char *p = buffer + 1; *p; p++) if (*p == '/') {
        *p = 0;
        if (mkdir(buffer, 0700) && errno != EEXIST) return -1;
        *p = '/';
    }
    return mkdir(buffer, 0700) == 0 || errno == EEXIST ? 0 : -1;
}

static int runChild(char *const args[]) {
    pid_t child = fork();
    if (child < 0) return -1;
    if (!child) { execv(args[0], args); _exit(127); }
    int status;
    while (waitpid(child, &status, 0) < 0) if (errno != EINTR) return -1;
    return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
}

static int ensureSteam(char *message, size_t messageSize) {
    NSString *bundleID = @"com.valvesoftware.steam";
    NSArray<NSRunningApplication *> *running = [NSRunningApplication runningApplicationsWithBundleIdentifier:bundleID];
    if (running.count && running.firstObject.finishedLaunching && !running.firstObject.terminated) {
        puts("MOSSLUME_STEAM existing Steam application detected");
        return 0;
    }
    NSWorkspace *workspace = [NSWorkspace sharedWorkspace];
    NSURL *steamURL = [workspace URLForApplicationWithBundleIdentifier:bundleID];
    if (!steamURL) {
        NSArray<NSString *> *candidates = @[@"/Applications/Steam.app", [NSHomeDirectory() stringByAppendingPathComponent:@"Applications/Steam.app"]];
        for (NSString *candidate in candidates) if ([[NSFileManager defaultManager] fileExistsAtPath:candidate]) {
            steamURL = [NSURL fileURLWithPath:candidate]; break;
        }
    }
    if (!steamURL && !running.count) {
        snprintf(message, messageSize, "此版本通过 Steam 启动游戏底座。未找到 Steam，请先安装 Steam、登录购买游戏的账号，再重新打开 Mosslume。");
        fputs("MOSSLUME_STEAM Steam application is not installed\n", stderr);
        return -1;
    }
    if (!running.count) {
        NSError *error = nil;
        NSRunningApplication *launched = [workspace launchApplicationAtURL:steamURL options:NSWorkspaceLaunchWithoutActivation configuration:@{} error:&error];
        if (!launched) {
            snprintf(message, messageSize, "无法自动打开 Steam。请手动打开 Steam 并完成登录，再重新打开 Mosslume。");
            fprintf(stderr, "MOSSLUME_STEAM launch failed: %s\n", error.localizedDescription.UTF8String ?: "Unknown error");
            return -1;
        }
        printf("MOSSLUME_STEAM launched installed Steam: %s\n", steamURL.path.UTF8String);
        fflush(stdout);
    }
    // Steam's application process can appear before its native API IPC endpoint is ready.
    // Keep this wait bounded; authentication itself remains entirely inside Steam/the kernel.
    for (int attempt = 0; attempt < 120; attempt++) {
        running = [NSRunningApplication runningApplicationsWithBundleIdentifier:bundleID];
        if (running.count && running.firstObject.finishedLaunching && !running.firstObject.terminated) {
            usleep(3000000);
            puts("MOSSLUME_STEAM application started; handing authentication to the game kernel");
            return 0;
        }
        usleep(100000);
    }
    snprintf(message, messageSize, "Steam 启动尚未完成。请在 Steam 中完成登录或更新，然后重新打开 Mosslume。原有游戏存档未修改。");
    fputs("MOSSLUME_STEAM timed out waiting for Steam application startup\n", stderr);
    return -1;
}

static int migrateProfile(const char *userHome, const char *profile) {
    char marker[PATH_MAX], destination[PATH_MAX], source[PATH_MAX], temporary[PATH_MAX];
    snprintf(marker, sizeof marker, "%s/.mosslume-profile-v1", profile);
    if (access(marker, F_OK) == 0) return 0;
    snprintf(destination, sizeof destination, "%s/saves", profile);
    snprintf(source, sizeof source, "%s/Library/Application Support/ForestIslandNecesse/saves", userHome);
    if (access(destination, F_OK) != 0 && access(source, F_OK) == 0) {
        snprintf(temporary, sizeof temporary, "%s/.migrate-saves-%d", profile, getpid());
        char *args[] = {"/usr/bin/ditto", "--noqtn", source, temporary, NULL};
        if (runChild(args) || rename(temporary, destination)) {
            fprintf(stderr, "MOSSLUME_PROFILE migration failed: %s\n", strerror(errno));
            return -1;
        }
        printf("MOSSLUME_PROFILE migrated saves from %s; original untouched\n", source);
    }
    snprintf(destination, sizeof destination, "%s/cfg", profile);
    if (mkdirs(destination)) return -1;
    snprintf(destination, sizeof destination, "%s/cfg/settings.cfg", profile);
    snprintf(source, sizeof source, "%s/Library/Application Support/ForestIslandNecesse/cfg/settings.cfg", userHome);
    if (access(destination, F_OK) != 0 && access(source, R_OK) == 0) {
        char *args[] = {"/bin/cp", "-p", source, destination, NULL};
        if (runChild(args)) return -1;
    }
    int fd = open(marker, O_WRONLY | O_CREAT | O_EXCL, 0600);
    if (fd >= 0) {
        const char *note = "Mosslume profile initialized. Existing Mosslume saves are never overwritten.\n";
        write(fd, note, strlen(note)); close(fd);
    }
    return 0;
}

int main(int argc, char **argv) {
    @autoreleasepool {
        char executable[PATH_MAX], resolved[PATH_MAX], game[PATH_MAX], mods[PATH_MAX], profile[PATH_MAX], logfile[PATH_MAX], java[PATH_MAX], icon[PATH_MAX], dockIcon[PATH_MAX + 32];
        uint32_t size = sizeof executable;
        if (_NSGetExecutablePath(executable, &size) || !realpath(executable, resolved)) {
            alertFailure("无法定位游戏安装目录。", NULL); return 1;
        }
        char *slash = strrchr(resolved, '/'); if (!slash) return 1; *slash = 0;
        slash = strrchr(resolved, '/'); if (!slash) return 1; *slash = 0;
        NSString *infoPath = [[NSString stringWithUTF8String:resolved] stringByAppendingPathComponent:@"Info.plist"];
        NSDictionary *bundleInfo = [NSDictionary dictionaryWithContentsOfFile:infoPath];
        NSString *profileName = bundleInfo[@"MosslumeProfileName"] ?: @"Mosslume";
        NSString *version = bundleInfo[@"CFBundleShortVersionString"] ?: @"0.3.0";
        snprintf(game, sizeof game, "%s/Resources/Game", resolved);
        snprintf(mods, sizeof mods, "%s/Resources/Mods", resolved);
        snprintf(icon, sizeof icon, "%s/Resources/Mosslume.icns", resolved);
        snprintf(dockIcon, sizeof dockIcon, "-Xdock:icon=%s", icon);
        const char *userHome = getenv("HOME");
        if (!userHome || !*userHome) { alertFailure("无法找到用户目录。", NULL); return 1; }
        const char *profileOverride = getenv("MOSSLUME_PROFILE");
        if (profileOverride && *profileOverride) snprintf(profile, sizeof profile, "%s", profileOverride);
        else snprintf(profile, sizeof profile, "%s/Library/Application Support/%s", userHome, profileName.UTF8String);
        if (mkdirs(profile)) { alertFailure("无法创建游戏存档目录。", NULL); return 1; }
        snprintf(logfile, sizeof logfile, "%s/runtime.log", profile);
        int fd = open(logfile, O_CREAT | O_WRONLY | O_APPEND, 0600);
        if (fd >= 0) { dup2(fd, 1); dup2(fd, 2); close(fd); }
        printf("\nMOSSLUME_NATIVE_APP version=%s kernel=Necesse-source-built-1.3.3.jar\n", version.UTF8String);
        printf("MOSSLUME_PROFILE %s\n", profile); fflush(stdout);
        BOOL migrateLegacy = bundleInfo[@"MosslumeMigrateLegacyProfile"] == nil || [bundleInfo[@"MosslumeMigrateLegacyProfile"] boolValue];
        if (!profileOverride && migrateLegacy && migrateProfile(userHome, profile)) {
            alertFailure("迁移小岛存档失败，原存档没有修改。请检查磁盘空间和新存档目录的权限。", logfile); return 1;
        }
        snprintf(java, sizeof java, "%s/jre/bin/java", game);
        char kernel[PATH_MAX], mod[PATH_MAX];
        snprintf(kernel, sizeof kernel, "%s/Necesse-source-built-1.3.3.jar", game);
        snprintf(mod, sizeof mod, "%s/localforest-island.jar", mods);
        if (access(java, X_OK) || access(kernel, R_OK) || access(mod, R_OK)) {
            fprintf(stderr, "MOSSLUME_LAUNCH missing Java, kernel or island adapter\n");
            alertFailure("游戏运行文件不完整。请重新运行打包脚本恢复应用。", logfile); return 1;
        }
        char *probe[] = {"/usr/bin/arch", "-x86_64", "/usr/bin/true", NULL};
        if (runChild(probe)) {
            alertFailure("此版本需要 macOS Rosetta 才能运行 Intel Java。请完成系统的 Rosetta 安装后重新打开。", logfile); return 1;
        }
        char steamFailure[1024];
        if (ensureSteam(steamFailure, sizeof steamFailure)) {
            alertFailure(steamFailure, logfile); return 1;
        }
        if (chdir(game)) { alertFailure("无法读取游戏运行目录。", logfile); return 1; }
        char *args[96] = {"/usr/bin/arch", "-x86_64", java, "-Xdock:name=Mosslume", dockIcon, "-Dapple.awt.application.name=Mosslume", "-Dmosslume=true", "-Dmosslume.windowTitle=Mosslume", "-Dlocalforest.kernel=Necesse-source-built-1.3.3.jar", "-Djdk.attach.allowAttachSelf=true", "-Xms512m", "-Xmx3g", "-jar", "Necesse-source-built-1.3.3.jar", "-datadir", profile, "-mod", mods, "-islandprofile", "-mosslume", "-windowpos", "100,70", "-windowsize", "1200,900", NULL};
        int count = 24;
        for (int i = 1; i < argc && count < 94; i++) if (strcmp(argv[i], "-psn_0_0") != 0 && strncmp(argv[i], "-psn_", 5) != 0) args[count++] = argv[i];
        args[count] = NULL;
        fflush(stdout); fflush(stderr);
        execv(args[0], args);
        fprintf(stderr, "MOSSLUME_LAUNCH exec failed: %s\n", strerror(errno));
        alertFailure("无法启动 Java 运行库。详细信息已写入启动日志。", logfile);
        return 1;
    }
}
