package necesse.engine.commands.serverCommands.setupCommand;

import java.awt.Point;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.level.maps.Level;

public abstract class FindAndBuildArenaCustom extends FindArenaWorldSetup {
   public final int size;

   public FindAndBuildArenaCustom(int size, int dimension, String... biomeIDs) {
      super(dimension, biomeIDs);
      this.size = size;
   }

   @Override
   public Point findArenaSpawnTile(Server server, ServerClient client, Level level) {
      for (int i = 0; i < 100; i++) {
         int tileX;
         if (level.tileWidth <= 0) {
            tileX = GameRandom.globalRandom.getIntBetween(0, 1000);
         } else {
            tileX = GameRandom.globalRandom.getIntBetween(this.size, level.tileWidth - 1 - this.size * 2);
         }

         int tileY;
         if (level.tileHeight <= 0) {
            tileY = GameRandom.globalRandom.getIntBetween(0, 1000);
         } else {
            tileY = GameRandom.globalRandom.getIntBetween(this.size, level.tileHeight - 1 - this.size * 2);
         }

         if (!level.getTile(tileX, tileY).isLiquid) {
            this.buildArena(server, client, level, tileX, tileY, this.size);
            WorldSetup.updateClientsLevel(level, tileX, tileY, this.size + 15);
            return new Point(tileX, tileY);
         }
      }

      return null;
   }

   public abstract void buildArena(Server var1, ServerClient var2, Level var3, int var4, int var5, int var6);
}
