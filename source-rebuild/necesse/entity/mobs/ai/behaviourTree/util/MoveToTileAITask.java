package necesse.entity.mobs.ai.behaviourTree.util;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiPredicate;
import java.util.function.Function;
import necesse.engine.DisposableExecutorService;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.pathfinding.PathResult;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.path.FinalPath;
import necesse.entity.mobs.ai.path.FinalPathPoint;
import necesse.entity.mobs.ai.path.PathOptions;
import necesse.entity.mobs.ai.path.TilePathfinding;

public class MoveToTileAITask extends FutureAITask<MoveToTileAITask.AIPathResult> {
   private MoveToTileAITask(Callable<MoveToTileAITask.AIPathResult> task, Function<MoveToTileAITask.AIPathResult, AINodeResult> handler) {
      super((DisposableExecutorService)null, task, handler);
   }

   private MoveToTileAITask(
      TickManager tickManagerChild,
      AIMover mover,
      AINode<?> node,
      int tileX,
      int tileY,
      BiPredicate<Point, Point> isAtTarget,
      int maxPathIterations,
      PathOptions pathOptions,
      Function<MoveToTileAITask.AIPathResult, AINodeResult> handler
   ) {
      super(
         node.mob().getLevel(),
         () -> {
            PathResult<Point, TilePathfinding> path = TilePathfinding.findPath(
               tickManagerChild, node.mob(), tileX, tileY, pathOptions, isAtTarget, maxPathIterations
            );
            return new MoveToTileAITask.AIPathResult(mover, node, path);
         },
         handler
      );
   }

   public static MoveToTileAITask pathToTile(
      AIMover mover,
      AINode<?> node,
      int tileX,
      int tileY,
      BiPredicate<Point, Point> isAtTarget,
      int maxPathIterations,
      PathOptions pathOptions,
      Function<MoveToTileAITask.AIPathResult, AINodeResult> handler
   ) {
      TickManager tickManager = node.mob().getLevel().tickManager();
      return new MoveToTileAITask(
         tickManager == null ? null : tickManager.getChild(), mover, node, tileX, tileY, isAtTarget, maxPathIterations, pathOptions, handler
      );
   }

   public static MoveToTileAITask directMoveToTile(
      AIMover mover, AINode<?> node, int tileX, int tileY, Function<MoveToTileAITask.AIPathResult, AINodeResult> handler
   ) {
      return new MoveToTileAITask(() -> new MoveToTileAITask.DirectAIPathResult(mover, node, tileX, tileY), handler);
   }

   public static class AIPathResult {
      protected final AIMover mover;
      protected final AINode<?> node;
      public final PathResult<Point, TilePathfinding> result;
      protected FinalPath finalPath;

      private AIPathResult(AIMover mover, AINode<?> node, PathResult<Point, TilePathfinding> result) {
         this.mover = mover;
         this.node = node;
         this.result = result;
      }

      public boolean isMobWithinStart(int tiles) {
         if (this.result.start == null) {
            return false;
         } else {
            return tiles < 0
               ? true
               : GameMath.squareDistance(
                     this.result.start.x,
                     this.result.start.y,
                     GameMath.getTileCoordinate(this.node.mob().getX() + this.result.finder.moveOffsetX),
                     GameMath.getTileCoordinate(this.node.mob().getY() + this.result.finder.moveOffsetY)
                  )
                  <= tiles;
         }
      }

      public boolean isResultWithin(int tiles) {
         if (this.result.target == null) {
            return false;
         }

         if (tiles < 0) {
            return true;
         }

         if (TilePathfinding.isResultWithin(this.result, this.result.target.x, this.result.target.y, tiles)) {
            return true;
         }

         Point last = this.result.getLastPathResult();
         return last != null && this.result.finder.doorOption.canBreakDown(last.x, last.y);
      }

