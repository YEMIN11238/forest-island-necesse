package necesse.gfx.drawOptions;

import necesse.engine.Settings;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTexture.GameTexture;

public class ChatBubbleDrawOptions {
   protected GameTexture texture;
   protected int width;
   protected int height;
   protected boolean hasTopIndicator;
   protected boolean hasBotIndicator;
   protected int topIndicatorX;
   protected int botIndicatorX;
   protected boolean topIndicatorMirrored;
   protected boolean botIndicatorMirrored;

   public ChatBubbleDrawOptions(GameTexture texture, int width, int height) {
      this.texture = texture;
      this.width = width;
      this.height = height;
   }

   public ChatBubbleDrawOptions(int width, int height) {
      this(Settings.UI.chatbubble, width, height);
   }

   public ChatBubbleDrawOptions setTopIndicator(int xOffset, boolean mirrored) {
      this.topIndicatorX = xOffset;
      this.topIndicatorMirrored = mirrored;
      this.hasTopIndicator = true;
      return this;
   }

   public ChatBubbleDrawOptions setBotIndicator(int xOffset, boolean mirrored) {
      this.botIndicatorX = xOffset;
      this.botIndicatorMirrored = mirrored;
      this.hasBotIndicator = true;
      return this;
   }

   public DrawOptions pos(int drawX, int drawY) {
      SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(this.texture);
      int spriteRes = 32;
      int padding = 4;
      int width = this.width + padding * 2;
      int height = this.height + padding * 2;
      drawX -= padding;
      drawY -= padding;
      int leftWidth = Math.min(spriteRes, width / 2);
      int rightWidth = Math.min(spriteRes, width - leftWidth);
      int topHeight = Math.min(spriteRes, height / 2);
      int botHeight = Math.min(spriteRes, height - topHeight);
      drawOptions.addSpriteSection(0, 0, spriteRes, 0, leftWidth, 0, topHeight).pos(drawX, drawY);
      drawOptions.addSpriteSection(0, 2, spriteRes, 0, leftWidth, spriteRes - topHeight, spriteRes).pos(drawX, drawY + height - botHeight);
      drawOptions.addSpriteSection(2, 0, spriteRes, spriteRes - rightWidth, spriteRes, 0, botHeight).pos(drawX + width - rightWidth, drawY);
      drawOptions.addSpriteSection(2, 2, spriteRes, spriteRes - rightWidth, spriteRes, spriteRes - topHeight, spriteRes)
         .pos(drawX + width - rightWidth, drawY + height - botHeight);
      int midWidth = width - spriteRes * 2;
      if (midWidth > 0) {
         for (int x = 0; x < midWidth; x += spriteRes) {
            int currentDrawWidth = Math.min(spriteRes, midWidth - x);
            drawOptions.addSpriteSection(1, 0, spriteRes, spriteRes, 0, currentDrawWidth, 0, topHeight).pos(drawX + spriteRes + x, drawY);
            drawOptions.addSpriteSection(1, 2, spriteRes, spriteRes, 0, currentDrawWidth, spriteRes - botHeight, spriteRes)
               .pos(drawX + spriteRes + x, drawY + height - botHeight);
         }
      }

      int midHeight = height - spriteRes * 2;
      if (midHeight > 0) {
         for (int y = 0; y < midHeight; y += spriteRes) {
            int currentDrawHeight = Math.min(spriteRes, midHeight - y);
            drawOptions.addSpriteSection(0, 1, spriteRes, spriteRes, 0, leftWidth, 0, currentDrawHeight).pos(drawX, drawY + spriteRes + y);
            drawOptions.addSpriteSection(2, 1, spriteRes, spriteRes, spriteRes - rightWidth, spriteRes, 0, currentDrawHeight)
               .pos(drawX + width - rightWidth, drawY + spriteRes + y);
         }
      }

      if (midWidth > 0 && midHeight > 0) {
         for (int x = 0; x < midWidth; x += spriteRes) {
            int currentDrawWidth = Math.min(spriteRes, midWidth - x);

            for (int y = 0; y < midHeight; y += spriteRes) {
               int currentDrawHeight = Math.min(spriteRes, midHeight - y);
               drawOptions.addSpriteSection(1, 1, spriteRes, 0, currentDrawWidth, 0, currentDrawHeight).pos(drawX + spriteRes + x, drawY + spriteRes + y);
            }
         }
      }

      if (this.hasTopIndicator) {
         drawOptions.addSprite(3, 0, spriteRes)
            .mirror(this.topIndicatorMirrored, false)
            .pos(drawX + this.topIndicatorX - 8 - (this.topIndicatorMirrored ? 8 : 0), drawY - 16);
      }

      if (this.hasBotIndicator) {
         drawOptions.addSprite(3, 1, spriteRes)
            .mirror(this.botIndicatorMirrored, false)
            .pos(drawX + this.botIndicatorX - 16 + (this.botIndicatorMirrored ? 10 : 0), drawY + height + padding - 16);
      }

      return drawOptions::draw;
   }
}
