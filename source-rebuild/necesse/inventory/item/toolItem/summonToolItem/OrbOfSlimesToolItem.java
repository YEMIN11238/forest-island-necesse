package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionSummonWeaponsLootTable;

public class OrbOfSlimesToolItem extends SummonToolItem {
   public OrbOfSlimesToolItem() {
      super("orbofslimesslime", FollowPosition.SLIME_CIRCLE_MOVEMENT, 1.0F, 1900, IncursionSummonWeaponsLootTable.incursionSummonWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(41.0F).setUpgradedValue(1.0F, 51.333347F);
      this.attackXOffset = 15;
      this.attackYOffset = 10;
   }

   @Override
   public int getItemAttackerStoppingDistance(ItemAttackerMob mob, InventoryItem item, int attackRange) {
      return 96;
   }

   @Override
   public int getItemAttackerRunAwayDistance(ItemAttackerMob attackerMob, InventoryItem item) {
      return 64;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "orbofslimestip"), 400);
      return tooltips;
   }
}
