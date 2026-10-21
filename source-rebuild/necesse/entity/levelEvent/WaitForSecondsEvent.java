package necesse.entity.levelEvent;

public abstract class WaitForSecondsEvent extends LevelEvent {
   protected int tickCounter;
   protected float timeToWaitInSeconds = 2.0F;

   public WaitForSecondsEvent() {
   }

   public WaitForSecondsEvent(float timeToWaitInSeconds) {
      super(false);
      this.timeToWaitInSeconds = timeToWaitInSeconds;
   }

   @Override
   public void init() {
      super.init();
      this.tickCounter = 0;
   }

   @Override
   public void clientTick() {
      this.tickCounter++;
      if (this.tickCounter > 20.0F * this.timeToWaitInSeconds) {
         this.over();
      } else {
         super.clientTick();
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 20.0F * this.timeToWaitInSeconds) {
         this.over();
      } else {
         super.serverTick();
      }
   }

   @Override
   public void over() {
      boolean prevIsOver = this.isOver();
      super.over();
      if (!prevIsOver) {
         this.onWaitOver();
      }
   }

   public abstract void onWaitOver();
}
