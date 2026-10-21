package necesse.gfx.gameIcon;

import java.awt.Color;
import java.awt.Dimension;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameTexture;

public class TextureGameEmote extends GameEmote {
   protected String texturePath;
   protected GameTexture texture;
   protected boolean applyDefaultColor;

   public TextureGameEmote(String texturePath, boolean applyDefaultColor) {
      this.texturePath = texturePath;
      this.applyDefaultColor = applyDefaultColor;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile(this.texturePath);
   }

   @Override
   public Dimension getDrawBoundingBox(int size) {
      TextureDrawOptionsEnd drawOptions = this.texture.initDraw();
      if (size > 0) {
         drawOptions = drawOptions.size(size, true);
      }

      return new Dimension(drawOptions.getWidth(), drawOptions.getHeight());
   }

   @Override
   public void drawIcon(int drawX, int drawY, boolean isHovering, Color defaultColor, int size) {
      TextureDrawOptionsEnd drawOptions = this.texture.initDraw();
      if (this.applyDefaultColor && defaultColor != null) {
         drawOptions.color(defaultColor);
      }

      if (size > 0) {
         drawOptions.size(size, true);
      }

      drawOptions.draw(drawX, drawY);
   }
}
