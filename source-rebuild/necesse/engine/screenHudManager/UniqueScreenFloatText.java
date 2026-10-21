package necesse.engine.screenHudManager;

import java.util.ArrayList;
import necesse.gfx.gameFont.FontOptions;

public class UniqueScreenFloatText extends ScreenFloatTextFade {
   public final String uniqueType;

   public UniqueScreenFloatText(int x, int y, String text, FontOptions options, String uniqueType) {
      super(x, y, text, options);
      this.avoidOtherText = false;
      this.uniqueType = uniqueType;
   }

   public UniqueScreenFloatText(int x, int y, String text, FontOptions options) {
      this(x, y, text, options, null);
   }

   @Override
   public void addThis(ArrayList<ScreenHudElement> elements) {
      if (this.uniqueType != null) {
         for (int i = 0; i < elements.size(); i++) {
            ScreenHudElement oText = elements.get(i);
            if (!oText.isRemoved() && oText != this && oText instanceof UniqueScreenFloatText) {
               String otherType = ((UniqueScreenFloatText)oText).uniqueType;
               if (otherType != null && otherType.equals(this.uniqueType)) {
                  elements.remove(i);
                  i--;
               }
            }
         }
      }

      super.addThis(elements);
   }
}
