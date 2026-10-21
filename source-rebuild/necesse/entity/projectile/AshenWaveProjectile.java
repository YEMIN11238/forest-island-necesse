package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.AshWaveParticle;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.projectile.followingProjectile.FollowingProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class AshenWaveProjectile extends FollowingProjectile {
   private double distBuffer;

   public AshenWaveProjectile() {
   }

   public AshenWaveProjectile(Level level, float x, float y, Mob target, float speed, int distance, GameDamage damage, Mob owner) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.setTarget(target.x, target.y);
      this.target = target;
      this.speed = speed;
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(distance);
   }

   @Override
   public void init() {
      super.init();
      this.maxMovePerTick = 32;
      this.turnSpeed = 0.2F;
      this.setWidth(16.0F, true);
      this.height = 0.0F;
      this.isSolid = false;
      this.particleRandomOffset = 10.0F;
      this.particleDirOffset = 0.0F;
   }

   @Override
   public void updateTarget() {
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      this.distBuffer += movedDist;
      if (!this.isServer()) {
         while (this.distBuffer > 32.0) {
            this.distBuffer -= 32.0;
            this.getLevel().entityManager.addParticle(new AshWaveParticle(this.getLevel(), this.x, this.y, 1000L), true, Particle.GType.CRITICAL);
         }
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer() && mob != null) {
         if (mob.buffManager.hasBuff(BuffRegistry.Debuffs.ASHEN_BURIAL)) {
            return;
         }

         ActiveBuff ab = new ActiveBuff(BuffRegistry.Debuffs.ASHEN_BURIAL, mob, 3.0F, this.getOwner());
         mob.addBuff(ab, true);
      }
   }

   @Override
   public Color getParticleColor() {
      return new Color(75, 75, 75);
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      super.modifySpinningParticle(particle);
      particle.sizeFades(10, 35);
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
