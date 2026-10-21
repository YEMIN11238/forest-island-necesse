package necesse.entity.projectile;

import java.util.Objects;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.trails.Trail;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class WeaponChargeSmiteBeamProjectile extends SmiteBeamProjectile {
   protected ItemAttackerMob attackerMob;
   private final float swordOffsetY;

   public WeaponChargeSmiteBeamProjectile() {
      this.swordOffsetY = 0.0F;
   }

   public WeaponChargeSmiteBeamProjectile(Level level, ItemAttackerMob owner, float startX, float startY) {
      super(level, owner, startX, startY, owner, new GameDamage(0.0F));
      Objects.requireNonNull(owner);
      this.attackerMob = owner;
      this.swordOffsetY = GameRandom.globalRandom.getFloatBetween(60.0F, 80.0F);
   }

   @Override
   protected void updateTargetMobPos() {
      if (this.attackerMob == null) {
         super.updateTargetMobPos();
      } else {
         int leftRight = this.attackerMob.getDirVector().x;
         if (leftRight == 0) {
            leftRight = 1;
         }

         float x = this.attackerMob.x + 16.0F * leftRight;
         float y = this.attackerMob.y - this.swordOffsetY;
         this.setTarget(x, y);
      }
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), this.getParticleColor(), 14.0F, 350, this.getHeight());
      trail.drawOnTop = true;
      trail.lightHue = 60.0F;
      trail.lightLevel = 120;
      return trail;
   }

   @Override
   public boolean canHit(Mob mob) {
      return false;
   }

   @Override
   public void onHit(Mob mob, LevelObjectHit object, float x, float y, boolean fromPacket, ServerClient packetSubmitter) {
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
   }

   @Override
   protected void spawnDeathParticles() {
   }
}
