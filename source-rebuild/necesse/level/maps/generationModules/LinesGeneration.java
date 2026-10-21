package necesse.level.maps.generationModules;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.HashMapPointEntry;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.level.maps.Level;

public class LinesGeneration {
   public final int x1;
   public final int y1;
   public final int x2;
   public final int y2;
   public final float width;
   protected Float dir;
   protected boolean lineWidthPriority;
   protected final LinesGeneration root;
   protected LinkedList<LinesGeneration> lines = new LinkedList<>();

   private LinesGeneration(LinesGeneration root, int x1, int y1, int x2, int y2, float width) {
      this.root = root;
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
      this.width = width;
   }

   public LinesGeneration(int x1, int y1, int x2, int y2, float width) {
      this(null, x1, y1, x2, y2, width);
   }

   public LinesGeneration(int x, int y, float range) {
      this(null, x, y, x, y, range);
   }

   public LinesGeneration(int x, int y) {
      this(null, x, y, x, y, 0.0F);
   }

   public Float getDir() {
      if (this.dir == null) {
         this.dir = GameMath.normalize(this.x1 - this.x2, this.y1 - this.y2);
      }

      return this.dir;
   }

   public float getDirX() {
      return this.getDir().x;
   }

   public float getDirY() {
      return this.getDir().y;
   }

   public LinesGeneration lineWidthPriority(boolean value) {
      this.lineWidthPriority = value;
      return this;
   }

   public LinesGeneration addLine(int x1, int y1, int x2, int y2, float width) {
      if (this.root != null) {
         return this.root.addLine(x1, y1, x2, y2, width);
      }

      LinesGeneration e = new LinesGeneration(this, x1, y1, x2, y2, width);
      this.lines.addLast(e);
      return e;
   }

   public LinesGeneration addLineTo(int x, int y, float width) {
      if (this.root != null) {
         return this.root.addLine(this.x2, this.y2, x, y, width);
      }

      LinesGeneration e = new LinesGeneration(this, this.x2, this.y2, x, y, width);
      this.lines.addLast(e);
      return e;
   }

   public void removeLastLine() {
      if (this.root != null) {
         this.root.removeLastLine();
      } else if (!this.lines.isEmpty()) {
         this.lines.removeLast();
      }
   }

   public LinesGeneration addLineToDelta(int dx, int dy, float width) {
      return this.addLineTo(this.x2 + dx, this.y2 + dy, width);
   }

   public LinesGeneration addPoint(int x, int y, float width) {
      return this.addLine(x, y, x, y, width);
   }

   public LinesGeneration addPointDelta(int dx, int dy, float width) {
      return this.addLine(this.x2 + dx, this.y2 + dy, this.x2 + dx, this.y2 + dy, width);
   }

   public LinesGeneration addRandomArms(GameRandom random, int arms, float minRange, float maxRange, float minWidth, float maxWidth) {
      int angle = random.nextInt(360);
      int anglePerArm = 360 / arms;

      for (int i = 0; i < arms; i++) {
         angle += random.getIntOffset(anglePerArm, anglePerArm / 2);
         float range = random.getFloatBetween(minRange, maxRange);
         float width = random.getFloatBetween(minWidth, maxWidth);
         this.addArm(angle, range, width);
      }

      return this;
   }

   public LinesGeneration addArm(float angle, float range, float width) {
      Float dir = GameMath.getAngleDir(angle);
      return this.addLineToDelta((int)(dir.x * range), (int)(dir.y * range), width);
   }

   public LinesGeneration addMultiArm(
      GameRandom random,
      int startAngle,
      int angleOffset,
      int maxLength,
      float armMinLength,
      float armMaxLength,
      float armMinWidth,
      float armMaxWidth,
      Predicate<LinesGeneration> stopAtArm
   ) {
      float currentLength = 0.0F;
      LinesGeneration currentArm = this;

      while (currentLength < maxLength) {
         startAngle = random.getIntOffset(startAngle, angleOffset);
         float armLength = random.getFloatBetween(armMinLength, armMaxLength);
         float armWidth = random.getFloatBetween(armMinWidth, armMaxWidth);
         currentLength += armLength;
         currentArm = currentArm.addArm(startAngle, armLength, armWidth);
         if (stopAtArm != null && stopAtArm.test(currentArm)) {
            break;
         }
      }

      return currentArm;
   }

