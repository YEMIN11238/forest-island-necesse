package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;

public class CallHelpAINode<T extends Mob> extends AINode<T> {
   public String currentTargetKey = "currentTarget";
   public String chaserTargetKey = "chaserTarget";
   public final String eventType;
   public int tileRange;
   public int callCooldown;
   public boolean recursive = false;
   public Mob calledTarget;
   public long lastCallTime;

   public CallHelpAINode(String eventType, int tileRange, int callCooldown) {
      this.eventType = eventType;
      this.tileRange = tileRange;
      this.callCooldown = callCooldown;
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent(this.eventType, e -> {
         if (e instanceof CallHelpAINode.AICallHelpEvent) {
            Mob target = blackboard.getObject(Mob.class, this.chaserTargetKey);
            if (target == null) {
               Mob newTarget = ((CallHelpAINode.AICallHelpEvent)e).target;
               this.calledTarget = newTarget;
               this.lastCallTime = mob.getWorldEntity().getTime();
               blackboard.put(this.currentTargetKey, newTarget);
            }
         }
      });
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.calledTarget != null && (!this.calledTarget.isSamePlace(mob) || this.calledTarget.removed())) {
         this.calledTarget = null;
      }

      Mob target = blackboard.getObject(Mob.class, this.chaserTargetKey);
      if (target != null) {
         if ((this.recursive || target != this.calledTarget) && this.lastCallTime + this.callCooldown < mob.getWorldEntity().getTime()) {
            mob.getLevel()
               .entityManager
               .mobs
               .getInRegionByTileRange(mob.getTileX(), mob.getTileY(), this.tileRange)
               .forEach(m -> m.ai.blackboard.submitEvent(this.eventType, new CallHelpAINode.AICallHelpEvent(target)));
            this.lastCallTime = mob.getWorldEntity().getTime();
            return AINodeResult.SUCCESS;
         }
      } else {
         this.calledTarget = null;
      }

      return AINodeResult.FAILURE;
   }

   public static class AICallHelpEvent extends AIEvent {
      public final Mob target;

      public AICallHelpEvent(Mob target) {
         this.target = target;
      }
   }
}
