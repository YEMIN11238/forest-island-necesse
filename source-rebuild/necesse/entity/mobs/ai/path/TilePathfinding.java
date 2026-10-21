package necesse.entity.mobs.ai.path;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.HashProxyLinkedList;
import necesse.engine.util.pathfinding.PathResult;
import necesse.engine.util.pathfinding.Pathfinding;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PathDoorOption;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.ui.HUD;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.multiTile.MultiTile;

public class TilePathfinding extends Pathfinding<Point> {
   public final TickManager tickManager;
   public final Level level;
   public final Mob mob;
   public final int moveOffsetX;
   public final int moveOffsetY;
   public final PathDoorOption doorOption;
   public final CollisionFilter collisionFilter;
   public final boolean canPassAllTiles;
   public final Rectangle tileCollisionOffsets;
   public BiPredicate<Point, Point> isAtTarget;
   public PathOptions options;
   public static final PathDir[] nonDiagonalPoints = new PathDir[]{PathDir.UP, PathDir.RIGHT, PathDir.DOWN, PathDir.LEFT};
   public static final PathDir[] diagonalPoints = PathDir.values();

   public static BiPredicate<Point, Point> isAtOrAdjacentObject(Level level, int tileX, int tileY) {
      MultiTile multiTile = level.getObject(tileX, tileY).getMultiTile(level, 0, tileX, tileY);
      Rectangle rect = multiTile.getAdjacentTileRectangle(tileX, tileY);
      return (current, target) -> rect.contains(current);
   }

   public static boolean isAtOrAdjacentObject(Level level, int targetTileX, int targetTileY, int currentTileX, int currentTileY) {
      MultiTile multiTile = level.getObject(targetTileX, targetTileY).getMultiTile(level, 0, targetTileX, targetTileY);
      Rectangle rect = multiTile.getAdjacentTileRectangle(targetTileX, targetTileY);
      return rect.contains(currentTileX, currentTileY);
   }

   public static PathResult<Point, TilePathfinding> findPath(
      TickManager tickManager, Mob mob, int targetX, int targetY, PathOptions pathOptions, BiPredicate<Point, Point> isAtTarget, int maxIterations
   ) {
      return findPath(tickManager, mob.getLevel(), mob, targetX, targetY, pathOptions, isAtTarget, maxIterations);
   }

   public static PathResult<Point, TilePathfinding> findPath(
      TickManager tickManager, Level level, Mob mob, int targetX, int targetY, PathOptions pathOptions, BiPredicate<Point, Point> isAtTarget, int maxIterations
   ) {
      TilePathfinding path = new TilePathfinding(tickManager, level, mob, isAtTarget, pathOptions);
      return path.findPath(targetX, targetY, maxIterations);
   }

   public TilePathfinding(TickManager tickManager, Level level, Mob mob, BiPredicate<Point, Point> isAtTarget, PathOptions options) {
      this(tickManager, level, mob, isAtTarget, options, mob.getPathDoorOption());
   }

   public TilePathfinding(TickManager tickManager, Level level, Mob mob, BiPredicate<Point, Point> isAtTarget, PathOptions options, PathDoorOption doorOption) {
      this.tickManager = tickManager;
      this.level = level;
      this.mob = mob;
      this.isAtTarget = isAtTarget;
      Point pathMoveOffset = mob.getPathMoveOffset();
      this.moveOffsetX = pathMoveOffset.x;
      this.moveOffsetY = pathMoveOffset.y;
      this.doorOption = doorOption;
      this.collisionFilter = mob.getLevelCollisionFilter();
      this.canPassAllTiles = this.collisionFilter == null || !this.collisionFilter.hasAdders();
      if (this.canPassAllTiles) {
         this.tileCollisionOffsets = new Rectangle();
      } else {
         Rectangle collision = mob.getCollision(this.moveOffsetX, this.moveOffsetY);
         int tileStartX = GameMath.getTileCoordinate(collision.x);
         int tileStartY = GameMath.getTileCoordinate(collision.y);
         int tileEndX = GameMath.getTileCoordinate(collision.x + collision.width);
         int tileEndY = GameMath.getTileCoordinate(collision.y + collision.height);
         this.tileCollisionOffsets = new Rectangle(tileStartX, tileStartY, tileEndX - tileStartX + 1, tileEndY - tileStartY + 1);
      }

      this.options = options;
      this.useBestOfConnected = true;
   }

