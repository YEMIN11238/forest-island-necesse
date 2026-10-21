package necesse.gfx.credits;

import java.awt.Dimension;
import necesse.engine.util.GameMath;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;

public class GameSpriteCreditsDisplay extends GameCreditsDisplay {
   public int fadeInTime;
   public int showTime;
   public int fadeOutTime;
   public int maxHeight;
   public int maxWidth;
   public boolean centered;
   public GameSprite sprite;

   public GameSpriteCreditsDisplay(int fadeInTime, int showTime, int fadeOutTime, GameSprite sprite, int maxHeight, int maxWidth, boolean centered) {
      this.fadeInTime = fadeInTime;
      this.showTime = showTime;
      this.fadeOutTime = fadeOutTime;
      this.sprite = sprite;
      this.maxHeight = maxHeight;
      this.maxWidth = maxWidth;
      this.centered = centered;
   }

   public GameSpriteCreditsDisplay(int fadeInTime, int showTime, int fadeOutTime, GameTexture texture, int maxHeight, int maxWidth, boolean centered) {
      this(fadeInTime, showTime, fadeOutTime, new GameSprite(texture), maxHeight, maxWidth, centered);
   }

   @Override
   public int initDrawAndGetTotalTimeShown() {
      return this.fadeInTime + this.showTime + this.fadeOutTime;
   }

   public TextureDrawOptionsEnd getDrawOptions(int drawX, int drawY, float alpha) {
      TextureDrawOptionsEnd drawOptions = this.sprite.initDraw();
      if (this.maxWidth > 0 && drawOptions.getWidth() > this.maxWidth) {
         drawOptions = drawOptions.shrinkWidth(this.maxWidth, false);
      }

      if (this.maxHeight > 0 && drawOptions.getHeight() > this.maxHeight) {
         drawOptions = drawOptions.shrinkHeight(this.maxHeight, false);
      }

      return drawOptions.alpha(alpha).pos(this.centered ? drawX - drawOptions.getWidth() / 2 : drawX, drawY);
   }

   @Override
   public Dimension getDrawBounds() {
      TextureDrawOptionsEnd drawOptions = this.getDrawOptions(0, 0, 1.0F);
      return new Dimension(drawOptions.getWidth(), drawOptions.getHeight());
   }

   @Override
   public void draw(int currentTime, int x, int y, float alpha) {
      if (this.fadeInTime > 0 && currentTime < this.fadeInTime) {
         float fadeInProgress = (float)currentTime / this.fadeInTime;
         alpha *= fadeInProgress;
      } else if (this.fadeOutTime > 0 && currentTime > this.fadeInTime + this.showTime) {
         float fadeOutProgress = (float)(currentTime - (this.fadeInTime + this.showTime)) / this.fadeOutTime;
         alpha *= 1.0F - fadeOutProgress;
      }

      this.getDrawOptions(x, y, GameMath.limit(alpha, 0.0F, 1.0F)).draw();
   }

   @Override
   public boolean isDone(int currentTime) {
      return currentTime >= this.fadeInTime + this.showTime + this.fadeOutTime;
   }
}
