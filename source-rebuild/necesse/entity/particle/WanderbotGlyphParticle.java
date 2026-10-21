package necesse.entity.particle;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
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

public class WanderbotGlyphParticle extends Particle {
   protected float alpha = 0.0F;
   protected int tickCounter = 0;
   protected ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

   public WanderbotGlyphParticle(Level level, float x, float y, long lifeTime) {
      super(level, x, y, lifeTime);
   }

   private float getChargeUpAlpha() {
      return (float)this.getLifeCycleTime() / (float)this.lifeTime;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickCounter++;
      if (this.tickCounter % 2 == 0) {
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-35.0F, 35.0F),
               this.y + 32.0F + GameRandom.globalRandom.getFloatBetween(-25.0F, 25.0F),
               this.typeSwitcher.next()
            )
            .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
            .color(new Color(255, 101, 247, 255))
            .height(46.0F)
            .dontRotate()
            .givesLight(300.0F, 0.5F)
            .movesConstant(0.0F, -2.0F)
            .fadesAlphaTimeToCustomAlpha(250, 250, 1.0F)
            .size((options, lifeTime, timeAlive, lifePercent) -> options.size(8))
            .lifeTime(750);
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
      GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
      int drawX = camera.getDrawX(this.getX());
      int drawY = camera.getDrawY(this.getY()) - 16;
      long remainingLifeTime = this.getRemainingLifeTime();
      if (remainingLifeTime < 175L) {
         this.alpha = Math.max(0.0F, (float)remainingLifeTime / 175.0F);
      } else if (this.getChargeUpAlpha() != 1.0F) {
         this.alpha = this.getChargeUpAlpha();
      } else {
         this.alpha = 1.0F;
      }

      DrawOptions options = GameResources.wanderbotGlyphParticle
         .initDraw()
         .light(new GameLight(GameMath.max(light.getLevel(), 100.0F)))
         .alpha(this.alpha)
         .posMiddle(drawX, drawY);
      tileList.add(tm -> options.draw());
   }
}
