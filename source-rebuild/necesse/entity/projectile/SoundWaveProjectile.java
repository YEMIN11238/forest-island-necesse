package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.ParticleOption;
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

public class SoundWaveProjectile extends FollowingProjectile {
   public static int HEIGHT_BOUNCE = 8;
   public static int DISTANCE_PER_BOUNCE = 175;
   protected GameRandom heightRandom = new GameRandom();
   private int currentSize;

   public SoundWaveProjectile() {
   }

   public SoundWaveProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.piercing = 2;
      this.givesLight = true;
      this.trailOffset = 5.0F;
      this.height = 4.0F;
      this.width = 20.0F;
      this.currentSize = 32;
   }

   @Override
   public Color getParticleColor() {
      return new Color(255, 255, 255);
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      int sprite = GameRandom.globalRandom.nextInt(4);
      particle.sprite(GameResources.musicNotesParticle.sprite(sprite % 2, sprite / 2, 10));
      particle.dontRotate();
      particle.sizeFades(14, 14);
      particle.lifeTime(1000);
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() - 1;
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(255, 255, 255, 128), this.getTrailThickness(), 500, this.getHeight() - this.texture.getHeight() / 2.0F);
   }

   @Override
   public float getTrailThickness() {
      return this.currentSize / 2.0F;
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 50.0F) {
         this.findTarget(m -> this.getOwner().isHostile || m.isHostile, 80.0F, 160.0F);
      }
   }

   @Override
   public float getHeight() {
      float traveledDistance = this.traveledDistance + this.heightRandom.seeded(this.getUniqueID()).nextFloat() * DISTANCE_PER_BOUNCE;
      float progress = traveledDistance % DISTANCE_PER_BOUNCE / DISTANCE_PER_BOUNCE;
      return this.height + (float)Math.sin(progress * Math.PI * 2.0) * HEIGHT_BOUNCE;
   }

   @Override
   public float tickMovement(float delta) {
      float maxDistance = this.distance;
      float baseSize = 32.0F;
      float minSize = baseSize * 0.75F;
      float maxSize = baseSize * 1.25F;
      this.currentSize = (int)(minSize + (maxSize - minSize) * (this.traveledDistance / maxDistance));
      this.width = this.currentSize;
      return super.tickMovement(delta);
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
         int drawX = camera.getDrawX(this.x) - this.currentSize / 2;
         int drawY = camera.getDrawY(this.y) - this.currentSize / 2;
         boolean shouldMirror = this.getAngle() > 0.0F && this.getAngle() < 180.0F;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .size(this.currentSize, this.currentSize)
            .light(light)
            .rotate(this.getAngle() + 90.0F)
            .mirror(shouldMirror, shouldMirror)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }
}
