package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.TrapperNetProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.arrowItem.ArrowItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.BowWeaponsLootTable;
import necesse.level.maps.Level;

public class CaptorsShortbowProjectileToolItem extends BowProjectileToolItem implements ItemInteractAction {
   protected IntUpgradeValue arrowsFiredUpgradeValue = new IntUpgradeValue(0, 0.0F);

   public CaptorsShortbowProjectileToolItem() {
      super(800, BowWeaponsLootTable.bowWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(750);
      this.attackDamage.setBaseValue(25.0F).setUpgradedValue(1.0F, 105.00003F);
      this.attackRange.setBaseValue(250).setUpgradedValue(1.0F, 275).setUpgradedValue(5.0F, 300).setUpgradedValue(6.0F, 300);
      this.velocity.setBaseValue(120);
      this.attackXOffset = 14;
      this.attackYOffset = 33;
      this.arrowsFiredUpgradeValue.setBaseValue(3).setUpgradedValue(1.0F, 4).setUpgradedValue(5.0F, 5).setUpgradedValue(6.0F, 5);
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return (int)(this.getAttackRange(item) * 0.9F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips preEnchantmentTooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      preEnchantmentTooltips.add(
         Localization.translate("itemtooltip", "captorsshortbowtip1", "amount", this.arrowsFiredUpgradeValue.getValue(this.getUpgradeTier(item)))
      );
      preEnchantmentTooltips.add(Localization.translate("itemtooltip", "captorsshortbowtip2"));
      return preEnchantmentTooltips;
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
      if (!attackerMob.isPlayer && this.canLevelInteract(level, x, y, attackerMob, item)) {
         GameRandom rnd = new GameRandom(seed);
         int rndX = x + rnd.getIntBetween(-25, 25);
         int rndY = y + rnd.getIntBetween(-25, 25);
         return this.throwNet(level, rndX, rndY, attackerMob, item, 65, 1500);
      } else {
         return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      }
   }

   @Override
   protected void fireProjectiles(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int seed, ArrowItem arrow, boolean dropItem, GNDItemMap mapContent
   ) {
      Integer arrowsFired;
      if (!attackerMob.isPlayer && attackerMob.isHostile) {
         arrowsFired = 1;
      } else {
         arrowsFired = this.arrowsFiredUpgradeValue.getValue(this.getUpgradeTier(item));
      }

      item.getGndData().setBoolean("throwingNet", false);
      int angleBetweenArrows = 8;
      int startAngle = -(arrowsFired * angleBetweenArrows / 2);

      for (int i = 0; i < arrowsFired; i++) {
         Projectile projectile = this.getProjectile(level, x, y, attackerMob, item, seed, arrow, false, mapContent);
         projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
         projectile.setAngle(projectile.getAngle() + startAngle);
         projectile.dropItem = false;
         projectile.getUniqueID(new GameRandom(seed));
         attackerMob.addAndSendAttackerProjectile(projectile, this.moveDist);
         startAngle += angleBetweenArrows;
      }
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return !attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.CAPTORS_SHORTBOW_COOLDOWN);
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      this.throwNet(level, x, y, attackerMob, item, 100, 3000);
      return item;
   }

   public InventoryItem throwNet(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int throwSpeed, int nettedDuration) {
      attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.CAPTORS_SHORTBOW_COOLDOWN, attackerMob, 5.0F, null), false);
      item.getGndData().setBoolean("throwingNet", true);
      int xOffset = 5;
      if (attackerMob.getDir() == 3) {
         xOffset = -5;
      }

      Float startPoints = new Float(attackerMob.x + xOffset, attackerMob.y);
      TrapperNetProjectile projectile = new TrapperNetProjectile(
         attackerMob.getLevel(), attackerMob, startPoints.x, startPoints.y, x, y, throwSpeed, 400, new GameDamage(0.0F), 0, nettedDuration
      );
      level.entityManager.projectiles.add(projectile);
      return item;
   }

   @Override
   public GameSprite getAttackSprite(InventoryItem item, PlayerMob player) {
      return item.getGndData().getBoolean("throwingNet") ? null : super.getAttackSprite(item, player);
   }
}