      public FinalPath getFinalPath() {
         if (this.finalPath == null) {
            ArrayList<FinalPathPoint> finalPathPoints = TilePathfinding.reducePathPoints(this.result.finder, this.result.path);
            this.finalPath = new FinalPath(finalPathPoints);
         }

         return this.finalPath;
      }

      public float getFullPathLength() {
         return this.getFinalPath().getFullLength();
      }

      public float getCurrentPathLength() {
         return this.getFinalPath().getCurrentLength();
      }

      public float estimateMillisToFullPathWithSpeed(float mobSpeed) {
         return Entity.getTravelTimeMillis(mobSpeed, this.getFullPathLength());
      }

      public int getNextPathTimeBasedOnPathTime(float mobSpeed, float timeDivisor, int minimumMillis, float randomPercentOffset) {
         float timeToPath = this.estimateMillisToFullPathWithSpeed(mobSpeed);
         int finalTime = Math.max((int)(timeToPath / timeDivisor), 0);
         int offset = (int)(finalTime * randomPercentOffset);
         int minTime = Math.max(finalTime - offset, 1);
         int maxTime = Math.max(minTime, finalTime + offset);
         return Math.max(minimumMillis, GameRandom.globalRandom.getIntBetween(minTime, maxTime));
      }

      public void removeFirstNodeIfBacktracking() {
         if (this.result.path.size() > 2) {
            Point firstTile = (Point)this.result.path.getFirst().item;
            Point lastTile = (Point)this.result.path.getLast().item;
            float pathAngle = GameMath.getAngle(new Float(firstTile.x - lastTile.x, firstTile.y - lastTile.y));
            Mob mob = this.node.mob();
            float initialAngle = GameMath.getAngle(new Float(mob.getTileX() - firstTile.x, mob.getTileY() - firstTile.y));
            if (Math.abs(GameMath.getAngleDifference(pathAngle, initialAngle)) >= 90.0F) {
               this.result.path.removeFirst();
            }
         }
      }

      public boolean moveIfWithin(int startTileRange, int resultTileRange, Runnable pathInvalidated) {
         if (this.isMobWithinStart(startTileRange) && this.isResultWithin(resultTileRange)) {
            this.move(pathInvalidated);
            return true;
         } else {
            return false;
         }
      }

      public void move(Runnable pathInvalidated) {
         this.mover.setPath(this.node, this.getFinalPath(), pathInvalidated);
      }
   }

   private static class DirectAIPathResult extends MoveToTileAITask.AIPathResult {
      private final int tileX;
      private final int tileY;

      public DirectAIPathResult(AIMover mover, AINode<?> node, int tileX, int tileY) {
         super(mover, node, null);
         this.tileX = tileX;
         this.tileY = tileY;
      }

      @Override
      public boolean isMobWithinStart(int tiles) {
         return true;
      }

      @Override
      public boolean isResultWithin(int tiles) {
         return true;
      }

      @Override
      public FinalPath getFinalPath() {
         if (this.finalPath == null) {
            Mob mob = this.node.mob();
            List<FinalPathPoint> pathPoints = Arrays.asList(
               new FinalPathPoint(mob.getTileX(), mob.getTileY(), () -> true), new FinalPathPoint(this.tileX, this.tileY, () -> true)
            );
            this.finalPath = new FinalPath(new ArrayList<>(pathPoints));
         }

         return this.finalPath;
      }

      @Override
      public float getFullPathLength() {
         return this.node.mob().getDistance(this.tileX * 32 + 16, this.tileY * 32 + 16);
      }

      @Override
      public float getCurrentPathLength() {
         return this.node.mob().getDistance(this.tileX * 32 + 16, this.tileY * 32 + 16);
      }

      @Override
      public boolean moveIfWithin(int startTileRange, int resultTileRange, Runnable pathInvalidated) {
         this.move(pathInvalidated);
         return true;
      }

      @Override
      public void move(Runnable pathInvalidated) {
         this.mover.directMoveTo(this.node, this.tileX * 32 + 16, this.tileY * 32 + 16);
      }
   }
}