   public LinesGeneration addMultiArm(
      GameRandom random,
      Level level,
      int startAngle,
      int angleOffset,
      int maxLength,
      float armMinLength,
      float armMaxLength,
      float armMinWidth,
      float armMaxWidth
   ) {
      return this.addMultiArm(
         random, startAngle, angleOffset, maxLength, armMinLength, armMaxLength, armMinWidth, armMaxWidth, lg -> !level.isTileWithinBounds(lg.x2, lg.y2)
      );
   }

   public CellAutomaton toCellularAutomaton(BiFunction<Point, LinesGeneration.PointDistance, Boolean> startAliveFunction, boolean ordered) {
      CellAutomaton cellAutomaton = new CellAutomaton();
      if (ordered) {
         Comparator<HashMapPointEntry<Point, LinesGeneration.PointDistance>> comparator = Comparator.comparingInt(e -> e.getKey().x);
         comparator = comparator.thenComparingInt(e -> e.getKey().y);
         this.getSmoothPoints().streamEntries().sorted(comparator).forEachOrdered(e -> {
            Point point = e.getKey();
            LinesGeneration.PointDistance dist = e.getValue();
            if (startAliveFunction.apply(point, dist)) {
               cellAutomaton.setAlive(point.x, point.y);
            }
         });
      } else {
         this.getSmoothPoints().forEach((x, y, dist) -> {
            if (startAliveFunction.apply(new Point(x, y), dist)) {
               cellAutomaton.setAlive(x, y);
            }
         });
      }

      return cellAutomaton;
   }

   public CellAutomaton toCellularAutomaton(GameRandom random) {
      return this.toCellularAutomaton((point, dist) -> {
         if (dist.lineWidth == 0.0) {
            return false;
         }

         double chance = Math.abs(dist.dist / dist.lineWidth - 1.0);
         return random.getChance(chance);
      }, true);
   }

   public CellAutomaton doCellularAutomaton(
      BiFunction<Point, LinesGeneration.PointDistance, Boolean> startAliveFunction, boolean ordered, int deathLimit, int birthLimit, int iterations
   ) {
      CellAutomaton cellAutomaton = this.toCellularAutomaton(startAliveFunction, ordered);
      cellAutomaton.doCellularAutomaton(deathLimit, birthLimit, iterations);
      return cellAutomaton;
   }

   public CellAutomaton doCellularAutomaton(GameRandom random, int deathLimit, int birthLimit, int iterations) {
      CellAutomaton cellAutomaton = this.toCellularAutomaton(random);
      cellAutomaton.doCellularAutomaton(deathLimit, birthLimit, iterations);
      return cellAutomaton;
   }

   public CellAutomaton doCellularAutomaton(BiFunction<Point, LinesGeneration.PointDistance, Boolean> startAliveFunction, boolean ordered) {
      return this.doCellularAutomaton(startAliveFunction, ordered, 4, 3, 4);
   }

   public CellAutomaton doCellularAutomaton(GameRandom random) {
      return this.doCellularAutomaton(random, 4, 3, 4);
   }

   public LinesGeneration getRoot() {
      return this.root == null ? this : this.root;
   }

   public PointHashMap<LinesGeneration.PointDistance> getSmoothPoints() {
      return this.getSmoothPoints(GameMath::getExactDistance);
   }

