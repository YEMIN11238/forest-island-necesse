package necesse.engine.util.voronoi;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.HashMap;

public class DelaunayTriangulator {
   private static final float EPSILON = 1.0E-6F;
   private static final int INSIDE = 0;
   private static final int COMPLETE = 1;
   private static final int INCOMPLETE = 2;

   public static ArrayList<TriangleData> compute(ArrayList<Float> points, boolean sorted, ArrayList<TriangleLine> voronoiLines) {
      if (points.isEmpty()) {
         return new ArrayList<>();
      }

      if (!sorted) {
         points.sort((p1, p2) -> java.lang.Float.compare(p1.x, p2.x));
      }

      Float first = points.get(0);
      Float min = new Float(first.x, first.y);
      Float max = new Float(first.x, first.y);

      for (Float point : points) {
         if (point.x < min.x) {
            min.x = point.x;
         }

         if (point.x > max.x) {
            max.x = point.x;
         }

         if (point.y < min.y) {
            min.y = point.y;
         }

         if (point.y > max.y) {
            max.y = point.y;
         }
      }

      float dx = max.x - min.x;
      float dy = max.y - min.y;
      float madD = Math.max(dx, dy) * 20.0F;
      float midX = (max.x + min.x) / 2.0F;
      float midY = (max.y + min.y) / 2.0F;
      TriangleData superTriangle = new TriangleData(new Float(midX - madD, midY - madD), new Float(midX, midY + madD), new Float(midX + madD, midY - madD));
      ArrayList<TriangleLine> edges = new ArrayList<>(points.size() / 2);
      ArrayList<TriangleData> triangles = new ArrayList<>(points.size());
      triangles.add(null);

      for (Float point : points) {
         for (int triangleIndex = triangles.size() - 1; triangleIndex >= 0; triangleIndex--) {
            TriangleData triangle = triangles.get(triangleIndex);
            if (triangle == null || !triangle.complete) {
               Float p1;
               Float p2;
               Float p3;
               if (triangle == null) {
                  p1 = superTriangle.p1;
                  p2 = superTriangle.p2;
                  p3 = superTriangle.p3;
               } else {
                  p1 = triangle.p1;
                  p2 = triangle.p2;
                  p3 = triangle.p3;
               }

               switch (circumCircle(point, p1, p2, p3)) {
                  case 0:
                     edges.add(new TriangleLine(p1, p2));
                     edges.add(new TriangleLine(p2, p3));
                     edges.add(new TriangleLine(p3, p1));
                     triangles.remove(triangleIndex);
                     break;
                  case 1:
                     if (triangle != null) {
                        triangle.complete = true;
                     }
               }
            }
         }

         int i = 0;

         for (int n = edges.size(); i < n; i++) {
            TriangleLine edge = edges.get(i);
            if (edge != null) {
               boolean skip = false;

               for (int ii = i + 1; ii < n; ii++) {
                  TriangleLine edge2 = edges.get(ii);
                  if (edge.equals(edge2)) {
                     skip = true;
                     edges.set(ii, null);
                  }
               }

               if (!skip) {
                  triangles.add(new TriangleData(edge.p1, edge.p2, point));
               }
            }
         }

         edges.clear();
      }

      if (voronoiLines != null) {
         voronoiLines.ensureCapacity(triangles.size() / 2);
      }

      HashMap<TriangleLine, TriangleData> lineTriangles = new HashMap<>();

      for (int i = triangles.size() - 1; i >= 0; i--) {
         TriangleData t = triangles.get(i);
         if (t != null
            && !isSamePoint(t.p1, superTriangle.p1)
            && !isSamePoint(t.p1, superTriangle.p2)
            && !isSamePoint(t.p1, superTriangle.p3)
            && !isSamePoint(t.p2, superTriangle.p1)
            && !isSamePoint(t.p2, superTriangle.p2)
            && !isSamePoint(t.p2, superTriangle.p3)
            && !isSamePoint(t.p3, superTriangle.p1)
            && !isSamePoint(t.p3, superTriangle.p2)
            && !isSamePoint(t.p3, superTriangle.p3)) {
            if (voronoiLines != null) {
               lineTriangles.compute(new TriangleLine(t.p1, t.p2), (line, prev) -> {
                  if (prev == null) {
                     return t;
                  }

                  voronoiLines.add(new TriangleLine(prev.average, t.average));
                  return null;
               });
               lineTriangles.compute(new TriangleLine(t.p2, t.p3), (line, prev) -> {
                  if (prev == null) {
                     return t;
                  }

                  voronoiLines.add(new TriangleLine(prev.average, t.average));
                  return null;
               });
               lineTriangles.compute(new TriangleLine(t.p3, t.p1), (line, prev) -> {
                  if (prev == null) {
                     return t;
                  }

                  voronoiLines.add(new TriangleLine(prev.average, t.average));
                  return null;
               });
            }
         } else {
            if (t != null) {
               t.complete = false;
            }

            triangles.remove(i);
         }
      }

      return triangles;
   }

   private static boolean isSamePoint(Float p1, Float p2) {
      return p1.x == p2.x && p1.y == p2.y;
   }

   private static int circumCircle(Float p, Float p1, Float p2, Float p3) {
      float y1y2 = Math.abs(p1.y - p2.y);
      float y2y3 = Math.abs(p2.y - p3.y);
      float xc;
      float yc;
      if (y1y2 < 1.0E-6F) {
         if (y2y3 < 1.0E-6F) {
            return 2;
         }

         float m2 = -(p3.x - p2.x) / (p3.y - p2.y);
         float mx2 = (p2.x + p3.x) / 2.0F;
         float my2 = (p2.y + p3.y) / 2.0F;
         xc = (p2.x + p1.x) / 2.0F;
         yc = m2 * (xc - mx2) + my2;
      } else {
         float m1 = -(p2.x - p1.x) / (p2.y - p1.y);
         float mx1 = (p1.x + p2.x) / 2.0F;
         float my1 = (p1.y + p2.y) / 2.0F;
         if (y2y3 < 1.0E-6F) {
            xc = (p3.x + p2.x) / 2.0F;
            yc = m1 * (xc - mx1) + my1;
         } else {
            float m2 = -(p3.x - p2.x) / (p3.y - p2.y);
            float mx2 = (p2.x + p3.x) / 2.0F;
            float my2 = (p2.y + p3.y) / 2.0F;
            xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            yc = m1 * (xc - mx1) + my1;
         }
      }

      float dx = p2.x - xc;
      float dy = p2.y - yc;
      float rsqr = dx * dx + dy * dy;
      dx = p.x - xc;
      dx *= dx;
      dy = p.y - yc;
      if (dx + dy * dy - rsqr <= 1.0E-6F) {
         return 0;
      } else {
         return p.x > xc && dx > rsqr ? 1 : 2;
      }
   }
}
