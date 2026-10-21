package necesse.level.maps.biomes.temple;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.level.gameObject.GameObject;

public class TempleArenaLevel extends TempleLevel {
   public static final int ARENA_SIZE = 40;
   public static final int LAVA_EDGE_SIZE = 7;
   public static final int EDGE_SIZE = 40;
   private static final int TOTAL_SIZE = 134;

   public TempleArenaLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public TempleArenaLevel(LevelIdentifier identifier, WorldEntity worldEntity) {
      super(identifier, 134, 134, worldEntity);
      this.baseBiome = BiomeRegistry.TEMPLE;
      this.isCave = true;
      this.isProtected = true;
      this.generateLevel();
   }

   public void generateLevel() {
      GameRandom random = new GameRandom(this.getSeed());
      int wall = ObjectRegistry.getObjectID("deepsandstonewall");
      int sandBrick = TileRegistry.getTileID("sandbrick");
      int woodFloor = TileRegistry.getTileID("woodfloor");
      int lavaTile = TileRegistry.getTileID("lavatile");
      int fireChalice = ObjectRegistry.getObjectID("templefirechalice");
      TicketSystemList<Integer> tiles = new TicketSystemList<>();
      tiles.addObject(100, sandBrick);
      tiles.addObject(50, woodFloor);

      for (int x = 0; x < this.tileWidth; x++) {
         for (int y = 0; y < this.tileHeight; y++) {
            this.setTile(x, y, tiles.getRandomObject(random));
         }
      }

      int centerX = this.tileWidth / 2;
      int centerY = this.tileHeight / 2;

      for (int x = 0; x < this.tileWidth; x++) {
         for (int y = 0; y < this.tileHeight; y++) {
            double dist = new Float(centerX, centerY).distance(x, y);
            if (dist <= 20.5) {
               this.setObject(x, y, 0);
            } else if (dist <= 27.5) {
               this.setObject(x, y, 0);
               this.setTile(x, y, lavaTile);
            } else {
               this.setObject(x, y, wall);
            }
         }
      }

      this.placeObjectAngle(centerX, centerY, 14.0F, -90.0F, fireChalice, 0, 0.0F, 0.0F);
      this.placeObjectAngle(centerX, centerY, 14.0F, 45.0F, fireChalice, 0, 0.0F, 0.0F);
      this.placeObjectAngle(centerX, centerY, 14.0F, -45.0F, fireChalice, 0, 0.0F, 0.0F);
      this.placeObjectAngle(centerX, centerY, 14.0F, 0.0F, fireChalice, 0, 0.0F, 0.0F);
      this.placeObjectAngle(centerX, centerY, 14.0F, 180.0F, fireChalice, 0, -1.0F, 0.0F);
      this.placeObjectAngle(centerX, centerY, 14.0F, 135.0F, fireChalice, 0, 0.0F, 0.0F);
      this.placeObjectAngle(centerX, centerY, 14.0F, -135.0F, fireChalice, 0, 0.0F, 0.0F);
      Point exitPosition = getExitPosition();
      GameObject exitObject = ObjectRegistry.getObject("templeexit");
      exitObject.placeObject(this, exitPosition.x, exitPosition.y, 0, false);
      Mob mob = MobRegistry.getMob("fallenwizard", this);
      Float bossPos = getBossPosition();
      this.entityManager.addMob(mob, bossPos.x, bossPos.y);
   }

   private void placeObjectAngle(int centerX, int centerY, float radius, float angle, int objectID, int rotation, float xOffset, float yOffset) {
      GameObject object = ObjectRegistry.getObject(objectID);
      Float dir = GameMath.getAngleDir(angle);
      object.placeObject(this, (int)(centerX + dir.x * radius + xOffset), (int)(centerY + dir.y * radius + yOffset), rotation, false);
   }

   public static Point getExitPosition() {
      return new Point(66, 82);
   }

   public static Float getBossPosition() {
      return new Float(2160.0F, 2160.0F);
   }
}
