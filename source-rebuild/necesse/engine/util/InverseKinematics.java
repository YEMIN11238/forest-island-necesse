package necesse.engine.util;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;

public class InverseKinematics {
   public GameLinkedList<InverseKinematics.Limb> limbs = new GameLinkedList<>();

   public InverseKinematics(float startX, float startY, float startAngle, float startLength, float maxLeftAngle, float maxRightAngle) {
      this.limbs.add(new InverseKinematics.Limb(startX, startY, startAngle, startLength, maxLeftAngle, maxRightAngle));
   }

   public InverseKinematics(float startX, float startY, float startAngle, float startLength) {
      this(startX, startY, startAngle, startLength, 180.0F, 180.0F);
   }

   public static InverseKinematics startFromPoints(float startX, float startY, float endX, float endY, float maxLeftAngle, float maxRightAngle) {
      Float dir = new Float(endX - startX, endY - startY);
      float length = (float)dir.distance(0.0, 0.0);
      float angle = GameMath.getAngle(dir);
      return new InverseKinematics(startX, startY, angle, length, maxLeftAngle, maxRightAngle);
   }

   public static InverseKinematics startFromPoints(float startX, float startY, float endX, float endY) {
      return startFromPoints(startX, startY, endX, endY, 180.0F, 180.0F);
   }

   public void addJointAngle(float angle, float length, float maxLeftAngle, float maxRightAngle) {
      InverseKinematics.Limb last = this.limbs.getLast();
      this.limbs.add(new InverseKinematics.Limb(last.outboundX, last.outboundY, angle, length, maxLeftAngle, maxRightAngle));
   }

   public void addJointAngle(float angle, float length) {
      this.addJointAngle(angle, length, 180.0F, 180.0F);
   }

   public void addJointPoint(float nextX, float nextY, float maxLeftAngle, float maxRightAngle) {
      InverseKinematics.Limb last = this.limbs.getLast();
      Float dir = new Float(nextX - last.outboundX, nextY - last.outboundY);
      float length = (float)dir.distance(0.0, 0.0);
      float angle = GameMath.getAngle(dir);
      this.limbs.add(new InverseKinematics.Limb(last.outboundX, last.outboundY, angle, length, maxLeftAngle, maxRightAngle));
   }

   public void addJointPoint(float nextX, float nextY) {
      this.addJointPoint(nextX, nextY, 180.0F, 180.0F);
   }

   public int getTotalJoints() {
      return this.limbs.size();
   }

   public InverseKinematics.Limb removeLastLimb() {
      if (this.limbs.size() == 1) {
         throw new IllegalStateException("Cannot remove the last joint");
      } else {
         return this.limbs.removeLast();
      }
   }

   public int apply(float targetX, float targetY, float maxDistChange, float targetMinDistance, int maxIterations) {
      if (this.limbs.size() == 1) {
         this.apply(targetX, targetY);
         return 1;
      }

      InverseKinematics.Limb first = this.limbs.getFirst();
      double distToTarget = new Float(targetX, targetY).distance(first.inboundX, first.inboundY);
      float totalLength = 0.0F;
      float largestLength = 0.0F;

      for (InverseKinematics.Limb limb : this.limbs) {
         if (limb.length > largestLength) {
            largestLength = limb.length;
         }

         totalLength += limb.length;
      }

      if (largestLength >= totalLength - largestLength) {
         float invalidDist = largestLength - (totalLength - largestLength);
         if (distToTarget < invalidDist) {
            targetMinDistance = (float)(targetMinDistance + (invalidDist - distToTarget));
         }
      }

      if (distToTarget > totalLength) {
         targetMinDistance = (float)(targetMinDistance + (distToTarget - totalLength));
      }

      double lastTargetDist = maxDistChange;
      int iterations = 0;

      double targetDist;
      double change;
      do {
         iterations++;
         this.apply(targetX, targetY);
         InverseKinematics.Limb last = this.limbs.getLast();
         targetDist = new Float(last.outboundX, last.outboundY).distance(targetX, targetY);
         change = Math.abs(targetDist - lastTargetDist);
         lastTargetDist = targetDist;
      } while (!(change <= maxDistChange) && iterations < maxIterations && !(targetDist <= targetMinDistance));

      return iterations;
   }

   public void apply(float targetX, float targetY) {
      InverseKinematics.Limb first = this.limbs.getFirst();
      Float rootPos = new Float(first.inboundX, first.inboundY);
      Float currentGoal = new Float(targetX, targetY);
      this.finalToRoot(currentGoal);
      this.rootToFinal(rootPos);
   }