   public PointHashMap<LinesGeneration.PointDistance> getSmoothPoints(LinesGeneration.DistanceFunction distanceFunction) {
      LinesGeneration root = this.root == null ? this : this.root;
      PointHashMap<LinesGeneration.PointDistance> pointDistances = new PointHashMap<>();
      this.addSmoothPoints(pointDistances, this.width, distanceFunction);

      for (LinesGeneration point : root.lines) {
         point.addSmoothPoints(pointDistances, point.width, distanceFunction);
      }

      return pointDistances;
   }

   public PointHashSet getDiamondPoints() {
      return this.getSimplePoints(GameMath::diamondDistance);
   }

   public PointHashSet getDiagonalDistancePoints() {
      return this.getSimplePoints(GameMath::diagonalMoveDistance);
   }

   public PointHashSet getSimplePoints(LinesGeneration.DistanceFunction distanceFunction) {
      LinesGeneration root = this.root == null ? this : this.root;
      PointHashSet points = new PointHashSet();
      this.addSimplePoints(points, this.width, distanceFunction);

      for (LinesGeneration point : root.lines) {
         point.addSimplePoints(points, point.width, distanceFunction);
      }

      return points;
   }

   public java.awt.geom.Line2D.Float getTileLine() {
      return new java.awt.geom.Line2D.Float(this.x1, this.y1, this.x2, this.y2);
   }

   public java.awt.geom.Line2D.Float getPosLine() {
      return new java.awt.geom.Line2D.Float(this.x1 * 32 + 16, this.y1 * 32 + 16, this.x2 * 32 + 16, this.y2 * 32 + 16);
   }

   public Iterable<LinesGeneration> getLines() {
      return this.lines;
   }

   public boolean isEmpty() {
      return this.lines.isEmpty();
   }

   public void recursiveLines(Function<LinesGeneration, Boolean> handler) {
      for (LinesGeneration line : this.lines) {
         if (handler.apply(line)) {
            line.recursiveLines(handler);
         }
      }
   }

   public static void pathTiles(java.awt.geom.Line2D.Float tileLine, boolean preferTop, BiConsumer<Point, Point> consumer) {
      pathTilesBreak(tileLine, preferTop, (from, next) -> {
         consumer.accept(from, next);
         return true;
      });
   }

   public static void pathTilesBreak(java.awt.geom.Line2D.Float tileLine, boolean preferTop, BiPredicate<Point, Point> consumer) {
      HashSet<Point> lastTiles = new HashSet<>();
      Point currentTile = new Point((int)Math.floor(tileLine.x1), (int)Math.floor(tileLine.y1));
      Point endTile = new Point((int)Math.floor(tileLine.x2), (int)Math.floor(tileLine.y2));
      java.awt.geom.Line2D.Float posLine = new java.awt.geom.Line2D.Float(
         tileLine.x1 * 32.0F + 16.0F, tileLine.y1 * 32.0F + 16.0F, tileLine.x2 * 32.0F + 16.0F, tileLine.y2 * 32.0F + 16.0F
      );
      if (consumer.test(null, currentTile)) {
         lastTiles.add(currentTile);

         Point next;
         while ((next = getNextPathLine(currentTile, lastTiles, posLine, preferTop)) != null) {
            lastTiles.add(next);
            Point lastTile = currentTile;
            currentTile = next;
            if (!consumer.test(lastTile, currentTile) || currentTile.x == endTile.x && currentTile.y == endTile.y) {
               break;
            }
         }
      }
   }

