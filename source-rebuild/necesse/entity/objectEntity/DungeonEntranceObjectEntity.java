package necesse.entity.objectEntity;

import java.util.function.Function;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.dungeon.DungeonArenaLevel;
import necesse.level.maps.biomes.dungeon.DungeonLevel;

public class DungeonEntranceObjectEntity extends PortalObjectEntity {
   public static LevelIdentifier getDungeonLevelIdentifier(LevelIdentifier currentIdentifier, int tileX, int tileY) {
      return new LevelIdentifier(currentIdentifier.stringID + "-dungeon" + tileX + "x" + tileY);
   }

   public static LevelIdentifier getDungeonArenaLevelIdentifier(LevelIdentifier dungeonIdentifier) {
      return new LevelIdentifier(dungeonIdentifier.stringID + "-arena");
   }

   public DungeonEntranceObjectEntity(Level level, int x, int y) {
      super(level, "dungeonentrance", x, y, level.getIdentifier(), x, y);
   }

   @Override
   public void init() {
      super.init();
      this.destinationTileX = Integer.MIN_VALUE;
      this.destinationTileY = Integer.MIN_VALUE;
   }

   @Override
   public void use(Server server, ServerClient client) {
      client.setFallbackLevel(this.getLevel(), this.tileX, this.tileY);
      boolean destinationIsArena = this.getLevel() instanceof DungeonLevel;
      LevelIdentifier currentIdentifier = this.getLevel().getIdentifier();
      if (destinationIsArena) {
         this.destinationIdentifier = new LevelIdentifier(currentIdentifier.stringID + "-arena");
      } else {
         this.destinationIdentifier = new LevelIdentifier(currentIdentifier.stringID + "-dungeon" + this.tileX + "x" + this.tileY);
      }

      Function<LevelIdentifier, Level> levelGenerator = identifier -> {
         Level level;
         if (destinationIsArena) {
            level = new DungeonArenaLevel(identifier, server.world.worldEntity);
         } else {
            level = new DungeonLevel(identifier, server.world.worldEntity);
         }

         this.getLevel().childLevels.add(this.destinationIdentifier);
         level.setFallbackLevel(this.getLevel(), this.tileX, this.tileY);
         return level;
      };
      this.teleportClientToAroundDestination(client, levelGenerator, level -> {
         if (this.destinationTileX != Integer.MIN_VALUE && this.destinationTileY != Integer.MIN_VALUE) {
            PortalObjectEntity exitEntity = level.entityManager.getObjectEntity(this.destinationTileX, this.destinationTileY, PortalObjectEntity.class);
            if (exitEntity != null) {
               exitEntity.destinationTileX = this.tileX;
               exitEntity.destinationTileY = this.tileY;
               exitEntity.destinationIdentifier = currentIdentifier;
               return true;
            }
         }

         int exitID = ObjectRegistry.getObjectID("dungeonexit");
         boolean foundExit = false;

         for (int tileX = 0; tileX < level.tileWidth; tileX++) {
            for (int tileY = 0; tileY < level.tileHeight; tileY++) {
               level.regionManager.ensureTileIsLoaded(tileX, tileY);
               if (level.getObjectID(tileX, tileY) == exitID) {
                  this.destinationTileX = tileX;
                  this.destinationTileY = tileY;
                  foundExit = true;
                  break;
               }
            }

            if (foundExit) {
               break;
            }
         }

         if (!foundExit) {
            this.destinationTileX = level.tileWidth / 2;
            this.destinationTileY = level.tileHeight / 2;
         }

         GameObject exitObject = ObjectRegistry.getObject(exitID);
         if (level.getObjectID(this.destinationTileX, this.destinationTileY) != exitID) {
            exitObject.placeObject(level, this.destinationTileX, this.destinationTileY, 0, false);
         }

         PortalObjectEntity exitEntity = level.entityManager.getObjectEntity(this.destinationTileX, this.destinationTileY, PortalObjectEntity.class);
         if (exitEntity != null) {
            exitEntity.destinationTileX = this.tileX;
            exitEntity.destinationTileY = this.tileY;
            exitEntity.destinationIdentifier = currentIdentifier;
         }

         this.runClearMobs(level, this.destinationTileX, this.destinationTileY);
         return true;
      }, true);
   }
}
