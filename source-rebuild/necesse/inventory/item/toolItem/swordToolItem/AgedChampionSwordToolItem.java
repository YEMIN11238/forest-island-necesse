package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.Point;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.AgedChampionWaveProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;

public class AgedChampionSwordToolItem extends SwordToolItem {
   protected FloatUpgradeValue projectileResilienceGain = new FloatUpgradeValue(0.0F, 0.0F);

   public AgedChampionSwordToolItem() {
      super(1650, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(60.0F).setUpgradedValue(1.0F, 81.66669F);
      this.attackRange.setBaseValue(70);
      this.attackXOffset = 8;
      this.attackYOffset = 8;
      this.knockback.setBaseValue(75);
      this.resilienceGain.setBaseValue(2.0F).setUpgradedValue(1.0F, 2.0F).setUpgradedValue(10.0F, 3.0F);
      this.projectileResilienceGain.setBaseValue(1.0F);
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "agedchampionswordtip"), 400);
      return tooltips;
   }

   @Override
   public void setupAttackMapContent(GNDItemMap map, Level level, int x, int y, ItemAttackerMob attackerMob, int seed, InventoryItem item) {
      super.setupAttackMapContent(map, level, x, y, attackerMob, seed, item);
      map.setBoolean("expectWaveProjectile", this.shouldFireProjectile(attackerMob, item));
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
      item = super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      boolean shouldFireProjectile = this.shouldFireProjectile(attackerMob, item);
      if (attackerMob.isPlayer && level.isServer() && !shouldFireProjectile && mapContent.getBoolean("expectWaveProjectile")) {
         attackerMob.sendHealthPacket(false);
      }

      if (shouldFireProjectile) {
         if (level.isClient()) {
            SoundManager.playSound(
               GameResources.venomSlasher, SoundEffect.effect(attackerMob).volume(0.25F).pitch(GameRandom.globalRandom.getFloatBetween(0.95F, 1.05F))
            );
         }

         float rangeMod = 10.0F;
         float velocity = 200.0F;
         float finalVelocity = Math.round(
            this.getEnchantment(item).applyModifierLimited(ToolItemModifiers.VELOCITY, ToolItemModifiers.VELOCITY.defaultBuffManagerValue)
               * velocity
               * attackerMob.buffManager.getModifier(BuffModifiers.PROJECTILE_VELOCITY)
         );
         Projectile projectile = new AgedChampionWaveProjectile(
            level,
            attackerMob.x,
            attackerMob.y,
            x,
            y,
            finalVelocity,
            (int)(this.getAttackRange(item) * rangeMod),
            new GameDamage(this.getAttackDamage(item).damage * 0.75F),
            attackerMob
         );
         projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item, this.projectileResilienceGain)));
         projectile.resetUniqueID(new GameRandom(seed));
         attackerMob.addAndSendAttackerProjectile(projectile, 20);
      }

      return item;
   }

   public boolean shouldFireProjectile(ItemAttackerMob mob, InventoryItem item) {
      return mob.getHealthPercent() >= 1.0F;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return this.shouldFireProjectile(mob, item) ? this.getAttackRange(item) * 9 : this.getAttackRange(item);
   }

   @Override
   public boolean canItemAttackerHitTarget(ItemAttackerMob attackerMob, float fromX, float fromY, Mob target, InventoryItem item) {
      float distance = attackerMob.getDistance(target);
      int attackRange = this.getAttackRange(item);
      if (distance < attackRange) {
         return super.canItemAttackerHitTarget(attackerMob, fromX, fromY, target, item);
      } else {
         return distance < attackRange * 5
            ? !attackerMob.getLevel()
               .collides(
                  new LineHitbox(fromX, fromY, target.x, target.y, 45.0F),
                  attackerMob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target)
               )
            : false;
      }
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      float velocity = 140.0F;
      float finalVelocity = Math.round(
         this.getEnchantment(item).applyModifierLimited(ToolItemModifiers.VELOCITY, ToolItemModifiers.VELOCITY.defaultBuffManagerValue)
            * velocity
            * attackerMob.buffManager.getModifier(BuffModifiers.PROJECTILE_VELOCITY)
      );
      return this.applyInaccuracy(attackerMob, item, this.getPredictedItemAttackerAttackPosition(attackerMob, target, finalVelocity, -20.0F));
   }
}
