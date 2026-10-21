package necesse.level.maps.presets;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;

public class RandomLootAreaPreset extends Preset {
   public RandomLootAreaPreset(GameRandom random, int size, String columnStringID, TicketSystemList<String> mobStringIDs) {
      super(size, size);
      int mid = size / 2;
      int maxDistance = size / 2 * 32;
      int woodFloor = TileRegistry.getTileID("woodfloor");
      int[] breakObjects = new int[]{ObjectRegistry.getObjectID("crate"), ObjectRegistry.getObjectID("vase"), ObjectRegistry.getObjectID("coinstack")};

      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            float distance = (float)new Point(mid * 32 + 16, mid * 32 + 16).distance(x * 32 + 16, y * 32 + 16);
            float distancePerc = distance / maxDistance;
            if (distancePerc < 0.5F) {
               this.setTile(x, y, woodFloor);
               this.setObject(x, y, 0);
            } else if (distancePerc <= 1.0F) {
               float chance = Math.abs((distancePerc - 0.5F) * 2.0F - 1.0F) * 2.0F;
               if (random.getChance(chance)) {
                  if (random.getChance(0.75F)) {
                     this.setTile(x, y, woodFloor);
                  }

                  this.setObject(x, y, 0);
               }
            }

            if (distancePerc <= 1.0F && this.getObject(x, y) != -1 && random.getChance(0.12F)) {
               this.setObject(x, y, breakObjects[random.nextInt(breakObjects.length)]);
            }
         }
      }

      int totalColumns = random.getIntBetween(3, 4);
      float columnAngle = random.nextInt(360);
      float anglePerColumn = 360.0F / totalColumns;
      int columnID = ObjectRegistry.getObjectID(columnStringID);

      for (int i = 0; i < totalColumns; i++) {
         columnAngle += random.getFloatOffset(anglePerColumn, anglePerColumn / 10.0F);
         Float dir = GameMath.getAngleDir(columnAngle);
         float distance = size / 4.0F;
         int tileX = (int)(mid + dir.x * distance);
         int tileY = (int)(mid + dir.y * distance);
         this.setObject(tileX, tileY, columnID);
      }

      int totalMobs = size / 3;

      for (int i = 0; i < totalMobs; i++) {
         if (!mobStringIDs.isEmpty()) {
            float angle = random.nextFloat() * 360.0F;
            float dx = (float)Math.cos(Math.toRadians(angle));
            float dy = (float)Math.sin(Math.toRadians(angle));
            int distance = size / 4 * 32;
            int tileX = GameMath.getTileCoordinate(mid * 32 + 16 + dx * distance);
            int tileY = GameMath.getTileCoordinate(mid * 32 + 16 + dy * distance);
            if (this.getObject(tileX, tileY) == 0) {
               this.addMob(mobStringIDs.getRandomObject(random), tileX, tileY, false);
            }
         }
      }
   }

   public RandomLootAreaPreset(GameRandom random, int size, String columnStringID, String... mobStringIDs) {
      this(random, size, columnStringID, toTicketSystem(mobStringIDs));
   }

   private static TicketSystemList<String> toTicketSystem(String... mobStringIDs) {
      TicketSystemList<String> out = new TicketSystemList<>();

      for (String mobStringID : mobStringIDs) {
         out.addObject(100, mobStringID);
      }

      return out;
   }
}
