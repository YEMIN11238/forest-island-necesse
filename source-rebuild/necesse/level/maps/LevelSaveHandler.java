package necesse.level.maps;

import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.network.server.SaveHandlerList;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.SingleSaveHandler;
import necesse.engine.save.LevelSave;

public class LevelSaveHandler extends SaveHandlerList {
   public LevelSaveHandler(PerformanceTimerManager performanceTimer, Server server, Level level) {
      super(performanceTimer, "levels", 10);
      boolean shouldSave = level.shouldSave();
      if (shouldSave) {
         this.addHandler(
            new SingleSaveHandler(performanceTimer, "levelData", () -> LevelSave.getSave(level).saveScript(server.world.fileSystem.getLevelFile(level)))
         );
      }

      this.addHandler(level.regionManager.getSaveHandler(performanceTimer, server));
      if (shouldSave) {
         this.addHandler(new SingleSaveHandler(performanceTimer, "presets", () -> level.getWorldEntity().saveGeneratedPresets(level.getIdentifier())));
      }
   }
}
