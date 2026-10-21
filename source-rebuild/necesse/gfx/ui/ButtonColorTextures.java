package necesse.gfx.ui;

import java.awt.Color;
import necesse.gfx.gameTexture.GameTexture;

public class ButtonColorTextures {
   public ButtonStateTextures base;
   public ButtonStateTextures green;
   public ButtonStateTextures yellow;
   public ButtonStateTextures red;

   public ButtonColorTextures(GameInterfaceStyle style, String path) {
      this.base = new ButtonStateTextures(style, path);
      this.green = new ButtonStateTextures(style, path + "_green");
      this.yellow = new ButtonStateTextures(style, path + "_yellow");
      this.red = new ButtonStateTextures(style, path + "_red");
   }

   public GameTexture getButtonTexture(ButtonColor color, ButtonState state) {
      return state.textureGetter.apply(color.colorGetter.apply(this));
   }

   public GameTexture getButtonDownTexture(ButtonColor color, ButtonState state) {
      return state.downTextureGetter.apply(color.colorGetter.apply(this));
   }

   public Color getButtonColor(GameInterfaceStyle style, ButtonState state) {
      return state.elementColorGetter.apply(style);
   }

   public Color getTextColor(GameInterfaceStyle style, ButtonState state) {
      return state.textColorGetter.apply(style);
   }
}