   private static Point getNextPathLine(Point currentTile, HashSet<Point> checkedTiles, java.awt.geom.Line2D.Float posLine, boolean preferTop) {
      if (pathLineIntersects(currentTile.x, currentTile.y - 1, checkedTiles, posLine)) {
         if (posLine.getX1() < posLine.getX2()) {
            checkedTiles.add(new Point(currentTile.x + 1, currentTile.y));
         } else if (posLine.getX1() > posLine.getX2()) {
            checkedTiles.add(new Point(currentTile.x - 1, currentTile.y));
         }

         return new Point(currentTile.x, currentTile.y - 1);
      } else if (pathLineIntersects(currentTile.x + 1, currentTile.y, checkedTiles, posLine)) {
         if (posLine.getY1() < posLine.getY2()) {
            checkedTiles.add(new Point(currentTile.x, currentTile.y + 1));
         } else if (posLine.getY1() > posLine.getY2()) {
            checkedTiles.add(new Point(currentTile.x, currentTile.y - 1));
         }

         return new Point(currentTile.x + 1, currentTile.y);
      } else if (pathLineIntersects(currentTile.x, currentTile.y + 1, checkedTiles, posLine)) {
         if (posLine.getX1() < posLine.getX2()) {
            checkedTiles.add(new Point(currentTile.x + 1, currentTile.y));
         } else if (posLine.getX1() > posLine.getX2()) {
            checkedTiles.add(new Point(currentTile.x - 1, currentTile.y));
         }

         return new Point(currentTile.x, currentTile.y + 1);
      } else if (pathLineIntersects(currentTile.x - 1, currentTile.y, checkedTiles, posLine)) {
         if (posLine.getY1() < posLine.getY2()) {
            checkedTiles.add(new Point(currentTile.x, currentTile.y - 1));
         } else if (posLine.getY1() > posLine.getY2()) {
            checkedTiles.add(new Point(currentTile.x, currentTile.y + 1));
         }

         return new Point(currentTile.x - 1, currentTile.y);
      } else if (pathLineIntersects(currentTile.x + 1, currentTile.y - 1, checkedTiles, posLine)) {
         if (preferTop) {
            checkedTiles.add(new Point(currentTile.x + 1, currentTile.y));
            return new Point(currentTile.x, currentTile.y - 1);
         } else {
            checkedTiles.add(new Point(currentTile.x, currentTile.y - 1));
            return new Point(currentTile.x + 1, currentTile.y);
         }
      } else if (pathLineIntersects(currentTile.x + 1, currentTile.y + 1, checkedTiles, posLine)) {
         if (preferTop) {
            checkedTiles.add(new Point(currentTile.x, currentTile.y + 1));
            return new Point(currentTile.x + 1, currentTile.y);
         } else {
            checkedTiles.add(new Point(currentTile.x + 1, currentTile.y));
            return new Point(currentTile.x, currentTile.y + 1);
         }
      } else if (pathLineIntersects(currentTile.x - 1, currentTile.y + 1, checkedTiles, posLine)) {
         if (preferTop) {
            checkedTiles.add(new Point(currentTile.x, currentTile.y + 1));
            return new Point(currentTile.x - 1, currentTile.y);
         } else {
            checkedTiles.add(new Point(currentTile.x - 1, currentTile.y));
            return new Point(currentTile.x, currentTile.y + 1);
         }
      } else if (pathLineIntersects(currentTile.x - 1, currentTile.y - 1, checkedTiles, posLine)) {
         if (preferTop) {
            checkedTiles.add(new Point(currentTile.x - 1, currentTile.y));
            return new Point(currentTile.x, currentTile.y - 1);
         } else {
            checkedTiles.add(new Point(currentTile.x, currentTile.y - 1));
            return new Point(currentTile.x - 1, currentTile.y);
         }
      } else {
         return null;
      }
   }

   private static boolean pathLineIntersects(int nextTileX, int nextTileY, HashSet<Point> checkedTiles, java.awt.geom.Line2D.Float posLine) {
      return !checkedTiles.contains(new Point(nextTileX, nextTileY)) && posLine.intersects(getTileRectangle(nextTileX, nextTileY));
   }

   private static Rectangle getTileRectangle(int tileX, int tileY) {
      return new Rectangle(tileX * 32, tileY * 32, 32, 32);
   }

