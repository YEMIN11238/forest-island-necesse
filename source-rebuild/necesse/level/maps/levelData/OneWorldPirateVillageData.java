package necesse.level.maps.levelData;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedList;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class OneWorldPirateVillageData extends SingleRegionBasedLevelData<OneWorldPirateVillageData.PirateVillageRegionData> implements RegionLevelDataComponent {
   public static OneWorldPirateVillageData getPirateVillageData(Level level, boolean createNewIfNull) {
      LevelData data = level.getLevelData("oneworldpiratevillagedata");
      if (data instanceof OneWorldPirateVillageData) {
         return (OneWorldPirateVillageData)data;
      } else if (createNewIfNull) {
         OneWorldPirateVillageData newData = new OneWorldPirateVillageData();
         level.addLevelData("oneworldpiratevillagedata", newData);
         return newData;
      } else {
         return null;
      }
   }

   public void addRegionSaveData(SaveData save, OneWorldPirateVillageData.PirateVillageRegionData data) {
      if (!data.pirateTileRectangles.isEmpty()) {
         for (Rectangle tileRectangle : data.pirateTileRectangles) {
            SaveData rectangleSave = new SaveData("PIRATE_TILE_RECTANGLE");
            rectangleSave.addPoint("tile", new Point(tileRectangle.x, tileRectangle.y));
            rectangleSave.addPoint("size", new Point(tileRectangle.width, tileRectangle.height));
            save.addSaveData(rectangleSave);
         }
      }
   }

   public OneWorldPirateVillageData.PirateVillageRegionData loadRegionData(Region region, LoadData save) {
      OneWorldPirateVillageData.PirateVillageRegionData data = new OneWorldPirateVillageData.PirateVillageRegionData(region.regionX, region.regionY);

      for (LoadData rectangleSave : save.getLoadDataByName("PIRATE_TILE_RECTANGLE")) {
         Point tile = rectangleSave.getPoint("tile", null, false);
         if (tile != null) {
            Point size = rectangleSave.getPoint("size", null, false);
            if (size != null) {
               data.pirateTileRectangles.add(new Rectangle(tile.x, tile.y, size.x, size.y));
            }
         }
      }

      return data.pirateTileRectangles.isEmpty() ? null : data;
   }

   public boolean isPirateTile(int tileX, int tileY) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      OneWorldPirateVillageData.PirateVillageRegionData data = this.getDataInRegion(regionX, regionY);
      return data != null && data.pirateTileRectangles.stream().anyMatch(r -> r.contains(tileX, tileY));
   }

   public void addPirateTileRectangle(Rectangle tileRectangle) {
      int regionStartX = GameMath.getRegionCoordByTile(tileRectangle.x);
      int regionStartY = GameMath.getRegionCoordByTile(tileRectangle.y);
      int regionEndX = GameMath.getRegionCoordByTile(tileRectangle.x + tileRectangle.width - 1);
      int regionEndY = GameMath.getRegionCoordByTile(tileRectangle.y + tileRectangle.height - 1);

      for (int regionX = regionStartX; regionX <= regionEndX; regionX++) {
         for (int regionY = regionStartY; regionY <= regionEndY; regionY++) {
            Region region = this.getLevel().regionManager.getRegion(regionX, regionY, true);
            if (region != null) {
               region.isPirateVillageRegion = true;
               int regionTileX = GameMath.getTileCoordByRegion(regionX);
               int regionTileY = GameMath.getTileCoordByRegion(regionY);
               Rectangle regionTileRectangle = new Rectangle(regionTileX, regionTileY, 16, 16);
               Rectangle intersection = tileRectangle.intersection(regionTileRectangle);
               OneWorldPirateVillageData.PirateVillageRegionData data = this.computeDataInRegion(regionX, regionY, (p, value) -> {
                  if (value == null) {
                     value = new OneWorldPirateVillageData.PirateVillageRegionData(p.x, p.y);
                  }

                  return value;
               });
               data.pirateTileRectangles.add(intersection);
            }
         }
      }
   }

   public static class PirateVillageRegionData extends SingleRegionBasedLevelData.RegionData {
      protected LinkedList<Rectangle> pirateTileRectangles = new LinkedList<>();

      public PirateVillageRegionData(int regionX, int regionY) {
         super(regionX, regionY);
      }
   }
}
