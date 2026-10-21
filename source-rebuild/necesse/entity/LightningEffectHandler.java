package necesse.entity;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import java.util.function.Supplier;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;

public class LightningEffectHandler {
   public float distanceBetweenZaps = 20.0F;
   public Color color = new Color(58, 220, 220);
   private final Level level;
   private float currentX;
   private float currentY;
   private float distanceMovedBuffer;
   private final LinkedList<LightningEffectHandler.TargetPoint> nextPoints = new LinkedList<>();
   private final LightningEffectHandler.TrailHandler[] trails;
   private boolean initialized;
   private Point startFirePosition;

   public LightningEffectHandler(Level level, int trailCount) {
      this.level = level;
      this.trails = new LightningEffectHandler.TrailHandler[trailCount];
   }

   public LightningEffectHandler(Level level, int trailCount, Point startFirePosition) {
      this.level = level;
      this.trails = new LightningEffectHandler.TrailHandler[trailCount];
      this.startFirePosition = startFirePosition;
   }

   public void addNextPoint(Supplier<Float> coordinate, int timeToPoint, boolean forceTrailHit, Runnable onPointReached) {
      this.nextPoints.add(new LightningEffectHandler.TargetPoint(coordinate, timeToPoint, forceTrailHit, onPointReached));
   }

   public void addNextPoint(float x, float y, int timeToPoint, boolean forceTrailHit, Runnable onPointReached) {
      this.addNextPoint(() -> new Float(x, y), timeToPoint, forceTrailHit, onPointReached);
   }

   public void setNextPoint(Supplier<Float> coordinate, int timeToPoint, boolean forceTrailHit, Runnable onPointReached) {
      this.nextPoints.clear();
      this.addNextPoint(coordinate, timeToPoint, forceTrailHit, onPointReached);
   }

   public void tickMovement(float delta) {
      if (!this.nextPoints.isEmpty()) {
         if (!this.initialized) {
            LightningEffectHandler.TargetPoint firstPoint = this.nextPoints.removeFirst();
            Float pos = firstPoint.coordinate.get();

            for (int i = 0; i < this.trails.length; i++) {
               this.trails[i] = new LightningEffectHandler.TrailHandler(pos.x, pos.y);
            }

            this.currentX = pos.x;
            this.currentY = pos.y;
            this.onPointHit(firstPoint);
            this.initialized = true;
            if (this.nextPoints.isEmpty()) {
               return;
            }
         }

         LightningEffectHandler.TargetPoint nextPoint;
         for (nextPoint = this.nextPoints.getFirst(); delta >= nextPoint.timeToPoint; nextPoint = this.nextPoints.getFirst()) {
            this.nextPoints.removeFirst();
            this.tickMovementProgress(nextPoint.timeToPoint, nextPoint);
            delta -= nextPoint.timeToPoint;
            if (this.nextPoints.isEmpty()) {
               nextPoint = null;
               break;
            }
         }

         if (nextPoint != null) {
            this.tickMovementProgress(delta, nextPoint);
         }
      }
   }

   private void tickMovementProgress(float delta, LightningEffectHandler.TargetPoint nextPoint) {
      float percentToMove;
      if (nextPoint.timeToPoint <= 0.0F) {
         percentToMove = 1.0F;
      } else {
         percentToMove = delta / nextPoint.timeToPoint;
      }

      Float pos = nextPoint.coordinate.get();
      Float dir = GameMath.normalize(pos.x - this.currentX, pos.y - this.currentY);
      float distanceToMove = GameMath.getExactDistance(this.currentX, this.currentY, pos.x, pos.y) * percentToMove;

      while (distanceToMove > 0.0F) {
         float currentDistanceToMove = Math.min(distanceToMove, this.distanceBetweenZaps - this.distanceMovedBuffer);
         this.distanceMovedBuffer += currentDistanceToMove;
         distanceToMove -= currentDistanceToMove;
         this.currentX = this.currentX + dir.x * currentDistanceToMove;
         this.currentY = this.currentY + dir.y * currentDistanceToMove;
         if (this.distanceMovedBuffer >= this.distanceBetweenZaps) {
            this.randomizeTrailPoint(this.currentX, this.currentY);
            this.distanceMovedBuffer = this.distanceMovedBuffer - this.distanceBetweenZaps;
         }
      }

      nextPoint.timeToPoint -= delta;
      if (percentToMove >= 1.0F) {
         this.onPointHit(nextPoint);
      }
   }

   private void onPointHit(LightningEffectHandler.TargetPoint point) {
      if (point.forceTrailHit) {
         this.randomizeTrailPoint(this.currentX, this.currentY);
      }

      if (point.onPointReached != null) {
         point.onPointReached.run();
      }
   }

   private void randomizeTrailPoint(float currentX, float currentY) {
      if (this.level.isClient()) {
         Float nextPoint;
         if (this.nextPoints.isEmpty()) {
            nextPoint = new Float(currentX, currentY);
         } else {
            nextPoint = this.nextPoints.getFirst().coordinate.get();
         }

         for (LightningEffectHandler.TrailHandler trail : this.trails) {
            float randomX = currentX + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F);
            float randomY = currentY + GameRandom.globalRandom.getFloatBetween(-15.0F, 15.0F);
            trail.trail.addPoint(new TrailVector(randomX, randomY, nextPoint.x - randomX, nextPoint.y - randomY, 10.0F, 0.0F));
         }
      }
   }

   public void dispose() {
      if (this.initialized) {
         if (this.level.isClient()) {
            for (LightningEffectHandler.TrailHandler trail : this.trails) {
               trail.trail.removeOnFadeOut = true;
            }
         }
      }
   }

   private class TargetPoint {
      public Supplier<Float> coordinate;
      public float timeToPoint;
      public boolean forceTrailHit;
      public Runnable onPointReached;

      public TargetPoint(Supplier<Float> coordinate, int timeToPoint, boolean forceTrailHit, Runnable onPointReached) {
         this.coordinate = coordinate;
         this.timeToPoint = timeToPoint;
         this.forceTrailHit = forceTrailHit;
         this.onPointReached = onPointReached;
      }
   }

   private class TrailHandler {
      public float lastX;
      public float lastY;
      public Trail trail;

      public TrailHandler(float startX, float startY) {
         this.lastX = startX;
         this.lastY = startY;
         if (LightningEffectHandler.this.level.isClient()) {
            Float nextPoint;
            if (LightningEffectHandler.this.nextPoints.isEmpty()) {
               nextPoint = new Float(LightningEffectHandler.this.currentX, LightningEffectHandler.this.currentY);
            } else {
               nextPoint = LightningEffectHandler.this.nextPoints.getFirst().coordinate.get();
            }

            this.trail = new Trail(
               new TrailVector(startX, startY, nextPoint.x - startX, nextPoint.y - startY, 10.0F, 0.0F),
               LightningEffectHandler.this.level,
               LightningEffectHandler.this.color,
               500
            );
            this.trail.removeOnFadeOut = false;
            this.trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
            this.trail.drawOnTop = true;
            this.trail.addLight(200.0F, 1.0F);
            LightningEffectHandler.this.level.entityManager.addTrail(this.trail);
         }
      }
   }
}