   protected Float finalToRoot(Float currentGoal) {
      GameLinkedList<InverseKinematics.Limb>.Element currentElement = this.limbs.getLastElement();
      GameLinkedList<InverseKinematics.Limb>.Element lastElement = null;

      while (currentElement != null) {
         InverseKinematics.Limb currentLimb = (InverseKinematics.Limb)currentElement.object;
         currentLimb.angle = GameMath.getAngle(new Float(currentGoal.x - currentLimb.inboundX, currentGoal.y - currentLimb.inboundY));
         if (lastElement != null && (currentLimb.maxLeftAngle < 180.0F || currentLimb.maxRightAngle < 180.0F)) {
            float lastAngle = ((InverseKinematics.Limb)lastElement.object).angle;
            float angleDiff = GameMath.getAngleDifference(lastAngle, currentLimb.angle);
            if (angleDiff < -currentLimb.maxLeftAngle) {
               currentLimb.angle = currentLimb.angle - (-angleDiff - currentLimb.maxLeftAngle);
            } else if (angleDiff > currentLimb.maxRightAngle) {
               currentLimb.angle = currentLimb.angle + (angleDiff - currentLimb.maxRightAngle);
            }
         }

         currentLimb.outboundX = currentGoal.x;
         currentLimb.outboundY = currentGoal.y;
         currentLimb.fixInboundPos();
         currentGoal = new Float(currentLimb.inboundX, currentLimb.inboundY);
         lastElement = currentElement;
         currentElement = currentElement.prev();
      }

      return currentGoal;
   }

   protected void rootToFinal(Float rootPos) {
      Float currentInbound = new Float(rootPos.x, rootPos.y);

      for (GameLinkedList<InverseKinematics.Limb>.Element currentElement = this.limbs.getFirstElement();
         currentElement != null;
         currentElement = currentElement.next()
      ) {
         InverseKinematics.Limb currentLimb = (InverseKinematics.Limb)currentElement.object;
         currentLimb.inboundX = currentInbound.x;
         currentLimb.inboundY = currentInbound.y;
         currentLimb.fixOutboundPos();
         currentInbound = new Float(currentLimb.outboundX, currentLimb.outboundY);
      }
   }

   public static void apply(GameLinkedList<InverseKinematics.Limb>.Element limb, boolean useInbound, float targetX, float targetY, boolean root) {
      GameLinkedList<InverseKinematics.Limb>.Element first = limb;
      GameLinkedList<InverseKinematics.Limb>.Element last = limb;

      while (first.hasPrev()) {
         first = first.prev();
      }

      while (last.hasNext()) {
         last = last.next();
      }

      if (limb == first && useInbound) {
         Float rootPos = new Float(((InverseKinematics.Limb)last.object).outboundX, ((InverseKinematics.Limb)last.object).outboundY);
         finalToRoot(limb, true, -1, new Float(targetX, targetY));
         if (root) {
            rootToFinal(last, null, true, -1, rootPos);
         }
      } else if (limb == last && !useInbound) {
         Float rootPos = new Float(((InverseKinematics.Limb)first.object).inboundX, ((InverseKinematics.Limb)first.object).inboundY);
         finalToRoot(limb, false, 1, new Float(targetX, targetY));
         if (root) {
            rootToFinal(first, null, false, 1, rootPos);
         }
      } else {
         Float lastRoot = new Float(((InverseKinematics.Limb)last.object).outboundX, ((InverseKinematics.Limb)last.object).outboundY);
         Float firstRoot = new Float(((InverseKinematics.Limb)first.object).inboundX, ((InverseKinematics.Limb)first.object).inboundY);
         if (useInbound) {
            finalToRoot(limb.prev(), false, 1, new Float(targetX, targetY));
            finalToRoot(limb, true, -1, new Float(targetX, targetY));
            if (root) {
               rootToFinal(first, null, false, 1, firstRoot);
            }
         } else {
            finalToRoot(limb, false, 1, new Float(targetX, targetY));
            finalToRoot(limb.next(), true, -1, new Float(targetX, targetY));
            if (root) {
               rootToFinal(last, null, true, -1, lastRoot);
            }
         }
      }
   }

   protected static void finalToRoot(GameLinkedList<InverseKinematics.Limb>.Element currentElement, boolean useInbound, int dir, Float currentGoal) {
      while (currentElement != null) {
         InverseKinematics.Limb currentLimb = (InverseKinematics.Limb)currentElement.object;
         if (useInbound) {
            currentLimb.angle = GameMath.getAngle(new Float(currentLimb.outboundX - currentGoal.x, currentLimb.outboundY - currentGoal.y));
            currentLimb.inboundX = currentGoal.x;
            currentLimb.inboundY = currentGoal.y;
            currentLimb.fixOutboundPos();
            currentGoal = new Float(currentLimb.outboundX, currentLimb.outboundY);
         } else {
            currentLimb.angle = GameMath.getAngle(new Float(currentGoal.x - currentLimb.inboundX, currentGoal.y - currentLimb.inboundY));
            currentLimb.outboundX = currentGoal.x;
            currentLimb.outboundY = currentGoal.y;
            currentLimb.fixInboundPos();
            currentGoal = new Float(currentLimb.inboundX, currentLimb.inboundY);
         }

         if (dir < 0) {
            currentElement = currentElement.next();
         } else {
            currentElement = currentElement.prev();
         }
      }
   }

