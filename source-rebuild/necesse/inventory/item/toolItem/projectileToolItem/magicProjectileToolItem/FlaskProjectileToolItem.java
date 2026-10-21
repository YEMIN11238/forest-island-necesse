package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.Entity;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public abstract class FlaskProjectileToolItem extends MagicProjectileToolItem {
   public FlaskProjectileToolItem(int enchantCost, OneOfLootItems lootTableCategory) {
      super(enchantCost, lootTableCategory);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 126.00004F);
      this.velocity.setBaseValue(800);
      this.attackXOffset = 8;
      this.attackYOffset = 10;
      this.attackCooldownTime.setBaseValue(500);
      this.attackRange.setBaseValue(300);
      this.manaCost.setBaseValue(1.25F).setUpgradedValue(1.0F, 1.25F);
      this.resilienceGain.setBaseValue(1.0F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = false;
   }

   protected abstract Projectile getProjectile(
      Level var1, Mob var2, float var3, float var4, float var5, float var6, float var7, int var8, GameDamage var9, int var10, GameRandom var11
   );

   @Override
   public GameSprite getAttackSprite(InventoryItem item, PlayerMob player) {
      return null;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient()) {
         SoundManager.playSound(GameResources.swing1, SoundEffect.effect(attackerMob).volume(0.7F).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.0F)));
      }
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      if (this.getAnimInverted(item)) {
         drawOptions.swingRotationInv(attackProgress);
      } else {
         drawOptions.swingRotation(attackProgress);
      }
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      int velocity = this.getProjectileVelocity(item, attackerMob);
      float velocityPercent = 1.0F / (velocity / 500.0F);
      int flaskAirTime = (int)(500.0F * velocityPercent);
      float predictedX = target.x + Entity.getPositionAfterMillis(target.dx, flaskAirTime);
      float predictedY = target.y + Entity.getPositionAfterMillis(target.dy, flaskAirTime);
      return this.applyInaccuracy(attackerMob, item, new Point((int)predictedX, (int)predictedY));
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
      int attackRange = this.getAttackRange(item);
      float distanceFromTarget = GameMath.getExactDistance(attackerMob.x, attackerMob.y, x, y);
      int distance = (int)GameMath.limit(distanceFromTarget, 0.0F, attackRange);
      Float normalizedVector = GameMath.normalize(x - attackerMob.x, y - attackerMob.y);
      CollisionFilter collisionFilter = new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock);
      RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
         level, attackerMob.x, attackerMob.y, normalizedVector.x, normalizedVector.y, distance, 0, collisionFilter
      );
      Ray<LevelObjectHit> first = hits.getLast();
      if (first != null && first.targetHit != null) {
         x = (int)first.x2;
         y = (int)first.y2;
         distance = (int)GameMath.getExactDistance(attackerMob.x, attackerMob.y, x, y);
      }

      float baseDistance = this.attackRange.getValue(0.0F).intValue();
      float distancePercent = distance / baseDistance;
      int velocity = this.getProjectileVelocity(item, attackerMob);
      float baseVelocity = this.velocity.getValue(0.0F).intValue();
      float velocityPercent = 1.0F / (velocity / baseVelocity);
      int flaskAirTime = (int)(baseVelocity * velocityPercent * distancePercent);
      float speed = Entity.getTravelSpeedForMillis(flaskAirTime, distance);
      Projectile projectile = this.getProjectile(
         level,
         attackerMob,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         speed,
         distance,
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob),
         new GameRandom(seed * 2548L)
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(projectile, 20);
      this.consumeMana(attackerMob, item);
      return item;
   }
}
