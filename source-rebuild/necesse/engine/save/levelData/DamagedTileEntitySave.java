package necesse.engine.save.levelData;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.DamagedObjectEntity;
import necesse.level.maps.Level;

public class DamagedTileEntitySave {
   public static DamagedObjectEntity loadSave(LoadData save, Level level) {
      try {
         int tileX = save.getInt("tileX");
         int tileY = save.getInt("tileY");
         level.regionManager.ensureTileIsLoaded(tileX, tileY);
         DamagedObjectEntity damagedTile = new DamagedObjectEntity(level, tileX, tileY);
         damagedTile.applyLoadData(save);
         return damagedTile;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static SaveData getSave(DamagedObjectEntity damagedTile) {
      SaveData save = new SaveData("DAMAGEDTILE");
      save.addInt("tileX", damagedTile.tileX);
      save.addInt("tileY", damagedTile.tileY);
      damagedTile.addSaveData(save);
      return save;
   }
}
