package necesse.gfx.fairType;

import java.awt.Color;
import java.util.function.Supplier;
import necesse.engine.input.InputEvent;

public class GlyphContainer {
   public final FairGlyph glyph;
   public final int index;
   public final int line;
   public final float lineHeight;
   public final float x;
   public final float y;
   public final Supplier<Color> currentColor;

   public GlyphContainer(FairGlyph glyph, int index, int line, float lineHeight, float x, float y, Supplier<Color> currentColor) {
      this.glyph = glyph;
      this.index = index;
      this.line = line;
      this.lineHeight = lineHeight;
      this.x = x;
      this.y = y;
      this.currentColor = currentColor;
   }

   public void draw(int drawX, int drawY, Color defaultColor) {
      this.glyph.draw(drawX + this.x, drawY + this.y, this.currentColor == null ? defaultColor : this.currentColor.get());
   }

   public void drawShadow(int drawX, int drawY) {
      this.glyph.drawShadow(drawX + this.x, drawY + this.y);
   }

   public void handleInputEvent(int drawX, int drawY, InputEvent event) {
      this.glyph.handleInputEvent(drawX + this.x, drawY + this.y, event);
   }
}
