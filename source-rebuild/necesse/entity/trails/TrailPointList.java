package necesse.entity.trails;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.util.GameMath;

public class TrailPointList {
   private final Trail trail;
   private final ArrayList<TrailPointList.TrailPoint> list = new ArrayList<>();

   public TrailPointList(Trail trail) {
      this.trail = trail;
   }

   public TrailPointList copy() {
      TrailPointList copy = new TrailPointList(this.trail);
      copy.list.addAll(this.list);
      return copy;
   }

   public TrailPointList.TrailPoint getLastPoint() {
      return this.list.get(this.list.size() - 1);
   }

   public TrailPointList.TrailPoint getNextPoint(TrailVector vector, long spawnTime) {
      TrailPointList.TrailPoint point = new TrailPointList.TrailPoint(vector, spawnTime);
      point.updatePrev(this.list.get(this.list.size() - 1));
      this.getLastPoint().updateNext(point);
      return point;
   }

   public TrailPointList getNextPointSection(TrailPointList.TrailPoint nextPoint) {
      TrailPointList out = new TrailPointList(this.trail);
      out.list.add(this.getLastPoint());
      out.list.add(nextPoint);
      return out;
   }

   public TrailPointList.TrailPoint add(TrailVector vector, long spawnTime) {
      return this.add(new TrailPointList.TrailPoint(vector, spawnTime));
   }

   private TrailPointList.TrailPoint add(TrailPointList.TrailPoint point) {
      if (!this.isEmpty()) {
         TrailPointList.TrailPoint lastPoint = this.getLastPoint();
         lastPoint.updateNext(point);
         point.updatePrev(lastPoint);
      } else {
         point.updatePrev(null);
      }

      this.list.add(point);
      return point;
   }

   public TrailPointList.TrailPoint get(int index) {
      return this.list.get(index);
   }

   public TrailPointList.TrailPoint removeFirst() {
      TrailPointList.TrailPoint remove = this.list.remove(0);
      if (!this.isEmpty()) {
         this.list.get(0).prevPoint = null;
      }

      return remove;
   }

   public int size() {
      return this.list.size();
   }

   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public void ensureCapacity(int minCapacity) {
      this.list.ensureCapacity(minCapacity);
   }

   public class TrailPoint {
      public final TrailVector vector;
      public final long spawnTime;
      private TrailPointList.TrailPoint prevPoint;
      private TrailPointList.TrailPoint nextPoint;
      private Float drawPos1;
      private Float drawPos2;

      private TrailPoint(TrailVector vector, long spawnTime) {
         this.vector = vector;
         this.spawnTime = spawnTime;
      }

      private void updatePrev(TrailPointList.TrailPoint prevPoint) {
         this.prevPoint = prevPoint;
         this.drawPos1 = this.getDrawPoint1(this.vector.thickness);
         this.drawPos2 = this.getDrawPoint2(this.vector.thickness);
         if (TrailPointList.this.trail.smoothCorners
            && prevPoint != null
            && new java.awt.geom.Line2D.Float(this.drawPos1, this.drawPos2)
               .intersectsLine(new java.awt.geom.Line2D.Float(prevPoint.drawPos1, prevPoint.drawPos2))) {
            float angleDiff = GameMath.getAngleDifference(prevPoint.vector.getAngle(), this.vector.getAngle());
            if (angleDiff == 0.0F) {
               this.drawPos1 = prevPoint.drawPos1;
               this.drawPos2 = prevPoint.drawPos2;
            } else if (angleDiff > 0.0F) {
               this.drawPos1 = prevPoint.drawPos1;
            } else {
               this.drawPos2 = prevPoint.drawPos2;
            }
         }
      }

      private void updateNext(TrailPointList.TrailPoint nextPoint) {
         this.nextPoint = nextPoint;
      }

      private Float getDrawPoint1(float thickness) {
         return GameMath.getPerpendicularPoint(this.vector.pos, thickness / 2.0F, this.vector.dx, this.vector.dy);
      }

      private Float getDrawPoint2(float thickness) {
         return GameMath.getPerpendicularPoint(this.vector.pos, -thickness / 2.0F, this.vector.dx, this.vector.dy);
      }

      public TrailPointList.TrailPoint getPrevPoint() {
         return this.prevPoint;
      }

      public TrailPointList.TrailPoint getNextPoint() {
         return this.nextPoint;
      }

      public Float getDrawPos1() {
         return this.drawPos1;
      }

      public Float getDrawPos2() {
         return this.drawPos2;
      }
   }
}
