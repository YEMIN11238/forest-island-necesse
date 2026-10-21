package necesse.gfx.gameFont;

import necesse.gfx.gameTexture.GameTexture;

public class CustomGameFont extends GameFont {
   public static final String[] fontLayout = new String[]{
      "abcdefghijklmnopqrstuvwyxz ",
      "ABCDEFGHIJKLMNOPQRSTUVWYXZ",
      "1234567890+-=,;.:-_/\\()[]{}",
      "!\"'#%&*|^<>$?`´~¡¿£€@ß",
      "àáâãäçèéêëìíîïñńňòóôõöùúûü",
      "ÀÁÂÃÄÇÈÉÊËÌÍÎÏÑŃŇÒÓÔÕÖÙÚÛÜ",
      "æøå",
      "ÆØÅ"
   };
   private final CustomGameFont.CharArray charArray;
   private final int fontHeight;
   private final float fontRatio;

   public static boolean fontTextureContains(char ch) {
      for (String line : fontLayout) {
         if (line.indexOf(ch) != -1) {
            return true;
         }
      }

      return false;
   }

   public CustomGameFont(GameTexture texture, int fontWidth, int fontHeight) {
      this.charArray = new CustomGameFont.CharArray(texture, fontWidth, fontHeight);
      this.fontHeight = fontHeight;
      this.fontRatio = (float)fontWidth / fontHeight;
   }

   @Override
   public float drawChar(float x, float y, char ch, FontBasicOptions options) {
      int sizeWidth = (int)(options.getSize() * this.fontRatio);
      GameFontGlyphPositionTexture glyph = null;
      if (ch < this.charArray.chars.length) {
         glyph = this.charArray.chars[ch];
      }

      if (glyph == null) {
         glyph = this.charArray.chars[63];
      }

      glyph.texture.bindTexture();
      options.applyGLColor();
      glyph.draw(x, y, sizeWidth, options.getSize());
      return sizeWidth;
   }

   @Override
   public float drawCharShadow(float x, float y, char ch, FontBasicOptions options) {
      int sizeWidth = (int)(options.getSize() * this.fontRatio);
      GameFontGlyphPositionTexture glyph = null;
      if (ch < this.charArray.chars.length) {
         glyph = this.charArray.chars[ch];
      }

      if (glyph == null) {
         glyph = this.charArray.chars[63];
      }

      glyph.texture.bindTexture();
      options.applyGLShadowColor();
      int[] offset = options.getShadowOffset();
      glyph.draw(x + offset[0], y + offset[1], sizeWidth, options.getSize());
      return sizeWidth;
   }

   @Override
   public float getWidth(char ch, FontBasicOptions options) {
      return options.getSize() * this.fontRatio;
   }

   @Override
   public float getHeight(char ch, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.fontHeight;
      return ratio * this.fontHeight;
   }

   @Override
   public int getWidthCeil(char ch, FontBasicOptions options) {
      return (int)(options.getSize() * this.fontRatio);
   }

   @Override
   public int getFontHeight() {
      return this.fontHeight;
   }

   @Override
   public int getHeightCeil(char ch, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.fontHeight;
      return (int)(ratio * this.fontHeight);
   }

   @Override
   public boolean canDraw(char ch) {
      return this.charArray.canDraw(ch);
   }

   @Override
   public void deleteTextures() {
      this.charArray.texture.delete();
   }

   @Override
   public GameFont updateFont(String additionalChars) {
      return this;
   }

   public CustomGameFont.CharArray getCharArray() {
      return this.charArray;
   }

   public static class CharArray {
      public final GameFontGlyphPositionTexture[] chars;
      public final GameTexture texture;

      public CharArray(GameTexture texture, int fontWidth, int fontHeight) {
         this.texture = texture;
         texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
         int maxChar = 0;

         for (String line : CustomGameFont.fontLayout) {
            for (int i = 0; i < line.length(); i++) {
               maxChar = Math.max(maxChar, line.charAt(i) + 1);
            }
         }

         this.chars = new GameFontGlyphPositionTexture[maxChar];

         for (int line = 0; line < CustomGameFont.fontLayout.length; line++) {
            for (int i = 0; i < CustomGameFont.fontLayout[line].length(); i++) {
               char ch = CustomGameFont.fontLayout[line].charAt(i);
               this.chars[ch] = new GameFontGlyphPositionTexture(texture, i * fontWidth, line * fontHeight, fontWidth, fontHeight);
            }
         }
      }

      public boolean canDraw(char ch) {
         return ch < this.chars.length && this.chars[ch] != null;
      }
   }
}
