package necesse.entity.mobs.hostile.bosses.ascendedWizard.ai;

import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedSlimeQuakeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.AscendedSlimeQuakeWarningEvent;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;

public class AscendedMotherSlimeStage<T extends AscendedWizardMob> extends AINode<T> implements AttackStageInterface<T> {
   public long warningTime;
   private int slimeOffset;
   private float slimeVelocity;
   public long quakeTime;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.warningTime != 0L && this.warningTime <= mob.getTime()) {
         int timeToQuake = (int)(this.quakeTime - mob.getTime());
         AscendedSlimeQuakeWarningEvent event = new AscendedSlimeQuakeWarningEvent(
            mob, mob.getX(), mob.getY(), new GameRandom(), 0.0F, 360.0F, this.slimeVelocity, 1200.0F, timeToQuake, this.slimeOffset
         );
         mob.getLevel().entityManager.events.add(event);
         mob.spawnedEvents.add(event);
         this.warningTime = 0L;
         return AINodeResult.RUNNING;
      } else if (this.quakeTime != 0L && this.quakeTime <= mob.getTime()) {
         AscendedSlimeQuakeEvent event = new AscendedSlimeQuakeEvent(
            mob,
            mob.getX(),
            mob.getY(),
            new GameRandom(),
            0.0F,
            360.0F,
            AscendedWizardMob.slimeCircleDamage,
            this.slimeVelocity,
            50.0F,
            1200.0F,
            this.slimeOffset
         );
         mob.getLevel().entityManager.events.add(event);
         mob.spawnedEvents.add(event);
         this.quakeTime = 0L;
         return AINodeResult.SUCCESS;
      } else {
         return AINodeResult.RUNNING;
      }
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      float exp = GameMath.expSmooth(mob.getHealthPercent(), 1.0F, 0.3F);
      this.warningTime = mob.getTime() + 750L;
      this.quakeTime = this.warningTime + GameMath.lerp(exp, 750, 1500);
      this.slimeOffset = GameRandom.globalRandom.nextInt(15000);
      this.slimeVelocity = GameMath.lerp(exp, 1000, 250);
      mob.playBossSoundAbility.runAndSend(AscendedWizardMob.BossSound.MOTHER_SLIME);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
