package necesse.entity.levelEvent.nightSwarmEvent.eventStages;

import necesse.entity.levelEvent.nightSwarmEvent.NightSwarmLevelEvent;

public class WaitNightSwarmEventStage extends NightSwarmEventStage {
   public int minTime;
   public int maxTime;
   public int timer;

   public WaitNightSwarmEventStage(int minTime, int maxTime) {
      this.minTime = minTime;
      this.maxTime = maxTime;
   }

   @Override
   public void onStarted(NightSwarmLevelEvent event) {
      int timeDelta = this.maxTime - this.minTime;
      this.timer = this.minTime + (int)((1.0F - event.lastHealthProgress) * timeDelta);
   }

   @Override
   public void serverTick(NightSwarmLevelEvent event) {
      this.timer -= 50;
   }

   @Override
   public boolean hasCompleted(NightSwarmLevelEvent event) {
      return this.timer <= 0;
   }

   @Override
   public void onCompleted(NightSwarmLevelEvent event) {
   }
}
