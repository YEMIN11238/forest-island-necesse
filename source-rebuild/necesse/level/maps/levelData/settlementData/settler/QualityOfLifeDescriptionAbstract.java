package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Rectangle;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.gfx.drawOptions.PositionedDrawOptionsBox;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.GameInterfaceStyle;

public abstract class QualityOfLifeDescriptionAbstract {
   public final int minLevel;

   public QualityOfLifeDescriptionAbstract(int minLevel) {
      this.minLevel = minLevel;
   }

   public abstract GameMessage getAdjective();

   public GameMessage getDescription() {
      return new LocalMessage("settlement", "qolcomposition", "adjective", this.getAdjective());
   }

   public GameMessage getFullMessage() {
      return new LocalMessage("settlement", "qolmessage", "qualityoflife", this.getDescription());
   }

   public PositionedDrawOptionsBox getIconDrawOptions(final int qualityOfLifeLevel, GameInterfaceStyle style) {
      GameTexture texture = style.qol_badges;
      final int resolution = texture.getHeight();
      int totalSprites = texture.getWidth() / resolution;
      final GameSprite icon = new GameSprite(texture, Math.min(qualityOfLifeLevel, totalSprites - 1), 0, resolution);
      if (qualityOfLifeLevel != this.minLevel) {
         final FontOptions fontOptions = new FontOptions(12).defaultColor(style.activeTextColor);
         return new PositionedDrawOptionsBox() {
            @Override
            public Rectangle getBoundingBox(int drawX, int drawY) {
               return new Rectangle(drawX, drawY, resolution, resolution + fontOptions.getSize());
            }

            @Override
            public void draw(int drawX, int drawY) {
               icon.initDraw().draw(drawX, drawY);
               String text = "(" + qualityOfLifeLevel + ")";
               int textWidth = FontManager.bit.getWidthCeil(text, fontOptions);
               FontManager.bit.drawString(drawX + resolution / 2 - textWidth / 2, drawY + resolution, text, fontOptions);
            }
         };
      } else {
         return new PositionedDrawOptionsBox() {
            @Override
            public Rectangle getBoundingBox(int drawX, int drawY) {
               return new Rectangle(drawX, drawY, resolution, resolution);
            }

            @Override
            public void draw(int drawX, int drawY) {
               icon.initDraw().draw(drawX, drawY);
            }
         };
      }
   }
}
