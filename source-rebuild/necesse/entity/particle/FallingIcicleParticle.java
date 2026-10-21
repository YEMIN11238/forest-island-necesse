package necesse.entity.particle;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.tween.Easings;
import necesse.engine.util.tween.Point2DTween;
import necesse.engine.util.tween.ShakeTween;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.gameObject.FallenIcicleObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FallingIcicleParticle extends Particle {
   protected final int sprite;
   protected long delayTime;
   protected Point2DTween startOffsetTween;
   protected ShakeTween shakeTween;
   protected double shakeOffset;

   public FallingIcicleParticle(Level level, float x, float y, long timeAlreadyPassed, long lifeTime, int sprite) {
      super(level, x, y, lifeTime);
      this.spawnTime -= timeAlreadyPassed;
      this.spawnTime = this.spawnTime - this.delayTime;
      this.sprite = sprite;
      this.delayTime = timeAlreadyPassed < 0L ? 0L : -timeAlreadyPassed;
      this.shakeTween = new ShakeTween(1.0, 20.0, 3.0, 0.6F, () -> this.shakeOffset, value -> this.shakeOffset = value).setFadeOut(true);
      if (this.delayTime > 0L) {
         this.shakeTween.setDelay(this.delayTime);
      }

      Point2D startOffset = FallenIcicleObject.getStartOffset(getTileCoordinate(x), getTileCoordinate(y));
      this.startOffsetTween = new Point2DTween(lifeTime, startOffset, new Double(0.0, 0.0)).setEase(Easings.SineIn);
      if (this.delayTime > 0L) {
         this.startOffsetTween.setDelay(this.delayTime);
      }

      this.startOffsetTween.play(this.spawnTime, this.getLocalTime());
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
      Point2D offset = this.startOffsetTween.updateAndGet(this.getLocalTime());
      int drawX = (int)(camera.getTileDrawX(this.getTileX()) + offset.getX());
      int drawY = (int)(camera.getTileDrawY(this.getTileY()) + offset.getY());
      float lifePercent = Math.max(0.0F, this.getLifeCyclePercent());
      float startShadowSize = FallenIcicleObject.getStartShadowSize(this.getTileX(), this.getTileY());
      float shadowSize = Easings.QuadIn.ease(lifePercent) * (1.0F - startShadowSize) + startShadowSize;
      int startDistance = (int)(250L * this.lifeTime / 1500L);
      int currentDistance = startDistance - (int)(Easings.QuadIn.ease(lifePercent) * startDistance);
      float icicleAlpha = Easings.QuadIn.ease(lifePercent);
      final int endYOffGround = FallenIcicleObject.getGeneratedYOffset(this.getTileX(), this.getTileY());
      int groundCutoff = GameMath.min(currentDistance - endYOffGround, 0);
      final DrawOptions spriteOptions = FallenIcicleObject.texture
         .initDraw()
         .sprite(this.sprite, 0, 64, 64 + groundCutoff)
         .light(light)
         .alpha(icicleAlpha)
         .pos((int)(drawX - 16 + this.shakeOffset), drawY - 32 + endYOffGround - currentDistance - 6);
      if (lifePercent > 0.95F) {
         float shakeTime = (lifePercent - 0.95F) / 0.05F;
         if (!this.shakeTween.isRunning()) {
            this.shakeTween.play(0.0, shakeTime);
         } else {
            this.shakeTween.update(shakeTime);
         }
      }

      DrawOptions shadowOptions = FallenIcicleObject.shadowTexture
         .initDraw()
         .section(18, 47, 42, 59)
         .size((int)(29.0F * shadowSize), (int)(17.0F * shadowSize))
         .light(light)
         .alpha(0.75F + lifePercent / 4.0F)
         .posMiddle(drawX + 16, drawY + 23);
      if (this.getLifeCycleTime() >= 0L) {
         list.add(new LevelSortedDrawable(this) {
            @Override
            public int getSortY() {
               return FallingIcicleParticle.this.getY() + endYOffGround;
            }

            @Override
            public void draw(TickManager tickManager) {
               spriteOptions.draw();
            }
         });
      }

      tileList.add(tm -> shadowOptions.draw());
   }

   @Override
   public void remove() {
      if (this.getLevel().getObject(this.getTileX(), this.getTileY()) instanceof FallenIcicleObject) {
         super.remove();
      }
   }
}
