package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.friendly.human.HumanMob;

public class HumanInteractingAINode<T extends HumanMob> extends AINode<T> {
   protected boolean lastInteractedWith = false;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (mob.isBeingInteractedWith()) {
         if (mob.objectUser != null) {
            mob.objectUser.stopUsing();
         }

         blackboard.mover.stopMoving(mob);
         ServerClient interactingClient = mob.getFirstInteractingServerClient();
         if (interactingClient != null) {
            int lastDir = mob.getDir();
            Float dir = GameMath.normalize(interactingClient.playerMob.x - mob.getX(), interactingClient.playerMob.y - mob.getY());
            mob.setFacingDir(dir.x, dir.y);
            if (lastDir != mob.getDir()) {
               mob.sendMovementPacket(false);
            }
         }

         this.lastInteractedWith = true;
         return AINodeResult.SUCCESS;
      } else {
         if (this.lastInteractedWith) {
            this.lastInteractedWith = false;
            blackboard.submitEvent("resetPathTime", new AIEvent());
         }

         return AINodeResult.FAILURE;
      }
   }
}
