package necesse.inventory.item.placeableItem.consumableItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class ItemSetsIncreaseItem extends ChangeItemSetsItem {
   public ItemSetsIncreaseItem(int totalSets) {
      super(totalSets);
      this.rarity = Item.Rarity.UNIQUE;
      this.allowRightClickToConsume = true;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "itemsetsinctip", "number", this.totalSets));
      return tooltips;
   }

   @Override
   public float getMaxSinking(ItemPickupEntity entity) {
      return Math.min(super.getMaxSinking(entity), 0.25F);
   }
}
