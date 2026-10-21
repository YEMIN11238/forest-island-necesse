package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.TheVoidBreathBeamLevelEvent;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidLaserMouthStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   private TheVoidBreathBeamLevelEvent event;
   private final int endAtTimeLeft;

   public TheVoidLaserMouthStage(int endAtTimeLeft) {
      this.endAtTimeLeft = endAtTimeLeft;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.event != null && !this.event.isOver() && this.event.getTimeLeft() >= this.endAtTimeLeft) {
         return AINodeResult.RUNNING;
      }

      this.event = null;
      return AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.event = new TheVoidBreathBeamLevelEvent(
         mob, 90.0F, 90.0F, mob.getTime(), 2000, GameRandom.globalRandom.nextInt(), 1500.0F, TheVoidMob.breathAttackDamage, 100, 1000, 0
      );
      mob.getLevel().entityManager.events.add(this.event);
      mob.spawnedEvents.add(this.event);
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
