import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import necesse.engine.GameLaunch;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.gfx.gameTexture.GameTexture;

public class PreAntialiasTextures {
   public static void main(String[] args) {
      HashMap<String, String> options = GameLaunch.parseLaunchOptions(args);
      String fullLaunchParameters = GameUtils.join(GameLaunch.quoteArgs(args), " ");
      if (args.length > 0) {
         System.out.println("Launched PreAntialiasTextures with arguments: " + fullLaunchParameters);
      }

      String foldersArg = options.remove("folders");
      String[] foldersArgSplit = foldersArg == null ? new String[0] : foldersArg.split(";");
      String filesArg = options.remove("files");
      String[] filesArgSplit = filesArg == null ? new String[0] : filesArg.split(";");
      Object lock = new Object();
      LinkedList<File> files = new LinkedList<>();

      for (String path : foldersArgSplit) {
         File currentFolder = new File(path);
         if (!currentFolder.exists()) {
            System.out.println("Could not find folder: " + path);
         } else {
            GameUtils.collectFiles(currentFolder, files, f -> f.getName().endsWith(".png"));
         }
      }

      for (String path : filesArgSplit) {
         File currentFile = new File(path);
         if (!currentFile.exists()) {
            System.out.println("Could not find file: " + path);
         } else if (currentFile.isDirectory()) {
            System.out.println("Submitted file is a directory. Use -folders instead for: " + path);
         } else {
            if (currentFile.getName().endsWith(".png")) {
               System.out.println("Submitted file is not a png: " + path);
            }

            files.add(currentFile);
         }
      }

      System.out.println("Running PreAntialiasing on " + files.size() + " found textures...");
      int totalFiles = files.size();
      AtomicInteger findDoneFiles = new AtomicInteger();
      AtomicInteger foundFiles = new AtomicInteger();
      AtomicLong lastFindAnnounceTime = new AtomicLong(System.currentTimeMillis());
      System.out.println("Found " + totalFiles + " to handle");
      System.out.println("Finding textures that needs PreAntialiasing...");
      ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), defaultThreadFactory());
      LinkedList<Future<PreAntialiasTextures.PreAntialiasTask>> findTasks = new LinkedList<>();

      for (File file : files) {
         FutureTask<PreAntialiasTextures.PreAntialiasTask> task = new FutureTask<>(
            () -> {
               GameTexture texture = new GameTexture(file.getPath() + " preAntialias", GameUtils.loadByteFile(file));
               boolean shouldHandle = texture.runPreAntialias(false);
               synchronized (lock) {
                  if (shouldHandle) {
                     foundFiles.addAndGet(1);
                  }

                  int doneFiles = findDoneFiles.addAndGet(1);
                  long timeSinceLastAnnouncex = System.currentTimeMillis() - lastFindAnnounceTime.get();
                  if (timeSinceLastAnnouncex >= 1000L) {
                     float percentCompletex = (float)doneFiles / totalFiles;
                     System.out
                        .println(
                           "Tested "
                              + doneFiles
                              + " / "
                              + totalFiles
                              + " ("
                              + GameMath.toDecimals(percentCompletex * 100.0F, 2)
                              + "%) textures and found "
                              + foundFiles.get()
                              + " to process so far"
                        );
                     lastFindAnnounceTime.set(System.currentTimeMillis());
                  }
               }

               return shouldHandle ? new PreAntialiasTextures.PreAntialiasTask(file, texture) : null;
            }
         );
         findTasks.add(task);
         executor.execute(task);
      }

      AtomicLong lastHandleAnnounceTime = new AtomicLong(System.currentTimeMillis());
      int filesPreAntialiased = 0;

      for (Future<PreAntialiasTextures.PreAntialiasTask> task : findTasks) {
         try {
            PreAntialiasTextures.PreAntialiasTask preAntialiasTask = task.get();
            if (preAntialiasTask != null) {
               preAntialiasTask.texture.runPreAntialias(false);
               preAntialiasTask.texture.saveTextureImage(preAntialiasTask.file.getAbsolutePath(), false);
               filesPreAntialiased++;
               synchronized (lock) {
                  long timeSinceLastAnnounce = System.currentTimeMillis() - lastHandleAnnounceTime.get();
                  if (timeSinceLastAnnounce >= 1000L) {
                     float percentComplete = (float)filesPreAntialiased / foundFiles.get();
                     System.out
                        .println(
                           "Processed "
                              + filesPreAntialiased
                              + " / "
                              + foundFiles.get()
                              + " textures so far ("
                              + GameMath.toDecimals(percentComplete * 100.0F, 2)
                              + "%)"
                        );
                     lastHandleAnnounceTime.set(System.currentTimeMillis());
                  }
               }
            }
         } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
         }
      }

      System.out.println("Completed PreAntialiasing. " + filesPreAntialiased + " textures were changed out of " + totalFiles + " tested");
      executor.shutdown();
   }

   public static ThreadFactory defaultThreadFactory() {
      AtomicInteger threadNum = new AtomicInteger(0);
      return r -> {
         Thread thread = new Thread(null, r, "texture-preAntialias-" + threadNum.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      };
   }

   private static class PreAntialiasTask {
      public File file;
      public GameTexture texture;

      public PreAntialiasTask(File file, GameTexture texture) {
         this.file = file;
         this.texture = texture;
      }
   }
}
