package necesse.level.maps.presets;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.objectEntity.AscendedPylonObjectEntity;

public class AscendedPylonAreaPreset extends Preset {
   public AscendedPylonAreaPreset(int size, GameRandom random, ArrayList<Integer> attackIndexes) {
      super(size, size);
      int mid = size / 2;
      int maxDistance = size / 2 * 32;
      TicketSystemList<Integer> tileList = new TicketSystemList<>();
      tileList.addObject(100, -1);
      tileList.addObject(100, TileRegistry.ascendedCorruptionID);
      tileList.addObject(100, TileRegistry.ascendedGrowthID);
      tileList.addObject(25, TileRegistry.ascendedVoidID);
      tileList.addObject(75, TileRegistry.getTileID("deadwoodfloor"));

      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            float distance = (float)new Point(mid * 32 + 16, mid * 32 + 16).distance(x * 32 + 16, y * 32 + 16);
            float distancePerc = distance / maxDistance;
            if (distancePerc < 0.5F) {
               this.setTile(x, y, tileList.getRandomObject(random));
               this.setObject(x, y, 0);
            } else if (distancePerc <= 1.0F) {
               float chance = Math.abs((distancePerc - 0.5F) * 2.0F - 1.0F) * 2.0F;
               if (random.getChance(chance)) {
                  this.setTile(x, y, tileList.getRandomObject(random));
                  this.setObject(x, y, 0);
               }
            }
         }
      }

      int totalColumns = random.getIntBetween(8, 10);
      float columnAngle = random.nextInt(360);
      float anglePerColumn = 360.0F / totalColumns;
      int columnID = ObjectRegistry.getObjectID("cryptcolumn");

      for (int i = 0; i < totalColumns; i++) {
         columnAngle += random.getFloatOffset(anglePerColumn, anglePerColumn / 10.0F);
         Float dir = GameMath.getAngleDir(columnAngle);
         float distance = size / 3.0F;
         int tileX = (int)(mid + dir.x * distance);
         int tileY = (int)(mid + dir.y * distance);
         this.setObject(tileX, tileY, columnID);
      }

      this.setObject(mid, mid, ObjectRegistry.getObjectID("ascendedpylon"));
      if (attackIndexes != null && !attackIndexes.isEmpty()) {
         this.addCustomApply(mid, mid, 0, (level, levelX, levelY, dirx, blackboard) -> {
            AscendedPylonObjectEntity objectEntity = level.entityManager.getObjectEntity(levelX, levelY, AscendedPylonObjectEntity.class);
            if (objectEntity != null) {
               objectEntity.possibleAttackIndexes = new ArrayList<>(attackIndexes);
            }

            return null;
         }, true);
      }
   }
}
