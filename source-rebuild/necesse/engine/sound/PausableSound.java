package necesse.engine.sound;

import necesse.engine.sound.gameSound.GameSound;

public class PausableSound {
   protected GameSound gameSound;
   protected SoundEffect soundEffect;
   protected SoundCooldown cooldown;
   protected SoundPlayer soundPlayer;
   protected double lastProgressTime = 0.0;

   public PausableSound(GameSound sound, SoundEffect soundEffect, SoundCooldown cooldown) {
      this.gameSound = sound;
      this.soundEffect = soundEffect;
      this.cooldown = cooldown;
      this.soundPlayer = this.startSound(0.0);
   }

   public PausableSound(GameSound sound, SoundEffect soundEffect) {
      this(sound, soundEffect, null);
   }

   public PausableSound gameTick() {
      if (this.gameSound != null && this.soundPlayer != null) {
         if (!this.soundPlayer.isDone()) {
            double nextPosition = this.soundPlayer.getPositionSeconds();
            if (this.lastProgressTime < nextPosition - 0.1F) {
               this.soundPlayer.fadeOutAndStop(0.2F);
               this.soundPlayer = this.startSound(this.lastProgressTime);
               if (this.soundPlayer == null) {
                  return null;
               }
            } else {
               this.lastProgressTime = nextPosition;
            }
         } else {
            if (!(this.lastProgressTime < this.gameSound.getLengthInSeconds() - 0.1F)) {
               this.gameSound = null;
               return null;
            }

            this.soundPlayer = this.startSound(this.lastProgressTime);
            if (this.soundPlayer == null) {
               return null;
            }
         }

         this.soundPlayer.fadeOutAndStop(0.2F);
         return this;
      } else {
         return null;
      }
   }

   protected SoundPlayer startSound(double seconds) {
      return SoundManager.playSoundAtPosition(this.gameSound, seconds, this.soundEffect, this.cooldown, sp -> sp.fadeIn(0.2F));
   }

   public boolean isOver() {
      return this.gameSound == null;
   }
}
