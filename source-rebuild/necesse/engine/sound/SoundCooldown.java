package necesse.engine.sound;

public interface SoundCooldown {
   boolean isBlockedBy(SoundPlayer var1, SoundTime var2);

   int getTimeToLive();

   default SoundCooldown initializeForSound(SoundPlayer sound) {
      return this;
   }
}
