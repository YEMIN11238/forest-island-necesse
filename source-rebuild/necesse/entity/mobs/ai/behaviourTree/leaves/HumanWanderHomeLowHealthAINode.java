package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.friendly.human.HumanMob;

public class HumanWanderHomeLowHealthAINode<T extends HumanMob> extends WanderHomeAtConditionAINode<T> {
   public long nextApplyBuffTime;

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (blackboard.mover.isCurrentlyMovingFor(this)) {
         if (this.nextApplyBuffTime <= mob.getTime()) {
            mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SETTLER_SPRINT, mob, 2.0F, null), true);
            this.nextApplyBuffTime = mob.getTime() + 1500L;
         }
      } else {
         this.nextApplyBuffTime = 0L;
      }

      return super.tickNode(mob, blackboard);
   }

   public boolean shouldGoHome(T mob) {
      return !mob.isVisitor() && mob.isSettlerOnCurrentLevel() ? mob.isHiding : false;
   }

   public Point getHomeTile(T mob) {
      return !mob.isSettlerOnCurrentLevel() ? null : mob.home;
   }

   public boolean isHomeRoom(T mob) {
      return mob.isSettlerOnCurrentLevel();
   }

   public boolean isHomeHouse(T mob) {
      return true;
   }
}
