package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class ChargeShowerProjectile extends Projectile {
   private boolean hitMob = false;

   public ChargeShowerProjectile() {
   }

   public ChargeShowerProjectile(Level level, float x, float y, float targetX, float targetY, int distance, GameDamage damage, int knockback, Mob owner) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(distance);
      this.knockback = knockback;
      this.speed = distance * 1.1F;
   }

   @Override
   public void init() {
      super.init();
      if (!this.isServer()) {
         SoundManager.playSound(
            GameResources.magicbolt3, SoundEffect.effect(this.x, this.y).volume(0.7F).pitch(GameRandom.globalRandom.getFloatBetween(1.0F, 1.2F))
         );
         SoundManager.playSound(
            GameResources.electricExplosion, SoundEffect.effect(this.x, this.y).volume(0.3F).pitch(GameRandom.globalRandom.getFloatBetween(1.0F, 1.2F))
         );
      }

      this.canBounce = false;
      this.height = 16.0F;
      this.piercing = 2;
      if (this.isClient()) {
         int amount = this.distance / 3;
         this.spawnSprayParticles(amount);
      }
   }

   @Override
   public float getWidth() {
      float progress = this.traveledDistance / this.distance;
      float maxWidth = Math.max(50.0F, this.distance * 0.7F);
      return GameMath.lerp(progress, 50.0F, maxWidth);
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      float progress = this.traveledDistance / this.distance;
      this.speed = GameMath.lerp(progress, this.distance * 1.1F, 5.0F);
   }

   public void spawnSprayParticles(int amount) {
      ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

      for (int i = 0; i < amount; i++) {
         float posX = this.x + GameRandom.globalRandom.floatGaussian();
         float posY = this.y + GameRandom.globalRandom.floatGaussian();
         float projectileHeight = this.getHeight();
         float startHeight = GameRandom.globalRandom.getFloatBetween(projectileHeight - 2.0F, projectileHeight + 4.0F);
         float startHeightSpeed = GameRandom.globalRandom.getFloatBetween(-20.0F, -60.0F);
         float endHeight = GameRandom.globalRandom.getFloatBetween(-10.0F, -5.0F);
         float gravity = GameRandom.globalRandom.getFloatBetween(-5.0F, -16.0F);
         float distanceLeft = this.distance - this.traveledDistance;
         float floatPower = GameRandom.globalRandom.getFloatBetween(0.1F, 1.0F);
         float power = floatPower * (distanceLeft + 50.0F);
         float friction = 1.0F;
         int lifeAdded = (int)(250.0F * floatPower);
         int timeToLive = GameRandom.globalRandom.getIntBetween(250 + lifeAdded, 750 + lifeAdded);
         int timeToFadeOut = GameRandom.globalRandom.getIntBetween(500, 1000);
         int totalTime = timeToLive + timeToFadeOut;
         ParticleOption.HeightMover heightMover = new ParticleOption.HeightMover(startHeight, startHeightSpeed, gravity, 1.0F, endHeight, 0.0F);
         float moveAngle = GameRandom.globalRandom.getFloatOffset(GameMath.getAngle(new Float(this.dx, this.dy)), 20.0F);
         Float moveDir = GameMath.getAngleDir(moveAngle);
         float moveX = moveDir.x * power;
         float moveY = moveDir.y * power;
         ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, friction);
         ParticleOption.CollisionMover mover = new ParticleOption.CollisionMover(this.getLevel(), frictionMover);
         Color color1 = new Color(199, 40, 34);
         Color color2 = new Color(222, 42, 75);
         Color color3 = new Color(220, 37, 92);
         GameTextureSection sprite = GameRandom.globalRandom
            .getOneOf(
               GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22),
               GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12)
            );
         this.getLevel()
            .entityManager
            .addParticle(posX, posY, particleTypeSwitcher.next())
            .fadesAlphaTime(0, timeToFadeOut)
            .color(GameRandom.globalRandom.getOneOf(color1, color2, color3))
            .sizeFadesInAndOut(16, 24, 100, 0)
            .sprite(sprite)
            .height(heightMover)
            .rotates()
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               if (heightMover.currentHeight > endHeight && (!this.removed() || !this.hitMob)) {
                  mover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
               }
            })
            .givesLight(110.0F, 0.7F)
            .lifeTime(totalTime);
      }
   }

   @Override
   public GameDamage getDamage() {
      float progress = this.traveledDistance / this.distance;
      float modifier = GameMath.lerp(progress, 1.0F, 0.9F);
      return super.getDamage().modFinalMultiplier(modifier);
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      this.hitMob = mob != null;
   }

   @Override
   public Color getParticleColor() {
      return null;
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   protected Color getWallHitColor() {
      return null;
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
   }
}
