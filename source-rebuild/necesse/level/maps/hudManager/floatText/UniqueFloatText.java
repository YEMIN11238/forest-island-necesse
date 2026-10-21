package necesse.level.maps.hudManager.floatText;

import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.HudManager;

public class UniqueFloatText extends FloatTextFade {
   public final String uniqueType;

   public UniqueFloatText(int x, int y, String text, FontOptions options, String uniqueType) {
      super(x, y, text, options);
      this.avoidOtherText = false;
      this.uniqueType = uniqueType;
   }

   public UniqueFloatText(int x, int y, String text, FontOptions options) {
      this(x, y, text, options, null);
   }

   @Override
   public void init(HudManager manager) {
      super.init(manager);
      if (this.uniqueType != null) {
         manager.removeElements(element -> {
            if (element.isRemoved()) {
               return false;
            } else if (element != this && element instanceof UniqueFloatText) {
               String otherType = ((UniqueFloatText)element).uniqueType;
               return otherType == null ? false : otherType.equals(this.uniqueType);
            } else {
               return false;
            }
         });
      }
   }
}
