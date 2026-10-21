package necesse.entity.levelEvent.fishingEvent;

public class SwingFishingPhase extends FishingPhase {
   public SwingFishingPhase(FishingEvent event) {
      super(event);
   }

   @Override
   public void tickMovement(float delta) {
      if (this.event.getFishingMob().isFishingSwingDone()) {
         this.event.setPhase(new HookFishingPhase(this.event));
      }
   }

   @Override
   public void clientTick() {
   }

   @Override
   public void serverTick() {
   }

   @Override
   public void end() {
   }

   @Override
   public void over() {
   }
}
