package necesse.inventory.item.arrowItem;

import necesse.engine.registries.ProjectileRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;

public class StoneArrowItem extends ArrowItem {
   public StoneArrowItem() {
      this.damage = 5;
   }

   @Override
   public Projectile getProjectile(
      float x, float y, float targetX, float targetY, float velocity, int range, GameDamage damage, int knockback, ItemAttackerMob owner
   ) {
      return ProjectileRegistry.getProjectile("stonearrow", owner.getLevel(), x, y, targetX, targetY, velocity, range, damage, knockback, owner);
   }
}
