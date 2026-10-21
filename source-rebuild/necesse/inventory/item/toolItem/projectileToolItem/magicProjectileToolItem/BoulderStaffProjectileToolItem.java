package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.BoulderStaffProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class BoulderStaffProjectileToolItem extends MagicProjectileToolItem {
   public BoulderStaffProjectileToolItem() {
      super(850, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(400);
      this.attackCooldownTime.setBaseValue(600);
      this.attackDamage.setBaseValue(55.0F).setUpgradedValue(1.0F, 147.00005F);
      this.velocity.setBaseValue(60);
      this.attackXOffset = 6;
      this.attackYOffset = 6;
      this.attackRange.setBaseValue(800);
      this.knockback.setBaseValue(50);
      this.manaCost.setBaseValue(2.0F).setUpgradedValue(1.0F, 2.0F);
      this.resilienceGain.setBaseValue(1.0F);
      this.itemAttackerProjectileCanHitWidth = 32.0F;
      this.itemAttackerPredictionDistanceOffset = -20.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.swingRotationInv(attackProgress, 150.0F, 45.0F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "boulderstafftip"));
      return tooltips;
   }

   @Override
   public InventoryItem onAttack(
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
      Projectile projectile = new BoulderStaffProjectile(
         level,
         attackerMob,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         this.getAttackRange(item),
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob)
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(projectile, 20);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return null;
   }
}
