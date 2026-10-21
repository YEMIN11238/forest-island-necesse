package necesse.gfx.gameIcon;

import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Function;
import necesse.engine.Settings;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.GameInterfaceStyle;

public class ColoredUITextureEmote extends GameEmote {
   public Function<GameInterfaceStyle, GameTexture> textureGetter;

   public ColoredUITextureEmote(Function<GameInterfaceStyle, GameTexture> textureGetter) {
      this.textureGetter = textureGetter;
   }

   @Override
   public Dimension getDrawBoundingBox(int size) {
      TextureDrawOptionsEnd drawOptions = this.textureGetter.apply(Settings.UI).initDraw();
      if (size > 0) {
         drawOptions = drawOptions.size(size, true);
      }

      return new Dimension(drawOptions.getWidth(), drawOptions.getHeight());
   }

   @Override
   public void drawIcon(int drawX, int drawY, boolean isHovering, Color defaultColor, int size) {
      TextureDrawOptionsEnd drawOptions = this.textureGetter.apply(Settings.UI).initDraw();
      if (size > 0) {
         drawOptions.size(size, true);
      }

      if (defaultColor != null) {
         drawOptions.color(defaultColor);
      }

      drawOptions.draw(drawX, drawY);
   }
}
