package necesse.entity.mobs.job.activeJob;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Float;
import java.util.Comparator;
import java.util.stream.Stream;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.entity.mobs.mobMovement.MobMovementRelative;

public abstract class MobActiveJob<T extends Mob> extends EntityActiveJob<T> {
   public MobActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, T target) {
      super(worker, priority, target);
   }

   public MobMovement getDirectMoveTo(Mob target) {
      return new MobMovementRelative(target, 0.0F, 0.0F);
   }

   public boolean hasProjectileLOS() {
      return this.hasProjectileLOS(this.target.x, this.target.y);
   }

   public boolean hasProjectileLOS(float targetPosX, float targetPosY) {
      Mob mob = this.worker.getMobWorker();
      Line2D line = new Float(mob.x, mob.y, targetPosX, targetPosY);
      Mob hit = GameUtils.castRayFirstHit(
         line,
         100.0,
         checkLine -> {
            Stream<Mob> mobStream = this.getLevel().entityManager.mobs.streamInRegionsShape(checkLine, 1);
            Stream<PlayerMob> playersStream = this.getLevel().entityManager.players.streamInRegionsShape(checkLine, 1);
            return Stream.concat(mobStream, playersStream)
               .filter(m -> m != mob)
               .filter(
                  m -> {
                     Rectangle collision = m.getCollision();
                     return checkLine.intersects(collision)
                        || GameMath.getPerpendicularLine(checkLine, 10.0F).intersects(collision)
                        || GameMath.getPerpendicularLine(checkLine, -10.0F).intersects(collision);
                  }
               )
               .min(Comparator.comparingDouble(mob::getDistance))
               .orElse(null);
         }
      );
      return hit == null || hit == this.target;
   }
}
