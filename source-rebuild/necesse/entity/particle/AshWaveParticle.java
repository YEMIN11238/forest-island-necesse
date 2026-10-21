package necesse.entity.particle;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AshWaveParticle extends Particle {
   public AshWaveParticle(Level level, float x, float y, long lifeTime) {
      super(level, x, y, lifeTime);
   }

   @Override
   public void init() {
      super.init();
      this.spawnParticles();
   }

   private void spawnParticles() {
      int particleCount = 5;
      GameRandom random = GameRandom.globalRandom;
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

      for (int j = 0; j < particleCount; j++) {
         this.getLevel()
            .entityManager
            .addParticle(this.x + random.getIntBetween(-16, 16), this.y + random.getIntBetween(-16, 16), typeSwitcher.next())
            .color(new Color(75, 75, 75))
            .sizeFades(10, 30)
            .movesFriction(0.0F, -150 + random.getIntBetween(0, 25), 2.0F)
            .lifeTime(250);
      }

      SoundManager.playSound(GameResources.stomp, SoundEffect.effect(this).volume(0.5F).pitch(GameRandom.globalRandom.getFloatBetween(1.9F, 2.1F)));
      SoundManager.playSound(GameResources.rollingPinHit, SoundEffect.effect(this).volume(0.3F).pitch(GameRandom.globalRandom.getFloatBetween(0.5F, 1.0F)));
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
      GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
      int drawX = camera.getDrawX(this.getX()) - 32;
      int drawY = camera.getDrawY(this.getY()) - 32;
      long remainingLifeTime = this.getRemainingLifeTime();
      int sprite = Math.min((int)((double)remainingLifeTime / this.lifeTime * 4.0), 3);
      float alpha = GameMath.limit((float)remainingLifeTime / 250.0F, 0.0F, 1.0F);
      DrawOptions options = GameResources.ascendedFractureParticle
         .initDraw()
         .sprite(sprite, 0, 64)
         .color(new Color(0, 0, 0))
         .light(light.minLevelCopy(150.0F))
         .alpha(alpha)
         .pos(drawX, drawY);
      tileList.add(tm -> options.draw());
   }

   public void despawnNow() {
      if (this.getRemainingLifeTime() > 250L) {
         this.lifeTime = 250L;
         this.spawnTime = this.getLocalTime();
      }
   }
}
