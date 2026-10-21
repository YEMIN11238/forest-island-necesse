package necesse.entity.mobs.ai.path;

import java.awt.Point;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.util.GameMath;
import necesse.engine.util.HashProxyLinkedList;
import necesse.engine.util.pathfinding.PathResult;
import necesse.engine.util.pathfinding.Pathfinding;
import necesse.entity.mobs.PathDoorOption;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.SubRegion;

public class RegionPathfinding extends Pathfinding<SubRegion> {
   public PathDoorOption doorOption;
   public BiPredicate<SubRegion, SubRegion> isAtTarget;

   public static boolean canMoveToTile(
      Level level, int fromTileX, int fromTileY, int toTileX, int toTileY, PathDoorOption doorOption, boolean acceptAdjacentTiles
   ) {
      return canMoveToTile(level, fromTileX, fromTileY, toTileX, toTileY, doorOption, acceptAdjacentTiles, 200);
   }

   public static boolean canMoveToTile(
      Level level, int fromTileX, int fromTileY, int toTileX, int toTileY, PathDoorOption doorOption, boolean acceptAdjacentTiles, int maxIterations
   ) {
      int fromRegionID = level.regionManager.getRegionIDByTile(fromTileX, fromTileY);
      if (fromRegionID == 0) {
         return false;
      }

      int toRegionID = level.regionManager.getRegionIDByTile(toTileX, toTileY);
      if (toRegionID == 0) {
         return false;
      }

      if (fromRegionID == toRegionID) {
         return true;
      }

      if (!acceptAdjacentTiles) {
         BiPredicate<SubRegion, SubRegion> isAtTarget = (current, target) -> current.getRegionID() == target.getRegionID();
         return findMoveToTile(level, fromTileX, fromTileY, toTileX, toTileY, doorOption, isAtTarget, maxIterations).foundTarget;
      }

      HashSet<SubRegion> accepted = new HashSet<>();
      accepted.add(level.regionManager.getSubRegionByTile(toTileX, toTileY));

      for (Point tile : level.getObject(toTileX, toTileY).getMultiTile(level, 0, toTileX, toTileY).getAdjacentTiles(toTileX, toTileY, true)) {
         if (level.regionManager.getRegionIDByTile(tile.x, tile.y) == fromRegionID) {
            return true;
         }

         accepted.add(level.regionManager.getSubRegionByTile(tile.x, tile.y));
      }

      BiPredicate<SubRegion, SubRegion> isAtTarget = (current, target) -> current.getRegionID() == target.getRegionID() || accepted.contains(current);
      return findMoveToTile(level, fromTileX, fromTileY, toTileX, toTileY, doorOption, isAtTarget, maxIterations).foundTarget;
   }

   public static PathResult<SubRegion, RegionPathfinding> findMoveToTile(
      Level level, int fromTileX, int fromTileY, int toTileX, int toTileY, PathDoorOption doorOption, BiPredicate<SubRegion, SubRegion> isAtTarget
   ) {
      return findMoveToTile(level, fromTileX, fromTileY, toTileX, toTileY, doorOption, isAtTarget, 200);
   }

   public static PathResult<SubRegion, RegionPathfinding> findMoveToTile(
      Level level,
      int fromTileX,
      int fromTileY,
      int toTileX,
      int toTileY,
      PathDoorOption doorOption,
      BiPredicate<SubRegion, SubRegion> isAtTarget,
      int maxIterations
   ) {
      RegionPathfinding regionPath = new RegionPathfinding(doorOption, isAtTarget);
      SubRegion from = level.regionManager.getSubRegionByTile(fromTileX, fromTileY);
      SubRegion to = level.regionManager.getSubRegionByTile(toTileX, toTileY);
      return regionPath.findPath(from, to, maxIterations);
   }

   public RegionPathfinding(PathDoorOption doorOption, BiPredicate<SubRegion, SubRegion> isAtTarget) {
      this.doorOption = doorOption;
      this.isAtTarget = isAtTarget;
   }

   public <C extends Pathfinding<SubRegion>> PathResult<SubRegion, C> findPath(SubRegion from, SubRegion target, int maxIterations) {
      return from != null && target != null
         ? Performance.record(from.region.manager.level.tickManager(), "regionPathfinding", () -> super.findPath(from, target, maxIterations))
         : super.findPath(from, target, maxIterations);
   }

   protected boolean isAtTarget(SubRegion current, SubRegion target) {
      return this.isAtTarget == null ? current == target : this.isAtTarget.test(current, target);
   }

   @Override
   protected void handleConnectedNodes(
      Pathfinding<SubRegion>.Node from,
      HashProxyLinkedList<Pathfinding<SubRegion>.Node, SubRegion> openNodes,
      HashProxyLinkedList<Pathfinding<SubRegion>.Node, SubRegion> closedNodes,
      HashSet<SubRegion> invalidChecked,
      Function<SubRegion, Boolean> handler,
      BiConsumer<Pathfinding<SubRegion>.Node, Pathfinding<SubRegion>.Node> connectedHandler,
      Runnable interrupt
   ) {
      for (SubRegion sr : ((SubRegion)from.item).getAdjacentRegions()) {
         Pathfinding<SubRegion>.Node openNode = openNodes.getObject(sr);
         if (openNode != null) {
            if (openNode.reverseDirection != from.reverseDirection) {
               connectedHandler.accept(openNode, from);
               return;
            }
         } else {
            Pathfinding<SubRegion>.Node closedNode = closedNodes.getObject(sr);
            if (closedNode != null) {
               if (closedNode.reverseDirection != from.reverseDirection) {
                  connectedHandler.accept(closedNode, from);
                  return;
               }
            } else {
               SubRegionPathResult pathThrough = this.doorOption.canPathThrough(sr);
               boolean valid;
               if (pathThrough == SubRegionPathResult.VALID) {
                  valid = true;
               } else if (pathThrough == SubRegionPathResult.CHECK_EACH_TILE) {
                  valid = sr.streamLevelTiles().anyMatch(tile -> this.doorOption.canPathThroughCheckTile(sr, tile.x, tile.y));
               } else {
                  valid = false;
               }

               if (valid) {
                  if (handler.apply(sr)) {
                     return;
                  }
               } else {
                  invalidChecked.add(sr);
               }
            }
         }
      }
   }

   protected double getNodeHeuristicCost(SubRegion currentNode, SubRegion targetNode) {
      return GameMath.diagonalMoveDistance(currentNode.region.regionX, currentNode.region.regionY, targetNode.region.regionX, targetNode.region.regionY);
   }

   protected double getNodeCost(SubRegion node) {
      return 0.0;
   }

   protected double getNodePathCost(SubRegion node, SubRegion cameFrom) {
      return 1.0;
   }

   public static double estimatePathTileLength(LinkedList<Pathfinding<SubRegion>.Node> path) {
      double length = 0.0;
      Pathfinding<SubRegion>.Node last = null;

      for (Pathfinding<SubRegion>.Node current : path) {
         if (last != null) {
            length += ((SubRegion)last.item).getAverageLevelTile().distance(((SubRegion)current.item).getAverageLevelTile());
         }

         last = current;
      }

      return length;
   }
}
