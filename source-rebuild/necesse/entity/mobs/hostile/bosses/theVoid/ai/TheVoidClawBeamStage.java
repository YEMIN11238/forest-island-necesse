package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidClawMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidClawBeamStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   public boolean waitForClaws;
   protected List<TheVoidClawMob> claws;

   public TheVoidClawBeamStage(boolean waitForClaws) {
      this.waitForClaws = waitForClaws;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.claws != null && !this.claws.isEmpty() && !this.claws.stream().allMatch(TheVoidClawMob::isIdle) ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      if (target != null) {
         List<TheVoidClawMob> claws = mob.spawnedClaws.stream().filter(TheVoidClawMob::isIdle).collect(Collectors.toList());
         boolean isAnyHornBroken = mob.isAnyHornBroken();
         boolean reversed = GameRandom.globalRandom.nextBoolean();

         for (TheVoidClawMob claw : claws) {
            claw.startBeamAbility
               .runAndSend(GameRandom.globalRandom.nextInt(), 30.0F, 55 + (isAnyHornBroken ? 15 : 0), isAnyHornBroken ? 1800 : 2500, reversed);
         }

         if (this.waitForClaws) {
            this.claws = claws;
         } else {
            this.claws = null;
         }
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
