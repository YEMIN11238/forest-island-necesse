package necesse.gfx.gameIcon;

import java.awt.Color;
import java.awt.Dimension;
import necesse.engine.registries.MobRegistry;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;

public class MobIconGameEmote extends GameEmote {
   protected String mobStringID;

   public MobIconGameEmote(String mobStringID) {
      this.mobStringID = mobStringID;
   }

   @Override
   public Dimension getDrawBoundingBox(int size) {
      return new Dimension(size, size);
   }

   @Override
   public void drawIcon(int drawX, int drawY, boolean isHovering, Color defaultColor, int size) {
      TextureDrawOptionsEnd drawOptions = MobRegistry.getMobIcon(this.mobStringID).initDraw();
      if (size > 0) {
         drawOptions.size(size, true);
      }

      drawOptions.draw(drawX, drawY);
   }
}
