package necesse.level.gameObject.furniture;

import java.awt.Rectangle;
import necesse.entity.objectEntity.BedObjectEntity;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;

public interface SettlerBedObject {
   default BedObjectEntity getBedObjectEntity(Level level, int tileX, int tileY) {
      LevelObject levelObject = this.getSettlerBedMasterLevelObject(level, tileX, tileY);
      return levelObject.object.getCurrentObjectEntity(level, tileX, tileY, BedObjectEntity.class);
   }

   boolean isMasterBedObject(Level var1, int var2, int var3);

   LevelObject getSettlerBedMasterLevelObject(Level var1, int var2, int var3);

   Rectangle getSettlerBedTileRectangle(Level var1, int var2, int var3);
}
