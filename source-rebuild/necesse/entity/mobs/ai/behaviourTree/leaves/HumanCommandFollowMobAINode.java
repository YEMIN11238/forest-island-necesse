package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.friendly.human.HumanMob;

public class HumanCommandFollowMobAINode<T extends HumanMob> extends FollowMobAINode<T> {
   public long nextApplyBuffTime;

   public AINodeResult tickFollowing(Mob target, T mob, Blackboard<T> blackboard) {
      AINodeResult out = super.tickFollowing(target, mob, blackboard);
      if (target != null) {
         double distance = GameMath.diagonalMoveDistance(mob.getX(), mob.getY(), target.getX(), target.getY());
         if (mob.commandMoveToFollowPoint && distance <= this.tileRadius * 1.5F * 32.0F) {
            mob.commandMoveToFollowPoint = false;
            blackboard.submitEvent("resetTarget", new AIEvent());
         }

         if (distance > this.tileRadius * 15 * 32) {
            mob.setPos(target.getX(), target.getY(), true);
            blackboard.mover.stopMoving(mob);
            blackboard.submitEvent("resetPathTime", new AIEvent());
         } else if (distance > this.tileRadius * 2.5F * 32.0F) {
            if (this.nextApplyBuffTime <= mob.getTime()) {
               mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SETTLER_SPRINT, mob, 2.0F, null), true);
               this.nextApplyBuffTime = mob.getTime() + 1500L;
            }
         } else {
            this.nextApplyBuffTime = mob.getTime() + 500L;
         }
      } else {
         this.nextApplyBuffTime = 0L;
         mob.commandMoveToFollowPoint = false;
      }

      return out;
   }

   public void onMovedToFollowTarget(Mob target, T mob, Blackboard<T> blackboard, boolean foundPosition) {
      super.onMovedToFollowTarget(target, mob, blackboard, foundPosition);
      if (mob.commandMoveToFollowPoint) {
         mob.commandMoveToFollowPoint = false;
         blackboard.submitEvent("resetTarget", new AIEvent());
      }
   }

   public Mob getFollowingMob(T mob) {
      if (mob.isHiding && mob.isSettlerOnCurrentLevel()) {
         return null;
      }

      if (mob.commandFollowMob != null && mob.commandFollowMob.removed()) {
         if (mob.isSettlerWithinSettlement()) {
            mob.commandFollowMob = null;
         } else {
            mob.commandGuard(null, mob.getX(), mob.getY());
         }
      }

      if (mob.commandFollowMob != null) {
         if (mob.objectUser != null) {
            mob.objectUser.stopUsing();
         }

         mob.setActivity("command", 15000, new LocalMessage("activities", "following", "target", mob.commandFollowMob.getLocalization()));
         return mob.commandFollowMob;
      } else {
         return null;
      }
   }
}
