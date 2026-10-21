package necesse.engine.util;

import java.awt.Point;

public class TeleportResult {
   public final boolean isValid;
   public final LevelIdentifier newDestination;
   public final Point targetPosition;

   public TeleportResult(boolean isValid, LevelIdentifier newDestination, Point targetPosition) {
      this.isValid = isValid;
      this.newDestination = newDestination;
      this.targetPosition = targetPosition;
   }

   public TeleportResult(boolean isValid, LevelIdentifier newDestination, int targetX, int targetY) {
      this(isValid, newDestination, new Point(targetX, targetY));
   }

   public TeleportResult(boolean isValid, Point targetPosition) {
      this(isValid, null, targetPosition);
   }

   public TeleportResult(boolean isValid, int targetX, int targetY) {
      this(isValid, new Point(targetX, targetY));
   }
}
