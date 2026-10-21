package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.sound.SoundSettings;
import necesse.entity.ParticleBeamHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;

public class TheVoidClawBeamLevelEvent extends GenericBeamLevelEvent {
   public TheVoidClawBeamLevelEvent() {
   }

   public TheVoidClawBeamLevelEvent(
      Mob owner,
      float startAngle,
      float endAngle,
      long startTime,
      int sweepTime,
      int seed,
      float distance,
      GameDamage damage,
      int knockback,
      int hitCooldown,
      int bounces
   ) {
      super(owner, startAngle, endAngle, startTime, sweepTime, seed, distance, damage, knockback, hitCooldown, bounces);
   }

   @Override
   public void init() {
      super.init();
   }

   @Override
   public int getHitboxWidth() {
      return this.getAlpha() < 0.5F ? 0 : 10;
   }

   @Override
   public Float getBeamStartPosition(float currentAngle, Float dir) {
      return new Float(this.owner.x, this.owner.y - 20.0F - this.owner.getFlyingHeight());
   }

   @Override
   public SoundSettings getSound() {
      return super.getSound().fallOffDistance(3000);
   }

   @Override
   public void modifyTrail(ParticleBeamHandler handler) {
      handler.sprite(new GameSprite(GameResources.chains, 11, 0, 32))
         .color(() -> new Color(1.0F, 1.0F, 1.0F, this.getAlpha()))
         .particleColor(() -> new Color(0.0F, 0.0F, 0.0F, this.getAlpha()))
         .particleThicknessMod(4.0F)
         .thickness(25, 20)
         .height(16.0F)
         .drawOnTop(1000);
   }

   public float getAlpha() {
      int fadeTime = Math.min(500, this.sweepTime / 5);
      int timeSinceStart = this.getTimeSinceStart();
      if (timeSinceStart < fadeTime) {
         float progress = (float)timeSinceStart / fadeTime;
         return Math.max(0.0F, progress);
      } else {
         int timeLeft = this.getTimeLeft();
         if (timeLeft < fadeTime) {
            float progress = (float)timeLeft / fadeTime;
            return Math.max(0.0F, progress);
         } else {
            return 1.0F;
         }
      }
   }
}
