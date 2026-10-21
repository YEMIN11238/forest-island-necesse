package necesse.level.maps.light;

public class GameParticleLight {
   public GameLight light;
   public long endTime;

   public GameParticleLight(GameLight light) {
      this.light = light;
   }

   public void updateLevel(long currentTime) {
      if (this.endTime == 0L) {
         this.light.setLevel(0.0F);
      } else {
         long deltaTime = this.endTime - currentTime;
         if (deltaTime < 0L) {
            this.light.setLevel(0.0F);
         } else {
            float progress = (float)deltaTime / 750.0F;
            this.light.setLevel((int)(255.0F * progress));
         }
      }
   }
}
