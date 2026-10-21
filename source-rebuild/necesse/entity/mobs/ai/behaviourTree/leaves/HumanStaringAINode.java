package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.function.Supplier;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.event.StartStaringAIEvent;
import necesse.entity.mobs.friendly.human.HumanMob;

public class HumanStaringAINode<T extends HumanMob> extends AINode<T> {
   protected Supplier<Point> levelPositionGetter;
   protected long stopStaringAtTime;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("startstaring", event -> {
         if (event instanceof StartStaringAIEvent) {
            StartStaringAIEvent staringEvent = (StartStaringAIEvent)event;
            this.levelPositionGetter = staringEvent.levelPositionGetter;
            this.stopStaringAtTime = mob.getTime() + staringEvent.totalTime;
         }
      });
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.stopStaringAtTime >= mob.getTime() && this.levelPositionGetter != null) {
         for (AIWasHitEvent e : blackboard.getLastHits()) {
            if (!e.event.wasPrevented) {
               this.stopStaringAtTime = 0L;
               this.levelPositionGetter = null;
               return AINodeResult.FAILURE;
            }
         }

         Point position = this.levelPositionGetter.get();
         if (position != null) {
            if (blackboard.mover.isMoving()) {
               blackboard.mover.stopMoving(mob);
            }

            int lastDir = mob.getDir();
            Float dir = GameMath.normalize(position.x - mob.getX(), position.y - mob.getY());
            mob.setFacingDir(dir.x, dir.y);
            if (lastDir != mob.getDir()) {
               mob.sendMovementPacket(false);
            }

            return AINodeResult.SUCCESS;
         }

         this.stopStaringAtTime = 0L;
         this.levelPositionGetter = null;
      }

      return AINodeResult.FAILURE;
   }
}
