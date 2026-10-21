package necesse.engine.util;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Comparator;

public class ExpandingPolygon extends Polygon {
   private GameLinkedList<Point> points = new GameLinkedList<>();

   public ExpandingPolygon() {
      this.updatePolygon();
   }

   public ExpandingPolygon(Shape... shapes) {
      for (Shape shape : shapes) {
         this.addShape(shape);
      }
   }

   public ExpandingPolygon(Point... points) {
      for (Point point : points) {
         this.addPoint(point);
      }
   }

   public void addShape(Shape shape) {
      float[] coords = new float[6];

      for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
         int SG = it.currentSegment(coords);
         if (SG == 4) {
            break;
         }

         if (SG == 0 || SG == 1) {
            this.addPoint(new Point((int)coords[0], (int)coords[1]));
         }
      }
   }

   public void addPoint(Point p) {
      if (this.points.size() < 2) {
         this.points.addLast(p);
      } else if (this.points.size() == 2) {
         Point first = this.points.getFirst();
         Point last = this.points.getLast();
         double d = (last.x - first.x) * (p.y - first.y) - (last.y - first.y) * (p.x - first.x);
         if (d < 0.0) {
            this.points.getFirstElement().insertAfter(p);
         } else {
            this.points.addLast(p);
         }
      } else {
         GameLinkedList<Point>.Element last = this.points.getLastElement();
         double lastDistance = ((Point)last.object).distance(p);
         GameLinkedList<ExpandingPolygon.TempLine> lines = new GameLinkedList<>();

         for (GameLinkedList<Point>.Element element : this.points.elements()) {
            Point current = (Point)element.object;
            double currentDistance = current.distance(p);
            double d = (current.x - ((Point)last.object).x) * (p.y - ((Point)last.object).y)
               - (current.y - ((Point)last.object).y) * (p.x - ((Point)last.object).x);
            lines.addLast(new ExpandingPolygon.TempLine(last, element, d, lastDistance + currentDistance));
            last = element;
            lastDistance = currentDistance;
         }

         GameLinkedList<ExpandingPolygon.TempLine>.Element best = lines.streamElements()
            .filter(l -> ((ExpandingPolygon.TempLine)l.object).d < 0.0)
            .min(Comparator.comparingDouble(l -> ((ExpandingPolygon.TempLine)l.object).distance))
            .orElse(null);
         if (best == null) {
            return;
         }

         ((ExpandingPolygon.TempLine)best.object).p1.insertAfter(p);
         GameLinkedList<ExpandingPolygon.TempLine>.Element prev = best.prevWrap();

         while (((ExpandingPolygon.TempLine)prev.object).d < 0.0 && !((ExpandingPolygon.TempLine)prev.object).p2.isRemoved()) {
            ((ExpandingPolygon.TempLine)prev.object).p2.remove();
            prev = prev.prevWrap();
            if (prev == best) {
               break;
            }
         }

         GameLinkedList<ExpandingPolygon.TempLine>.Element next = best.nextWrap();

         while (((ExpandingPolygon.TempLine)next.object).d < 0.0 && !((ExpandingPolygon.TempLine)next.object).p1.isRemoved()) {
            ((ExpandingPolygon.TempLine)next.object).p1.remove();
            next = next.nextWrap();
            if (next == best) {
               break;
            }
         }
      }

      this.updatePolygon();
   }

   private void updatePolygon() {
      this.xpoints = new int[this.points.size()];
      this.ypoints = new int[this.points.size()];
      this.npoints = this.points.size();
      int i = 0;

      for (Point point : this.points) {
         this.xpoints[i] = point.x;
         this.ypoints[i] = point.y;
         i++;
      }

      this.invalidate();
   }

   private static class TempLine {
      public GameLinkedList<Point>.Element p1;
      public GameLinkedList<Point>.Element p2;
      public double d;
      public double distance;

      public TempLine(GameLinkedList<Point>.Element p1, GameLinkedList<Point>.Element p2, double d, double distance) {
         this.p1 = p1;
         this.p2 = p2;
         this.d = d;
         this.distance = distance;
      }
   }
}
