package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.util.HashSet;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.friendly.HoneyBeeMob;
import necesse.entity.mobs.friendly.QueenBeeMob;
import necesse.entity.objectEntity.AbstractBeeHiveObjectEntity;

public class MigrateToApiaryAINode<T extends QueenBeeMob> extends MoveTaskAINode<T> {
   public long findPathHomeCooldown;
   public boolean movingToApiary;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.movingToApiary && mob.migrationApiary != null && mob.shouldMigrate()) {
         if (blackboard.mover.isCurrentlyMovingFor(this)) {
            return AINodeResult.RUNNING;
         }

         AbstractBeeHiveObjectEntity beeHiveEntity = mob.getLevel()
            .entityManager
            .getObjectEntity(mob.migrationApiary.x, mob.migrationApiary.y, AbstractBeeHiveObjectEntity.class);
         if (beeHiveEntity == null
            || !TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), mob.migrationApiary.x, mob.migrationApiary.y, mob.getTileX(), mob.getTileY())) {
            this.movingToApiary = false;
         } else if (beeHiveEntity.canTakeMigratingQueen()) {
            HashSet<Integer> removedUniqueIDs = new HashSet<>();

            for (int uniqueID : mob.honeyBeeUniqueIDs) {
               Mob foundMob = mob.getLevel().entityManager.mobs.get(uniqueID, false);
               if (foundMob instanceof HoneyBeeMob) {
                  HoneyBeeMob honeyBeeMob = (HoneyBeeMob)foundMob;
                  honeyBeeMob.setApiaryHome(mob.migrationApiary.x, mob.migrationApiary.y);
                  honeyBeeMob.followingQueen.uniqueID = -1;
               } else {
                  removedUniqueIDs.add(uniqueID);
               }
            }

            removedUniqueIDs.forEach(uniqueIDx -> mob.honeyBeeUniqueIDs.remove(uniqueIDx));
            beeHiveEntity.migrateQueen(mob);
            mob.remove();
         } else {
            mob.clearMigrationApiary();
         }
      }

      if (this.findPathHomeCooldown <= mob.getWorldEntity().getTime()) {
         this.findPathHomeCooldown = mob.getWorldEntity().getTime() + GameRandom.globalRandom.getIntBetween(10, 30) * 1000;
         if (mob.migrationApiary != null && mob.shouldMigrate()) {
            AbstractBeeHiveObjectEntity beeHiveEntity = mob.getLevel()
               .entityManager
               .getObjectEntity(mob.migrationApiary.x, mob.migrationApiary.y, AbstractBeeHiveObjectEntity.class);
            if (beeHiveEntity != null && mob.estimateCanMoveTo(mob.migrationApiary.x, mob.migrationApiary.y, true)) {
               return this.moveToTileTask(
                  mob.migrationApiary.x,
                  mob.migrationApiary.y,
                  TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), mob.migrationApiary.x, mob.migrationApiary.y),
                  path -> {
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

            mob.clearMigrationApiary();
         }
      }

      return AINodeResult.FAILURE;
   }
}