   public PathResult<Point, TilePathfinding> findPath(int targetX, int targetY, int maxIterations) {
      Point closest = this.findClosestTile();
      return this.findPath(closest, new Point(targetX, targetY), maxIterations);
   }

   public <C extends Pathfinding<Point>> PathResult<Point, C> findPath(Point from, Point target, int maxIterations) {
      return Performance.record(this.tickManager, "tilePathfinding", () -> {
         PathResult<Point, C> path = super.findPath(from, target, maxIterations);
         HUD.submitPath((PathResult)path);
         return path;
      });
   }

   public Point findClosestTile() {
      Point node = null;
      double distance = -1.0;
      int mobTileX = GameMath.getTileCoordinate(this.mob.getX() - this.moveOffsetX + 16);
      int mobTileY = GameMath.getTileCoordinate(this.mob.getY() - this.moveOffsetY + 16);
      CollisionFilter nextFilter = this.collisionFilter;
      Set<Point> collisionTiles = new HashSet<>();
      if (nextFilter != null) {
         ArrayList<LevelObjectHit> currentCollisions = this.level.getCollisions(this.mob.getCollision(), nextFilter);
         if (!currentCollisions.isEmpty()) {
            collisionTiles = currentCollisions.stream().filter(h -> !h.invalidPos()).map(h -> new Point(h.tileX, h.tileY)).collect(Collectors.toSet());
            Set<Point> finalCollisionTiles = collisionTiles;
            nextFilter = nextFilter.copy().addFilter(tp -> !finalCollisionTiles.contains(new Point(tp.tileX, tp.tileY)));
         }
      }

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            if (i == 0 || j == 0) {
               Point tile = new Point(mobTileX + i, mobTileY + j);
               if (this.level.isTileWithinBounds(tile.x, tile.y) && this.level.regionManager.isTileLoaded(tile.x, tile.y) && !collisionTiles.contains(tile)) {
                  Point mobPoint = new Point(tile.x * 32 + this.moveOffsetX, tile.y * 32 + this.moveOffsetY);
                  if (this.canMoveNearbyTile(tile.x, tile.y, mobPoint, nextFilter, i != 0, j != 0)) {
                     double cDist = mobPoint.distance(this.mob.x, this.mob.y);
                     if (distance < 0.0 || cDist < distance) {
                        distance = cDist;
                        node = tile;
                     }
                  }
               }
            }
         }
      }

      if (node == null) {
         for (int i = 2; i < 7; i++) {
            Point tile = new Point(mobTileX + i, mobTileY);
            if (this.level.isTileXWithinBounds(tile.x) && this.level.regionManager.isTileLoaded(tile.x, tile.y) && !collisionTiles.contains(tile)) {
               Point mobPoint = new Point(tile.x * 32 + this.moveOffsetX, this.mob.getY());
               if (this.canMoveNearbyTile(tile.x, tile.y, mobPoint, nextFilter, true, false)) {
                  node = tile;
                  break;
               }

               tile = new Point(mobTileX - i, mobTileY);
               if (this.level.isTileXWithinBounds(tile.x) && this.level.regionManager.isTileLoaded(tile.x, tile.y) && !collisionTiles.contains(tile)) {
                  mobPoint = new Point(tile.x * 32 + this.moveOffsetX, this.mob.getY());
                  if (this.canMoveNearbyTile(tile.x, tile.y, mobPoint, nextFilter, true, false)) {
                     node = tile;
                     break;
                  }

                  tile = new Point(mobTileX, mobTileY + i);
                  if (this.level.isTileYWithinBounds(tile.y) && this.level.regionManager.isTileLoaded(tile.x, tile.y) && !collisionTiles.contains(tile)) {
                     mobPoint = new Point(tile.x * 32 + this.moveOffsetX, tile.y * 32 + this.moveOffsetY);
                     if (this.canMoveNearbyTile(tile.x, tile.y, mobPoint, nextFilter, false, true)) {
                        node = tile;
                        break;
                     }

                     tile = new Point(mobTileX, mobTileY - i);
                     if (this.level.isTileYWithinBounds(tile.y) && this.level.regionManager.isTileLoaded(tile.x, tile.y) && !collisionTiles.contains(tile)) {
                        mobPoint = new Point(tile.x * 32 + this.moveOffsetX, tile.y * 32 + this.moveOffsetY);
                        if (this.canMoveNearbyTile(tile.x, tile.y, mobPoint, nextFilter, false, true)) {
                           node = tile;
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      return node;
   }

   public boolean canMoveNearbyTile(int tileX, int tileY, Point mobPoint, CollisionFilter filter, boolean checkHorizontal, boolean checkVertical) {
      if (this.canMoveLine(filter, this.mob.getX(), this.mob.getY(), mobPoint.x, mobPoint.y)) {
         return true;
      } else if (checkHorizontal
         && this.canMoveLine(filter, this.mob.getX(), this.mob.getY(), mobPoint.x, this.mob.getY())
         && this.canMoveLine(filter, mobPoint.x, this.mob.getY(), mobPoint.x, mobPoint.y)) {
         return true;
      } else {
         return !checkVertical
            ? false
            : this.canMoveLine(filter, this.mob.getX(), this.mob.getY(), this.mob.getX(), mobPoint.y)
               && this.canMoveLine(filter, this.mob.getX(), mobPoint.y, mobPoint.x, mobPoint.y);
      }
   }

   public boolean canMoveLine(CollisionFilter filter, int startX, int startY, int endX, int endY) {
      return this.options.canMoveLine(this.tickManager, this.level, this.mob, filter, startX, startY, endX, endY);
   }

   protected boolean isAtTarget(Point current, Point target) {
      if (this.isAtTarget == null) {
         for (int x = 0; x < this.tileCollisionOffsets.width; x++) {
            for (int y = 0; y < this.tileCollisionOffsets.height; y++) {
               if (current.x + x == target.x && current.y + y == target.y) {
                  return true;
               }
            }
         }

         return false;
      } else {
         for (int x = 0; x < this.tileCollisionOffsets.width; x++) {
            for (int y = 0; y < this.tileCollisionOffsets.height; y++) {
               if (this.isAtTarget.test(new Point(current.x + x, current.y + y), target)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   protected boolean handleNewNode(
      Point node,
      Pathfinding<Point>.Node current,
      HashProxyLinkedList<Pathfinding<Point>.Node, Point> openNodes,
      HashProxyLinkedList<Pathfinding<Point>.Node, Point> closedNodes
   ) {
      return false;
   }

   @Override
   protected void handleConnectedNodes(
      Pathfinding<Point>.Node from,
      HashProxyLinkedList<Pathfinding<Point>.Node, Point> openNodes,
      HashProxyLinkedList<Pathfinding<Point>.Node, Point> closedNodes,
      HashSet<Point> invalidChecked,
      Function<Point, Boolean> handler,
      BiConsumer<Pathfinding<Point>.Node, Pathfinding<Point>.Node> connectedHandler,
      Runnable interrupt
   ) {
      Point fromTile = (Point)from.item;
      double currentTileCost = from.nodeCost;

      for (PathDir offset : diagonalPoints) {
         Point targetTile = new Point(fromTile.x + offset.x, fromTile.y + offset.y);
         if (this.level.isTileWithinBounds(targetTile.x, targetTile.y)) {
            Pathfinding<Point>.Node openNode = openNodes.getObject(targetTile);
            if (openNode != null) {
               if (openNode.reverseDirection != from.reverseDirection) {
                  connectedHandler.accept(openNode, from);
                  return;
               }
            } else {
               Pathfinding<Point>.Node closedNode = closedNodes.getObject(targetTile);
               if (closedNode != null) {
                  if (closedNode.reverseDirection != from.reverseDirection) {
                     connectedHandler.accept(closedNode, from);
                     return;
                  }
               } else if (this.checkCanPassDoorOrTile(targetTile)) {
                  if ((!offset.isDiagonal || this.checkCanPassDiagonalWithCost(fromTile, currentTileCost, offset.point)) && handler.apply(targetTile)) {
                     return;
                  }
               } else {
                  invalidChecked.add(targetTile);
               }
            }
         }
      }
   }

   protected boolean checkCanPassDiagonalWithCost(Point fromTile, double currentTileCost, Point offsetTile) {
      return Performance.record(
         this.tickManager,
         "checkCanPassDiagonal",
         () -> {
            if (this.canPassAllTiles) {
               return true;
            }

            boolean isLeft = offsetTile.x < 0;
            int xCheck = isLeft
               ? fromTile.x + this.tileCollisionOffsets.x + offsetTile.x
               : fromTile.x + this.tileCollisionOffsets.x + offsetTile.x + this.tileCollisionOffsets.width - 1;
            if (!this.options.canPassTile(this.tickManager, this.level, this.mob, this.doorOption, this.collisionFilter, xCheck, fromTile.y)) {
               return false;
            }

            if (this.options.getTileCost(this.level, this.mob, this.doorOption, xCheck, fromTile.y) > currentTileCost) {
               return false;
            }

            boolean isUp = offsetTile.y < 0;
            int yCheck = isUp
               ? fromTile.y + this.tileCollisionOffsets.y + offsetTile.y
               : fromTile.y + this.tileCollisionOffsets.y + offsetTile.y + this.tileCollisionOffsets.height - 1;
            return !this.options.canPassTile(this.tickManager, this.level, this.mob, this.doorOption, this.collisionFilter, fromTile.x, yCheck)
               ? false
               : !(this.options.getTileCost(this.level, this.mob, this.doorOption, fromTile.x, yCheck) > currentTileCost);
         }
      );
   }

   protected double getTileCost(int tileX, int tileY) {
      return Performance.record(this.tickManager, "getTileCost", () -> {
         double cost = 0.0;

         for (int x = 0; x < this.tileCollisionOffsets.width; x++) {
            int targetX = tileX + this.tileCollisionOffsets.x + x;

            for (int y = 0; y < this.tileCollisionOffsets.height; y++) {
               int targetY = tileY + this.tileCollisionOffsets.y + y;
               cost = Math.max(cost, this.options.getTileCost(this.level, this.mob, this.doorOption, targetX, targetY));
            }
         }

         return cost;
      });
   }

   protected boolean checkCanPassDoorOrTile(Point targetTile) {
      return Performance.record(this.tickManager, "checkIsAndCanPassDoor", () -> {
         if (this.canPassAllTiles) {
            return true;
         }

         for (int x = 0; x < this.tileCollisionOffsets.width; x++) {
            int tileX = targetTile.x + this.tileCollisionOffsets.x + x;

            for (int y = 0; y < this.tileCollisionOffsets.height; y++) {
               int tileY = targetTile.y + this.tileCollisionOffsets.y + y;
               if (!this.options.checkCanPassDoorOrTile(this.tickManager, this.level, this.mob, this.doorOption, this.collisionFilter, tileX, tileY)) {
                  return false;
               }
            }
         }

         return true;
      });
   }

   protected boolean checkCanPassDiagonalNoRecord(Point fromTile, Point offsetTile) {
      if (this.canPassAllTiles) {
         return true;
      }

      boolean isLeft = offsetTile.x < 0;
      int xCheck = isLeft
         ? fromTile.x + this.tileCollisionOffsets.x + offsetTile.x
         : fromTile.x + this.tileCollisionOffsets.x + offsetTile.x + this.tileCollisionOffsets.width - 1;
      if (!this.options.canPassTile(this.tickManager, this.level, this.mob, this.doorOption, this.collisionFilter, xCheck, fromTile.y)) {
         return false;
      }

      boolean isUp = offsetTile.y < 0;
      int yCheck = isUp
         ? fromTile.y + this.tileCollisionOffsets.y + offsetTile.y
         : fromTile.y + this.tileCollisionOffsets.y + offsetTile.y + this.tileCollisionOffsets.height - 1;
      return this.options.canPassTile(this.tickManager, this.level, this.mob, this.doorOption, this.collisionFilter, fromTile.x, yCheck);
   }

   protected boolean checkCanPassDoorOrTileNoRecord(Point targetTile) {
      if (this.canPassAllTiles) {
         return true;
      }

      for (int x = 0; x < this.tileCollisionOffsets.width; x++) {
         int tileX = targetTile.x + this.tileCollisionOffsets.x + x;

         for (int y = 0; y < this.tileCollisionOffsets.height; y++) {
            int tileY = targetTile.y + this.tileCollisionOffsets.y + y;
            if (!this.options.checkCanPassDoorOrTile(this.tickManager, this.level, this.mob, this.doorOption, this.collisionFilter, tileX, tileY)) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   protected double getNodeComparable(Pathfinding<Point>.Node node) {
      return this.options.nodePriority.cost.getCost(node);
   }

   protected double getNodeHeuristicCost(Point currentNode, Point targetNode) {
      int dx = targetNode.x - currentNode.x;
      int dy = targetNode.y - currentNode.y;
      return Math.abs(dx) + Math.abs(dy);
   }

   protected double getNodeCost(Point node) {
      return this.getTileCost(node.x, node.y);
   }

   protected double getNodePathCost(Point node, Point cameFrom) {
      return GameMath.diagonalMoveDistance(node, cameFrom);
   }

   public static DrawOptions getPathLineDrawOptions(List<Pathfinding<Point>.Node> path, GameCamera camera) {
      DrawOptionsList drawOptions = new DrawOptionsList();
      Pathfinding<Point>.Node last = null;
      int i = 0;

      for (Pathfinding<Point>.Node node : path) {
         if (last != null) {
            Color col = Color.getHSBColor((float)i / path.size(), 1.0F, 1.0F);
            int x1 = camera.getTileDrawX(((Point)last.item).x) + 16;
            int y1 = camera.getTileDrawY(((Point)last.item).y) + 16;
            int x2 = camera.getTileDrawX(((Point)node.item).x) + 16;
            int y2 = camera.getTileDrawY(((Point)node.item).y) + 16;
            if (camera.getBounds().intersectsLine(x1, y1, x2, y2)) {
               drawOptions.add(() -> Renderer.drawLineRGBA(x1, y1, x2, y2, col.getRed() / 255.0F, col.getGreen() / 255.0F, col.getBlue() / 255.0F, 1.0F));
            }
         }

         i++;
         last = node;
      }

      return drawOptions;
   }

   public static void drawPathLine(List<Pathfinding<Point>.Node> path, GameCamera camera) {
      getPathLineDrawOptions(path, camera).draw();
   }

   public static void drawPathProcess(PathResult<Point, TilePathfinding> result, GameCamera camera) {
      new TilePathFindingDrawOptions(result, camera).draw();
   }

   public static DrawOptions getPathDrawOptions(Mob mob, ArrayList<? extends Point> pathArray, GameCamera camera) {
      DrawOptionsList drawOptions = new DrawOptionsList();
      if (pathArray != null) {
         Point moveOffset = mob == null ? new Point(16, 16) : mob.getPathMoveOffset();

         for (int i = 0; i < pathArray.size(); i++) {
            Color col = i % 2 == 0 ? new Color(0.0F, 0.0F, 1.0F) : new Color(0.0F, 1.0F, 0.0F);
            Point p1 = pathArray.get(i);
            int lineX1 = camera.getTileDrawX(p1.x) + moveOffset.x;
            int lineY1 = camera.getTileDrawY(p1.y) + moveOffset.y;
            int lineX2;
            int lineY2;
            if (i == 0) {
               if (mob == null) {
                  continue;
               }

               lineX2 = camera.getDrawX(mob.x);
               lineY2 = camera.getDrawY(mob.y);
            } else {
               Point p2 = pathArray.get(i - 1);
               lineX2 = camera.getTileDrawX(p2.x) + moveOffset.x;
               lineY2 = camera.getTileDrawY(p2.y) + moveOffset.y;
            }

            drawOptions.add(
               () -> Renderer.drawLineRGBA(lineX1, lineY1, lineX2, lineY2, col.getRed() / 255.0F, col.getGreen() / 255.0F, col.getBlue() / 255.0F, 1.0F)
            );
         }
      }

      return drawOptions;
   }

   public static void drawPath(Mob mob, ArrayList<? extends Point> pathArray, GameCamera camera) {
      getPathDrawOptions(mob, pathArray, camera).draw();
   }

   public static boolean isResultWithin(PathResult<Point, TilePathfinding> result, int tileX, int tileY, int tiles) {
      if (result.foundTarget) {
         return true;
      } else {
         Point last = result.getLastPathResult();
         if (last != null) {
            int xDiff = Math.abs(last.x - tileX);
            int yDiff = Math.abs(last.y - tileY);
            int max = Math.max(xDiff, yDiff);
            return max <= tiles;
         } else {
            return false;
         }
      }
   }

   public static ArrayList<FinalPathPoint> reducePathPoints(TilePathfinding finder, LinkedList<Pathfinding<Point>.Node> path) {
      ArrayList<FinalPathPoint> out = new ArrayList<>();
      if (path != null && !path.isEmpty()) {
         if (path.size() > 1) {
            Pathfinding<Point>.Node first = path.getFirst();
            if (finder.level != null && !finder.level.isSolidTile(((Point)first.item).x, ((Point)first.item).y)) {
               path.removeFirst();
               Pathfinding<Point>.Node next = path.getFirst();
               Point mobPoint = new Point(((Point)next.item).x * 32 + finder.moveOffsetX, ((Point)next.item).y * 32 + finder.moveOffsetY);
               if (!finder.canMoveLine(finder.collisionFilter, finder.mob.getX(), finder.mob.getY(), mobPoint.x, mobPoint.y)) {
                  path.addFirst(first);
               }
            }
         }

         Pathfinding<Point>.Node lastNode = path.removeFirst();
         Pathfinding<Point>.Node firstNode = lastNode;
         out.add(new FinalPathPoint(((Point)lastNode.item).x, ((Point)lastNode.item).y, () -> finder.checkCanPassDoorOrTileNoRecord((Point)firstNode.item)));
         if (path.isEmpty()) {
            return out;
         }

         PathDir lastDir = PathDir.getDir((Point)path.getFirst().item, (Point)lastNode.item);
         LinkedList<Supplier<Boolean>> nextValidChecks = new LinkedList<>();
         int currentCount = -1;
         boolean forceNext = false;

         while (!path.isEmpty()) {
            currentCount++;
            Pathfinding<Point>.Node currentNode = path.removeFirst();
            PathDir currentDir = PathDir.getDir((Point)currentNode.item, (Point)lastNode.item);
            if (forceNext || currentDir != lastDir || currentCount >= 5) {
               LinkedList<Supplier<Boolean>> finalNextValidChecks = nextValidChecks;
               out.add(new FinalPathPoint(((Point)lastNode.item).x, ((Point)lastNode.item).y, () -> finalNextValidChecks.stream().allMatch(Supplier::get)));
               nextValidChecks = new LinkedList<>();
               forceNext = false;
               currentCount = 0;
            } else if (finder.level != null && finder.level.getObjectID(((Point)lastNode.item).x, ((Point)lastNode.item).y) != 0) {
               LevelObject lo = finder.level.getLevelObject(((Point)lastNode.item).x, ((Point)lastNode.item).y);
               if (lo.object.isDoor || lo.isSolid()) {
                  LinkedList<Supplier<Boolean>> finalNextValidChecks = nextValidChecks;
                  out.add(new FinalPathPoint(((Point)lastNode.item).x, ((Point)lastNode.item).y, () -> finalNextValidChecks.stream().allMatch(Supplier::get)));
                  nextValidChecks = new LinkedList<>();
                  forceNext = true;
                  currentCount = 0;
               }
            }

            if (currentDir != null && currentDir.isDiagonal) {
               Point fromPoint = (Point)lastNode.item;
               nextValidChecks.add(() -> finder.checkCanPassDiagonalNoRecord(fromPoint, currentDir.point));
            } else {
               nextValidChecks.add(() -> finder.checkCanPassDoorOrTileNoRecord((Point)currentNode.item));
            }

            lastDir = currentDir;
            lastNode = currentNode;
            if (path.isEmpty()) {
               LinkedList<Supplier<Boolean>> finalNextValidChecks = nextValidChecks;
               out.add(
                  new FinalPathPoint(((Point)currentNode.item).x, ((Point)currentNode.item).y, () -> finalNextValidChecks.stream().allMatch(Supplier::get))
               );
               nextValidChecks = new LinkedList<>();
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public enum NodePriority {
      HEURISTIC_TILE_COST(n -> n.heuristicCost + n.nodeCost),
      HEURISTIC_COST(n -> n.heuristicCost),
      PATH_TILE_COST(n -> n.pathCost + n.nodeCost),
      PATH_COST(n -> n.pathCost),
      TOTAL_COST(n -> n.heuristicCost + n.pathCost + n.nodeCost);

      public final TilePathfinding.NodePriorityFunction cost;

      NodePriority(TilePathfinding.NodePriorityFunction cost) {
         this.cost = cost;
      }
   }

   @FunctionalInterface
   public interface NodePriorityFunction {
      double getCost(Pathfinding<Point>.Node var1);
   }
}
