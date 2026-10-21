package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class GrizzboltSphereSummonToolItem extends SummonToolItem {
   public static float finalDamageModifier = 0.25F;

   public GrizzboltSphereSummonToolItem() {
      super("grizzboltsummon", FollowPosition.WALK_CLOSE, 1.0F, 1750, null);
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 24.500008F);
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
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY);
   }

   @Override
   public GameTooltips getSpaceTakenTooltip(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "grizzboltquote", null));
      tooltips.add(Localization.translate("itemtooltip", "grizzboltspheretip1"));
      tooltips.add(Localization.translate("itemtooltip", "grizzboltspheretip2"));
      return tooltips;
   }

   @Override
   public float getSummonSpaceTaken(InventoryItem item, ItemAttackerMob attackerMob) {
      return attackerMob != null ? attackerMob.buffManager.getModifier(BuffModifiers.MAX_SUMMONS).intValue() : super.getSummonSpaceTaken(item, attackerMob);
   }
}
