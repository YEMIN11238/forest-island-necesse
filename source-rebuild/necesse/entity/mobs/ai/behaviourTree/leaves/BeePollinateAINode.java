package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.GameTileRange;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.friendly.HoneyBeeMob;
import necesse.entity.objectEntity.AbstractBeeHiveObjectEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.PollinateObject;
import necesse.level.gameObject.PollinateObjectHandler;

public class BeePollinateAINode<T extends HoneyBeeMob> extends MoveTaskAINode<T> {
   public PollinateObjectHandler target;

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      if (this.target != null) {
         this.target.reservable.reserve(mob);
      }

      return super.tick(mob, blackboard);
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (mob.pollinateTime == 0L) {
         return AINodeResult.FAILURE;
      }

      if (blackboard.mover.isCurrentlyMovingFor(this)) {
         return AINodeResult.SUCCESS;
      }

      if (this.target == null && mob.pollinateTime <= mob.getTime()) {
         Point baseTile = mob.apiaryHome != null ? mob.apiaryHome : new Point(mob.getTileX(), mob.getTileY());
         GameTileRange range = AbstractBeeHiveObjectEntity.pollinateTileRange;
         ArrayList<PollinateObjectHandler> validTiles = new ArrayList<>();
         Point pathOffset = mob.getPathMoveOffset();

         for (Point tile : range.getValidTiles(baseTile.x, baseTile.y)) {
            GameObject object = mob.getLevel().getObject(tile.x, tile.y);
            if (!mob.getLevel().isSolidTile(tile.x, tile.y)
               && object.isSeed
               && !mob.collidesWith(mob.getLevel(), tile.x * 32 + pathOffset.x, tile.y * 32 + pathOffset.y)
               && mob.estimateCanMoveTo(tile.x, tile.y, false)
               && object instanceof PollinateObject) {
               PollinateObjectHandler handler = ((PollinateObject)object).getPollinateHandler(mob.getLevel(), tile.x, tile.y);
               if (handler != null && handler.canPollinate() && handler.reservable.isAvailable(mob)) {
                  validTiles.add(handler);
               }
            }
         }

         if (!validTiles.isEmpty()) {
            this.target = GameRandom.globalRandom.getOneOf(validTiles);
            this.target.reservable.reserve(mob);
            return this.moveToTileTask(
               this.target.tileX, this.target.tileY, null, path -> path.moveIfWithin(-1, 0, null) ? AINodeResult.SUCCESS : AINodeResult.FAILURE
            );
         }
      }

      if (this.target != null && this.target.isValid()) {
         if (this.target.canPollinate()
            && !mob.isPollinating()
            && GameMath.diagonalMoveDistance(this.target.tileX, this.target.tileY, mob.getTileX(), mob.getTileY()) <= 1.0) {
            mob.pollinateAbility.runAndSend(this.target, 10000);
         }

         if (mob.isPollinating()) {
            return AINodeResult.SUCCESS;
         }

         this.target = null;
         mob.pollinateTime = 0L;
         return AINodeResult.SUCCESS;
      } else {
         this.target = null;
         return AINodeResult.FAILURE;
      }
   }
}
