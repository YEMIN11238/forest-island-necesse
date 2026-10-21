package necesse.entity.mobs;

import java.awt.Color;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.level.maps.light.GameLight;

public class QuestMarkerOptions {
   public static Color orangeColor = new Color(200, 200, 50);
   public static Color grayColor = new Color(50, 50, 50);
   public final String icons;
   public final Color color;

   public QuestMarkerOptions(String icons, Color color) {
      this.icons = icons;
      this.color = color;
   }

   public QuestMarkerOptions(char icon, Color color) {
      this(Character.toString(icon), color);
   }

   public QuestMarkerOptions combine(QuestMarkerOptions other) {
      StringBuilder builder = new StringBuilder().append(this.icons);

      for (int i = 0; i < other.icons.length(); i++) {
         char c = other.icons.charAt(i);
         if (builder.indexOf(String.valueOf(c)) == -1) {
            builder.append(c);
         }
      }

      float thisLum = this.color.getRed() * 0.2126F + this.color.getGreen() * 0.7152F + this.color.getBlue() * 0.0722F;
      float otherLum = other.color.getRed() * 0.2126F + other.color.getGreen() * 0.7152F + other.color.getBlue() * 0.0722F;
      Color newColor = thisLum > otherLum ? this.color : other.color;
      return new QuestMarkerOptions(builder.toString(), newColor);
   }

   public static QuestMarkerOptions combine(QuestMarkerOptions first, QuestMarkerOptions second) {
      if (first == null) {
         return second;
      } else {
         return second == null ? first : first.combine(second);
      }
   }

   public DrawOptions getDrawOptions(int x, int y, GameLight light, GameCamera camera, int xOffset, int yOffset) {
      return QuestGiver.getMarkerDrawOptions(this.icons, this.color, x, y, light, camera, xOffset, yOffset);
   }
}
