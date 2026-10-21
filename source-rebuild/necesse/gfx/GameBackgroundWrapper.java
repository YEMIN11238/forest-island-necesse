package necesse.gfx;

import java.awt.Color;
import necesse.engine.util.PointSetAbstract;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;

public class GameBackgroundWrapper extends GameBackground {
   public final GameBackground background;

   public GameBackgroundWrapper(GameBackground background) {
      this.background = background;
   }

   @Override
   public SharedTextureDrawOptions getOutlineDrawOptions(int x, int y, int width, int height) {
      return this.background.getOutlineDrawOptions(x, y, width, height);
   }

   @Override
   public SharedTextureDrawOptions getCenterDrawOptions(int x, int y, int width, int height) {
      return this.background.getCenterDrawOptions(x, y, width, height);
   }

   @Override
   public SharedTextureDrawOptions getDrawOptions(int x, int y, int width, int height) {
      return this.background.getDrawOptions(x, y, width, height);
   }

   @Override
   public SharedTextureDrawOptions getOutlineEdgeDrawOptions(int x, int y, int width, int height) {
      return this.background.getOutlineEdgeDrawOptions(x, y, width, height);
   }

   @Override
   public SharedTextureDrawOptions getCenterEdgeDrawOptions(int x, int y, int width, int height) {
      return this.background.getCenterEdgeDrawOptions(x, y, width, height);
   }

   @Override
   public SharedTextureDrawOptions getEdgeDrawOptions(int x, int y, int width, int height) {
      return this.background.getEdgeDrawOptions(x, y, width, height);
   }

   @Override
   public SharedTextureDrawOptions getTiledDrawOptions(int x, int y, int xPadding, int yPadding, PointSetAbstract<?> tiles, int tileWidth, int tileHeight) {
      return this.background.getTiledDrawOptions(x, y, xPadding, yPadding, tiles, tileWidth, tileHeight);
   }

   @Override
   public SharedTextureDrawOptions getTiledEdgeDrawOptions(int x, int y, int xPadding, int yPadding, PointSetAbstract<?> tiles, int tileWidth, int tileHeight) {
      return this.background.getTiledEdgeDrawOptions(x, y, xPadding, yPadding, tiles, tileWidth, tileHeight);
   }

   @Override
   public Color getCenterColor() {
      return this.background.getCenterColor();
   }

   @Override
   public int getContentPadding() {
      return this.background.getContentPadding();
   }
}
