package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionSummonWeaponsLootTable;

public class IgnitionKeySummonToolItem extends SummonToolItem {
   public static float finalDamageModifier = 0.25F;

   public IgnitionKeySummonToolItem() {
      super("junkmech", FollowPosition.WALK_CLOSE, 1.0F, 2000, IncursionSummonWeaponsLootTable.incursionSummonWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(10.0F).setUpgradedValue(1.0F, 12.833337F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ignitionkeytip1"));
      return tooltips;
   }

   @Override
   public GameDamage getAttackDamage(InventoryItem item) {
      return super.getAttackDamage(item).modFinalMultiplier(finalDamageModifier);
   }

   @Override
   public int getAttackDamageValue(InventoryItem item, Attacker attacker) {
      GameDamage damage = this.getAttackDamage(item);
      if (attacker != null) {
         Mob attackOwner = attacker.getAttackOwner();
         if (attackOwner != null) {
            damage = damage.modDamage(attackOwner.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue());
         }
      }

      return Math.round(damage.getBuffedDamage(attacker) / (1.0F / finalDamageModifier));
   }

   @Override
   public GameTooltips getSpaceTakenTooltip(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "ignitionkeytip2"));
      tooltips.add(Localization.translate("itemtooltip", "ignitionkeytip3"));
      return tooltips;
   }

   @Override
   public float getSummonSpaceTaken(InventoryItem item, ItemAttackerMob attackerMob) {
      return attackerMob != null ? attackerMob.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue() : super.getSummonSpaceTaken(item, attackerMob);
   }
}
