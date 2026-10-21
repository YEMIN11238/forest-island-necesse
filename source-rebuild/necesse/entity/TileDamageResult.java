package necesse.entity;

import java.util.ArrayList;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.level.maps.LevelObject;
import necesse.level.maps.LevelTile;

public class TileDamageResult extends AbstractDamageResult {
   public TileDamageResult(
      DamagedObjectEntity damagedObjectEntity,
      LevelTile levelTile,
      LevelObject levelObject,
      int totalDamage,
      int addedDamage,
      boolean destroyed,
      ArrayList<ItemPickupEntity> itemsDropped,
      boolean showEffects,
      int mouseX,
      int mouseY
   ) {
      super(damagedObjectEntity, levelTile, levelObject, totalDamage, addedDamage, destroyed, itemsDropped, showEffects, mouseX, mouseY);
   }
}
