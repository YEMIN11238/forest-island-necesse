package necesse.entity.levelEvent.nightSwarmEvent.batStages;

import necesse.entity.mobs.hostile.bosses.NightSwarmBatMob;

public class WaitNightSwarmBatStage extends NightSwarmBatStage {
   public int time;
   private long endTime;

   public WaitNightSwarmBatStage(boolean idleAllowed, int time) {
      super(idleAllowed);
      this.time = time;
   }

   @Override
   public void onStarted(NightSwarmBatMob mob) {
      this.endTime = mob.getWorldEntity().getTime() + this.time;
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
