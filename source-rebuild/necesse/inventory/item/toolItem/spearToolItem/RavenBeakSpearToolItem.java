package necesse.inventory.item.toolItem.spearToolItem;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.RavenBeakSpearProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.lootTable.presets.IncursionSpearWeaponsLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.IncursionData;

public class RavenBeakSpearToolItem extends SpearToolItem implements ItemInteractAction {
   public int defaultFeatherStrikeCount = 3;
   public int rightClickFeatherCount = 30;

   public RavenBeakSpearToolItem() {
      super(1900, IncursionSpearWeaponsLootTable.incursionSpearWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(600);
      this.attackDamage.setBaseValue(45.0F).setUpgradedValue(1.0F, 52.500015F);
      this.attackRange.setBaseValue(120);
      this.knockback.setBaseValue(50);
      this.resilienceGain.setBaseValue(4.0F).setUpgradedValue(1.0F, 4.0F).setUpgradedValue(10.0F, 6.0F);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.25F;
      this.defaultLootTier = 1.0F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ravenbeakspeartip"), 400);
      tooltips.add(Localization.translate("itemtooltip", "ravenbeakspearsecondarytip"), 400);
      return tooltips;
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      final ItemAttackerMob attackerMob,
      int attackHeight,
      final InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      final int seed,
      GNDItemMap mapContent
   ) {
      InventoryItem out = super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      if (animAttack == 0) {
         final GameRandom random = new GameRandom(seed);
         final Float dir = GameMath.normalize(x - attackerMob.x, y - attackerMob.y + attackHeight);
         Mob mount = attackerMob.getMount();
         if (mount != null) {
            attackHeight -= mount.getRiderDrawYOffset();
         }

         final int finalAttackHeight = attackHeight;
         final float playerStartX = attackerMob.x;
         final float playerStartY = attackerMob.y;

         for (int i = 0; i < this.defaultFeatherStrikeCount; i++) {
            level.entityManager
               .events
               .addHidden(
                  new WaitForSecondsEvent(0.1F * i) {
                     @Override
                     public void onWaitOver() {
                        int rndX = random.getOneOf(random.getIntBetween(-10, -5), random.getIntBetween(5, 10));
                        int rndY = random.getOneOf(random.getIntBetween(-10, -5), random.getIntBetween(5, 10));
                        GameDamage spectralStrikeAttackDamage = RavenBeakSpearToolItem.this.getAttackDamage(item).modFinalMultiplier(0.5F);
                        RavenBeakSpearProjectile projectile = new RavenBeakSpearProjectile(
                           this.level,
                           playerStartX + rndX - dir.x * 25.0F,
                           playerStartY + rndY - dir.y * 25.0F,
                           playerStartX + rndX + dir.x * 90.0F,
                           playerStartY + rndY + dir.y * 90.0F,
                           40.0F,
                           80,
                           spectralStrikeAttackDamage,
                           attackerMob,
                           finalAttackHeight
                        );
                        projectile.setLevel(this.level);
                        projectile.resetUniqueID(new GameRandom(seed));
                        attackerMob.addAndSendAttackerProjectile(projectile, () -> {
                           projectile.moveDist(RavenBeakSpearToolItem.this.getAttackRange(item) - 35);
                           projectile.traveledDistance = 0.0F;
                           projectile.setAngle(
                              projectile.getAngle() + RavenBeakSpearToolItem.this.getRandomAngleOffsetBasedOnDirection(dir.x, dir.y, rndX, rndY)
                           );
                        });
                     }
                  }
               );
         }
      }

      return out;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.ravenBeakSpear);
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return !attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.RAVENBEAK_SPEAR_COOLDOWN);
   }

   @Override
   public float getItemCooldownPercent(InventoryItem item, PlayerMob perspective) {
      return perspective.buffManager.getBuffDurationLeftSeconds(BuffRegistry.Debuffs.RAVENBEAK_SPEAR_COOLDOWN) / 12.0F;
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level,
      int x,
      int y,
      final ItemAttackerMob attackerMob,
      int attackHeight,
      final InventoryItem item,
      ItemAttackSlot slot,
      final int seed,
      GNDItemMap mapContent
   ) {
      InventoryItem out = super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, 0, seed, mapContent);
      final GameRandom random = new GameRandom(seed);
      attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.RAVENBEAK_SPEAR_COOLDOWN, attackerMob, 12.0F, null), false);
      final Float dir = GameMath.normalize(x - attackerMob.x, y - attackerMob.y + attackHeight);
      Mob mount = attackerMob.getMount();
      if (mount != null) {
         attackHeight -= mount.getRiderDrawYOffset();
      }

      final int finalAttackHeight = attackHeight;
      final float playerStartX = attackerMob.x;
      final float playerStartY = attackerMob.y;

      for (int i = 0; i < this.rightClickFeatherCount; i++) {
         level.entityManager
            .events
            .addHidden(
               new WaitForSecondsEvent(0.05F * i) {
                  @Override
                  public void onWaitOver() {
                     int rndX = random.getOneOf(random.getIntBetween(-10, 10));
                     int rndY = random.getOneOf(random.getIntBetween(-10, 10));
                     GameDamage spectralStrikeAttackDamage = RavenBeakSpearToolItem.this.getAttackDamage(item).modFinalMultiplier(0.25F);
                     RavenBeakSpearProjectile projectile = new RavenBeakSpearProjectile(
                        this.level,
                        playerStartX + rndX - dir.x * 25.0F,
                        playerStartY + rndY - dir.y * 25.0F,
                        playerStartX + rndX + dir.x * 90.0F,
                        playerStartY + rndY + dir.y * 90.0F,
                        40.0F,
                        80,
                        spectralStrikeAttackDamage,
                        attackerMob,
                        finalAttackHeight
                     );
                     projectile.setLevel(this.level);
                     projectile.resetUniqueID(new GameRandom(seed));
                     attackerMob.addAndSendAttackerProjectile(projectile, () -> {
                        projectile.moveDist(RavenBeakSpearToolItem.this.getAttackRange(item) - 35);
                        projectile.traveledDistance = 0.0F;
                        projectile.setAngle(projectile.getAngle() + RavenBeakSpearToolItem.this.getRandomAngleOffsetBasedOnDirection(dir.x, dir.y, rndX, rndY));
                     });
                  }
               }
            );
      }

      return out;
   }

   public float getRandomAngleOffsetBasedOnDirection(float dirX, float dirY, float randomXAngle, float randomYAngle) {
      if (dirX < 0.5F && dirX > -0.5F && dirY > 0.0F) {
         return randomXAngle;
      } else if (dirX < 0.5F && dirX > -0.5F && dirY < 0.0F) {
         return randomXAngle;
      } else if (dirX > 0.0F && dirY < 0.5F && dirY > -0.5F) {
         return randomYAngle;
      } else {
         return dirX < 0.0F && dirY < 0.5F && dirY > -0.5F ? randomYAngle : 0.0F;
      }
   }
}
