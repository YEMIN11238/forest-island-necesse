package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.splashEvent.NecroPoisonSplashEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.NecroticFlaskProjectile;
import necesse.entity.projectile.Projectile;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class NecroticFlaskProjectileToolItem extends FlaskProjectileToolItem {
   public NecroticFlaskProjectileToolItem() {
      super(850, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(45.0F).setUpgradedValue(1.0F, 98.00003F);
      this.velocity.setBaseValue(800);
      this.attackXOffset = 8;
      this.attackYOffset = 10;
      this.attackCooldownTime.setBaseValue(500);
      this.attackRange.setBaseValue(300);
      this.manaCost.setBaseValue(1.25F).setUpgradedValue(1.0F, 2.0F);
      this.resilienceGain.setBaseValue(1.0F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = false;
   }

   @Override
   protected Projectile getProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, GameRandom random
   ) {
      return new NecroticFlaskProjectile(level, owner, x, y, targetX, targetY, speed, distance, damage, knockback);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      float seconds = NecroPoisonSplashEvent.poisonDuration;
      float totalDotDamage = this.getAttackDamage(item).getBuffedDamage(perspective);
      tooltips.add(Localization.translate("itemtooltip", "necroticflasktip", "value1", (int)totalDotDamage, "value2", (int)seconds), 400);
      return tooltips;
   }
}
