package necesse.entity.projectile;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.Comparator;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.followingProjectile.PhantomMissileProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class PhantomBobbleProjectile extends Projectile {
   private float startSpeed;

   public PhantomBobbleProjectile() {
   }

   public PhantomBobbleProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
      this.givesLight = true;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.startSpeed);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startSpeed = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.canHitMobs = false;
      this.startSpeed = this.speed;
      this.setWidth(10.0F);
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      float perc = Math.abs(GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F) - 1.0F);
      this.speed = GameMath.lerp(perc, Math.max(this.startSpeed / 4.0F, 10.0F), this.startSpeed);
   }

   @Override
   protected void spawnDeathParticles() {
      Color particleColor = this.getParticleColor();
      if (particleColor != null) {
         float height = this.getHeight();

         for (int i = 0; i < 24; i++) {
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, Particle.GType.COSMETIC)
               .movesConstant(
                  GameRandom.globalRandom.getIntBetween(10, 40) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1),
                  GameRandom.globalRandom.getIntBetween(10, 40) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1)
               )
               .color(this.getParticleColor())
               .height(height);
         }
      }

      SoundManager.playSound(GameResources.slimeSplash2, SoundEffect.effect(this).volume(0.7F).pitch(GameRandom.globalRandom.getFloatBetween(0.95F, 1.0F)));
      SoundManager.playSound(GameResources.magicExplosion, SoundEffect.effect(this).volume(0.2F).pitch(GameRandom.globalRandom.getFloatBetween(1.1F, 1.2F)));
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         int projectiles;
         if (mob == null) {
            projectiles = 4;
         } else {
            projectiles = 2;
         }

         float startX = x - this.dx * 2.0F;
         float startY = y - this.dy * 2.0F;
         Mob target = GameUtils.streamTargetsRange(this.getOwner(), this.getX(), this.getY(), this.distance * 10)
            .filter(m -> m != null && !m.removed() && m.isHostile)
            .map(m -> new ComputedObjectValue<>(m, () -> m.getDistance(this.getX(), this.getY())))
            .filter(o -> o.get() <= this.distance * 10)
            .min(Comparator.comparing(ComputedValue::get))
            .map(o -> (Mob)o.object)
            .orElse(null);
         float angle = GameMath.getAngle(new Float(this.dx, this.dy));
         float randomAngleOffset = 90.0F;

         for (int i = 0; i < projectiles; i++) {
            Float dir = GameMath.getAngleDir(angle + GameRandom.globalRandom.getFloatBetween(-randomAngleOffset, randomAngleOffset));
            PhantomMissileProjectile projectile = new PhantomMissileProjectile(
               this.getLevel(),
               this.getOwner(),
               startX,
               startY,
               startX + dir.x * 100.0F,
               startY + dir.y * 100.0F,
               this.startSpeed * 2.5F,
               this.distance * 15,
               this.getDamage().modFinalMultiplier(0.5F),
               this.knockback
            );
            if (this.modifier != null) {
               this.modifier.initChildProjectile(projectile, 0.5F, projectiles);
            }

            if (target != null) {
               projectile.targetPos = new Point(
                  target.getX() + GameRandom.globalRandom.getIntBetween(-20, 20), target.getY() + GameRandom.globalRandom.getIntBetween(-20, 20)
               );
            }

            if (mob != null) {
               projectile.startHitCooldown(mob);
            }

            this.getLevel().entityManager.projectiles.add(projectile);
         }
      }
   }

   @Override
   public Color getParticleColor() {
      return new Color(108, 37, 92);
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 260.0F, this.lightSaturation);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), this.getParticleColor(), 12.0F, 200, this.getHeight());
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
         GameLight light = level.getLightLevel(this).minLevelCopy(100.0F);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.shadowTexture.getWidth() / 2, this.shadowTexture.getHeight() / 2)
            .pos(drawX, drawY);
         tileList.add(tm -> shadowOptions.draw());
      }
   }

   @Override
   public float getAngle() {
      return (float)(this.getWorldEntity().getTime() - this.spawnTime) / 2.0F;
   }

   @Override
   protected SoundSettings getMoveSound() {
      return new SoundSettings(GameResources.swing2).volume(0.1F);
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.magicbolt1).volume(0.4F);
   }
}
