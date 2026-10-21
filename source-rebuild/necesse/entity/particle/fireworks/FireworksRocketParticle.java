package necesse.entity.particle.fireworks;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class FireworksRocketParticle extends Particle {
   public GameRandom random;
   protected float currentHeight;
   public float heightTrajectory = 0.5F;
   public float height;
   public FireworksExplosion explosion;

   public FireworksRocketParticle(Level level, float x, float y, long lifeTime, int height, FireworksExplosion explosion, GameRandom random) {
      super(level, x, y, lifeTime);
      this.explosion = explosion;
      this.height = height;
      this.random = random;
      this.dx = random.floatGaussian() * 10.0F;
      this.dy = random.floatGaussian() * 10.0F;
      this.friction = 0.3F;
   }

   public FireworksRocketParticle(
      Level level, float x, float y, long lifeTime, int height, FireworksRocketParticle.ParticleGetter<FireworksPath> pathGetter, GameRandom random
   ) {
      this(level, x, y, lifeTime, height, new FireworksExplosion(pathGetter), random);
   }

   @Override
   public void init() {
      super.init();
      SoundManager.playSound(GameResources.fireworkFuse, SoundEffect.effect(this.x, this.y).pitch(this.random.getOneOf(0.95F, 1.0F, 1.05F)).volume(0.7F));
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.currentHeight = (float)Math.pow(this.getLifeCyclePercent(), this.heightTrajectory) * this.height;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel().entityManager.addParticle(this.x, this.y, null).height(this.currentHeight).flameColor().givesLight(ParticleOption.defaultFlameHue, 0.7F);
   }

   @Override
   public void remove() {
      if (!this.removed()) {
         this.explosion.spawnExplosion(this.getLevel(), this.x, this.y, this.currentHeight, this.random);
      }

      super.remove();
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

   public interface ExplosionModifier {
      void play(FireworksExplosion var1, float var2, GameRandom var3);
   }

   public interface ParticleGetter<T> {
      T get(int var1, float var2, GameRandom var3);
   }

   public interface RandomSoundPlayer {
      void play(Float var1, float var2, GameRandom var3);
   }
}
