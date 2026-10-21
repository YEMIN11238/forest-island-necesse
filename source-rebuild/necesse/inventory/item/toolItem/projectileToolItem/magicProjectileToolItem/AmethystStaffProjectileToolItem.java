package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AmethystGlyphEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.staticBuffs.LifeEssenceStacksBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.AmethystStaffProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class AmethystStaffProjectileToolItem extends MagicProjectileToolItem implements ItemInteractAction {
   protected int rightClickLifeEssenceCost = LifeEssenceStacksBuff.STACKS_PER_LIFE_ESSENCE;
   protected IntUpgradeValue beams = new IntUpgradeValue(0, 0.0F);

   public AmethystStaffProjectileToolItem() {
      super(1000, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(575);
      this.attackDamage.setBaseValue(29.0F).setUpgradedValue(1.0F, 67.20002F);
      this.velocity.setBaseValue(800);
      this.attackXOffset = 14;
      this.attackYOffset = 4;
      this.attackRange.setBaseValue(800);
      this.knockback.setBaseValue(40);
      this.manaCost.setBaseValue(2.5F).setUpgradedValue(1.0F, 2.5F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.beams.setBaseValue(3).setUpgradedValue(1.0F, 3);
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY).forEachItemSprite(i -> i.itemRotateOffset(45.0F));
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "amethyststafftip1", "value", this.beams.getValue(this.getUpgradeTier(item))), 400);
      tooltips.add(
         Localization.translate("itemtooltip", "amethyststafftip2", "value", this.rightClickLifeEssenceCost / LifeEssenceStacksBuff.STACKS_PER_LIFE_ESSENCE),
         400
      );
      tooltips.add(Localization.translate("itemtooltip", "amethyststafftip3"), 400);
      return tooltips;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, final ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient() && !mapContent.getBoolean("glyphAttack")) {
         int beamAmount = this.beams.getValue(this.getUpgradeTier(item));

         for (int i = 0; i < beamAmount; i++) {
            level.entityManager.events.addHidden(new WaitForSecondsEvent(0.075F * (i + 1)) {
               @Override
               public void onWaitOver() {
                  AmethystStaffProjectileToolItem.this.playAttackSound(attackerMob);
               }
            });
         }
      }
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.amethystStaff).volume(0.15F).basePitch(0.9F).pitchVariance(0.1F);
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      final int x,
      final int y,
      final ItemAttackerMob attackerMob,
      int attackHeight,
      final InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      GameRandom random = new GameRandom(seed);
      if (!attackerMob.isPlayer && this.canCastGlyph(attackerMob)) {
         int checkForMobsRange = 384;
         Mob targetChosen = level.entityManager
            .players
            .streamInRegionsInRange(attackerMob.x, attackerMob.y, checkForMobsRange)
            .filter(p -> !p.canBeTargeted(attackerMob, attackerMob.getPvPOwner()))
            .filter(m -> m.getDistance(attackerMob) <= checkForMobsRange)
            .min(Comparator.comparing(Mob::getSpeed))
            .orElse(null);
         Float targetPos;
         if (targetChosen != null) {
            targetPos = Projectile.getPredictedTargetPos(targetChosen, attackerMob.x, attackerMob.y, 1000.0F, -10.0F);
         } else {
            targetPos = new Float(attackerMob.x, attackerMob.y);
         }

         AmethystGlyphEvent glyphEvent = this.getGlyphEvent(level, (int)targetPos.x, (int)targetPos.y, attackerMob, random);
         attackerMob.addAndSendAttackerLevelEvent(glyphEvent);
         mapContent.setBoolean("glyphAttack", true);
      } else {
         final AtomicInteger givesLifeEssence = new AtomicInteger(3);
         int beamAmount = this.beams.getValue(this.getUpgradeTier(item));

         for (int i = 0; i < beamAmount; i++) {
            final GameRandom beamRandom = random.nextSeeded(27 * (i + 1));
            level.entityManager
               .events
               .addHidden(
                  new WaitForSecondsEvent(0.075F * (i + 1)) {
                     @Override
                     public void onWaitOver() {
                        Projectile projectile = new AmethystStaffProjectile(
                           this.level,
                           attackerMob,
                           attackerMob.x,
                           attackerMob.y,
                           x,
                           y,
                           AmethystStaffProjectileToolItem.this.getProjectileVelocity(item, attackerMob),
                           AmethystStaffProjectileToolItem.this.getAttackRange(item),
                           AmethystStaffProjectileToolItem.this.getAttackDamage(item),
                           AmethystStaffProjectileToolItem.this.getKnockback(item, attackerMob),
                           givesLifeEssence
                        );
                        projectile.setModifier(new ResilienceOnHitProjectileModifier(AmethystStaffProjectileToolItem.this.getResilienceGain(item)));
                        projectile.getUniqueID(beamRandom);
                        attackerMob.addAndSendAttackerProjectile(projectile, 70, (beamRandom.nextFloat() - 0.5F) * 10.0F);
                     }
                  }
               );
         }

         this.consumeMana(attackerMob, item);
      }

      return item;
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return this.canCastGlyph(attackerMob);
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      GameRandom random = new GameRandom(seed);
      AmethystGlyphEvent event = this.getGlyphEvent(level, x, y, attackerMob, random);
      level.entityManager.events.add(event);
      return item;
   }

   public boolean canCastGlyph(Mob mob) {
      return mob.buffManager.getStacks(BuffRegistry.LIFE_ESSENCE) >= this.rightClickLifeEssenceCost;
   }

   public AmethystGlyphEvent getGlyphEvent(Level level, int x, int y, Mob player, GameRandom random) {
      if (level.isServer()) {
         for (int i = 0; i < this.rightClickLifeEssenceCost; i++) {
            player.buffManager.removeStack(BuffRegistry.LIFE_ESSENCE, true, true);
         }
      }

      Float targetPoints = new Float(x, y);
      Float normalizedVector = GameMath.normalize(targetPoints.x - player.x, targetPoints.y - player.y);
      RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
         level,
         player.x,
         player.y,
         normalizedVector.x,
         normalizedVector.y,
         targetPoints.distance(player.x, player.y),
         0,
         new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock)
      );
      if (!hits.isEmpty()) {
         Ray<LevelObjectHit> first = hits.getLast();
         targetPoints.x = (float)first.x2;
         targetPoints.y = (float)first.y2;
      }

      return new AmethystGlyphEvent(player, (int)targetPoints.x, (int)targetPoints.y, random);
   }
}
