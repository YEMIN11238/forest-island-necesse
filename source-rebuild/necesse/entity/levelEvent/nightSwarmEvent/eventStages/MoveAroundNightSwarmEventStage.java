package necesse.entity.levelEvent.nightSwarmEvent.eventStages;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.nightSwarmEvent.NightSwarmLevelEvent;
import necesse.entity.levelEvent.nightSwarmEvent.batStages.MoveNightSwarmBatStage;
import necesse.entity.levelEvent.nightSwarmEvent.batStages.SetIdleNightSwarmBatStage;
import necesse.entity.mobs.hostile.bosses.NightSwarmBatMob;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;

public class MoveAroundNightSwarmEventStage extends NightSwarmEventStage {
   public int points;
   public int range;
   public int maxAngle;
   public boolean forceSpread;

   public MoveAroundNightSwarmEventStage(int points, int range, boolean forceSpread, int maxAngle) {
      this.points = points;
      this.range = range;
      this.forceSpread = forceSpread;
      this.maxAngle = GameMath.limit(maxAngle, 0, 360);
   }

   public MoveAroundNightSwarmEventStage(int points, int range, boolean forceSpread) {
      this(points, range, forceSpread, 360);
   }

   @Override
   public void onStarted(NightSwarmLevelEvent event) {
      ArrayList<Float> points = new ArrayList<>();
      int anglePerPoint = this.maxAngle / this.points;
      float angleOffset;
      if (this.maxAngle >= 360) {
         angleOffset = GameRandom.globalRandom.nextInt(360);
      } else {
         Float dir = GameMath.normalize(event.nextLevelX - event.currentTarget.x, event.nextLevelY - event.currentTarget.y);
         float currentAngle = GameMath.fixAngle(GameMath.getAngle(dir));
         angleOffset = currentAngle - this.maxAngle / 2.0F;
      }

      float averageX = 0.0F;
      float averageY = 0.0F;

      for (int i = 0; i < this.points; i++) {
         float angle;
         if (this.forceSpread) {
            angle = angleOffset + i * anglePerPoint + GameRandom.globalRandom.nextInt(anglePerPoint);
         } else {
            angle = angleOffset + GameRandom.globalRandom.nextInt(this.maxAngle);
         }

         angle = GameMath.fixAngle(angle);
         Float dir = GameMath.getAngleDir(angle);
         float nextX = event.currentTarget.x + dir.x * this.range;
         float nextY = event.currentTarget.y + dir.y * this.range;
         points.add(new Float(nextX, nextY));
         averageX += nextX;
         averageY += nextY;
      }

      for (NightSwarmBatMob bat : event.getBats(false)) {
         Float pos = GameRandom.globalRandom.getOneOf(points);
         bat.stages
            .add(
               new MoveNightSwarmBatStage(
                  new MobMovementLevelPos(pos.x + GameRandom.globalRandom.getIntBetween(-50, 50), pos.y + GameRandom.globalRandom.getIntBetween(-50, 50))
               )
            );
         bat.stages.add(new SetIdleNightSwarmBatStage(pos.x, pos.y));
      }

      event.nextLevelX = averageX / points.size();
      event.nextLevelY = averageY / points.size();
   }

   @Override
   public void serverTick(NightSwarmLevelEvent event) {
   }

   @Override
   public boolean hasCompleted(NightSwarmLevelEvent event) {
      return true;
   }

   @Override
   public void onCompleted(NightSwarmLevelEvent event) {
   }
}
