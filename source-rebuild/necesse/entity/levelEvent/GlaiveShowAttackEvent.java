package necesse.entity.levelEvent;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.PointSetAbstract;
import necesse.entity.mobs.AttackAnimMob;

public abstract class GlaiveShowAttackEvent extends LevelEvent {
   protected float lastAngleTick;
   protected float anglePerTick;
   protected int aimX;
   protected int aimY;
   protected int attackSeed;
   protected AttackAnimMob attackMob;

   public GlaiveShowAttackEvent(AttackAnimMob attackMob, int x, int y, int attackSeed, float anglePerTick) {
      super(false);
      this.attackMob = attackMob;
      this.aimX = x - attackMob.getX();
      this.aimY = y - attackMob.getY();
      this.attackSeed = attackSeed;
      this.anglePerTick = anglePerTick;
   }

   @Override
   public void init() {
      super.init();
      if (this.attackMob == null) {
         this.over();
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isOver()) {
         if (this.attackMob.isAttacking && this.attackMob.attackSeed == this.attackSeed) {
            float nextProgress = this.attackMob.getAttackAnimProgress();
            float nextAngle = nextProgress * 360.0F;
            if (nextAngle >= this.lastAngleTick + this.anglePerTick) {
               for (float i = this.lastAngleTick + this.anglePerTick; i <= nextAngle; i += this.anglePerTick) {
                  this.tick(i);
                  this.lastAngleTick = i;
               }
            }
         } else {
            this.over();
         }
      }
   }

   public abstract void tick(float var1);

   public Float getAngleDir(float angle) {
      return this.attackMob != null && this.attackMob.getDir() == 3 ? GameMath.getAngleDir(-angle - 110.0F) : GameMath.getAngleDir(angle + 110.0F);
   }

   @Override
   public PointSetAbstract<?> getRegionPositions() {
      return this.attackMob != null ? this.attackMob.getRegionPositions() : super.getRegionPositions();
   }

   @Override
   public Point getSaveToRegionPos() {
      return this.attackMob != null
         ? new Point(
            this.level.regionManager.getRegionCoordByTile(this.attackMob.getTileX()), this.level.regionManager.getRegionCoordByTile(this.attackMob.getTileY())
         )
         : super.getSaveToRegionPos();
   }
}
