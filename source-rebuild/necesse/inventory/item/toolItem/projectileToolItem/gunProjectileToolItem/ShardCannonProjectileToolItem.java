package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.ShardCannonAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.bulletProjectile.CrystalBulletProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.bulletItem.BulletItem;
import necesse.inventory.lootTable.presets.IncursionGunWeaponsLootTable;
import necesse.level.maps.Level;

public class ShardCannonProjectileToolItem extends GunProjectileToolItem {
   public ShardCannonProjectileToolItem() {
      super(NORMAL_AMMO_TYPES, 1900, IncursionGunWeaponsLootTable.incursionGunWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(100);
      this.ammoConsumeChance = 0.5F;
      this.attackDamage.setBaseValue(28.0F).setUpgradedValue(1.0F, 32.666676F);
      this.attackXOffset = 12;
      this.attackYOffset = 14;
      this.resilienceGain.setBaseValue(0.3F);
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(350);
      this.addGlobalIngredient("bulletuser");
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.raidTicketsModifier = 0.5F;
   }

   @Override
   protected void addAmmoTooltips(ListGameTooltips tooltips, InventoryItem item) {
      super.addAmmoTooltips(tooltips, item);
      tooltips.add(Localization.translate("itemtooltip", "machineguntip"));
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "shardcannontip"));
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
      attackerMob.startAttackHandler(new ShardCannonAttackHandler(attackerMob, slot, item, this, seed, x, y));
      return item;
   }

   @Override
   public boolean shouldRunOnAttackedBuffEvent(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent
   ) {
      return false;
   }

   public InventoryItem superOnAttack(
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
      return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
   }

   @Override
   protected void fireProjectiles(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int seed, BulletItem bullet, boolean dropItem, GNDItemMap mapContent
   ) {
      float distance = GameMath.diamondDistance(attackerMob.x, attackerMob.y, x, y);
      float t = 20.0F / distance;
      float projectileX = (1.0F - t) * attackerMob.x + t * x;
      float projectileY = (1.0F - t) * attackerMob.y + t * y;
      GameRandom random = new GameRandom(seed);
      GameRandom spreadRandom = new GameRandom(seed + 10);
      Projectile projectile = this.getNormalProjectile(
         projectileX, projectileY, x, y, 300.0F, 2000, this.getAttackDamage(item), this.getKnockback(item, attackerMob), attackerMob
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.dropItem = dropItem;
      projectile.getUniqueID(random);
      attackerMob.addAndSendAttackerProjectile(projectile, this.moveDist, spreadRandom.getFloatOffset(0.0F, 3.0F));
   }

   @Override
   public Projectile getNormalProjectile(
      float x, float y, float targetX, float targetY, float velocity, int range, GameDamage toolItemDamage, int knockback, ItemAttackerMob attackerMob
   ) {
      return new CrystalBulletProjectile(x, y, targetX, targetY, velocity, range, toolItemDamage, knockback, attackerMob);
   }

   @Override
   protected void playAttackSound(Mob source) {
   }
}
