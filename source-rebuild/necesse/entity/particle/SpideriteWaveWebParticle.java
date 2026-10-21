package necesse.entity.particle;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class SpideriteWaveWebParticle extends Particle {
   public int sprite = GameRandom.globalRandom.nextInt(4);
   private int particleBuffer = 0;

   public SpideriteWaveWebParticle(Level level, float x, float y, long lifeTime) {
      super(level, x, y, lifeTime);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.particleBuffer++;
      if (this.particleBuffer > 10) {
         this.drawParticles();
         this.particleBuffer -= 10;
      }
   }

   private void drawParticles() {
      this.getLevel()
         .entityManager
         .addParticle(
            this.x + GameRandom.globalRandom.floatGaussian() * 16.0F,
            this.y + GameRandom.globalRandom.floatGaussian() * 12.0F,
            Particle.GType.IMPORTANT_COSMETIC
         )
         .color(new Color(255, 246, 79))
         .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(3), 0, 22))
         .givesLight(75.0F, 0.5F);
   }

   public void despawnNow() {
      if (this.getRemainingLifeTime() > 500L) {
         this.lifeTime = 500L;
         this.spawnTime = this.getWorldEntity().getLocalTime();
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
      int drawX = camera.getDrawX(this.getX()) - 16;
      int drawY = camera.getDrawY(this.getY()) - 16;
      long remainingLifeTime = this.getRemainingLifeTime();
      float alpha = GameMath.limit((float)remainingLifeTime / 500.0F, 0.0F, 1.0F);
      DrawOptions options = GameResources.webParticles.initDraw().sprite(this.sprite, 0, 32).pos(drawX, drawY).color(new Color(166, 204, 52)).alpha(alpha);
      tileList.add(tm -> options.draw());
   }
}
