package necesse.gfx.ui;

import java.io.FileNotFoundException;
import necesse.gfx.gameTexture.GameTexture;

public class ProgressBarTextures {
   public GameTexture background;
   public GameTexture fill;
   public GameTexture indicator;
   public int topPadding;
   public int horizontalPadding;

   public ProgressBarTextures(GameInterfaceStyle style, String path) {
      this.background = style.fromFile(path + "_background");
      this.fill = style.fromFile(path + "_fill");
      this.indicator = this.fromFile(style, path, "_indicator", null);
      this.topPadding = (this.background.getHeight() - this.fill.getHeight()) / 2;
      this.horizontalPadding = this.topPadding;
   }

   protected GameTexture fromFile(GameInterfaceStyle style, String basePath, String addedPath, GameTexture defaultReturn) {
      try {
         return style.fromFileRaw(basePath + addedPath);
      } catch (FileNotFoundException e) {
         try {
            return style.fromDefaultFileRaw(basePath + addedPath);
         } catch (FileNotFoundException ex) {
            return defaultReturn;
         }
      }
   }
}
