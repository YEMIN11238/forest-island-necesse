package necesse.entity.mobs.hostile.bosses.ascendedWizard.animations;

import necesse.entity.mobs.Mob;

public class SimpleSingleAnimation extends SimpleLoopingAnimation {
   public SimpleSingleAnimation(int startSpriteX, int startSpriteY, int frameCount, int animDuration) {
      super(startSpriteX, startSpriteY, frameCount, animDuration);
   }

   @Override
   public boolean isAnimationFinished(Mob mob) {
      long elapsed = mob.getTime() - this.animationStartTime;
      return elapsed > this.animDuration;
   }

   @Override
   protected long getElapsedTime(Mob mob) {
      return Math.min(super.getElapsedTime(mob), this.animDuration);
   }
}
