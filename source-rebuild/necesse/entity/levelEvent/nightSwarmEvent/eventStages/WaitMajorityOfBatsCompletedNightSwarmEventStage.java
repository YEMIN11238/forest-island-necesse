package necesse.entity.levelEvent.nightSwarmEvent.eventStages;

import necesse.entity.levelEvent.nightSwarmEvent.NightSwarmLevelEvent;

public class WaitMajorityOfBatsCompletedNightSwarmEventStage extends NightSwarmEventStage {
   @Override
   public void onStarted(NightSwarmLevelEvent event) {
   }

   @Override
   public void serverTick(NightSwarmLevelEvent event) {
   }

   @Override
   public boolean hasCompleted(NightSwarmLevelEvent event) {
      return this.majorityOfBatsCompleted(event);
   }

   @Override
   public void onCompleted(NightSwarmLevelEvent event) {
   }
}
