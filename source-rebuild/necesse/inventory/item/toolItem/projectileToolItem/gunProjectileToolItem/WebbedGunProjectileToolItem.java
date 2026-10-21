package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.bulletProjectile.WebbedGunBulletProjectile;
import necesse.gfx.GameResources;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GunWeaponsLootTable;

public class WebbedGunProjectileToolItem extends GunProjectileToolItem {
   public WebbedGunProjectileToolItem() {
      super(NORMAL_AMMO_TYPES, 550, GunWeaponsLootTable.gunWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(26.0F).setUpgradedValue(1.0F, 116.6667F);
      this.attackXOffset = 12;
      this.attackYOffset = 14;
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(400);
      this.addGlobalIngredient("bulletuser");
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.handgun).volume(0.4F);
   }

   @Override
   public Projectile getNormalProjectile(
      float x, float y, float targetX, float targetY, float velocity, int range, GameDamage toolItemDamage, int knockback, ItemAttackerMob attackerMob
   ) {
      return new WebbedGunBulletProjectile(x, y, targetX, targetY, velocity, range, toolItemDamage, knockback, attackerMob);
   }
}
