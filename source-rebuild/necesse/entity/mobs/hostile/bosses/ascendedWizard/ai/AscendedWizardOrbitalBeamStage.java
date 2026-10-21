package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.projectile.AscendedBeamProjectile;
import necesse.entity.projectile.Projectile;

public class AscendedWizardOrbitalBeamStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   private Mob target;
   private Point startPoint;
   private int currentChargeTime;
   private int chargeTime;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.target = blackboard.getObject(Mob.class, "currentTarget");
      this.chargeTime = mob.isTransformed() ? 1000 : 1500;
      this.currentChargeTime = 0;
      this.startPoint = new Point(
         this.target.getX() + GameRandom.globalRandom.getIntBetween(-50, 50), this.target.getY() + GameRandom.globalRandom.getIntBetween(-50, 50)
      );
      mob.startBeamSpawningAnimationAbility.runAndSend(this.startPoint.x, this.startPoint.y, this.chargeTime);
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.currentChargeTime > this.chargeTime) {
         GameRandom random = GameRandom.globalRandom;
         Projectile projectile = new AscendedBeamProjectile(
            this.startPoint.x,
            this.startPoint.y,
            this.target,
            50.0F,
            mob.isTransformed() ? 4000 : 2000,
            AscendedWizardMob.boltDamage,
            100,
            mob.isTransformed(),
            mob
         );
         projectile.getUniqueID(random);
         mob.getLevel().entityManager.projectiles.add(projectile);
         mob.spawnedProblematicProjectiles.add(projectile);
         return AINodeResult.SUCCESS;
      } else {
         this.currentChargeTime += 50;
         return AINodeResult.RUNNING;
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
