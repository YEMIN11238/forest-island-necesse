package necesse.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.loading.GameNoticeException;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.DevModProvider;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.ModRuntimeException;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.world.RecentJVMECrashException;
import necesse.reports.CrashJFrame;
import necesse.reports.CrashReportData;
import necesse.reports.ModCrashJFrame;
import necesse.reports.NoticeJFrame;
import necesse.reports.RecentCrashJFrame;

public class GameCrashLog {
   private static final String LOG_PATH = "latest-crash.log";

   public static void printCrashLog(Throwable e, Client client, Server server, String state, boolean openCrashReport) {
      printCrashLog(Collections.singletonList(e), client, server, state, openCrashReport);
   }

   public static void printCrashLog(List<Throwable> errors, Client client, Server server, String state, boolean openCrashReport) {
      File file = new File("latest-crash.log");
      System.err.println("Printing crash log to " + file.getAbsolutePath());

      try {
         CrashReportData data = new CrashReportData(errors, client, server, state);
         FileOutputStream fo = new FileOutputStream(file);
         PrintStream printStream = new PrintStream(fo);
         data.printFullReport(printStream, file);
         fo.close();
         if (client != null) {
            client.serverCrashReport = data;
         }

         if (openCrashReport) {
            openCrashFrame(data);
            if (GlobalData.getCurrentGameLoop() != null) {
               GlobalData.getCurrentGameLoop().stopMainGameLoop();
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static void reportRecentJVMCrashes() {
      if (DevModProvider.devMod == null) {
         try {
            Stream<Path> pathStream = Files.find(
               Paths.get("./"),
               1,
               (pathx, attr) -> attr.isRegularFile()
                  && pathx.getFileName().toString().startsWith("hs_err_pid")
                  && pathx.toString().endsWith(".log")
                  && attr.creationTime().toInstant().isAfter(Instant.now().minus(30L, ChronoUnit.MINUTES)),
               FileVisitOption.FOLLOW_LINKS
            );

            try {
               boolean isFirst = true;

               for (Path path : pathStream.collect(Collectors.toList())) {
                  GameLog.warn.println("Found recent crash file: " + path);
                  if (isFirst) {
                     isFirst = false;

                     try {
                        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        List<Throwable> throwables = new ArrayList<>();
                        throwables.add(new RecentJVMECrashException(lines));
                        RecentCrashJFrame.showAndBlock(new CrashReportData(throwables, null, null, "startup"));
                     } catch (IOException e) {
                        System.err.println("Could not report recent JVM crash because of error:");
                        e.printStackTrace();
                     }
                  }

                  File file = path.toFile();
                  file.renameTo(new File(file.getParentFile(), file.getName() + ".old"));
               }
            } catch (Throwable var9) {
               if (pathStream != null) {
                  try {
                     pathStream.close();
                  } catch (Throwable var7) {
                     var9.addSuppressed(var7);
                  }
               }

               throw var9;
            }

            if (pathStream != null) {
               pathStream.close();
            }
         } catch (IOException e) {
            GameLog.warn.println("Failed to check for recent JVM crashes. See exception below.");
            e.printStackTrace(GameLog.warn);
         }
      }
   }

   public static void openCrashFrame(CrashReportData data) {
      ModRuntimeException[] modExceptions = data.errors
         .stream()
         .filter(e -> e instanceof ModRuntimeException)
         .map(e -> (ModRuntimeException)e)
         .toArray(ModRuntimeException[]::new);
      if (modExceptions.length > 0) {
         ModCrashJFrame crashJFrame = new ModCrashJFrame(Collections.singletonList(modExceptions[0].mod), modExceptions);
         crashJFrame.setVisible(true);
         crashJFrame.requestFocus();
      } else {
         ArrayList<LoadedMod> responsibleMods = ModLoader.getResponsibleMods(data.errors, true);
         if (!responsibleMods.isEmpty()) {
            ModCrashJFrame crashJFrame = new ModCrashJFrame(responsibleMods, data.errors.toArray(new Exception[0]));
            crashJFrame.setVisible(true);
            crashJFrame.requestFocus();
            return;
         }

         GameNoticeException noticeException = findAnyCause(data.errors, GameNoticeException.class, null);
         if (noticeException != null) {
            NoticeJFrame noticeFrame = new NoticeJFrame(400, noticeException.message.translate(), Localization.translate("ui", "closebutton"));
            noticeFrame.setVisible(true);
            noticeFrame.requestFocus();
         } else if (findAnyCause(data.errors, OutOfMemoryError.class, null) != null) {
            NoticeJFrame noticeFrame = new NoticeJFrame(400, Localization.translate("misc", "outofmemory"), Localization.translate("ui", "closebutton"));
            noticeFrame.setVisible(true);
            noticeFrame.requestFocus();
         } else {
            CrashJFrame crashFrame = new CrashJFrame(data);
            crashFrame.setVisible(true);
            crashFrame.requestFocus();
         }
      }
   }

   public static <T extends Throwable> T findAnyCause(Iterable<Throwable> errors, Class<T> throwableClass, Predicate<T> predicate) {
      for (Throwable error : errors) {
         if (throwableClass.isAssignableFrom(error.getClass())) {
            T cause = findAnyCause(throwableClass.cast(error), throwableClass, predicate);
            if (cause != null) {
               return cause;
            }
         }
      }

      return null;
   }

   public static <T extends Throwable> T findAnyCause(T error, Class<T> throwableClass, Predicate<T> predicate) {
      if (predicate != null && !predicate.test(error)) {
         Throwable cause = error.getCause();
         if (cause == null) {
            return null;
         } else {
            return !throwableClass.isAssignableFrom(error.getClass()) ? null : findAnyCause(throwableClass.cast(cause), throwableClass, predicate);
         }
      } else {
         return error;
      }
   }
}
