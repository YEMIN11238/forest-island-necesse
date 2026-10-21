package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.followingProjectile.FollowingProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CrescentDiscFollowingProjectile extends FollowingProjectile {
   private final float fadeDistance = 0.1F;
   private float lastAngle;

   public CrescentDiscFollowingProjectile() {
   }

   public CrescentDiscFollowingProjectile(Level level, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, Mob owner) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(distance);
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(70.0F, 70.0F);
      this.turnSpeed = 5.0F;
      this.height = 0.0F;
      this.trailOffset = 0.0F;
      this.lastAngle = 0.0F;
      this.isSolid = false;
      this.givesLight = true;
      this.piercing = 2;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      GameRandom random = GameRandom.globalRandom;
      AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
      float distance = 90.0F;
      this.getLevel()
         .entityManager
         .addParticle(this.x + GameMath.sin(currentAngle.get()) * distance, this.y + GameMath.cos(currentAngle.get()) * distance, Particle.GType.CRITICAL)
         .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
         .color(new Color(255, 255, 255))
         .givesLight(247.0F, 0.3F)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            float angle = currentAngle.accumulateAndGet(-delta * 10.0F / 250.0F, Float::sum);
            pos.x = this.x + GameMath.sin(angle) * distance;
            pos.y = this.y + GameMath.cos(angle) * distance;
         })
         .lifeTime(1000)
         .sizeFades(16, 24);
   }

   @Override
   public void onMoveTick(java.awt.geom.Point2D.Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      this.lastAngle = this.lastAngle + 5.0F * this.getIntensity();
   }

   @Override
   protected void spawnSpinningParticle() {
   }

   @Override
   public Color getParticleColor() {
      return new Color(220, 212, 255);
   }

   @Override
   protected void replaceTrail() {
      super.replaceTrail();
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 50.0F) {
         this.findTarget(m -> m.isPlayer, 0.0F, 1000.0F);
      }
   }

   @Override
   public boolean canHit(Mob mob) {
      return this.getIntensity() == 1.0F && super.canHit(mob);
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
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.lastAngle + this.getIntensity(), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight())
            .alpha(this.getIntensity());
         topList.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }

   private float getIntensity() {
      if (this.traveledDistance < this.distance * 0.1F) {
         return this.traveledDistance / (this.distance * 0.1F);
      } else {
         return this.distance - this.traveledDistance < this.distance * 0.1F ? (this.distance - this.traveledDistance) / (this.distance * 0.1F) : 1.0F;
      }
   }
}
