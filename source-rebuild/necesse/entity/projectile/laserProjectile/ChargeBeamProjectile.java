package necesse.entity.projectile.laserProjectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class ChargeBeamProjectile extends Projectile {
   public ChargeBeamProjectile() {
   }

   public ChargeBeamProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      if (!this.isServer()) {
         SoundManager.playSound(
            GameResources.electricExplosion,
            SoundEffect.effect(this.x, this.y).volume(0.7F).pitch(GameRandom.globalRandom.getFloatBetween(1.0F, 1.2F)).falloffDistance(1400)
         );
      }

      this.setWidth(16.0F, false);
      this.givesLight = true;
      this.height = 14.0F;
      this.bouncing = 10;
      this.piercing = 1000;
      this.maxMovePerTick = 24;
   }

   @Override
   protected void spawnSpinningParticle() {
      Float perp = GameMath.getPerpendicularDir(this.dx, this.dy);
      float var10002 = this.x + this.dx * this.particleDirOffset;
      this.modifySpinningParticle(
         this.getLevel()
            .entityManager
            .addParticle(var10002, this.y + this.dy * this.particleDirOffset, this.spinningTypeSwitcher.next())
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 22))
            .movesFriction(perp.x * 20.0F, perp.y * 20.0F, 2.0F)
            .sizeFades(28, 32)
            .color(this.getParticleColor())
            .height(this.height)
      );
      var10002 = this.x + this.dx * this.particleDirOffset;
      this.modifySpinningParticle(
         this.getLevel()
            .entityManager
            .addParticle(var10002, this.y + this.dy * this.particleDirOffset, this.spinningTypeSwitcher.next())
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 22))
            .movesFriction(-perp.x * 20.0F, -perp.y * 20.0F, 2.0F)
            .sizeFades(28, 32)
            .color(this.getParticleColor())
            .height(this.height)
      );
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 3;
   }

   @Override
   public Color getParticleColor() {
      Color color1 = new Color(199, 40, 34);
      Color color2 = new Color(222, 42, 75);
      Color color3 = new Color(220, 37, 92);
      return GameRandom.globalRandom.getOneOf(color1, color2, color3);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(222, 42, 75), 5.0F, 500, this.height);
   }

   @Override
   public void onMaxMoveTick() {
      if (this.isClient()) {
         this.spawnSpinningParticle();
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
   }

   @Override
   protected Color getWallHitColor() {
      return null;
   }
}
