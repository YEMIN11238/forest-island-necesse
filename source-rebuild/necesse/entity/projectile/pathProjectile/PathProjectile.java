package necesse.entity.projectile.pathProjectile;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.util.GameMath;
import necesse.engine.util.IntersectionPoint;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.TrailVector;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;

public abstract class PathProjectile extends Projectile {
   protected boolean canMoveLessThanDist;
   protected boolean autoSetDirection;

   public PathProjectile() {
      this.isSolid = false;
      this.canMoveLessThanDist = false;
      this.autoSetDirection = true;
      this.dx = 1.0F;
      this.dy = 0.0F;
   }

   @Override
   protected double getDistanceMovedBeforeCollision(double dist) {
      Float startPoint = new Float(this.x, this.y);
      Float endPoint = this.getPosition(dist);
      this.x = endPoint.x;
      this.y = endPoint.y;
      if (this.autoSetDirection) {
         Float norm = GameMath.normalize(endPoint.x - startPoint.x, endPoint.y - startPoint.y);
         this.dx = norm.x;
         this.dy = norm.y;
         if (this.dx == 0.0F && this.dy == 0.0F) {
            this.dx = 1.0F;
            this.dy = 0.0F;
         }

         this.angle = (float)Math.toDegrees(Math.atan2(this.dy, this.dx));
         this.angle += 90.0F;
         this.fixAngle();
      }

      java.awt.geom.Line2D.Float hitLine = new java.awt.geom.Line2D.Float(startPoint, endPoint);
      if (!this.isSolid) {
         this.checkHitCollision(hitLine);
      } else {
         Float hitStartPoint = startPoint;
         java.awt.geom.Line2D.Float objectHitLine = hitLine;
         CollisionFilter collisionFilter = this.getLevelCollisionFilter();
         ArrayList<LevelObjectHit> hitCollisions = this.getLevel().getCollisions(hitLine, collisionFilter);
         if (hitCollisions.isEmpty() && this.useWidthForCollision) {
            float width = this.getWidth();

            for (int i = 8; i < width / 2.0F; i += 8) {
               hitStartPoint = GameMath.getPerpendicularPoint(startPoint, i, this.dx, this.dy);
               Float perpEndPoint = new Float(hitStartPoint.x + (float)(this.dx * dist), hitStartPoint.y + (float)(this.dy * dist));
               java.awt.geom.Line2D.Float perpLine = new java.awt.geom.Line2D.Float(hitStartPoint, perpEndPoint);
               hitCollisions = this.getLevel().getCollisions(perpLine, collisionFilter);
               if (!hitCollisions.isEmpty()) {
                  objectHitLine = perpLine;
                  break;
               }

               hitStartPoint = GameMath.getPerpendicularPoint(startPoint, -i, this.dx, this.dy);
               perpEndPoint = new Float(hitStartPoint.x + (float)(this.dx * dist), hitStartPoint.y + (float)(this.dy * dist));
               perpLine = new java.awt.geom.Line2D.Float(hitStartPoint, perpEndPoint);
               hitCollisions = this.getLevel().getCollisions(perpLine, collisionFilter);
               if (!hitCollisions.isEmpty()) {
                  objectHitLine = perpLine;
                  break;
               }
            }
         }

         IntersectionPoint<LevelObjectHit> p = this.getLevel().getCollisionPoint(hitCollisions, objectHitLine, false);
         if (p != null) {
            Float objectHitOffset = new Float(this.x - hitStartPoint.x, this.y - hitStartPoint.y);
            this.x = (float)p.getX() + objectHitOffset.x;
            this.y = (float)p.getY() + objectHitOffset.y;
            if (this.bounced < this.getTotalBouncing() && this.canBounce) {
               this.onBounce(p);
               this.updateAngle();
               this.sendPositionUpdate = true;
            } else if (p.dir == IntersectionPoint.Dir.UP) {
               this.y += 8.0F;
            } else if (p.dir == IntersectionPoint.Dir.RIGHT) {
               this.x -= 8.0F;
            } else if (p.dir == IntersectionPoint.Dir.DOWN) {
               this.y -= 8.0F;
            } else if (p.dir == IntersectionPoint.Dir.LEFT) {
               this.x += 8.0F;
            }

            this.onHit(null, p.target, (float)p.getX(), (float)p.getY(), false, null);
            this.bounced++;
            this.checkHitCollision(new java.awt.geom.Line2D.Float(this.x, this.y, (float)p.x, (float)p.y));
         } else {
            this.checkHitCollision(hitLine);
         }
      }

      if (this.canMoveLessThanDist) {
         double actualDist = startPoint.distance(endPoint);
         if (actualDist < dist) {
            double delta = dist - actualDist;
            this.traveledDistance = (float)(this.traveledDistance - delta);
            this.tickDistMoved = (float)(this.tickDistMoved - delta);
            this.lightDistMoved = (float)(this.lightDistMoved - delta);
         }
      }

      return dist;
   }

   public void onBounce(IntersectionPoint p) {
      if (this.trail != null) {
         this.trail.addBreakPoint(new TrailVector((float)p.getX(), (float)p.getY(), this.dx, this.dy, this.trail.thickness, this.getHeight()));
      }

      if (p.dir == IntersectionPoint.Dir.RIGHT || p.dir == IntersectionPoint.Dir.LEFT) {
         this.dx = -this.dx;
         this.x = this.x + Math.signum(this.dx) * 4.0F;
      } else if (p.dir == IntersectionPoint.Dir.UP || p.dir == IntersectionPoint.Dir.DOWN) {
         this.dy = -this.dy;
         this.y = this.y + Math.signum(this.dy) * 4.0F;
      }
   }

   public abstract Float getPosition(double var1);
}
