package necesse.entity.particle;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.tween.Easings;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class GlyphTrapParticle extends Particle {
   public int chargeTime = 100;
   public int fadeOutTime = 750;
   public GameTexture texture;
   public Color color;
   public float colorHue;
   public float alpha;

   public GlyphTrapParticle(Level level, float x, float y, GameTexture texture, Color color, float colorHue) {
      super(level, x, y, 1000L);
      this.texture = texture;
      this.color = color;
      this.colorHue = colorHue;
   }

   @Override
   public void init() {
      super.init();

      for (int i = 0; i < 20; i++) {
         float length;
         if (i <= 4) {
            length = GameRandom.globalRandom.floatGaussian() * 8.0F;
         } else {
            length = GameRandom.globalRandom.floatGaussian() * 8.0F + 32.0F;
         }

         length = GameMath.limit(length, -46.0F, 46.0F);
         float angle = GameRandom.globalRandom.getFloatBetween(0.0F, 359.0F);
         float rndX = this.x + GameMath.sin(angle) * length;
         float rndY = this.y + GameMath.cos(angle) * length;
         this.getLevel()
            .entityManager
            .addParticle(rndX, rndY, Particle.GType.IMPORTANT_COSMETIC)
            .color(this.color)
            .dontRotate()
            .sprite(GameResources.puffParticles.sprite(0, 0, 12))
            .movesConstant(0.0F, -2.0F)
            .fadesAlphaTimeToCustomAlpha(50, 500, 1.0F)
            .size((options, lifeTime, timeAlive, lifePercent) -> options.size(8))
            .lifeTime(750 + GameRandom.globalRandom.getIntBetween(-100, 100));
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, this.colorHue, 0.75F, (int)(150.0F * (1.0F - this.getLifeCyclePercent())));
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
      int drawY = camera.getDrawY(this.getY());
      this.alpha = 1.0F;
      if (this.getLifeCycleTime() <= this.chargeTime) {
         this.alpha = (float)this.getLifeCycleTime() / this.chargeTime;
      }

      if (this.getLifeCycleTime() >= this.lifeTime - this.fadeOutTime) {
         this.alpha = (float)(1.0 - Easings.ExpoOut.ease((double)(this.getLifeCycleTime() - this.lifeTime + this.fadeOutTime) / this.fadeOutTime));
      }

      DrawOptions options = this.texture.initDraw().light(light).pos(drawX, drawY).color(this.color).alpha(this.alpha).posMiddle(drawX, drawY);
      tileList.add(tm -> options.draw());
   }
}
