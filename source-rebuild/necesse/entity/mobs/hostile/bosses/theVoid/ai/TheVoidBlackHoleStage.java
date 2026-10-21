package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.TheVoidBlackHoleGroundEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidBlackHoleStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   public int maxRelativeOffset;

   public TheVoidBlackHoleStage(int maxRelativeOffset) {
      this.maxRelativeOffset = maxRelativeOffset;
   }

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
         int beforeActiveTime = GameMath.lerp(mob.getHealthPercent(), 800, 1200);
         int targetX = target.getX() + GameRandom.globalRandom.getIntBetween(-this.maxRelativeOffset, this.maxRelativeOffset);
         int targetY = target.getY() + GameRandom.globalRandom.getIntBetween(-this.maxRelativeOffset, this.maxRelativeOffset);
         TheVoidBlackHoleGroundEvent e = new TheVoidBlackHoleGroundEvent(
            mob, targetX, targetY, TheVoidMob.blackHoleDamage, beforeActiveTime, 10000 + beforeActiveTime, GameRandom.globalRandom
         );
         mob.getLevel().entityManager.events.add(e);
         mob.spawnedEvents.add(e);
      }
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
