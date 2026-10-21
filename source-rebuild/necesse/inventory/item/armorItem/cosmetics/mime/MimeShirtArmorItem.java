package necesse.inventory.item.armorItem.cosmetics.mime;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.CosmeticArmorLootTable;

public class MimeShirtArmorItem extends ChestArmorItem {
   public MimeShirtArmorItem() {
      super(0, 0, Item.Rarity.UNCOMMON, "mimeshirt", "mimeshirtarms", CosmeticArmorLootTable.cosmeticArmor);
   }

   @Override
   protected ListGameTooltips getBaseTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips baseTooltips = super.getBaseTooltips(item, perspective, blackboard);
      baseTooltips.add(GameUtils.getDevQuoteTooltip(new LocalMessage("itemtooltip", "mimesetquote"), "Stormes", 300));
      return baseTooltips;
   }
}
