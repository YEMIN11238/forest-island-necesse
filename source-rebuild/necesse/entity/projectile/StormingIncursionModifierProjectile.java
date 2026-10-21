package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class StormingIncursionModifierProjectile extends Projectile {
   private float xChangeCounter = 0.1F;
   private float initialX;

   public StormingIncursionModifierProjectile() {
   }

   public StormingIncursionModifierProjectile(Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(new GameDamage(0.0F));
      this.knockback = 0;
   }

   @Override
   public void init() {
      super.init();
      this.height = 40.0F;
      this.piercing = 999;
      this.isSolid = false;
      this.heightBasedOnDistance = true;
      this.trailOffset = -25.0F;
      this.removeIfOutOfBounds = false;
      this.canBreakObjects = false;
      this.setWidth(6.0F, false);
      this.initialX = this.x;
   }

   @Override
   public boolean canHit(Mob mob) {
      return false;
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      GameRandom random = new GameRandom();
      if (this.traveledDistance > this.distance * this.xChangeCounter) {
         float rndCounterChange = random.getFloatBetween(0.025F, 0.1F);
         this.xChangeCounter += rndCounterChange;
         float rndX = random.getIntBetween(-8, 8);
         this.x += rndX;
      } else if (this.traveledDistance > this.distance * 0.75F && this.x < this.initialX) {
         this.x = this.x + random.getIntBetween(1, 2);
      } else if (this.traveledDistance > this.distance * 0.75F && this.x > this.initialX) {
         this.x = this.x - random.getIntBetween(1, 2);
      }
   }

   @Override
   public Color getParticleColor() {
      return new Color(63, 210, 229);
   }

   @Override
   public Trail getTrail() {
      GameRandom random = new GameRandom();
      int rndColor = random.getIntBetween(130, 200);
      Trail trail = new Trail(this, this.getLevel(), new Color(rndColor, 210, 210), 14.0F, 500, this.getHeight());
      trail.drawOnTop = true;
      return trail;
   }

   @Override
   public float getTrailThickness() {
      float v = (this.distance - this.traveledDistance) / 16.0F;
      if (v < 5.0F) {
         return 5.0F;
      } else {
         return v > 20.0F ? 20.0F : v;
      }
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light.minLevelCopy(Math.min(light.getLevel() + 100.0F, 150.0F)))
            .rotate(this.getAngle(), this.texture.getWidth() / 2, 0)
            .pos(drawX, drawY);
         topList.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }
}
