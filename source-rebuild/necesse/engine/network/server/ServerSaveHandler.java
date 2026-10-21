package necesse.engine.network.server;

import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.entity.manager.WorldSavedEntityComponent;
import necesse.level.maps.Level;
import necesse.level.maps.LevelSaveHandler;

public class ServerSaveHandler extends SaveHandlerList {
   public static int saveClientsPerTick = 1;
   public static int regionsPerTickOnClient = 20;
   public static int regionsPerTickOnServer = 10;
   public ArrayList<ServerSaveHandler.SaveInterruptedOrFinishedEventHandler> saveEvents = new ArrayList<>();

   public ServerSaveHandler(PerformanceTimerManager performanceTimer, Server server) {
      super(performanceTimer, null, 10);
      if (server.getLocalClient() == null) {
         this.ticksToSkip = 1;
      }

      this.addHandler(
         new SingleSaveHandler(
            performanceTimer,
            "worldData",
            () -> {
               server.world.saveWorldEntity();
               server.world.settings.saveSettings();
               server.world
                  .worldEntity
                  .dataComponentManager
                  .<WorldSavedEntityComponent, Class<WorldSavedEntityComponent>>streamAll(WorldSavedEntityComponent.class)
                  .forEach(WorldSavedEntityComponent::onWorldSaved);
            }
         )
      );
      this.addHandler(new ClientsSaveHandler(performanceTimer, server));

      for (Level level : server.world.levelManager.getLoadedLevels()) {
         this.addHandler(new LevelSaveHandler(performanceTimer, server, level));
      }
   }

   public interface SaveInterruptedOrFinishedEventHandler {
      default void onSaveInterrupted(ServerSaveHandler newSaveHandler) {
      }

      default void onSaveFinished() {
      }
   }
}
