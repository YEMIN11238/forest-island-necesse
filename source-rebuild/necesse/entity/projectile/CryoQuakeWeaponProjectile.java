package necesse.entity.projectile;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GroundPillarList;
import necesse.entity.manager.GroundPillarHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.CryoQueenMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class CryoQuakeWeaponProjectile extends Projectile {
   private double distCounter;
   private double distBuffer;
   private final GroundPillarList<CryoQueenMob.CryoPillar> pillars = new GroundPillarList<>();

   public CryoQuakeWeaponProjectile() {
   }

   public CryoQuakeWeaponProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.maxMovePerTick = 12;
      this.height = 0.0F;
      this.piercing = 1;
      this.setWidth(30.0F);
      if (this.isClient()) {
         this.getLevel().entityManager.addPillarHandler(new GroundPillarHandler<CryoQueenMob.CryoPillar>(this.pillars) {
            @Override
            protected boolean canRemove() {
               return CryoQuakeWeaponProjectile.this.removed();
            }

            @Override
            public double getCurrentDistanceMoved() {
               return CryoQuakeWeaponProjectile.this.distCounter;
            }
         });
      }
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      this.distCounter += movedDist;
      this.distBuffer += movedDist;

      while (this.distBuffer > 12.0) {
         this.distBuffer -= 12.0;
         synchronized (this.pillars) {
            this.pillars
               .add(
                  new CryoQueenMob.CryoPillar(
                     (int)(this.x + GameRandom.globalRandom.floatGaussian() * 6.0F),
                     (int)(this.y + GameRandom.globalRandom.floatGaussian() * 4.0F),
                     this.distCounter,
                     this.getWorldEntity().getLocalTime()
                  )
               );
         }
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
