package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.projectile.AscendedBoltProjectile;
import necesse.entity.projectile.Projectile;

public class AscendedGatlingGunStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   private int projectileCount;
   private int projectilesShot;
   private int buffer;
   private int fireRate;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.fireRate = GameMath.lerp(mob.getHealthPercent(), 100, 50);
      this.projectileCount = mob.isTransformed() ? 50 : 30;
      this.projectilesShot = 0;
      mob.startArmsUpAnimation.runAndSend();
      if (mob.isTransformed()) {
         Point baseTile = mob.getBaseTile(blackboard);
         int centerX = baseTile.x * 32 + 16;
         int centerY = baseTile.y * 32 + 16;
         int radius = 384;
         float speed = MobMovementCircleLevelPos.convertToRotSpeed(radius, mob.getSpeed());
         blackboard.mover.setCustomMovement(this, new MobMovementCircleLevelPos(mob, centerX, centerY, radius, speed, GameRandom.globalRandom.nextBoolean()));
      }
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.projectileCount > this.projectilesShot) {
         this.buffer += 50;
         if (this.buffer > this.fireRate) {
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            GameRandom random = GameRandom.globalRandom;
            Projectile projectile = new AscendedBoltProjectile(
               mob.getLevel(),
               mob.x,
               mob.y,
               target.x + random.getIntBetween(-50, 50),
               target.y + random.getIntBetween(-50, 50),
               200.0F,
               1000,
               AscendedWizardMob.boltDamage,
               mob
            );
            projectile.getUniqueID(random);
            mob.getLevel().entityManager.projectiles.add(projectile);
            mob.spawnedProjectiles.add(projectile);
            mob.laserBoltSoundAbility.runAndSend(projectile.getX(), projectile.getY());
            this.projectilesShot++;
            this.buffer = 0;
         }

         return AINodeResult.RUNNING;
      } else {
         return AINodeResult.SUCCESS;
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
      if (blackboard.mover.isCurrentlyMovingFor(this)) {
         blackboard.mover.stopMoving(mob);
      }

      mob.startArmsDownAnimation.runAndSend();
   }
}
