package necesse.level.maps.levelData;

import necesse.engine.util.GameMath;
import necesse.engine.util.PointHashMap;
import necesse.entity.manager.RegionUnloadedListenerEntityComponent;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class PathBreakDownLevelData extends LevelData implements RegionUnloadedListenerEntityComponent {
   protected PointHashMap<PathBreakDownLevelData.RegionBreakData> regionBreakData = new PointHashMap<>();

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public void onRegionUnloaded(Region region) {
      this.regionBreakData.remove(region.regionX, region.regionY);
   }

   protected PathBreakDownLevelData.RegionBreakData getRegionData(int regionX, int regionY, boolean createIfMissing) {
      return !createIfMissing
         ? this.regionBreakData.get(regionX, regionY)
         : this.regionBreakData.compute(regionX, regionY, (key, old) -> old == null ? new PathBreakDownLevelData.RegionBreakData() : old);
   }

   protected PathBreakDownLevelData.PathBreakData getTileData(int tileX, int tileY, boolean createIfMissing) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      PathBreakDownLevelData.RegionBreakData regionData = this.getRegionData(regionX, regionY, createIfMissing);
      if (regionData == null) {
         return null;
      } else {
         return !createIfMissing
            ? regionData.tileBreakData.get(tileX, tileY)
            : regionData.tileBreakData.compute(tileX, tileY, (tile, old) -> old == null ? new PathBreakDownLevelData.PathBreakData(tileX, tileY) : old);
      }
   }

   public int getFinalDamage(int tileX, int tileY, int damage) {
      return this.getTileData(tileX, tileY, true).doDamage(damage);
   }

   public void clear(int tileX, int tileY) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      PathBreakDownLevelData.RegionBreakData regionData = this.getRegionData(regionX, regionY, false);
      if (regionData != null) {
         regionData.tileBreakData.remove(tileX, tileY);
      }
   }

   public static PathBreakDownLevelData getPathBreakDownData(Level level) {
      LevelData pathBreak = level.getLevelData("pathbreak");
      if (pathBreak instanceof PathBreakDownLevelData) {
         return (PathBreakDownLevelData)pathBreak;
      }

      PathBreakDownLevelData newPathBreak = new PathBreakDownLevelData();
      level.addLevelData("pathbreak", newPathBreak);
      return newPathBreak;
   }

   protected class PathBreakData {
      public final int tileX;
      public final int tileY;
      public int objectID;
      public float damageBuffer;
      public long lastDamageDoneTime;

      public PathBreakData(int tileX, int tileY) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.objectID = PathBreakDownLevelData.this.getLevel().getObjectID(tileX, tileY);
      }

      public int doDamage(int damage) {
         float floatDamage = damage;
         GameObject object = PathBreakDownLevelData.this.getLevel().getObject(this.tileX, this.tileY);
         if (object.getID() != this.objectID) {
            this.damageBuffer = 0.0F;
         } else {
            long timeSinceLastDamage = PathBreakDownLevelData.this.getLevel().getTime() - this.lastDamageDoneTime;
            if (timeSinceLastDamage > 60000L) {
               this.damageBuffer = 0.0F;
            }

            if (timeSinceLastDamage < 1000L) {
               float percentSince = (float)timeSinceLastDamage / 1000.0F;
               floatDamage = Math.max(0.1F, floatDamage * percentSince);
            }
         }

         this.damageBuffer += floatDamage;
         this.lastDamageDoneTime = PathBreakDownLevelData.this.getLevel().getTime();
         if (this.damageBuffer >= 1.0F) {
            int finalDamage = (int)this.damageBuffer;
            this.damageBuffer -= finalDamage;
            return finalDamage;
         } else {
            return 0;
         }
      }
   }

   protected class RegionBreakData {
      PointHashMap<PathBreakDownLevelData.PathBreakData> tileBreakData = new PointHashMap<>();
   }
}
