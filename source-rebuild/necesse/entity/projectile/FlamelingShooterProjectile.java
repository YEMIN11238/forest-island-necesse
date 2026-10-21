package necesse.entity.projectile;

import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;

public class FlamelingShooterProjectile extends EvilsProtectorAttack1Projectile {
   public FlamelingShooterProjectile() {
   }

   public FlamelingShooterProjectile(float x, float y, float angle, float speed, int distance, GameDamage damage, Mob owner) {
      super(x, y, angle, speed, distance, damage, owner);
   }

   @Override
   public void init() {
      super.init();
      this.isSolid = true;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      Mob owner = this.getOwner();
      return owner == null ? super.getDeathMessages() : owner.getDeathMessages();
   }
}
