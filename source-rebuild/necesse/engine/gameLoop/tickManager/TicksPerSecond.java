package necesse.engine.gameLoop.tickManager;

public class TicksPerSecond {
   public int msPerTick;
   private float counter;

   private TicksPerSecond(int msPerTick) {
      this.msPerTick = msPerTick;
   }

   public static TicksPerSecond msPerTick(int msPerTick) {
      return new TicksPerSecond(msPerTick);
   }

   public static TicksPerSecond ticksPerSecond(int ticksPerSecond) {
      return msPerTick(1000 / ticksPerSecond);
   }

   public void tick(float delta) {
      this.counter += delta;
   }

   public void gameTick() {
      this.tick(50.0F);
   }

   public boolean peekTick() {
      return this.counter > this.msPerTick;
   }

   public boolean shouldTick() {
      if (this.peekTick()) {
         this.counter = this.counter - this.msPerTick;
         return true;
      } else {
         return false;
      }
   }

   public float getTicksPerSecond() {
      return 1000.0F / this.msPerTick;
   }
}
