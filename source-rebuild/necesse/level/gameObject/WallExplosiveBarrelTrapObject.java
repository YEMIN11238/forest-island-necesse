package necesse.level.gameObject;

import necesse.entity.objectEntity.BarrelTrapObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;

public class WallExplosiveBarrelTrapObject extends WallTrapObject {
   public WallExplosiveBarrelTrapObject(WallObject wallObject) {
      super(wallObject, "barreltrap");
   }

   public WallExplosiveBarrelTrapObject(WallObject wallObject, float toolTier, ToolType toolType) {
      super(wallObject, "barreltrap", toolTier, toolType);
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new BarrelTrapObjectEntity(level, x, y);
   }
}
