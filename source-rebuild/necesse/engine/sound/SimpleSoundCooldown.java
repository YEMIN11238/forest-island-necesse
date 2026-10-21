package necesse.engine.sound;

public class SimpleSoundCooldown implements SoundCooldown {
   private final int withinMillis;

   public SimpleSoundCooldown() {
      this.withinMillis = 20;
   }

   public SimpleSoundCooldown(int withinMillis) {
      this.withinMillis = withinMillis;
   }

   @Override
   public boolean isBlockedBy(SoundPlayer me, SoundTime other) {
      return me.gameSound == other.player.gameSound && other.getAge() <= this.withinMillis;
   }

   @Override
   public int getTimeToLive() {
      return this.withinMillis;
   }
}
