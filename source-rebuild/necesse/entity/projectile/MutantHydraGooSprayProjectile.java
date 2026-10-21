package necesse.entity.projectile;

import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.Entity;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.followingProjectile.FollowingProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class MutantHydraGooSprayProjectile extends FollowingProjectile {
   public MutantHydraGooSprayProjectile() {
   }

   public MutantHydraGooSprayProjectile(float x, float y, Mob target, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this();
      this.setLevel(owner.getLevel());
      this.x = x;
      this.y = y;
      this.setTarget(target.x, target.y);
      this.target = target;
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(30.0F, true);
      this.turnSpeed = 0.13F;
      this.particleSpeedMod = 0.03F;
      this.piercing = 200;
      this.isSolid = false;
   }

   @Override
   public boolean canHit(Mob mob) {
      return this.getTimeSinceSpawned() >= 750L && super.canHit(mob);
   }

   @Override
   public float getTurnSpeed(int targetX, int targetY, float delta) {
      return this.getTurnSpeed(delta) * this.invDynamicTurnSpeedMod(targetX, targetY, this.getTurnRadius() / 1.5F);
   }

   @Override
   public void updateTarget() {
      Entity lastTarget = this.target;
      this.findTarget(m -> m.isPlayer, 0.0F, 100.0F);
      if (lastTarget != null && this.target == null) {
         this.target = lastTarget;
      }
   }

   @Override
   public void onMaxMoveTick() {
      if (this.isClient()) {
         this.spawnSpinningParticle();
      }
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Mob owner = this.getOwner();
      if (owner == null || owner.removed()) {
         this.remove();
      }
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
   }
}
