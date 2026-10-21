package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.BoneSpikeMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.BoneSpikesProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class RuneboundScepterProjectileToolItem extends MagicProjectileToolItem implements ItemInteractAction {
   protected IntUpgradeValue maxSpikes = new IntUpgradeValue(0, 0.0F);

   public RuneboundScepterProjectileToolItem() {
      super(800, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(600);
      this.attackDamage.setBaseValue(48.0F).setUpgradedValue(1.0F, 95.200035F);
      this.knockback.setBaseValue(80);
      this.attackXOffset = 16;
      this.attackYOffset = 20;
      this.velocity.setBaseValue(120);
      this.attackCooldownTime.setBaseValue(400);
      this.attackRange.setBaseValue(450);
      this.manaCost.setBaseValue(2.0F).setUpgradedValue(1.0F, 2.0F);
      this.itemAttackerProjectileCanHitWidth = 24.0F;
      this.maxSpikes.setBaseValue(3).setUpgradedValue(1.0F, 4).setUpgradedValue(5.0F, 5);
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.swingRotationInv(attackProgress, 150.0F, 45.0F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "runeboundscepternewtip1"), 400);
      tooltips.add(Localization.translate("itemtooltip", "runeboundscepternewtip2"), 400);
      tooltips.add(Localization.translate("itemtooltip", "runeboundscepternewtip3", "amount", this.maxSpikes.getValue(this.getUpgradeTier(item))), 400);
      return tooltips;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return (int)(this.getAttackRange(item) * 0.7F);
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
      float distance = attackerMob.getDistance(x, y);
      int attackRange = this.getAttackRange(item);
      float limitedAttackRange = GameMath.limit(distance, attackRange * 0.3F, attackRange);
      if (!attackerMob.isPlayer) {
         if (!this.getNearbySpikes(level, attackerMob).isEmpty() && !attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.RUNEBOUND_SCEPTER_COOLDOWN)) {
            attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.RUNEBOUND_SCEPTER_COOLDOWN, attackerMob, 4.0F, null), false);
            this.onLevelInteract(level, x, y, attackerMob, attackHeight, item, slot, seed, mapContent);
         } else {
            this.shootSpike(level, x, y, attackerMob, item, seed, (int)limitedAttackRange, 1);
         }
      } else {
         this.shootSpike(level, x, y, attackerMob, item, seed, (int)limitedAttackRange, this.maxSpikes.getValue(this.getUpgradeTier(item)));
      }

      return item;
   }

   private void shootSpike(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int seed, int limitedAttackRange, int maxSpikes) {
      BoneSpikesProjectile projectile = new BoneSpikesProjectile(
         level,
         attackerMob,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         limitedAttackRange,
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob),
         maxSpikes
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(projectile);
      this.consumeMana(attackerMob, item);
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return !this.getNearbySpikes(level, attackerMob).isEmpty();
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      if (attackerMob.isServer()) {
         List<BoneSpikeMob> nearbySpikes = this.getNearbySpikes(level, attackerMob);
         if (!nearbySpikes.isEmpty()) {
            this.consumeMana(this.getManaCost(item) * 2.0F, attackerMob);

            for (BoneSpikeMob nearbySpike : nearbySpikes) {
               if (!nearbySpike.isCracking) {
                  nearbySpike.startCrackAbility.runAndSend(500);
               }
            }
         }
      }

      return item;
   }

   public List<BoneSpikeMob> getNearbySpikes(Level level, Mob owner) {
      int checkInRange = 640;
      return level.entityManager
         .mobs
         .streamInRegionsInRange(owner.x, owner.y, checkInRange)
         .filter(s -> s instanceof BoneSpikeMob)
         .map(s -> (BoneSpikeMob)s)
         .filter(s -> s.mobOwner == owner)
         .filter(s -> s.getDistance(owner) <= checkInRange)
         .collect(Collectors.toList());
   }

   @Override
   protected void playAttackSound(Mob source) {
      SoundManager.playSound(GameResources.swoosh2, SoundEffect.effect(source).volume(0.4F).pitch(0.3F).falloffDistance(2000));
   }
}
