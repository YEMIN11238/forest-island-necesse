package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionSummonWeaponsLootTable;

public class CrystallizedSkullSummonToolItem extends SummonToolItem {
   public CrystallizedSkullSummonToolItem() {
      super("rubydragonhead", FollowPosition.FLYING_CIRCLE_FAST, 2.0F, 1900, IncursionSummonWeaponsLootTable.incursionSummonWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(180.0F).setUpgradedValue(1.0F, 210.00006F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "crystallizedskulltip"));
      return tooltips;
   }
}
