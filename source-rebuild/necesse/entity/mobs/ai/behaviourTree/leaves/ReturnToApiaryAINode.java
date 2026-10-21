package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.friendly.HoneyBeeMob;
import necesse.entity.objectEntity.AbstractBeeHiveObjectEntity;

public class ReturnToApiaryAINode<T extends HoneyBeeMob> extends MoveTaskAINode<T> {
   public long findPathHomeCooldown;
   public boolean movingToApiary;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.movingToApiary && mob.apiaryHome != null && mob.shouldReturnToApiary()) {
         if (blackboard.mover.isCurrentlyMovingFor(this)) {
            return AINodeResult.RUNNING;
         }

         AbstractBeeHiveObjectEntity beeHiveEntity = mob.getLevel()
            .entityManager
            .getObjectEntity(mob.apiaryHome.x, mob.apiaryHome.y, AbstractBeeHiveObjectEntity.class);
         if (beeHiveEntity != null && TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), mob.apiaryHome.x, mob.apiaryHome.y, mob.getTileX(), mob.getTileY())) {
            beeHiveEntity.onRoamingBeeReturned(mob);
            mob.remove();
         } else {
            this.movingToApiary = false;
         }
      }

      if (this.findPathHomeCooldown <= mob.getWorldEntity().getTime()) {
         this.findPathHomeCooldown = mob.getWorldEntity().getTime() + GameRandom.globalRandom.getIntBetween(10, 30) * 1000;
         if (mob.apiaryHome != null && mob.shouldReturnToApiary()) {
            AbstractBeeHiveObjectEntity beeHiveEntity = mob.getLevel()
               .entityManager
               .getObjectEntity(mob.apiaryHome.x, mob.apiaryHome.y, AbstractBeeHiveObjectEntity.class);
            if (beeHiveEntity != null && mob.estimateCanMoveTo(mob.apiaryHome.x, mob.apiaryHome.y, true)) {
               return this.moveToTileTask(
                  mob.apiaryHome.x, mob.apiaryHome.y, TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), mob.apiaryHome.x, mob.apiaryHome.y), path -> {
                     if (path.result.foundTarget) {
                        this.movingToApiary = true;
                        path.move(null);
                        return AINodeResult.RUNNING;
                     } else {
                        return AINodeResult.FAILURE;
                     }
                  }
               );
            }

            mob.clearApiaryHome();
         }
      }

      return AINodeResult.FAILURE;
   }
}
