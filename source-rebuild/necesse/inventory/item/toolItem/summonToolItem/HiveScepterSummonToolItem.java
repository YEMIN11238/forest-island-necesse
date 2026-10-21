package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;
import necesse.level.maps.Level;

public class HiveScepterSummonToolItem extends SummonToolItem {
   public HiveScepterSummonToolItem() {
      super("bee", FollowPosition.SWARM_CLOSE, 0.25F, 350, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(9.0F).setUpgradedValue(1.0F, 17.500004F);
   }

   @Override
   public GameTooltips getSpaceTakenTooltip(InventoryItem item, PlayerMob perspective) {
      return null;
   }

   @Override
   public void runServerSummon(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      for (int i = 0; i < 1.0F / this.getSummonSpaceTaken(item, attackerMob); i++) {
         super.runServerSummon(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      }
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "hivesceptertip"), 400);
      return tooltips;
   }
}
