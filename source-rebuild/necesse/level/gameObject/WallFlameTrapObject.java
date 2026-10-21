package necesse.level.gameObject;

import necesse.entity.mobs.GameDamage;
import necesse.entity.objectEntity.FlameTrapObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;

public class WallFlameTrapObject extends WallTrapObject {
   public int range = 8;
   public int perpendicularWidth = 0;
   public GameDamage damage;

   public WallFlameTrapObject(WallObject wallObject) {
      super(wallObject, "flametrap");
   }

   public WallFlameTrapObject(WallObject wallObject, float toolTier, ToolType toolType) {
      super(wallObject, "flametrap", toolTier, toolType);
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new FlameTrapObjectEntity(level, x, y);
   }
}
