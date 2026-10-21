package necesse.gfx.ui;

import java.util.function.Function;

public enum ButtonColor {
   BASE(t -> t.base),
   GREEN(t -> t.green),
   YELLOW(t -> t.yellow),
   RED(t -> t.red);

   public final Function<ButtonColorTextures, ButtonStateTextures> colorGetter;

   ButtonColor(Function<ButtonColorTextures, ButtonStateTextures> colorGetter) {
      this.colorGetter = colorGetter;
   }
}
