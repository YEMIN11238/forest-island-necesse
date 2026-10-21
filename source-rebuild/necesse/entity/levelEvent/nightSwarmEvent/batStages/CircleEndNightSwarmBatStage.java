package necesse.entity.levelEvent.nightSwarmEvent.batStages;

import java.util.Comparator;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.NightSwarmBatMob;
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;

public class CircleEndNightSwarmBatStage extends NightSwarmBatStage {
   public float midX;
   public float midY;
   public float angle;
   public float angleOffsetX;
   public float angleOffsetY;
   public int range;
   public long chargeTime;
   public long endTime;

   public CircleEndNightSwarmBatStage(float midX, float midY, float angle, int range, long chargeTime, long endTime) {
      super(false);
      this.midX = midX;
      this.midY = midY;
      this.angle = angle;
      this.range = range;
      this.chargeTime = chargeTime;
      this.endTime = endTime;
   }

   public CircleEndNightSwarmBatStage(float midX, float midY, float angleOffsetX, float angleOffsetY, int range, long chargeTime, long endTime) {
      super(false);
      this.midX = midX;
      this.midY = midY;
      this.angle = Float.NaN;
      this.angleOffsetX = angleOffsetX;
      this.angleOffsetY = angleOffsetY;
      this.range = range;
      this.chargeTime = chargeTime;
      this.endTime = endTime;
   }

   @Override
   public void onStarted(NightSwarmBatMob mob) {
      float speed = MobMovementCircle.convertToRotSpeed(this.range, mob.getSpeed() * GameRandom.globalRandom.getFloatBetween(0.5F, 1.0F));
      if (Float.isNaN(this.angle)) {
         mob.setMovement(new MobMovementCircleLevelPos(mob, this.midX, this.midY, this.range, speed, this.angleOffsetX, this.angleOffsetY, false));
      } else {
         mob.setMovement(new MobMovementCircleLevelPos(mob, this.midX, this.midY, this.range, speed, this.angle, false));
      }
   }

   @Override
   public void serverTick(NightSwarmBatMob mob) {
   }

   @Override
   public boolean hasCompleted(NightSwarmBatMob mob) {
      long currentTime = mob.getWorldEntity().getTime();
      return this.chargeTime <= currentTime || this.endTime <= currentTime;
   }

   @Override
   public void onCompleted(NightSwarmBatMob mob) {
      long currentTime = mob.getWorldEntity().getTime();
      if (this.endTime > currentTime) {
         int var10001 = (int)this.midX;
         int var10002 = (int)this.midY;
         int var10003 = this.range + 50;
         PlayerMob target = mob.getLevel()
            .entityManager
            .players
            .streamAreaTileRange(var10001, var10002, var10003 / 32)
            .filter(m -> m != null && !m.removed() && m.isVisible())
            .filter(m -> m.getDistance(this.midX, this.midY) <= this.range)
            .findBestDistance(0, Comparator.comparingDouble(mob::getDistance))
            .orElse(null);
         if (target != null) {
            mob.stages.add(0, new ChargeEndNightSwarmBatStage(target, this.midX, this.midY, this.range, this.endTime));
         } else {
            mob.stages.add(0, new CircleEndNightSwarmBatStage(this.midX, this.midY, this.angle, this.range, this.endTime, this.endTime));
         }
      }
   }
}
