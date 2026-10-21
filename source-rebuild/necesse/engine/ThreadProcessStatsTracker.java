package necesse.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import necesse.engine.util.GameMath;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class ThreadProcessStatsTracker extends Thread {
   public String statsDirectoryPath;

   public ThreadProcessStatsTracker(String statsDirectoryPath) {
      super("Process stats tracker");
      this.statsDirectoryPath = statsDirectoryPath;
      this.setDaemon(true);
   }

   @Override
   public void run() {
      long startTime = System.currentTimeMillis();
      if (this.statsDirectoryPath == null) {
         this.statsDirectoryPath = GlobalData.appDataPath() + "stats/";
      } else if (!this.statsDirectoryPath.endsWith("/") && !this.statsDirectoryPath.endsWith("\\")) {
         this.statsDirectoryPath = this.statsDirectoryPath + "/";
      }

      String filePath = this.statsDirectoryPath + new SimpleDateFormat("yyyy-MM-dd HH'h'mm'm'ss's'").format(new Date()) + ".csv";
      File file = new File(filePath);

      try {
         File parent = file.getParentFile();
         if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.out.println("Could not create folders to stats tracking: " + file.getAbsolutePath());
            return;
         }

         FileOutputStream stream = new FileOutputStream(file);
         PrintStream printStream = new PrintStream(stream);
         final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
         RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
         String name = runtime.getName();
         int pid = name.contains("@") ? Integer.parseInt(name.substring(0, name.indexOf("@"))) : -1;
         OperatingSystem os = GameSystemInfo.getOS();
         System.out.println("Started stats tracking to: " + file.getAbsolutePath() + ", process id: " + pid);
         ArrayList<ThreadProcessStatsTracker.CSVLine> lines = new ArrayList<>();
         lines.add(new ThreadProcessStatsTracker.CSVLine("Time") {
            @Override
            public Object getData(long timeSinceStart, OSProcess process) {
               return timeSinceStart / 1000.0;
            }
         });
         final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
         lines.add(new ThreadProcessStatsTracker.CSVLine("Heap usage") {
            @Override
            public Object getData(long timeSinceStart, OSProcess process) {
               return GameMath.toDecimals(memory.getHeapMemoryUsage().getUsed() / 1048576.0, 1);
            }
         });
         lines.add(new ThreadProcessStatsTracker.CSVLine("Thread count") {
            @Override
            public Object getData(long timeSinceStart, OSProcess process) {
               return threads.getThreadCount();
            }
         });
         OSProcess process;
         if (pid != -1) {
            process = os.getProcess(pid);
            lines.add(new ThreadProcessStatsTracker.CSVLine("Process rss") {
               @Override
               public Object getData(long timeSinceStart, OSProcess process) {
                  return process == null ? "" : GameMath.toDecimals(process.getResidentSetSize() / 1048576.0, 1);
               }
            });
         } else {
            process = null;
            GameLog.warn.println("Could not get process stats, invalid pid in name: " + name);
         }

         lines.forEach(line -> printStream.print(line.name + ","));
         printStream.println();

         while (!this.isInterrupted()) {
            long currentTime = System.currentTimeMillis();
            long timeSinceStart = currentTime - startTime;
            lines.forEach(line -> printStream.print(line.getData(timeSinceStart, process).toString() + ","));
            printStream.println();
            if (process != null) {
               process.updateAttributes();
            }

            try {
               long timeTaken = System.currentTimeMillis() - currentTime;
               long sleepTime = Math.max(500L - timeTaken, 0L);
               Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private abstract static class CSVLine {
      public final String name;

      public CSVLine(String name) {
         this.name = name;
      }

      public abstract Object getData(long var1, OSProcess var3);
   }
}
