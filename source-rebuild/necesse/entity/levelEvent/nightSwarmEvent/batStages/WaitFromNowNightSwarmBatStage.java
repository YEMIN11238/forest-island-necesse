package necesse.entity.levelEvent.nightSwarmEvent.batStages;

import necesse.entity.mobs.hostile.bosses.NightSwarmBatMob;

public class WaitFromNowNightSwarmBatStage extends NightSwarmBatStage {
   private long endTime;

   public WaitFromNowNightSwarmBatStage(boolean idleAllowed, NightSwarmBatMob mob, int time) {
      super(idleAllowed);
      this.endTime = mob.getWorldEntity().getTime() + time;
   }

   @Override
   public void onStarted(NightSwarmBatMob mob) {
   }

   @Override
   public void serverTick(NightSwarmBatMob mob) {
   }

   @Override
   public boolean hasCompleted(NightSwarmBatMob mob) {
      return this.endTime <= mob.getWorldEntity().getTime();
   }

   @Override
   public void onCompleted(NightSwarmBatMob mob) {
   }
}
