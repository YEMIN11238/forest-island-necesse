package necesse.entity.mobs.itemAttacker;

import java.awt.Point;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleRelative;

public class FollowPosition {
   public static FollowPosition WALK_CLOSE = new FollowPosition((m, i, t) -> null, (m, i, t) -> null);
   public static FollowPosition CIRCLE_FAR = new FollowPosition((m, i, t) -> null, (m, i, t) -> circlingPos(m, i, t, 60, 0.0F, 100, 5));
   public static FollowPosition FLYING_CIRCLE = new FollowPosition((m, i, t) -> null, (m, i, t) -> circlingPos(m, i, t, 50, 30.0F, 50, 10));
   public static FollowPosition FLYING_CIRCLE_FAST = new FollowPosition((m, i, t) -> null, (m, i, t) -> circlingPos(m, i, t, 40, 60.0F, 50, 10));
   public static FollowPosition SLIME_CIRCLE_MOVEMENT = new FollowPosition((m, i, t) -> null, (m, i, t) -> circlingPos(m, i, t, 40, 85.0F, 70, 10));
   public static FollowPosition WIDE_CIRCLE_MOVEMENT = new FollowPosition((m, i, t) -> null, (m, i, t) -> circlingPos(m, i, t, 40, 85.0F, 250, 10));
   public static FollowPosition SWARM_CLOSE = new FollowPosition((mob, index, totalMobs) -> null, (mob, index, totalMobs) -> {
      GameRandom rand = new GameRandom(mob.getUniqueID() * 31L + index);
      int finalRange = rand.getIntBetween(40, 80);
      float baseAngle = totalMobs <= 0 ? 0.0F : 360.0F / totalMobs * index;
      float angleOffset = baseAngle + rand.getFloatBetween(-10.0F, 10.0F);
      float rotSpeed = MobMovementCircle.convertToRotSpeed(finalRange, mob.getSpeed() * 1.1F);
      Point offset = MobMovementCircle.getOffsetPosition(mob, finalRange, rotSpeed, angleOffset, false);
      boolean reversed = rand.nextBoolean();
      return new FollowerPosition(offset.x, offset.y, target -> new MobMovementCircleRelative(mob, target, finalRange, rotSpeed, angleOffset, reversed));
   });
   public static FollowPosition FLYING = newFlying(0, 0, 30, -40);
   public static FollowPosition PYRAMID = newPyramid(30, 30);
   public static FollowPosition LARGE_PYRAMID = newPyramid(60, 60);
   public final FollowPositionGetter getDefaultPoint;
   public final FollowPositionGetter getRelativePos;

   public static FollowerPosition circlingPos(Mob target, int index, int totalMobs, int size, float speed, int eachCircleDistance, int firstAddedDistance) {
      int circle = 1;

      while (true) {
         int range = firstAddedDistance + circle * eachCircleDistance;
         int circumference = (int)(Math.PI * range * 2.0);
         int mobsInCircle = circumference / size;
         if (mobsInCircle > index) {
            circumference = range;
            boolean reversed = circle % 2 == 0;
            float anglePerMob = 360.0F / Math.min(totalMobs, mobsInCircle);
            float angleOffset = anglePerMob * index;
            float rotSpeed = MobMovementCircle.convertToRotSpeed(circumference, speed);
            Point offset = MobMovementCircle.getOffsetPosition(target, circumference, rotSpeed, angleOffset, false);
            return new FollowerPosition(offset.x, offset.y, t -> new MobMovementCircleRelative(target, t, range, rotSpeed, angleOffset, reversed));
         }

         circle++;
         index -= mobsInCircle;
         totalMobs -= mobsInCircle;
      }
   }

   public static FollowPosition newFlying(int xOffset, int yOffset, int rowSize, int colSize) {
      return new FollowPosition((m, i, t) -> {
         int row = i / 8 + 1;
         int col = i % 8 + 1;
         return new FollowerPosition(xOffset + rowSize * col, yOffset + colSize * row);
      }, (m, i, t) -> {
         int row = i / 8 + 1;
         int col = i % 8 + 1;
         int dir = m.getDir();
         if (dir == 1) {
            return new FollowerPosition(-xOffset - rowSize * col, yOffset + colSize * row);
         } else {
            return dir == 3 ? new FollowerPosition(xOffset + rowSize * col, yOffset + colSize * row) : null;
         }
      });
   }

   public static FollowPosition newPyramid(int rowSize, int colSize) {
      return new FollowPosition((m, i, t) -> null, (m, i, t) -> {
         int indexOffset = 4;
         int rowOffset = 2;
         int row = (int)((-1.0 + Math.sqrt(1 + 8 * (i + indexOffset))) / 2.0) - rowOffset;
         int lastRow = (int)((-1.0 + Math.sqrt(1 + 8 * (t + indexOffset))) / 2.0) - rowOffset;
         int realRow = row + rowOffset;
         int mobsBefore = (realRow * realRow + realRow) / 2 - indexOffset;
         int indexInRow = i - mobsBefore;
         int mobsInRow = 3 + row + row % 2;
         if (lastRow == row) {
            mobsInRow = t - mobsBefore + row % 2;
         }

         int rowPos = rowSize * row;
         int colPos;
         if (row == 0) {
            colPos = i == 0 ? colSize : -colSize;
         } else {
            colPos = colSize * mobsInRow / 2 - indexInRow * colSize - row % 2 * colSize / 2 - colSize / 2;
         }

         int dir = m.getDir();
         if (dir == 0) {
            return new FollowerPosition(colPos, rowPos);
         } else if (dir == 1) {
            return new FollowerPosition(-rowPos, colPos);
         } else {
            return dir == 2 ? new FollowerPosition(colPos, -rowPos) : new FollowerPosition(rowPos, colPos);
         }
      });
   }

   public FollowPosition(FollowPositionGetter getDefaultPoint, FollowPositionGetter getRelativePos) {
      this.getDefaultPoint = getDefaultPoint;
      this.getRelativePos = getRelativePos;
   }

   public FollowerPosition getRelativePos(Mob target, FollowerPosition currentPos, int index, int totalMobs) {
      FollowerPosition pos = this.getRelativePos.getPosition(target, index, totalMobs);
      if (currentPos == null && pos == null) {
         return this.getDefaultPoint.getPosition(target, index, totalMobs);
      } else {
         return pos == null ? currentPos : pos;
      }
   }
}
