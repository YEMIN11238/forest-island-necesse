package necesse.entity.mobs.hostile.bosses.scrapyardBoss;

import java.awt.geom.Point2D.Float;
import java.util.function.Function;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.entity.mobs.mobMovement.MobMovementLimitedCircleRelative;
import necesse.entity.mobs.mobMovement.MobMovementRelative;

public enum MutantHydraHeadType {
   TOP_LEFT(m -> m.topLeftHead, 270, 180, 180, 270),
   TOP_RIGHT(m -> m.topRightHead, 270, 0, 270, 0),
   BOTTOM_LEFT(m -> m.bottomLeftHead, 90, 180, 90, 180),
   BOTTOM_RIGHT(m -> m.bottomRightHead, 90, 0, 0, 90);

   private static final int horizontalOffset = 4;
   public final Function<MutantHydraBossMob, MutantHydraBossMob.HeadData> getter;
   public final int verticalAngle;
   public final int horizontalAngle;
   public final int startAngle;
   public final int endAngle;

   MutantHydraHeadType(Function<MutantHydraBossMob, MutantHydraBossMob.HeadData> getter, int verticalAngle, int horizontalAngle, int startAngle, int endAngle) {
      this.getter = getter;
      this.verticalAngle = verticalAngle;
      this.horizontalAngle = horizontalAngle;
      this.startAngle = startAngle;
      this.endAngle = endAngle;
   }

   public MobMovement setupSweepStage(
      MutantHydraBossMob bossMob, MutantHydraBossMob.HeadData headData, MutantHydraHeadMob headMob, int radius, boolean vertical
   ) {
      int angleDelta = (int)GameMath.getAngleDifference(this.verticalAngle, this.horizontalAngle);
      int startAngle = vertical ? this.verticalAngle : this.horizontalAngle - (angleDelta < 0 ? 4 : -4);
      Float dir = GameMath.getAngleDir(startAngle);
      return new MobMovementRelative(bossMob, headData.startXOffset + dir.x * radius, headData.startYOffset + dir.y * radius);
   }

   public MobMovement startSweepStage(
      MutantHydraBossMob bossMob, MutantHydraBossMob.HeadData headData, MutantHydraHeadMob headMob, int radius, int extraTime, boolean vertical
   ) {
      int angleDelta = (int)GameMath.getAngleDifference(this.verticalAngle, this.horizontalAngle);
      int startAngle = vertical ? this.verticalAngle : this.horizontalAngle - (angleDelta < 0 ? 4 : -4);
      int angleOffset = vertical ? -angleDelta - (angleDelta < 0 ? 4 : -4) : angleDelta - (angleDelta < 0 ? -4 : 4);
      return new MobMovementLimitedCircleRelative(
         headMob, extraTime, bossMob, headData.startXOffset, headData.startYOffset, radius, 0.0F, startAngle, angleOffset
      );
   }

   public MobMovement setupRotateStage(
      MutantHydraBossMob bossMob, MutantHydraBossMob.HeadData headData, MutantHydraHeadMob headMob, int radius, int rotationOffset, boolean reversed
   ) {
      int startAngle = reversed ? this.startAngle - rotationOffset : this.endAngle + rotationOffset;
      Float dir = GameMath.getAngleDir(startAngle);
      return new MobMovementRelative(bossMob, headData.startXOffset + dir.x * radius, headData.startYOffset + dir.y * radius);
   }

   public MobMovement startRotateStage(
      MutantHydraBossMob bossMob,
      MutantHydraBossMob.HeadData headData,
      MutantHydraHeadMob headMob,
      int radius,
      int extraTime,
      int rotationOffset,
      boolean reversed
   ) {
      int angleDelta = (int)GameMath.getAngleDifference(this.endAngle, this.startAngle);
      int startAngle = reversed ? this.startAngle - rotationOffset : this.endAngle + rotationOffset;
      int angleOffset = reversed ? angleDelta + rotationOffset * 2 : -angleDelta - rotationOffset * 2;
      return new MobMovementLimitedCircleRelative(
         headMob, extraTime, bossMob, headData.startXOffset, headData.startYOffset, radius, 0.0F, startAngle, angleOffset
      );
   }
}
