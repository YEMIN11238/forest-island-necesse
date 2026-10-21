package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.SlimeGreatBowArrowProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.arrowItem.ArrowItem;
import necesse.inventory.lootTable.presets.IncursionGreatbowWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.incursion.IncursionData;

public class SlimeGreatbowProjectileToolItem extends GreatbowProjectileToolItem {
   public int attackSpriteStretch = 8;
   public Color particleColor = new Color(255, 219, 36);
   public int projectileMaxHeight;

   public SlimeGreatbowProjectileToolItem() {
      super(1900, IncursionGreatbowWeaponsLootTable.incursionGreatbowWeapons);
      this.knockback.setBaseValue(10);
      this.projectileMaxHeight = 600;
      this.attackAnimTime.setBaseValue(500);
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(96.0F).setUpgradedValue(1.0F, 122.50003F);
      this.velocity.setBaseValue(350);
      this.attackRange.setBaseValue(1600);
      this.attackXOffset = 12;
      this.attackYOffset = 38;
      this.resilienceGain.setBaseValue(1.0F);
      this.canBeUsedForRaids = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.2F;
      this.useForRaidsOnlyIfObtained = true;
   }

   public SlimeGreatBowArrowProjectile getSlimeGreatbowProjectile(
      Level level, int x, int y, Mob owner, GameDamage damage, float velocity, float range, float knockback, float resilienceGain
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
         Double hitVector = GameMath.normalize(first.x1 - first.x2, first.y1 - first.y2);
         targetPoints.x = (float)(targetPoints.x + hitVector.x * 2.0);
         targetPoints.y = (float)(targetPoints.y + hitVector.y * 2.0);
      }

      return new SlimeGreatBowArrowProjectile(
         level,
         owner,
         owner.x,
         owner.y,
         owner.x,
         owner.y - 1.0F,
         velocity,
         this.projectileMaxHeight,
         damage,
         resilienceGain,
         (int)knockback,
         targetPoints,
         false
      );
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.rotation(-85.0F);
   }

   @Override
   protected void addExtraBowTooltips(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      super.addExtraBowTooltips(tooltips, item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "slimegreatbowtip"), 400);
   }

   @Override
   public Projectile getProjectile(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int seed, ArrowItem arrow, boolean consumeAmmo, GNDItemMap mapContent
   ) {
      float percentCharge = item.getGndData().getFloat("chargePercent");
      percentCharge = GameMath.limit(percentCharge, 0.0F, 1.0F);
      float velocityMod;
      float rangeMod;
      float damageMod;
      float knockbackMod;
      if (percentCharge >= 1.0F) {
         velocityMod = 1.0F;
         rangeMod = 1.0F;
         damageMod = 1.0F;
         knockbackMod = 1.0F;
      } else {
         velocityMod = GameMath.lerp(percentCharge, 0.1F, 0.4F);
         rangeMod = GameMath.lerp(percentCharge, 0.05F, 0.4F);
         damageMod = GameMath.lerp(percentCharge, 0.05F, 0.4F);
         knockbackMod = GameMath.lerp(percentCharge, 0.05F, 0.2F);
      }

      GameDamage damage = arrow.modDamage(this.getAttackDamage(item)).modDamage(damageMod);
      float velocity = arrow.modVelocity(this.getProjectileVelocity(item, attackerMob)) * velocityMod;
      float range = arrow.modRange(this.getAttackRange(item)) * rangeMod;
      float knockback = arrow.modKnockback(this.getKnockback(item, attackerMob)) * knockbackMod;
      float resilienceGain = this.getResilienceGain(item);
      return this.getSlimeGreatbowProjectile(level, x, y, attackerMob, damage, velocity, range, knockback, resilienceGain);
   }

   @Override
   public Point getControllerAttackLevelPos(Level level, float aimDirX, float aimDirY, PlayerMob player, InventoryItem item) {
      float range = 500.0F;
      return new Point((int)(player.x + aimDirX * range), (int)(player.y + aimDirY * range));
   }

   @Override
   protected SoundSettings getGreatbowShootSoundWeak() {
      return new SoundSettings(GameResources.slimeGreatBowWeak).volume(0.4F);
   }

   @Override
   protected SoundSettings getGreatbowShootSoundStrong() {
      return new SoundSettings(GameResources.slimeGreatBowStrong).volume(0.6F);
   }
}