   protected static void rootToFinal(
      GameLinkedList<InverseKinematics.Limb>.Element currentElement,
      GameLinkedList<InverseKinematics.Limb>.Element endElement,
      boolean useInbound,
      int dir,
      Float rootPos
   ) {
      Float currentPos = new Float(rootPos.x, rootPos.y);

      while (currentElement != null) {
         if (currentElement == endElement) {
            return;
         }

         InverseKinematics.Limb currentLimb = (InverseKinematics.Limb)currentElement.object;
         if (useInbound) {
            currentLimb.outboundX = currentPos.x;
            currentLimb.outboundY = currentPos.y;
            currentLimb.fixInboundPos();
            currentPos = new Float(currentLimb.inboundX, currentLimb.inboundY);
         } else {
            currentLimb.inboundX = currentPos.x;
            currentLimb.inboundY = currentPos.y;
            currentLimb.fixOutboundPos();
            currentPos = new Float(currentLimb.outboundX, currentLimb.outboundY);
         }

         if (dir < 0) {
            currentElement = currentElement.prev();
         } else {
            currentElement = currentElement.next();
         }
      }
   }

   public void drawDebug(GameCamera camera, Color color, Color limitsColor) {
      this.drawDebug(camera, 0, 0, color, limitsColor);
   }

   public void drawDebug(GameCamera camera, int offsetX, int offsetY, Color color, Color limitsColor) {
      for (InverseKinematics.Limb limb : this.limbs) {
         Renderer.drawLine(
            offsetX + camera.getDrawX(limb.inboundX),
            offsetY + camera.getDrawY(limb.inboundY),
            offsetX + camera.getDrawX(limb.outboundX),
            offsetY + camera.getDrawY(limb.outboundY),
            color
         );
         float limitsLength = Math.min(50.0F, limb.length / 2.0F);
         if (limb.maxLeftAngle < 180.0F) {
            Float dir = GameMath.getAngleDir(limb.angle - limb.maxLeftAngle);
            Renderer.drawLine(
               offsetX + camera.getDrawX(limb.outboundX),
               offsetY + camera.getDrawY(limb.outboundY),
               offsetX + camera.getDrawX(limb.outboundX + dir.x * limitsLength),
               offsetY + camera.getDrawY(limb.outboundY + dir.y * limitsLength),
               limitsColor
            );
         }

         if (limb.maxRightAngle < 180.0F) {
            Float dir = GameMath.getAngleDir(limb.angle + limb.maxRightAngle);
            Renderer.drawLine(
               offsetX + camera.getDrawX(limb.outboundX),
               offsetY + camera.getDrawY(limb.outboundY),
               offsetX + camera.getDrawX(limb.outboundX + dir.x * limitsLength),
               offsetY + camera.getDrawY(limb.outboundY + dir.y * limitsLength),
               limitsColor
            );
         }

         Renderer.drawCircle(offsetX + camera.getDrawX(limb.inboundX), offsetY + camera.getDrawY(limb.inboundY), 4, 12, color, false);
         Renderer.drawCircle(offsetX + camera.getDrawX(limb.outboundX), offsetY + camera.getDrawY(limb.outboundY), 4, 12, color, false);
      }
   }

   public static class Limb {
      public float inboundX;
      public float inboundY;
      public float outboundX;
      public float outboundY;
      public float angle;
      public float length;
      public float maxLeftAngle;
      public float maxRightAngle;

      public Limb(float x, float y, float angle, float length, float maxLeftAngle, float maxRightAngle) {
         this.inboundX = x;
         this.inboundY = y;
         this.angle = angle;
         this.length = length;
         this.maxLeftAngle = maxLeftAngle;
         this.maxRightAngle = maxRightAngle;
         this.fixOutboundPos();
      }

      public void fixInboundPos() {
         Float inboundDir = GameMath.getAngleDir(this.angle + 180.0F);
         this.inboundX = this.outboundX + inboundDir.x * this.length;
         this.inboundY = this.outboundY + inboundDir.y * this.length;
      }

      public void fixOutboundPos() {
         Float outboundDir = GameMath.getAngleDir(this.angle);
         this.outboundX = this.inboundX + outboundDir.x * this.length;
         this.outboundY = this.inboundY + outboundDir.y * this.length;
      }
   }
}
