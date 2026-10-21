package necesse.entity.particle;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class PhoenixFeatherGroundFireParticle extends Particle {
   private int particleBuffer = 0;

   public PhoenixFeatherGroundFireParticle(Level level, float x, float y, long lifeTime) {
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
         .heightMoves(0.0F, 50.0F)
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
      int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 6, 400);
      final DrawOptions options = GameResources.phoenixGroundFire.initDraw().sprite(anim, 0, 32).pos(drawX, drawY).alpha(alpha);
      list.add(new EntityDrawable(this) {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }

         @Override
         public int getSortY() {
            return super.getSortY() + 8;
         }
      });
   }
}
