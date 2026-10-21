package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.util.Comparator;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidClawMob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidClawSingleSlamStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return AINodeResult.SUCCESS;
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, "currentTarget");
      if (target != null) {
         TheVoidClawMob claw = mob.spawnedClaws
            .stream()
            .filter(TheVoidClawMob::isIdle)
            .min(Comparator.comparingDouble(c -> c.getDistance(target)))
            .orElse(null);
         if (claw != null) {
            float offsetX = GameMath.limit(target.dx * 10.0F, -50.0F, 50.0F);
            float offsetY = GameMath.limit(target.dy * 10.0F, -50.0F, 50.0F);
            claw.flyToTargetAndSlam(target, offsetX, offsetY, 2000, 300, 1000, 1000, false);
         }
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
