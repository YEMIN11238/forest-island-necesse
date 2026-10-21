package necesse.entity.particle;

import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SapphireStaffGlyphCircleParticle extends Particle {
   public int sprite = GameRandom.globalRandom.nextInt(4);
   private float alpha = 0.0F;

   public SapphireStaffGlyphCircleParticle(Level level, float x, float y, long lifeTime) {
      super(level, x, y, lifeTime);
   }

   private float getChargeUpAlpha() {
      return (float)this.getLifeCycleTime() / (float)this.lifeTime;
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
      long remainingLifeTime = this.getRemainingLifeTime();
      if (remainingLifeTime < 175L) {
         this.alpha = Math.max(0.0F, (float)remainingLifeTime / 175.0F);
      } else if (this.getChargeUpAlpha() != 1.0F) {
         this.alpha = this.getChargeUpAlpha();
      } else {
         this.alpha = 1.0F;
      }

      DrawOptions options = GameResources.sapphireGlyphCircleParticle.initDraw().light(light).alpha(this.alpha).pos(drawX, drawY).posMiddle(drawX, drawY);
      tileList.add(tm -> options.draw());
   }
}
