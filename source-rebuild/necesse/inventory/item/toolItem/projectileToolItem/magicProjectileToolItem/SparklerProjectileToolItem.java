package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.SparklerProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class SparklerProjectileToolItem extends MagicProjectileToolItem {
   public SparklerProjectileToolItem() {
      super(300, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(200);
      this.attackDamage.setBaseValue(10.0F).setUpgradedValue(1.0F, 70.00002F);
      this.velocity.setBaseValue(100);
      this.attackXOffset = 14;
      this.attackYOffset = 4;
      this.attackRange.setBaseValue(250);
      this.manaCost.setBaseValue(0.5F).setUpgradedValue(1.0F, 0.5F);
      this.resilienceGain.setBaseValue(0.5F);
      this.itemAttackerProjectileCanHitWidth = 25.0F;
      this.itemAttackerPredictionDistanceOffset = -20.0F;
      this.canBeUsedForRaids = true;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "sparklertip1"));
      tooltips.add(Localization.translate("itemtooltip", "sparklertip2"));
      return tooltips;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY).forEachItemSprite(i -> i.itemRotateOffset(45.0F));
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
      Projectile projectile = new SparklerProjectile(
         level, attackerMob.x, attackerMob.y, x, y, this.getAttackRange(item), this.getAttackDamage(item), attackerMob
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(projectile, 20);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.sparkler).volume(0.08F).basePitch(0.73F).pitchVariance(0.02F);
   }

   @Override
   protected SoundSettings getSwingSound() {
      return new SoundSettings(GameResources.magicbolt1).volume(0.5F).basePitch(1.2F);
   }
}
