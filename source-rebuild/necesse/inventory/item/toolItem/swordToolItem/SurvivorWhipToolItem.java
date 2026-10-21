package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Ray;
import necesse.engine.util.tween.Easings;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.entity.projectile.pathProjectile.SurvivorWhipProjectile;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class SurvivorWhipToolItem extends SwordToolItem {
   protected FloatUpgradeValue projectileResilienceGain = new FloatUpgradeValue(0.0F, 0.0F);

   public SurvivorWhipToolItem() {
      super(650, null);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(800);
      this.attackDamage.setBaseValue(55.0F).setUpgradedValue(1.0F, 190.0F);
      this.attackRange.setBaseValue(200).setUpgradedValue(1.0F, 300);
      this.knockback.setBaseValue(75);
      this.projectileResilienceGain.setBaseValue(3.0F);
      this.canBeUsedForRaids = false;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
      this.attackXOffset = 16;
      this.attackYOffset = 16;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "survivorwhiptip"), 350);
      tooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "survivorwhipquote", "Bo"));
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
      float frontAttackRange = this.getAttackRange(item);
      float behindAttackRange = frontAttackRange;
      Float attackDir = GameMath.normalize(x - attackerMob.x, y - attackerMob.y);
      CollisionFilter collisionFilter = new CollisionFilter().projectileCollision();
      Ray<LevelObjectHit> frontHit = GameUtils.castRayFirstHit(level, attackerMob.x, attackerMob.y, attackDir.x, attackDir.y, frontAttackRange, collisionFilter);
      if (frontHit != null) {
         frontAttackRange = GameMath.limit(
            (float)GameMath.getExactDistance(attackerMob.x, attackerMob.y, frontHit.x2, frontHit.y2) + 32.0F, 32.0F, frontAttackRange
         );
      }

      Ray<LevelObjectHit> behindHit = GameUtils.castRayFirstHit(
         level, attackerMob.x, attackerMob.y, -attackDir.x, -attackDir.y, behindAttackRange, collisionFilter
      );
      if (behindHit != null) {
         behindAttackRange = GameMath.limit(
            (float)GameMath.getExactDistance(attackerMob.x, attackerMob.y, behindHit.x2, behindHit.y2) + 32.0F, 32.0F, behindAttackRange
         );
      }

      Projectile projectile = new SurvivorWhipProjectile(
         attackerMob,
         x,
         y,
         this.getAttackAnimTime(item, attackerMob),
         frontAttackRange,
         behindAttackRange,
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob),
         item.item.getUpgradeTier(item) > 0.0F
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item, this.projectileResilienceGain)));
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(projectile);
      return item;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      if (attackProgress < 0.5F) {
         attackProgress *= 2.0F;
         attackProgress = GameMath.limit(GameMath.lerp(attackProgress, 0.0F, 1.5F), 0.0F, 1.0F);
         attackProgress = Easings.CubicIn.ease(attackProgress);
         drawOptions.rotation(this.getSwingRotation(item, drawOptions.dir, attackProgress) - 110.0F);
      } else {
         attackProgress = (attackProgress - 0.5F) * 2.0F;
         attackProgress = GameMath.limit(GameMath.lerp(attackProgress, 0.0F, 1.5F), 0.0F, 1.0F);
         attackProgress = Easings.CubicIn.ease(attackProgress);
         drawOptions.rotation(this.getSwingRotation(item, drawOptions.dir, 1.0F - attackProgress) - 110.0F);
      }
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return (int)(this.getAttackRange(item) * 0.9F);
   }

   @Override
   protected SoundSettings getSwingSound() {
      return null;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return null;
   }
}
