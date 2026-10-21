package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.friendly.human.HumanMob;

public class HumanCommandMoveToAINode<T extends HumanMob> extends CommandMoveToAINode<T> {
   public boolean isBeforeChaser;

   public HumanCommandMoveToAINode(boolean isBeforeChaser) {
      this.isBeforeChaser = isBeforeChaser;
   }

   public Point getLevelPosition(T mob) {
      if (mob.isHiding && mob.isSettlerOnCurrentLevel()) {
         return null;
      }

      if (this.isBeforeChaser && !mob.commandMoveToGuardPoint) {
         return null;
      }

      if (mob.commandGuardPoint != null) {
         if (mob.objectUser != null) {
            mob.objectUser.stopUsing();
         }

         return mob.commandGuardPoint;
      } else {
         return null;
      }
   }

   public void onArrived(T mob) {
      mob.commandMoveToGuardPoint = false;
   }

   public void tickMoving(T mob) {
      mob.resetCommandsBuffer = Math.max(180000, mob.resetCommandsBuffer);
   }

   public void tickStandingStill(T mob) {
   }

   public void onCannotMoveTo(T mob, int targetX, int targetY) {
      Point tile = findClosestMoveToTile(mob, targetX, targetY);
      if (tile != null) {
         mob.commandGuardPoint = new Point(tile.x * 32 + 16, tile.y * 32 + 16);
         this.nextPathFindTime = 0L;
         this.nextCheckArrivedTime = 0L;
      } else {
         mob.commandGuardPoint = new Point(mob.getX(), mob.getY());
      }
   }

   public void updateActivityDescription(T mob, GameMessage description) {
      if (description == null) {
         mob.clearActivity("command");
      } else {
         mob.setActivity("command", 15000, description);
      }
   }
}
