package necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraBossMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadMob;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.MutantHydraHeadType;
import necesse.entity.projectile.MutantHydraSpitProjectile;

public class HeadSpitStage<T extends MutantHydraBossMob> extends AINode<T> implements AttackStageInterface<T> {
   public MutantHydraHeadType head;
   public int minProjectileCount;
   public int maxProjectileCount;
   public int minTimeTaken;
   public int maxTimeTaken;
   public float nextProjectileBuffer;
   public float timeTakenBuffer;

   public HeadSpitStage(MutantHydraHeadType head, int minProjectileCount, int maxProjectileCount, int minTimeTaken, int maxTimeTaken) {
      this.head = head;
      this.minProjectileCount = minProjectileCount;
      this.maxProjectileCount = maxProjectileCount;
      this.minTimeTaken = minTimeTaken;
      this.maxTimeTaken = maxTimeTaken;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.timeTakenBuffer = 0.0F;
      this.nextProjectileBuffer = 0.0F;
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
      MutantHydraHeadMob headMob = this.head.getter.apply(mob).getHeadMob();
      int currentTimeTaken = GameMath.lerp(mob.getHealthPercent(), this.minTimeTaken, this.maxTimeTaken);
      if (currentTarget != null && headMob != null) {
         int currentProjectileCount = GameMath.lerp(mob.getHealthPercent(), this.minProjectileCount, this.maxProjectileCount);
         float projectilesPerSecond = currentProjectileCount / (currentTimeTaken / 1000.0F);
         this.nextProjectileBuffer += projectilesPerSecond / 20.0F;

         while (this.nextProjectileBuffer >= 1.0F) {
            this.nextProjectileBuffer--;
            float randomAngle = GameRandom.globalRandom.nextFloat() * 360.0F;
            Float dir = GameMath.getAngleDir(randomAngle);
            float offset = GameRandom.globalRandom.getFloatBetween(0.0F, 140.0F);
            float targetX = currentTarget.x + dir.x * offset;
            float targetY = currentTarget.y + dir.y * offset;
            int distance = (int)GameMath.getExactDistance(headMob.x, headMob.y, targetX, targetY);
            int timeToLand = GameMath.lerp(mob.getHealthPercent(), 1000, 2000);
            float speed = Entity.getTravelSpeedForMillis(timeToLand, distance);
            MutantHydraSpitProjectile projectile = new MutantHydraSpitProjectile(
               mob.getLevel(), mob, headMob.x, headMob.y, targetX, targetY, speed, distance, MutantHydraBossMob.gooSpitDamage, 50
            );
            mob.getLevel().entityManager.projectiles.add(projectile);
            headMob.startLaunchAnimation.runAndSend(500);
         }
      }

      this.timeTakenBuffer += 1.0F / (currentTimeTaken / 50.0F);
      return this.timeTakenBuffer >= 1.0F ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
