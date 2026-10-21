package necesse.entity.mobs.hostile.bosses.theVoid.ai;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.VoidRainAttackEvent;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;

public class TheVoidStillRainStage<T extends TheVoidMob> extends AINode<T> implements AttackStageInterface<T> {
   private final ArrayList<Point> targetPoints = new ArrayList<>();
   private long startTime;
   private int startupTime;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (mob.getTime() - this.startTime < this.startupTime) {
         return AINodeResult.RUNNING;
      } else if (!this.targetPoints.isEmpty()) {
         Point target = this.targetPoints.remove(0);
         VoidRainAttackEvent e = new VoidRainAttackEvent(mob, target.x, target.y, GameRandom.globalRandom, TheVoidMob.voidRainDamage);
         mob.getLevel().entityManager.events.add(e);
         return AINodeResult.RUNNING;
      } else {
         return AINodeResult.SUCCESS;
      }
   }

   public void onStarted(T mob, Blackboard<T> blackboard) {
      for (int i = 0; i < 100; i++) {
         Point p = new Point(
            (int)(mob.x + GameRandom.globalRandom.getIntBetween(-1000, 1000)), (int)(mob.y + GameRandom.globalRandom.getIntBetween(-1000, 1000))
         );
         if (!this.targetPoints.contains(p)) {
            this.targetPoints.add(p);
         }
      }

      this.startTime = mob.getTime();
      this.startupTime = GameMath.lerp(mob.getHealthPercent(), 250, 1000);
      mob.telegraphVoidRainAbility.runAndSend();
   }

   public void onEnded(T mob, Blackboard<T> blackboard) {
   }
}
