package necesse.gfx.gameTooltips;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.function.Supplier;

public class HorizontalListGameTooltips extends LinkedList<GameTooltips> implements GameTooltips {
   public int xMargin;
   public int drawOrder = 0;

   public HorizontalListGameTooltips() {
      this.xMargin = 0;
   }

   @Override
   public int getHeight() {
      int height = 0;

      for (GameTooltips tip : this) {
         height = Math.max(height, tip.getHeight());
      }

      return height;
   }

   @Override
   public int getWidth() {
      int width = 0;

      for (GameTooltips tip : this) {
         width += tip.getWidth() + this.xMargin;
      }

      if (!this.isEmpty()) {
         width -= this.xMargin;
      }

      return width;
   }

   @Override
   public void draw(int x, int y, Supplier<Color> defaultColor) {
      this.sort(Comparator.comparing(GameTooltips::getDrawOrder, Comparator.reverseOrder()));

      for (GameTooltips tip : this) {
         tip.draw(x, y, defaultColor);
         x += tip.getWidth() + this.xMargin;
      }
   }

   @Override
   public int getDrawOrder() {
      return this.drawOrder;
   }

   @Override
   public boolean matchesSearch(String search) {
      return this.stream().anyMatch(t -> t.matchesSearch(search));
   }
}