   private void addTestPoints(HashSet<Point> points, float lineRange) {
      for (int pX = (int)Math.floor(this.x1 - lineRange); pX <= this.x1 + lineRange; pX++) {
         for (int pY = (int)Math.floor(this.y1 - lineRange); pY <= this.y1 + lineRange; pY++) {
            Point next = new Point(pX, pY);
            if (!points.contains(next) && GameMath.diamondDistance(this.x1, this.y1, next.x, next.y) <= lineRange) {
               points.add(next);
            }
         }
      }

      for (int pX = (int)Math.floor(this.x2 - lineRange); pX <= this.x2 + lineRange; pX++) {
         for (int pY = (int)Math.floor(this.y2 - lineRange); pY <= this.y2 + lineRange; pY++) {
            Point next = new Point(pX, pY);
            if (!points.contains(next) && GameMath.diamondDistance(this.x2, this.y2, next.x, next.y) <= lineRange) {
               points.add(next);
            }
         }
      }

      int deltaX = this.x1 - this.x2;
      int deltaY = this.y1 - this.y2;
      if (deltaX != 0 || deltaY != 0) {
         int deltaAbsX = Math.abs(deltaX);
         int deltaAbsY = Math.abs(deltaY);
         if (deltaAbsY <= deltaAbsX) {
            Point start;
            Point end;
            if (this.x1 < this.x2) {
               start = new Point(this.x1, this.y1);
               end = new Point(this.x2, this.y2);
            } else {
               start = new Point(this.x2, this.y2);
               end = new Point(this.x1, this.y1);
            }

            int moveY = end.y - start.y;
            int moveX = end.x - start.x;

            for (int pX = start.x + 1; pX < end.x; pX++) {
               float currentY = start.y + moveY * ((float)(pX - start.x) / moveX);
               System.out.println("On X " + pX + ", " + currentY);
               int startY = (int)Math.floor(currentY - lineRange);
               int endY = (int)Math.ceil(currentY + lineRange);

               for (int pY = startY; pY <= endY; pY++) {
                  System.out.println("Checking Y " + pY);
                  Point next = new Point(pX, pY);
                  if (!points.contains(next) && Math.abs(pY - currentY) <= lineRange) {
                     points.add(next);
                  }
               }
            }
         } else {
            Point start;
            Point end;
            if (this.y1 < this.y2) {
               start = new Point(this.x1, this.y1);
               end = new Point(this.x2, this.y2);
            } else {
               start = new Point(this.x2, this.y2);
               end = new Point(this.x1, this.y1);
            }

            int moveY = end.y - start.y;
            int moveX = end.x - start.x;

            for (int pY = start.y + 1; pY < end.y; pY++) {
               float currentX = start.x + moveX * ((float)(pY - start.y) / moveY);
               int startX = (int)Math.floor(currentX - lineRange);
               int endY = (int)Math.ceil(currentX + lineRange);

               for (int pX = startX; pX <= endY; pX++) {
                  Point next = new Point(pX, pY);
                  if (!points.contains(next) && Math.abs(pX - currentX) <= lineRange) {
                     points.add(next);
                  }
               }
            }
         }
      }
   }

