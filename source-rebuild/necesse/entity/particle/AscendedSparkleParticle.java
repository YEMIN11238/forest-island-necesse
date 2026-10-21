package necesse.entity.particle;

import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;

public class AscendedSparkleParticle extends Particle {
   private final GameTexture animTexture = GameResources.ascendedSparklesParticles;
   private final int spriteRes = 22;

   public AscendedSparkleParticle(Level level, float x, float y, long lifeTime) {
      super(level, x, y, lifeTime);
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
      int drawX = camera.getDrawX(this.x) - this.spriteRes / 2;
      int drawY = camera.getDrawY(this.y) - this.spriteRes / 2 + 4;
      int sprites = this.animTexture.getWidth() / this.spriteRes;
      int timePerSprite = (int)this.lifeTime / 10;
      int sprite = (float)this.getRemainingLifeTime() / (float)this.lifeTime > 0.5F
         ? (int)(sprites * (this.getRemainingLifeTime() / this.lifeTime))
         : sprites - (int)(this.getRemainingLifeTime() / timePerSprite);
      if (sprite >= 0) {
         float alpha = Math.min(1.0F, this.getLifeCyclePercent() * 1.5F);
         TextureDrawOptions options = this.animTexture.initDraw().sprite(sprite, 0, this.spriteRes).alpha(alpha).pos(drawX, drawY);
         tileList.add(tm -> options.draw());
      }
   }
}
