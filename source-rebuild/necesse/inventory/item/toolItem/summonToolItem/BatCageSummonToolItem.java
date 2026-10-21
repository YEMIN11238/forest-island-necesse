package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;

public class BatCageSummonToolItem extends SummonToolItem {
   public BatCageSummonToolItem() {
      super("vampiricbat", FollowPosition.FLYING_CIRCLE, 1.0F, 400, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackDamage.setBaseValue(12.0F).setUpgradedValue(1.0F, 35.000008F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "batcagetip"), 400);
      return tooltips;
   }
}
