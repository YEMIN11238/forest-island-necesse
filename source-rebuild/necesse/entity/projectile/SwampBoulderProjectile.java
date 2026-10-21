package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.BoulderHitExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class SwampBoulderProjectile extends Projectile {
   public SwampBoulderProjectile() {
   }

   public SwampBoulderProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.speed = speed;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setDistance(distance);
      this.setOwner(owner);
   }

   public SwampBoulderProjectile(Level level, Mob owner, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback) {
      this(level, owner, owner.x, owner.y, targetX, targetY, speed, distance, damage, knockback);
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getWorldEntity().getTime();
      this.isSolid = false;
      this.canHitMobs = false;
      this.trailOffset = 0.0F;
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      float distPerc = this.traveledDistance / this.distance;
      float heightF = GameMath.sin(distPerc * 180.0F);
      this.height = heightF * 200.0F;
      return out;
   }

   @Override
   public Color getParticleColor() {
      return new Color(52, 67, 48);
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         Mob owner = this.getOwner();
         if (owner != null && !owner.removed()) {
            BoulderHitExplosionEvent event = new BoulderHitExplosionEvent(x, y, owner);
            this.getLevel().entityManager.events.add(event);
         }
      }
   }

   @Override
   protected void spawnDeathParticles() {
      Color particleColor = this.getParticleColor();
      if (particleColor != null) {
         for (int i = 0; i < 20; i++) {
            int angle = GameRandom.globalRandom.nextInt(360);
            Float dir = GameMath.getAngleDir(angle);
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, Particle.GType.CRITICAL)
               .movesConstant(GameRandom.globalRandom.getIntBetween(20, 50) * dir.x, GameRandom.globalRandom.getIntBetween(20, 50) * dir.y)
               .color(this.getParticleColor())
               .height(this.getHeight());
         }
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
         GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         float angle = (float)(this.getWorldEntity().getTime() - this.spawnTime) / 1.5F;
         if (this.dx < 0.0F) {
            angle = -angle;
         }

         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(angle, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         float shadowAlpha = Math.abs(GameMath.limit(this.height / 300.0F, 0.0F, 1.0F) - 1.0F);
         int shadowX = camera.getDrawX(this.x) - this.shadowTexture.getWidth() / 2;
         int shadowY = camera.getDrawY(this.y) - this.shadowTexture.getHeight() / 2;
         TextureDrawOptions shadowOptions = this.shadowTexture.initDraw().light(light).alpha(shadowAlpha).pos(shadowX, shadowY);
         tileList.add(tm -> shadowOptions.draw());
      }
   }
}
