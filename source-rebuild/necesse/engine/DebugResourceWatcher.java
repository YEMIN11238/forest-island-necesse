package necesse.engine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.GameLoopListener;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.window.GameWindow;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.GameShader;

public class DebugResourceWatcher implements GameLoopListener {
   private static WatchService debugResourceWatcher;

   public DebugResourceWatcher() {
      try {
         debugResourceWatcher = FileSystems.getDefault().newWatchService();
         Path[] directories = new Path[]{Paths.get(GlobalData.rootPath() + "res/"), Paths.get(GlobalData.rootPath() + "locale/")};

         for (Path directory : directories) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
               public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                  dir.register(DebugResourceWatcher.debugResourceWatcher, StandardWatchEventKinds.ENTRY_MODIFY);
                  return super.preVisitDirectory(dir, attrs);
               }
            });
         }

         GlobalData.getCurrentGameLoop().addGameLoopListener(this);
         GameLog.debug.println("Started debug filer watcher. The game will automatically reload shader when files are changed.");
      } catch (IOException e) {
         GameLog.warn.println("Could not create debug resource watcher. See exception below:");
         e.printStackTrace(GameLog.warn);
      }
   }

   @Override
   public void frameTick(TickManager tickManager, GameWindow window) {
      WatchKey key = debugResourceWatcher.poll();
      if (key != null) {
         Path directory = (Path)key.watchable();

         for (WatchEvent<?> pollEvent : key.pollEvents()) {
            Kind<?> kind = pollEvent.kind();
            if (kind != StandardWatchEventKinds.OVERFLOW && pollEvent.context() instanceof Path) {
               Path filePath = directory.resolve((Path)pollEvent.context()).toAbsolutePath();
               String stringFilePath = filePath.toString();
               if (Files.isRegularFile(filePath)) {
                  if (stringFilePath.endsWith(".glsl")) {
                     GameLog.debug.println("Reloading shader file: " + filePath);
                     List<GameShader> affectedShaders = GameResources.getShaders()
                        .values()
                        .stream()
                        .filter(shader -> stringFilePath.endsWith(shader.vertexPath + ".glsl") || stringFilePath.endsWith(shader.fragmentPath + ".glsl"))
                        .collect(Collectors.toList());

                     for (GameShader affectedShader : affectedShaders) {
                        GameLog.debug.println("\t - reloading " + affectedShader);
                        affectedShader.reload();
                     }

                     if (affectedShaders.isEmpty()) {
                        GameLog.debug.println("\t - no shaders affected");
                     }
                  } else if (stringFilePath.endsWith(".lang")) {
                     Localization.reloadLanguageFiles();
                     GameLog.debug.println("Reloading localization file: " + filePath);
                  } else if (stringFilePath.endsWith(".png")) {
                     boolean success = GameTexture.tryReloadTexture(filePath.toString());
                     GameLog.debug.println("Reloading texture file: " + filePath + " - " + (success ? "success" : "failed"));
                  } else if (stringFilePath.endsWith(".ogg")) {
                     boolean success = GameSound.tryReloadSound(filePath.toString());
                     GameLog.debug.println("Reloading sound file: " + filePath + " - " + (success ? "success" : "failed"));
                  }
               }
            }
         }

         key.reset();
      }
   }

   @Override
   public void drawTick(TickManager tickManager) {
   }

   @Override
   public boolean isDisposed() {
      return false;
   }
}
