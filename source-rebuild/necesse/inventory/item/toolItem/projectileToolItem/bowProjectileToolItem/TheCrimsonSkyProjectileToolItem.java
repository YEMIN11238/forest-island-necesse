package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LineHitbox;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.CrimsonSkyArrowProjectile;
import necesse.entity.projectile.Projectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemControllerInteract;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.arrowItem.ArrowItem;
import necesse.inventory.lootTable.presets.IncursionBowWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.incursion.IncursionData;

public class TheCrimsonSkyProjectileToolItem extends BowProjectileToolItem implements ItemInteractAction {
   public int projectileMaxHeight;
   public int specialAttackProjectileCount;

   public TheCrimsonSkyProjectileToolItem() {
      super(1900, IncursionBowWeaponsLootTable.incursionBowWeapons);
      this.attackAnimTime.setBaseValue(500);
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(90.0F).setUpgradedValue(1.0F, 110.83337F);
      this.velocity.setBaseValue(350);
      this.attackRange.setBaseValue(1600);
      this.attackXOffset = 12;
      this.attackYOffset = 38;
      this.projectileMaxHeight = 600;
      this.specialAttackProjectileCount = 5;
      this.resilienceGain.setBaseValue(1.0F);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.25F;
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      int travelTime = (int)Projectile.getTravelTimeMillis(this.getProjectileVelocity(item, attackerMob), this.projectileMaxHeight * 2);
      return this.applyInaccuracy(attackerMob, item, this.getPredictedItemAttackerAttackPositionMillis(target, travelTime));
   }

   public CrimsonSkyArrowProjectile getTheCrimsonSkyProjectile(
      Level level, int x, int y, Mob owner, GameDamage damage, float velocity, int knockback, float resilienceGain
   ) {
      Float targetPoints = new Float(x, y);
      Float normalizedVector = GameMath.normalize(targetPoints.x - owner.x, targetPoints.y - owner.y);
      RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
         level,
         owner.x,
         owner.y,
         normalizedVector.x,
         normalizedVector.y,
         targetPoints.distance(owner.x, owner.y),
         0,
         new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock)
      );
      if (!hits.isEmpty()) {
         Ray<LevelObjectHit> first = hits.getLast();
         targetPoints.x = (float)first.x2;
         targetPoints.y = (float)first.y2;
      }

      return new CrimsonSkyArrowProjectile(
         level, owner, owner.x, owner.y, owner.x, owner.y - 1.0F, velocity, this.projectileMaxHeight, damage, resilienceGain, knockback, targetPoints, false
      );
   }

   @Override
   protected void addExtraBowTooltips(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      super.addExtraBowTooltips(tooltips, item, perspective, blackboard);
   }

   @Override
   protected void addAmmoTooltips(ListGameTooltips tooltips, InventoryItem item) {
      super.addAmmoTooltips(tooltips, item);
      tooltips.add(Localization.translate("itemtooltip", "thecrimsonskytip"), 400);
      tooltips.add(Localization.translate("itemtooltip", "thecrimsonskysecondarytip"), 400);
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.rotation(-85.0F);
   }

   @Override
   public Projectile getProjectile(
      Level level,
      int x,
      int y,
      ItemAttackerMob owner,
      InventoryItem item,
      int seed,
      ArrowItem arrow,
      boolean consumeAmmo,
      float velocity,
      int range,
      GameDamage damage,
      int knockback,
      float resilienceGain,
      GNDItemMap mapContent
   ) {
      return this.getTheCrimsonSkyProjectile(level, x, y, owner, damage, velocity, knockback, resilienceGain);
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return !attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.THE_CRIMSON_SKY_COOLDOWN);
   }

   @Override
   public float getItemCooldownPercent(InventoryItem item, PlayerMob perspective) {
      return perspective.buffManager.getBuffDurationLeftSeconds(BuffRegistry.Debuffs.THE_CRIMSON_SKY_COOLDOWN) / 8.0F;
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level,
      final int x,
      final int y,
      final ItemAttackerMob attackerMob,
      int attackHeight,
      final InventoryItem item,
      ItemAttackSlot slot,
      int seed,
      GNDItemMap mapContent
   ) {
      final GameRandom random = new GameRandom(seed);
      attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.THE_CRIMSON_SKY_COOLDOWN, attackerMob, 8.0F, null), false);
      this.consumeLife(10, attackerMob, item);

      for (int i = 0; i < this.specialAttackProjectileCount; i++) {
         level.entityManager
            .events
            .addHidden(
               new WaitForSecondsEvent(i / 10.0F) {
                  @Override
                  public void onWaitOver() {
                     Float targetPoints = new Float(x, y);
                     int rndX = random.getIntBetween(-75, 75);
                     int rndY = random.getIntBetween(-75, 75);
                     targetPoints.x += rndX;
                     targetPoints.y += rndY;
                     RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
                        this.level,
                        attackerMob.x,
                        attackerMob.y,
                        targetPoints.x - attackerMob.x,
                        targetPoints.y - attackerMob.y,
                        targetPoints.distance(attackerMob.x, attackerMob.y),
                        0,
                        new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock)
                     );
                     if (!hits.isEmpty()) {
                        Ray<LevelObjectHit> first = hits.getLast();
                        targetPoints.x = (float)first.x2;
                        targetPoints.y = (float)first.y2;
                     }

                     GameDamage specialAttackDmg = TheCrimsonSkyProjectileToolItem.this.getAttackDamage(item).modFinalMultiplier(1.25F);
                     CrimsonSkyArrowProjectile projectile = new CrimsonSkyArrowProjectile(
                        this.level,
                        attackerMob,
                        attackerMob.x,
                        attackerMob.y,
                        attackerMob.x,
                        attackerMob.y - 1.0F,
                        TheCrimsonSkyProjectileToolItem.this.getProjectileVelocity(item, attackerMob),
                        TheCrimsonSkyProjectileToolItem.this.projectileMaxHeight,
                        specialAttackDmg,
                        TheCrimsonSkyProjectileToolItem.this.getResilienceGain(item),
                        TheCrimsonSkyProjectileToolItem.this.getKnockback(item, attackerMob),
                        targetPoints,
                        false
                     );
                     projectile.getUniqueID(random);
                     attackerMob.addAndSendAttackerProjectile(projectile);
                  }
               }
            );
      }

      return item;
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
   public ItemControllerInteract getControllerInteract(
      Level level,
      PlayerMob player,
      InventoryItem item,
      boolean beforeObjectInteract,
      int interactDir,
      LinkedList<Rectangle> mobInteractBoxes,
      LinkedList<Rectangle> tileInteractBoxes
   ) {
      Float controllerAimDir = player.getControllerAimDir();
      Point levelPos = this.getControllerAttackLevelPos(level, controllerAimDir.x, controllerAimDir.y, player, item);
      return new ItemControllerInteract(levelPos.x, levelPos.y) {
         @Override
         public DrawOptions getDrawOptions(GameCamera camera) {
            return null;
         }

         @Override
         public void onCurrentlyFocused(GameCamera camera) {
         }
      };
   }

   @Override
   public Point getControllerAttackLevelPos(Level level, float aimDirX, float aimDirY, PlayerMob player, InventoryItem item) {
      float range = 500.0F;
      return new Point((int)(player.x + aimDirX * range), (int)(player.y + aimDirY * range));
   }
}
