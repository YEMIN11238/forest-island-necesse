package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidHornMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;
import necesse.entity.projectile.AscendedBoltProjectile;
import necesse.entity.projectile.Projectile;

public class TheVoidHornBulletHell<T extends TheVoidMob> extends TheVoidKeepMovingAroundTargetStage<T> implements AttackStageInterface<T> {
   private int bulletsPerHorn;
   private int startupDelay;

   public TheVoidHornBulletHell(float keepRunningAndFindNewPositionsAtDistance) {
      super(keepRunningAndFindNewPositionsAtDistance);
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      this.tickFindNextPosition(mob, blackboard);
      if (this.startupDelay > 0) {
         this.startupDelay -= 50;
         return AINodeResult.RUNNING;
      }

      for (LevelMob<TheVoidHornMob> lm : mob.spawnedHorns) {
         TheVoidHornMob horn = lm.get(mob.getLevel());
         if (horn != null && !horn.isBroken) {
            this.fireProjectile(mob, horn.isLeftHorn);
         }
      }

      this.bulletsPerHorn--;
      return this.bulletsPerHorn <= 0 ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.bulletsPerHorn = GameMath.lerp(mob.getHealthPercent(), 120, 60);
      this.startupDelay = GameMath.lerp(mob.getHealthPercent(), 0, 1000);
      mob.showHornChargeUpAbility.runAndSend(this.startupDelay);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }

   private void fireProjectile(T mob, boolean isLeftHorn) {
      Point offset = mob.getHornOffset(isLeftHorn);
      Projectile projectile = new AscendedBoltProjectile(
         mob.getLevel(), mob.x + offset.x, mob.y + offset.y, GameRandom.globalRandom.getFloatBetween(0.0F, 360.0F), 200.0F, 2000, TheVoidMob.boltDamage, mob
      );
      projectile.getUniqueID(GameRandom.globalRandom);
      mob.getLevel().entityManager.projectiles.add(projectile);
      mob.spawnedProjectiles.add(projectile);
      mob.magicBoltSoundAbility.runAndSend(mob.getX() + offset.x, mob.getY() + offset.y);
   }
}
