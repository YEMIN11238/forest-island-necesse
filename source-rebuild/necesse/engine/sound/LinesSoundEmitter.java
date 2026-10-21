package necesse.engine.sound;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameMath;

public interface LinesSoundEmitter extends PrimitiveSoundEmitter {
   Iterable<Line2D> getSoundLines();

   @Override
   default float getSoundDistance(float listenerX, float listenerY) {
      java.awt.geom.Point2D.Float listenerPoint = new java.awt.geom.Point2D.Float(listenerX, listenerY);
      float bestDist = -1.0F;

      for (Line2D soundLine : this.getSoundLines()) {
         Point2D p = GameMath.getClosestPointOnLine(soundLine, listenerPoint, false);
         ComputedValue<Float> pDist = new ComputedValue<>(() -> (float)p.distance(listenerPoint));
         if (bestDist == -1.0F || pDist.get() < bestDist) {
            bestDist = pDist.get();
         }
      }

      return bestDist;
   }

   @Override
   default java.awt.geom.Point2D.Float getSoundDirection(float listenerX, float listenerY) {
      java.awt.geom.Point2D.Float listenerPoint = new java.awt.geom.Point2D.Float(listenerX, listenerY);
      Point2D bestPoint = null;
      float bestDist = -1.0F;

      for (Line2D soundLine : this.getSoundLines()) {
         Point2D p = GameMath.getClosestPointOnLine(soundLine, listenerPoint, false);
         ComputedValue<Float> pDist = new ComputedValue<>(() -> (float)p.distance(listenerPoint));
         if (bestPoint == null || pDist.get() < bestDist) {
            bestPoint = p;
            bestDist = pDist.get();
         }
      }

      return bestPoint == null ? null : GameMath.normalize((float)bestPoint.getX() - listenerX, (float)bestPoint.getY() - listenerY);
   }
}