   private void addSmoothPoints(PointHashMap<LinesGeneration.PointDistance> points, float maxRange, LinesGeneration.DistanceFunction distanceFunction) {
      if (this.x1 == this.x2 && this.y1 == this.y2) {
         int startX = (int)Math.floor(this.x1 - maxRange);
         int endX = (int)Math.ceil(this.x1 + maxRange);
         int startY = (int)Math.floor(this.y1 - maxRange);
         int endY = (int)Math.ceil(this.y1 + maxRange);

         for (int pX = startX; pX <= endX; pX++) {
            for (int pY = startY; pY <= endY; pY++) {
               double dist = distanceFunction.getDistance(this.x1, this.y1, pX, pY);
               if (!(dist > maxRange)) {
                  points.compute(
                     pX,
                     pY,
                     (point, lastDist) -> {
                        if (lastDist == null) {
                           return new LinesGeneration.PointDistance(dist, maxRange, this.lineWidthPriority);
                        } else {
                           return !(dist < lastDist.dist)
                              ? lastDist
                              : new LinesGeneration.PointDistance(
                                 dist,
                                 this.lineWidthPriority && !lastDist.lineWidthPriority ? maxRange : Math.min(maxRange, lastDist.lineWidth),
                                 lastDist.lineWidthPriority || this.lineWidthPriority
                              );
                        }
                     }
                  );
               }
            }
         }
      } else if (Math.abs(this.y1 - this.y2) <= Math.abs(this.x1 - this.x2)) {
         Point start;
         Point end;
         if (this.x1 < this.x2) {
            start = new Point(this.x1, this.y1);
            end = new Point(this.x2, this.y2);
         } else {
            start = new Point(this.x2, this.y2);
            end = new Point(this.x1, this.y1);
         }

         int moveY = end.y - start.y;
         int moveX = end.x - start.x;

         for (int currentX = start.x; currentX <= end.x; currentX++) {
            float currentY = start.y + moveY * ((float)(currentX - start.x) / moveX);
            int startX = (int)Math.floor(currentX - maxRange);
            int endX = (int)Math.ceil(currentX + maxRange);
            int startY = (int)Math.floor(currentY - maxRange);
            int endY = (int)Math.ceil(currentY + maxRange);

            for (int pX = startX; pX <= endX; pX++) {
               for (int pY = startY; pY <= endY; pY++) {
                  double dist = this.getDistToPoint(pX, pY, distanceFunction);
                  if (!(dist > maxRange)) {
                     points.compute(
                        pX,
                        pY,
                        (point, lastDist) -> {
                           if (lastDist == null) {
                              return new LinesGeneration.PointDistance(dist, maxRange, this.lineWidthPriority);
                           } else {
                              return !(dist < lastDist.dist)
                                 ? lastDist
                                 : new LinesGeneration.PointDistance(
                                    dist,
                                    this.lineWidthPriority && !lastDist.lineWidthPriority ? maxRange : Math.min(maxRange, lastDist.lineWidth),
                                    lastDist.lineWidthPriority || this.lineWidthPriority
                                 );
                           }
                        }
                     );
                  }
               }
            }
         }
      } else {
         Point start;
         Point end;
         if (this.y1 < this.y2) {
            start = new Point(this.x1, this.y1);
            end = new Point(this.x2, this.y2);
         } else {
            start = new Point(this.x2, this.y2);
            end = new Point(this.x1, this.y1);
         }

         int moveY = end.y - start.y;
         int moveX = end.x - start.x;

         for (int currentY = start.y; currentY <= end.y; currentY++) {
            float currentX = start.x + moveX * ((float)(currentY - start.y) / moveY);
            int startX = (int)Math.floor(currentX - maxRange);
            int endX = (int)Math.ceil(currentX + maxRange);
            int startY = (int)Math.floor(currentY - maxRange);
            int endY = (int)Math.ceil(currentY + maxRange);

            for (int pX = startX; pX <= endX; pX++) {
               for (int pY = startY; pY <= endY; pY++) {
                  double dist = this.getDistToPoint(pX, pY, distanceFunction);
                  if (!(dist > maxRange)) {
                     points.compute(
                        pX,
                        pY,
                        (point, lastDist) -> {
                           if (lastDist == null) {
                              return new LinesGeneration.PointDistance(dist, maxRange, this.lineWidthPriority);
                           } else {
                              return !(dist < lastDist.dist)
                                 ? lastDist
                                 : new LinesGeneration.PointDistance(
                                    dist,
                                    this.lineWidthPriority && !lastDist.lineWidthPriority ? maxRange : Math.min(maxRange, lastDist.lineWidth),
                                    lastDist.lineWidthPriority || this.lineWidthPriority
                                 );
                           }
                        }
                     );
                  }
               }
            }
         }
      }
   }

