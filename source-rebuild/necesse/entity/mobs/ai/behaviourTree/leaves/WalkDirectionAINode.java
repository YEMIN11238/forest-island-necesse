package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;

public class WalkDirectionAINode<T extends Mob> extends MoveTaskAINode<T> {
   private final Float direction;
   private final int maxTileSearchDistance;
   private long nextSearchTime;
   private Point keepCenterTile;

   public WalkDirectionAINode(WalkDirectionAINode.Direction direction, int maxTileSearchDistance) {
      this(direction.dirX, direction.dirY, maxTileSearchDistance);
   }

   public WalkDirectionAINode(float dirX, float dirY, int maxTileSearchDistance) {
      this.direction = GameMath.normalize(dirX, dirY);
      this.maxTileSearchDistance = maxTileSearchDistance;
   }

   public WalkDirectionAINode<T> setKeepCenterTile(int tileX, int tileY) {
      this.keepCenterTile = new Point(tileX, tileY);
      return this;
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.nextSearchTime <= mob.getLocalTime()) {
         int fromTileX = mob.getTileX();
         int fromTileY = mob.getTileY();
         if (this.keepCenterTile != null) {
            if (this.direction.x == 0.0F) {
               fromTileX = this.keepCenterTile.x;
            }

            if (this.direction.y == 0.0F) {
               fromTileY = this.keepCenterTile.y;
            }
         }

         Point tile = null;

         for (int currentTileDistance = this.maxTileSearchDistance; currentTileDistance > 0; currentTileDistance--) {
            int perpSearchDistance = this.maxTileSearchDistance - currentTileDistance;
            int middleTileX = (int)(fromTileX + this.direction.x * currentTileDistance);
            int middleTileY = (int)(fromTileY + this.direction.y * currentTileDistance);
            if (this.checkTile(mob, blackboard, middleTileX, middleTileY)) {
               tile = new Point(middleTileX, middleTileY);
               break;
            }

            for (int offset = 1; offset <= perpSearchDistance; offset++) {
               int leftTileX = (int)(fromTileX + this.direction.x * currentTileDistance - this.direction.y * offset);
               int leftTileY = (int)(fromTileY + this.direction.y * currentTileDistance + this.direction.x * offset);
               if (this.checkTile(mob, blackboard, leftTileX, leftTileY)) {
                  tile = new Point(leftTileX, leftTileY);
                  break;
               }

               int rightTileX = (int)(fromTileX + this.direction.x * currentTileDistance + this.direction.y * offset);
               int rightTileY = (int)(fromTileY + this.direction.y * currentTileDistance - this.direction.x * offset);
               if (this.checkTile(mob, blackboard, rightTileX, rightTileY)) {
                  tile = new Point(rightTileX, rightTileY);
                  break;
               }
            }
         }

         if (tile != null) {
            return this.moveToTileTask(tile.x, tile.y, null, path -> {
               path.removeFirstNodeIfBacktracking();
               path.moveIfWithin(-1, -1, null);
               int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 500, 0.1F);
               this.nextSearchTime = mob.getLocalTime() + nextPathTimeAdd;
               return AINodeResult.SUCCESS;
            });
         }

         this.nextSearchTime = mob.getLocalTime() + 2000L;
      }

      return AINodeResult.SUCCESS;
   }

   protected boolean checkTile(T mob, Blackboard<T> blackboard, int tileX, int tileY) {
      return !mob.getLevel().isSolidTile(tileX, tileY);
   }

   public enum Direction {
      NORTH(0, -1),
      NORTH_EAST(1, -1),
      EAST(1, 0),
      SOUTH_EAST(1, 1),
      SOUTH(0, 1),
      SOUTH_WEST(-1, 1),
      WEST(-1, 0),
      NORTH_WEST(-1, -1);

      public final int dirX;
      public final int dirY;

      Direction(int dirX, int dirY) {
         this.dirX = dirX;
         this.dirY = dirY;
      }
   }
}
