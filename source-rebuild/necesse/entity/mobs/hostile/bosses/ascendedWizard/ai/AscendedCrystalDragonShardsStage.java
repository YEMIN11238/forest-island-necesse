package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.projectile.AscendedShardBombProjectile;

public class AscendedCrystalDragonShardsStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   public float minBombsPerSec = 4.0F;
   public float maxBombsPerSec = 6.0F;
   public int minBombs = 20;
   public int maxBombs = 30;
   public float nextFireBuffer;
   public int totalFired;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      float totalProjectiles = GameMath.lerp(mob.getHealthPercent(), this.maxBombs, this.minBombs);
      if (this.totalFired >= totalProjectiles) {
         return AINodeResult.SUCCESS;
      }

      GameRandom random = GameRandom.globalRandom;
      float projectilesPerSec = GameMath.lerp(mob.getHealthPercent(), this.maxBombsPerSec, this.minBombsPerSec);
      float projectilesPerTick = projectilesPerSec * 50.0F / 1000.0F;
      this.nextFireBuffer += projectilesPerTick;

      while (this.nextFireBuffer >= 1.0F) {
         this.nextFireBuffer--;
         this.totalFired++;
         float randomAngle = random.getFloatBetween(0.0F, 360.0F);
         Float dir = GameMath.getAngleDir(randomAngle);
         float randomDistance = random.getFloatBetween(200.0F, 600.0F);
         int targetX = (int)(mob.getX() + dir.x * randomDistance);
         int targetY = (int)(mob.getY() + dir.y * randomDistance);
         AscendedShardBombProjectile projectile = new AscendedShardBombProjectile(
            mob.x, mob.y, targetX, targetY, 100, (int)randomDistance, AscendedWizardMob.shardBombDamage, mob
         );
         mob.getLevel().entityManager.projectiles.add(projectile);
         mob.spawnedProjectiles.add(projectile);
         mob.playLightningBoltSoundAbility.runAndSend();
      }

      return AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.totalFired = 0;
      Point baseTile = mob.getBaseTile(blackboard);
      int centerX = baseTile.x * 32 + 16;
      int centerY = baseTile.y * 32 + 16;
      int radius = 384;
      float speed = MobMovementCircleLevelPos.convertToRotSpeed(radius, mob.getSpeed());
      blackboard.mover.setCustomMovement(this, new MobMovementCircleLevelPos(mob, centerX, centerY, radius, speed, GameRandom.globalRandom.nextBoolean()));
      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.CRYSTAL_DRAGON);
      mob.startArmsUpAnimation.runAndSend();
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
      if (blackboard.mover.isCurrentlyMovingFor(this)) {
         blackboard.mover.stopMoving(mob);
      }

      mob.startArmsDownAnimation.runAndSend();
   }
}
