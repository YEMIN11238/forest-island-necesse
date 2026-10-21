package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.projectile.AscendedSlashProjectile;
import necesse.entity.projectile.EmpressSlashWarningProjectile;

public class AscendedSpiderEmpressStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   public long warningTime;
   private int slashesRemaining;
   private final ArrayList<Float> attackAngles = new ArrayList<>();
   private final ArrayList<Point> attackPos = new ArrayList<>();
   private float warningBuffer;
   private float slashBuffer;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      if (target == null) {
         return AINodeResult.RUNNING;
      }

      if (mob.getTime() >= this.warningTime) {
         if (this.attackAngles.isEmpty()) {
            return AINodeResult.SUCCESS;
         }

         this.slashBuffer += 50.0F;
         if (this.slashBuffer > 100.0F) {
            this.slashBuffer -= 100.0F;
            float angle = this.attackAngles.get(0);
            int attackX = this.attackPos.get(0).x;
            int attackY = this.attackPos.get(0).y;
            AscendedSlashProjectile slashProjectile = new AscendedSlashProjectile(attackX, attackY, angle, AscendedWizardMob.spiderSlashDamage, mob);
            mob.getLevel().entityManager.projectiles.add(slashProjectile);
            mob.spawnedProjectiles.add(slashProjectile);
            AscendedSlashProjectile slashProjectileReverse = new AscendedSlashProjectile(
               attackX, attackY, angle - 180.0F, AscendedWizardMob.spiderSlashDamage, mob
            );
            mob.getLevel().entityManager.projectiles.add(slashProjectileReverse);
            mob.spawnedProjectiles.add(slashProjectileReverse);
            mob.slashSoundAbility
               .runAndSend(
                  (int)(attackX + slashProjectile.dx * slashProjectile.distance / 2.0F), (int)(attackY + slashProjectile.dy * slashProjectile.distance / 2.0F)
               );
            this.attackAngles.remove(0);
            this.attackPos.remove(0);
         }

         if (this.slashesRemaining < 1) {
            return AINodeResult.RUNNING;
         }
      }

      GameRandom random = GameRandom.globalRandom;

      for (this.warningBuffer += 50.0F; this.slashesRemaining > 0 && this.warningBuffer > 100.0F; this.slashesRemaining--) {
         int randomX = random.getIntBetween(-200, 200);
         int randomY = random.getIntBetween(-200, 200);
         float angle = random.getIntBetween(-180, 180);
         EmpressSlashWarningProjectile slashProjectile = new EmpressSlashWarningProjectile(
            (int)(target.x + randomX), (int)(target.y + randomY), angle, AscendedWizardMob.spiderSlashDamage, mob
         );
         mob.getLevel().entityManager.projectiles.add(slashProjectile);
         mob.spawnedProjectiles.add(slashProjectile);
         EmpressSlashWarningProjectile slashProjectileReverse = new EmpressSlashWarningProjectile(
            (int)(target.x + randomX), (int)(target.y + randomY), angle - 180.0F, AscendedWizardMob.spiderSlashDamage, mob
         );
         mob.getLevel().entityManager.projectiles.add(slashProjectileReverse);
         mob.spawnedProjectiles.add(slashProjectileReverse);
         this.attackAngles.add(angle);
         this.attackPos.add(new Point((int)(target.x + randomX), (int)(target.y + randomY)));
         this.warningBuffer -= 100.0F;
      }

      return AINodeResult.RUNNING;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.slashesRemaining = mob.isTransformed() ? 40 : 20;
      this.warningTime = mob.getTime() + 750L;
      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.SPIDER_EMPRESS);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
