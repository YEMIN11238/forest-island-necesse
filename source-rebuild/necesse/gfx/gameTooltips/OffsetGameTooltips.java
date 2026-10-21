package necesse.gfx.gameTooltips;

import java.awt.Color;
import java.util.function.Supplier;

public class OffsetGameTooltips implements GameTooltips {
   private final GameTooltips tooltips;
   private final int offset;
   private final int widthPadding;

   public OffsetGameTooltips(GameTooltips tooltips, int offset, int widthPadding) {
      this.tooltips = tooltips;
      this.offset = offset;
      this.widthPadding = widthPadding;
   }

   public OffsetGameTooltips(GameTooltips tooltips, int offset) {
      this(tooltips, offset, 0);
   }

   @Override
   public int getHeight() {
      return this.tooltips.getHeight();
   }

   @Override
   public int getWidth() {
      return this.tooltips.getWidth() + this.widthPadding;
   }

   @Override
   public int getDrawXOffset() {
      return this.offset + this.tooltips.getDrawXOffset();
   }

   @Override
   public void draw(int x, int y, Supplier<Color> defaultColor) {
      this.tooltips.draw(x, y, defaultColor);
   }

   @Override
   public int getDrawOrder() {
      return this.tooltips.getDrawOrder();
   }

   @Override
   public boolean matchesSearch(String search) {
      return this.tooltips.matchesSearch(search);
   }
}
