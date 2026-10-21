package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.hostile.pirates.PirateMob;

public class PirateEscapeAINode<T extends PirateMob> extends MoveTaskAINode<T> {
   public final int maxDistance;
   public boolean firedEvent;
   public long nextPathFindTime;

   public PirateEscapeAINode(int maxDistance) {
      this.maxDistance = maxDistance;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      blackboard.onEvent("resetPathTime", e -> this.nextPathFindTime = 0L);
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (mob.baseTile == null) {
         return AINodeResult.FAILURE;
      }

      if (mob.getDistance(mob.baseTile.x * 32 + 16, mob.baseTile.y * 32 + 16) > this.maxDistance) {
         if (!this.firedEvent) {
            this.nextPathFindTime = 0L;
            this.onEscape(mob);
            this.firedEvent = true;
         }

         if (!mob.buffManager.hasBuff(BuffRegistry.PIRATE_ESCAPE)) {
            mob.addBuff(new ActiveBuff(BuffRegistry.PIRATE_ESCAPE, mob, 0, null), true);
         }

         if (this.nextPathFindTime <= mob.getWorldEntity().getLocalTime()) {
            this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + 1000L;
            return this.moveToTileTask(mob.baseTile.x, mob.baseTile.y, null, path -> {
               if (path.moveIfWithin(-1, 1, () -> this.nextPathFindTime = 0L)) {
                  int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 2000, 0.1F);
                  this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + nextPathTimeAdd;
               }

               return AINodeResult.SUCCESS;
            });
         } else {
            return AINodeResult.SUCCESS;
         }
      } else {
         this.firedEvent = false;
         if (mob.buffManager.hasBuff(BuffRegistry.PIRATE_ESCAPE)) {
            mob.buffManager.removeBuff(BuffRegistry.PIRATE_ESCAPE, true);
         }

         return AINodeResult.FAILURE;
      }
   }

   public void onEscape(T mob) {
   }
}
