package necesse.entity.mobs.hostile.bosses.ascendedWizard.animations;

import java.awt.Point;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;

public class SimpleLoopingAnimation extends AscendedWizardAnimation {
   protected int startSpriteX;
   protected int startSpriteY;
   protected int frameCount;
   protected int animDuration;
   protected int totalTime = -1;
   protected long animationStartTime = -1L;

   public SimpleLoopingAnimation(int startSpriteX, int startSpriteY, int frameCount, int animDuration) {
      this.startSpriteX = startSpriteX;
      this.startSpriteY = startSpriteY;
      this.frameCount = frameCount;
      this.animDuration = animDuration;
   }

   public SimpleLoopingAnimation setTotalTime(int totalTime) {
      this.totalTime = totalTime;
      return this;
   }

   @Override
   public void onAnimationStarted(Mob mob) {
      this.animationStartTime = mob.getTime();
   }

   @Override
   public boolean isAnimationFinished(Mob mob) {
      return this.totalTime >= 0 ? this.getElapsedTime(mob) >= this.totalTime : false;
   }

   protected long getElapsedTime(Mob mob) {
      return mob.getTime() - this.animationStartTime;
   }

   @Override
   public Point getSprite(Mob mob) {
      long elapsedTime = this.getElapsedTime(mob);
      int currentFrame = GameUtils.getAnim(elapsedTime, this.frameCount, this.animDuration);
      int spriteX = (this.startSpriteX + currentFrame) % 4;
      int spriteY = this.startSpriteY + (this.startSpriteX + currentFrame) / 4;
      return new Point(spriteX, spriteY);
   }
}