   private double getDistToPoint(int x, int y, LinesGeneration.DistanceFunction distanceFunction) {
      int a = x - this.x1;
      int b = y - this.y1;
      int c = this.x2 - this.x1;
      int d = this.y2 - this.y1;
      int dot = a * c + b * d;
      int lenSq = c * c + d * d;
      float param = -1.0F;
      if (lenSq != 0) {
         param = (float)dot / lenSq;
      }

      float xx;
      float yy;
      if (param < 0.0F) {
         xx = this.x1;
         yy = this.y1;
      } else if (param > 1.0F) {
         xx = this.x2;
         yy = this.y2;
      } else {
         xx = this.x1 + param * c;
         yy = this.y1 + param * d;
      }

      return distanceFunction.getDistance(x, y, xx, yy);
   }

   private void addSimplePoints(PointHashSet points, float lineRange, LinesGeneration.DistanceFunction distanceFunction) {
      if (this.x1 == this.x2 && this.y1 == this.y2) {
         int startX = (int)Math.floor(this.x1 - lineRange);
         int endX = (int)Math.ceil(this.x1 + lineRange);
         int startY = (int)Math.floor(this.y1 - lineRange);
         int endY = (int)Math.ceil(this.y1 + lineRange);

         for (int pX = startX; pX <= endX; pX++) {
            for (int pY = startY; pY <= endY; pY++) {
               if (!points.contains(pX, pY) && distanceFunction.getDistance(this.x1, this.y1, pX, pY) <= lineRange) {
                  points.add(pX, pY);
               }
            }
         }
      } else if (Math.abs(this.y1 - this.y2) <= Math.abs(this.x1 - this.x2)) {
         Point start;
         Point end;
         if (this.x1 < this.x2) {
            start = new Point(this.x1, this.y1);
            end = new Point(this.x2, this.y2);
         } else {
            start = new Point(this.x2, this.y2);
            end = new Point(this.x1, this.y1);
         }

         int moveY = end.y - start.y;
         int moveX = end.x - start.x;

         for (int currentX = start.x; currentX <= end.x; currentX++) {
            float currentY = start.y + moveY * ((float)(currentX - start.x) / moveX);
            int startX = (int)Math.floor(currentX - lineRange);
            int endX = (int)Math.ceil(currentX + lineRange);
            int startY = (int)Math.floor(currentY - lineRange);
            int endY = (int)Math.ceil(currentY + lineRange);

            for (int pX = startX; pX <= endX; pX++) {
               for (int pY = startY; pY <= endY; pY++) {
                  if (!points.contains(pX, pY) && distanceFunction.getDistance(pX, pY, currentX, currentY) <= lineRange) {
                     points.add(pX, pY);
                  }
               }
            }
         }
      } else {
         Point start;
         Point end;
         if (this.y1 < this.y2) {
            start = new Point(this.x1, this.y1);
            end = new Point(this.x2, this.y2);
         } else {
            start = new Point(this.x2, this.y2);
            end = new Point(this.x1, this.y1);
         }

         int moveY = end.y - start.y;
         int moveX = end.x - start.x;

         for (int currentY = start.y; currentY <= end.y; currentY++) {
            float currentX = start.x + moveX * ((float)(currentY - start.y) / moveY);
            int startX = (int)Math.floor(currentX - lineRange);
            int endX = (int)Math.ceil(currentX + lineRange);
            int startY = (int)Math.floor(currentY - lineRange);
            int endY = (int)Math.ceil(currentY + lineRange);

            for (int pX = startX; pX <= endX; pX++) {
               for (int pY = startY; pY <= endY; pY++) {
                  if (!points.contains(pX, pY) && distanceFunction.getDistance(pX, pY, currentX, currentY) <= lineRange) {
                     points.add(pX, pY);
                  }
               }
            }
         }
      }
   }

   public interface DistanceFunction {
      double getDistance(double var1, double var3, double var5, double var7);
   }

   public static class PointDistance {
      public final double dist;
      public final double lineWidth;
      protected boolean lineWidthPriority;

      protected PointDistance(double dist, double lineWidth, boolean lineWidthPriority) {
         this.dist = dist;
         this.lineWidth = lineWidth;
         this.lineWidthPriority = lineWidthPriority;
      }
   }
}
