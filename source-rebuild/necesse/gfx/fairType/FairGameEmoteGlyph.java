package necesse.gfx.fairType;

import java.awt.Color;
import necesse.engine.input.InputEvent;
import necesse.gfx.gameIcon.GameEmote;

public class FairGameEmoteGlyph extends FairButtonGlyph {
   private final GameEmote emote;
   private final int size;

   public FairGameEmoteGlyph(GameEmote emote, int size) {
      super(size, size);
      this.emote = emote;
      this.size = size;
   }

   @Override
   public void handleEvent(float drawX, float drawY, InputEvent event) {
      this.emote.handleEvent(event);
   }

   @Override
   public void draw(float x, float y, Color defaultColor) {
      boolean hovering = this.isHovering();
      this.emote.drawIcon((int)x, (int)y - this.size, hovering, defaultColor, this.size);
   }

   @Override
   public String getParseString() {
      return TypeParsers.getGameEmoteParseString(this.emote.getEmoteParseStringID());
   }
}
