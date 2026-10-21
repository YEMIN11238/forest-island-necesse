package necesse.inventory.container.travel;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.util.GameMath;

public enum TravelDir {
   NorthWest("ui", "dirnorthwest"),
   North("ui", "dirnorth"),
   NorthEast("ui", "dirnortheast"),
   West("ui", "dirwest"),
   East("ui", "direast"),
   SouthWest("ui", "dirsouthwest"),
   South("ui", "dirsouth"),
   SouthEast("ui", "dirsoutheast"),
   All(new StaticMessage("DIR_ALL")),
   None(new StaticMessage("DIR_NONE"));

   public final GameMessage dirMessage;
   public final GameMessage travelMessage;

   TravelDir(GameMessage dirMessage) {
      this.dirMessage = dirMessage;
      this.travelMessage = new LocalMessage("ui", "traveldir", "dir", dirMessage);
   }

   TravelDir(String category, String key) {
      this(new LocalMessage(category, key));
   }

   public static TravelDir getDeltaDir(int startX, int startY, int targetX, int targetY) {
      return getDeltaDir(startX, startY, targetX, targetY, None);
   }

   public static TravelDir getDeltaDirAngled(int startX, int startY, int targetX, int targetY) {
      Float dir = GameMath.normalize(targetX - startX, targetY - startY);
      TravelDir[] dirs = new TravelDir[]{East, SouthEast, South, SouthWest, West, NorthWest, North, NorthEast};
      float angle = GameMath.getAngle(dir);
      float anglePerDir = 360.0F / dirs.length;
      float startAngleOffset = 22.5F;
      float fixedAngle = GameMath.fixAngle(angle + startAngleOffset);

      for (int i = 0; i < 8; i++) {
         float currentAngle = i * anglePerDir;
         if (fixedAngle >= currentAngle && fixedAngle < currentAngle + anglePerDir) {
            return dirs[i];
         }
      }

      return None;
   }

   public static TravelDir getDeltaDir(int startX, int startY, int targetX, int targetY, TravelDir defaultDir) {
      if (targetY < startY) {
         if (targetX < startX) {
            return NorthWest;
         } else {
            return targetX > startX ? NorthEast : North;
         }
      } else if (targetY > startY) {
         if (targetX < startX) {
            return SouthWest;
         } else {
            return targetX > startX ? SouthEast : South;
         }
      } else if (targetX < startX) {
         return West;
      } else {
         return targetX > startX ? East : defaultDir;
      }
   }
}
