package necesse.entity.mobs.job.activeJob;

import java.awt.geom.Point2D.Float;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.projectile.Projectile;
import necesse.level.maps.levelData.jobs.EntityLevelJob;

public class HuntMobActiveJob extends MobActiveJob<Mob> {
   public EntityLevelJob<? extends Mob> job;
   public GameLinkedListJobSequence sequence;
   protected String meleeItemStringID;
   protected GameDamage meleeDamage;
   protected int meleeRange;
   protected int meleeAimSpeed;
   protected int meleeCooldown;
   protected String rangedItemStringID;
   protected GameDamage rangedDamage;
   protected int rangedRange;
   protected int rangedAimSpeed;
   protected int rangedCooldown;
   protected String rangedProjectileStringID;
   protected int rangedProjectileSpeed;
   public long lastAttackTime;
   public int lastAttackCooldown;

   public HuntMobActiveJob(
      EntityJobWorker worker,
      JobTypeHandler.TypePriority priority,
      EntityLevelJob<? extends Mob> job,
      GameLinkedListJobSequence sequence,
      String attackItemStringID,
      GameDamage damage,
      int attackRange,
      int attackAnimTime,
      int attackCooldown
   ) {
      super(worker, priority, job.target);
      this.job = job;
      this.sequence = sequence;
      this.meleeItemStringID = attackItemStringID;
      this.meleeDamage = damage;
      this.meleeRange = attackRange;
      this.meleeAimSpeed = attackAnimTime;
      this.meleeCooldown = attackCooldown;
   }

   public HuntMobActiveJob addRangedAttack(
      String itemStringID, GameDamage damage, int attackRange, int attackAnimTime, int attackCooldown, String projectileStringID, int projectileSpeed
   ) {
      this.rangedItemStringID = itemStringID;
      this.rangedDamage = damage;
      this.rangedRange = attackRange;
      this.rangedAimSpeed = attackAnimTime;
      this.rangedCooldown = attackCooldown;
      this.rangedProjectileStringID = projectileStringID;
      this.rangedProjectileSpeed = projectileSpeed;
      return this;
   }

   protected Float getProjectileTargetPos() {
      Mob mob = this.worker.getMobWorker();
      return Projectile.getPredictedTargetPos(this.target, mob.x, mob.y, this.rangedProjectileSpeed, -10.0F);
   }

   @Override
   public boolean isAtTarget() {
      double distanceToTarget = this.getDistanceToTarget();
      if (distanceToTarget <= this.meleeRange) {
         return this.hasLOS();
      } else if (this.rangedProjectileStringID != null && distanceToTarget <= this.rangedRange && this.hasLOS()) {
         Float targetPos = this.getProjectileTargetPos();
         return this.hasProjectileLOS(targetPos.x, targetPos.y);
      } else {
         return false;
      }
   }

   @Override
   public int getCompleteRange() {
      return this.rangedProjectileStringID != null ? Math.max(this.meleeRange, this.rangedRange) : this.meleeRange;
   }

   @Override
   public boolean isJobValid(boolean isCurrent) {
      return this.job.reservable.isAvailable(this.worker.getMobWorker()) && this.target.canTakeDamage();
   }

   @Override
   public boolean isInvalidIfTargetRemoved(boolean isCurrent) {
      return this.lastAttackTime == 0L;
   }

   @Override
   public void tick(boolean isCurrent, boolean isMovingTo) {
      this.job.reservable.reserve(this.worker.getMobWorker());
   }

   @Override
   public ActiveJobResult perform() {
      if (this.worker.isInWorkAnimation()) {
         return ActiveJobResult.PERFORMING;
      }

      if (!this.target.removed() && this.target.getHealth() > 0) {
         Mob mob = this.worker.getMobWorker();
         if (this.lastAttackTime + this.lastAttackCooldown > mob.getWorldEntity().getTime()) {
            return ActiveJobResult.PERFORMING;
         }

         double distanceToTarget = this.getDistanceToTarget();
         if (distanceToTarget <= this.meleeRange + 10) {
            if (this.hasLOS()) {
               this.lastAttackCooldown = this.meleeCooldown;
               if (this.meleeItemStringID != null) {
                  this.worker
                     .showAttackAnimation(this.target.getX(), this.target.getY(), ItemRegistry.getItem(this.meleeItemStringID), this.meleeAimSpeed, true);
               } else {
                  this.worker.showPlaceAnimation(this.target.getX(), this.target.getY(), null, this.meleeAimSpeed, true);
               }

               this.target.isServerHit(this.meleeDamage, this.target.x - mob.x, this.target.y - mob.y, 100.0F, mob);
               this.lastAttackTime = mob.getWorldEntity().getTime();
               return ActiveJobResult.PERFORMING;
            } else {
               return ActiveJobResult.MOVE_TO;
            }
         } else {
            if (this.rangedProjectileStringID == null || !(distanceToTarget <= this.rangedRange + 50)) {
               return ActiveJobResult.MOVE_TO;
            }

            if (this.hasLOS()) {
               Float targetPos = this.getProjectileTargetPos();
               if (this.hasProjectileLOS(targetPos.x, targetPos.y)) {
                  this.lastAttackCooldown = this.rangedCooldown;
                  if (this.rangedItemStringID != null) {
                     this.worker
                        .showAttackAnimation((int)targetPos.x, (int)targetPos.y, ItemRegistry.getItem(this.rangedItemStringID), this.rangedAimSpeed, true);
                  } else {
                     this.worker.showPlaceAnimation((int)targetPos.x, (int)targetPos.y, null, this.rangedAimSpeed, true);
                  }

                  Projectile projectile = ProjectileRegistry.getProjectile(
                     this.rangedProjectileStringID,
                     this.target.getLevel(),
                     mob.x,
                     mob.y,
                     targetPos.x,
                     targetPos.y,
                     this.rangedProjectileSpeed,
                     this.rangedRange + 150,
                     this.rangedDamage,
                     mob
                  );
                  projectile.moveDist(10.0);
                  this.target.getLevel().entityManager.projectiles.add(projectile);
                  this.lastAttackTime = mob.getWorldEntity().getTime();
                  return ActiveJobResult.PERFORMING;
               } else {
                  return ActiveJobResult.MOVE_TO;
               }
            } else {
               return ActiveJobResult.MOVE_TO;
            }
         }
      } else {
         if (this.sequence != null) {
            PickupItemEntityActiveJob.addItemPickupJobs(this.worker, this.priority, this.target.itemsDropped, this.sequence);
         }

         return ActiveJobResult.FINISHED;
      }
   }

   @Override
   public ActiveJobResult performTarget() {
      return ActiveJobResult.FAILED;
   }
}
