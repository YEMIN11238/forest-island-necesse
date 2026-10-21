package necesse.gfx.gameFont;

import java.awt.Color;
import java.awt.Point;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import necesse.engine.GameLog;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.MergeFunction;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTPackedchar.Buffer;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class TrueTypeGameFont extends GameFont {
   public static int START_TEXTURE_SIZE = 1024;
   public final int fontSize;
   public final int addedFontSize;
   public final int strokeSize;
   private final TrueTypeGameFontSize[] fonts;
   private final HashMap<Point, Float> strokeDistanceMap = new HashMap<>();
   private final HashMap<Integer, TrueTypeGameFont.CharacterInfo> charactersData = new HashMap<>();
   private Buffer nullBuffer;
   private final LinkedList<Buffer> buffers = new LinkedList<>();
   private STBTTPackContext packContext;
   private int bitmapWidth = START_TEXTURE_SIZE;
   private int bitmapHeight = START_TEXTURE_SIZE;
   private ByteBuffer fontBitmap;
   private ByteBuffer strokeBitmap;
   private int fontTexture;
   private int strokeTexture;
   private final STBTTAlignedQuad alignedQuad = STBTTAlignedQuad.malloc();
   private final FloatBuffer xBuffer = MemoryUtil.memAllocFloat(1);
   private final FloatBuffer yBuffer = MemoryUtil.memAllocFloat(1);
   private final Object bufferLock = new Object();
   private final int yDrawOffset;
   private final CustomGameFont.CharArray preDefinedChars;

   public TrueTypeGameFont(
      int fontSize, int strokeSize, int addedFontSize, int yDrawOffset, CustomGameFont.CharArray preDefinedChars, TrueTypeGameFontInfo... fonts
   ) {
      this.fontSize = fontSize;
      this.strokeSize = strokeSize;
      this.addedFontSize = addedFontSize;
      this.yDrawOffset = yDrawOffset;
      this.preDefinedChars = preDefinedChars;
      this.fonts = new TrueTypeGameFontSize[fonts.length];

      for (int i = 0; i < fonts.length; i++) {
         this.fonts[i] = new TrueTypeGameFontSize(fonts[i], fontSize);
      }

      if (strokeSize > 0) {
         for (int y = -strokeSize; y <= strokeSize; y++) {
            for (int x = -strokeSize; x <= strokeSize; x++) {
               Point p = new Point(x, y);
               double distance = p.distance(0.0, 0.0);
               this.strokeDistanceMap.put(p, (float)distance);
            }
         }
      }

      HashSet<Integer> codePoints = new HashSet<>();

      for (String s : CustomGameFont.fontLayout) {
         for (int i = 0; i < s.length(); i++) {
            codePoints.add(s.codePointAt(i));
         }
      }

      this.addCharacters(codePoints);
   }

   private void increaseSize() {
      this.bitmapWidth *= 2;
      this.bitmapHeight *= 2;
      GameLog.debug.println("Increased " + this.fontSize + "x" + this.strokeSize + " font map to " + this.bitmapWidth + "x" + this.bitmapHeight);
      if (this.packContext != null) {
         STBTruetype.stbtt_PackEnd(this.packContext);
         this.packContext.free();
      }

      this.buffers.forEach(CustomBuffer::free);
      this.buffers.clear();
      this.nullBuffer = null;
      this.packContext = null;
      this.charactersData.clear();
   }

   private void addCharacters(HashSet<Integer> codePoints) {
      codePoints.removeIf(this.charactersData::containsKey);
      if (!codePoints.isEmpty()) {
         if (this.packContext == null) {
            this.packContext = STBTTPackContext.malloc();
            this.fontBitmap = BufferUtils.createByteBuffer(this.bitmapWidth * this.bitmapHeight);
            if (this.strokeSize > 0) {
               this.strokeBitmap = BufferUtils.createByteBuffer(this.bitmapWidth * this.bitmapHeight);
            }

            int padding = this.strokeSize * 2 + 1;
            STBTruetype.stbtt_PackBegin(this.packContext, this.fontBitmap, this.bitmapWidth, this.bitmapHeight, 0, padding);
         }

         for (TrueTypeGameFontSize font : this.fonts) {
            ArrayList<Integer> thisFontCodePoints = new ArrayList<>(codePoints.size());
            int firstCodePoint = Integer.MAX_VALUE;

            for (int codePoint : codePoints) {
               if (font.info.canDisplay(codePoint)) {
                  thisFontCodePoints.add(codePoint);
                  firstCodePoint = Math.min(firstCodePoint, codePoint);
               }
            }

            if (!thisFontCodePoints.isEmpty()) {
               Buffer data = this.packCharacters(font, firstCodePoint, thisFontCodePoints);
               if (data == null) {
                  HashSet<Integer> currentCodePoints = new HashSet<>(this.charactersData.keySet());
                  currentCodePoints.addAll(codePoints);
                  this.increaseSize();
                  this.addCharacters(currentCodePoints);
                  return;
               }

               thisFontCodePoints.forEach(codePoints::remove);
               this.buffers.add(data);
               int i = 0;

               for (int codePoint : thisFontCodePoints) {
                  this.charactersData.put(codePoint, new TrueTypeGameFont.CharacterInfo(codePoint, i++, data));
               }
            }
         }

         if (!codePoints.isEmpty()) {
            if (this.nullBuffer == null) {
               this.nullBuffer = this.packCharacters(this.fonts[0], 0, Collections.singleton(0));
               if (this.nullBuffer == null) {
                  HashSet<Integer> currentCodePoints = new HashSet<>(this.charactersData.keySet());
                  this.increaseSize();
                  this.addCharacters(currentCodePoints);
                  return;
               }

               this.buffers.add(this.nullBuffer);
            }

            for (int codePoint : codePoints) {
               this.charactersData.put(codePoint, new TrueTypeGameFont.CharacterInfo(0, 0, this.nullBuffer));
            }
         }

         if (this.fontTexture != 0) {
            GL11.glDeleteTextures(this.fontTexture);
         }

         this.fontTexture = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.fontTexture);
         ByteBuffer fontColorBuffer = this.bitBufferToColorBuffer(this.fontBitmap, this.bitmapWidth, this.bitmapHeight);
         GL11.glTexImage2D(3553, 0, 32856, this.bitmapWidth, this.bitmapHeight, 0, 6408, 5121, fontColorBuffer);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
         if (this.strokeBitmap != null) {
            if (this.strokeTexture != 0) {
               GL11.glDeleteTextures(this.strokeTexture);
            }

            this.strokeTexture = GL11.glGenTextures();
            GL11.glBindTexture(3553, this.strokeTexture);
            ByteBuffer strokeColorBuffer = this.bitBufferToColorBuffer(this.strokeBitmap, this.bitmapWidth, this.bitmapHeight);
            GL11.glTexImage2D(3553, 0, 32856, this.bitmapWidth, this.bitmapHeight, 0, 6408, 5121, strokeColorBuffer);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glTexParameteri(3553, 10241, 9729);
         }
      }
   }

   private ByteBuffer generateFontBuffer(ByteBuffer fontBitmap, ByteBuffer strokeBitmap, int width, int height) {
      ByteBuffer colorBuffer = BufferUtils.createByteBuffer(width * height * 4);
      fontBitmap.position(0);
      strokeBitmap.position(0);
      colorBuffer.position(0);

      while (fontBitmap.hasRemaining()) {
         int fontAlpha = fontBitmap.get() & 255;
         int strokeAlpha = strokeBitmap.get() & 255;
         Color color = MergeFunction.MULTIPLY.merge(new Color(0, 0, 0, strokeAlpha), new Color(255, 255, 255, fontAlpha));
         colorBuffer.put(new byte[]{(byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue(), (byte)color.getAlpha()});
      }

      fontBitmap.position(0);
      strokeBitmap.position(0);
      colorBuffer.position(0);
      return colorBuffer;
   }

   private ByteBuffer bitBufferToColorBuffer(ByteBuffer bitBuffer, int width, int height) {
      ByteBuffer colorBuffer = BufferUtils.createByteBuffer(width * height * 4);
      bitBuffer.position(0);
      colorBuffer.position(0);

      while (bitBuffer.hasRemaining()) {
         byte alpha = bitBuffer.get();
         colorBuffer.put(new byte[]{-1, -1, -1, alpha});
      }

      bitBuffer.position(0);
      colorBuffer.position(0);
      return colorBuffer;
   }

   private synchronized Buffer packCharacters(TrueTypeGameFontSize font, int firstCodePoint, Collection<Integer> codePoints) {
      MemoryStack stack = MemoryStack.stackPush();

      Buffer var16;
      label52: {
         try {
            byte horizontalOversample = 1;
            byte verticalOversample = 1;
            Buffer packedData = STBTTPackedchar.malloc(codePoints.size());
            org.lwjgl.stb.STBTTPackRange.Buffer packRanges = STBTTPackRange.malloc(1, stack);
            IntBuffer ints = stack.mallocInt(codePoints.size());

            for (int codePoint : codePoints) {
               ints.put(codePoint);
            }

            ints.position(0);
            packRanges.put(
               STBTTPackRange.malloc(stack)
                  .set(font.fontSize + this.addedFontSize, firstCodePoint, ints, codePoints.size(), packedData, horizontalOversample, verticalOversample)
            );
            packRanges.flip();
            boolean success = STBTruetype.stbtt_PackFontRanges(this.packContext, font.info.getTTFBuffer(), 0, packRanges);
            packedData.clear();
            if (!success) {
               packedData.free();
               var16 = null;
               break label52;
            }

            this.generateStrokeBitmap(packedData);
            var16 = packedData;
         } catch (Throwable var13) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (stack != null) {
            stack.close();
         }

         return var16;
      }

      if (stack != null) {
         stack.close();
      }

      return var16;
   }

   private synchronized void generateStrokeBitmap(Buffer data) {
      if (this.strokeBitmap != null) {
         data.position(0);

         while (data.hasRemaining()) {
            STBTTPackedchar packedChar = data.get();
            short startX = packedChar.x0();
            short endX = packedChar.x1();
            short startY = packedChar.y0();
            short endY = packedChar.y1();

            for (int x = startX; x < endX; x++) {
               for (int y = startY; y < endY; y++) {
                  int currentFontAlpha = this.fontBitmap.get(x + y * this.bitmapHeight) & 255;
                  if (currentFontAlpha > 0) {
                     for (Entry<Point, Float> e : this.strokeDistanceMap.entrySet()) {
                        Point offset = e.getKey();
                        float distance = e.getValue();
                        if (!(distance > this.strokeSize + 0.5F)) {
                           Point p = new Point(x + offset.x, y + offset.y);
                           int currentStrokeAlpha = this.strokeBitmap.get(p.x + p.y * this.bitmapHeight) & 255;
                           int desiredAlpha;
                           if (distance > this.strokeSize - 0.5F) {
                              desiredAlpha = currentFontAlpha;
                           } else {
                              desiredAlpha = 255;
                           }

                           if (currentStrokeAlpha < desiredAlpha) {
                              this.strokeBitmap.put(p.x + p.y * this.bitmapHeight, (byte)desiredAlpha);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void addCharOffset(String chars, float xOffset, float yOffset) {
      for (int i = 0; i < chars.length(); i++) {
         this.addCharOffset(chars.charAt(i), xOffset, yOffset);
      }
   }

   public boolean addCharOffset(char character, float xOffset, float yOffset) {
      TrueTypeGameFont.CharacterInfo info = this.charactersData.get(Integer.valueOf(character));
      if (info != null) {
         info.xOffset += xOffset;
         info.yOffset += yOffset;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public synchronized float drawString(float x, float y, String text, FontBasicOptions options) {
      synchronized (this.bufferLock) {
         boolean pixelFont = options.isPixelFont();
         float ratio = (float)options.getSize() / this.fontSize;
         float height = ratio * this.fontSize;
         y += this.yDrawOffset * ratio;
         y -= this.strokeSize * ratio;
         y += height;
         float textureStrokeX = 0.0F;
         float textureStrokeY = 0.0F;
         TrueTypeGameFont.TextureSwapper swapper = null;
         if (this.strokeTexture != 0) {
            textureStrokeX = (float)this.strokeSize / this.bitmapWidth;
            textureStrokeY = (float)this.strokeSize / this.bitmapHeight;
            this.xBuffer.put(0, x);
            this.yBuffer.put(0, y);
            options.applyGLStrokeColor();

            for (int i = 0; i < text.length(); i++) {
               char c = text.charAt(i);
               this.drawCharQuad(c, textureStrokeX, textureStrokeY, ratio, swapper, true, pixelFont);
            }
         }

         this.xBuffer.put(0, x);
         this.yBuffer.put(0, y);
         options.applyGLColor();

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            this.drawCharQuad(c, textureStrokeX, textureStrokeY, ratio, swapper, false, pixelFont);
         }

         return this.xBuffer.get(0) - x;
      }
   }

   @Override
   public float drawStringShadow(float x, float y, String text, FontBasicOptions options) {
      synchronized (this.bufferLock) {
         int[] offset = options.getShadowOffset();
         x += offset[0];
         y += offset[1];
         boolean pixelFont = options.isPixelFont();
         float ratio = (float)options.getSize() / this.fontSize;
         float height = ratio * this.fontSize;
         y += this.yDrawOffset * ratio;
         y -= this.strokeSize * ratio;
         y += height;
         float textureStrokeX = 0.0F;
         float textureStrokeY = 0.0F;
         TrueTypeGameFont.TextureSwapper swapper = null;
         if (this.strokeTexture != 0) {
            textureStrokeX = (float)this.strokeSize / this.bitmapWidth;
            textureStrokeY = (float)this.strokeSize / this.bitmapHeight;
            this.xBuffer.put(0, x);
            this.yBuffer.put(0, y);
            options.applyGLShadowColor();

            for (int i = 0; i < text.length(); i++) {
               char c = text.charAt(i);
               this.drawCharQuad(c, textureStrokeX, textureStrokeY, ratio, swapper, true, pixelFont);
            }
         }

         this.xBuffer.put(0, x);
         this.yBuffer.put(0, y);
         options.applyGLShadowColor();

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            this.drawCharQuad(c, textureStrokeX, textureStrokeY, ratio, swapper, false, pixelFont);
         }

         return this.xBuffer.get(0) - x;
      }
   }

   private synchronized void drawCharQuad(
      char ch, float textureStrokeX, float textureStrokeY, float ratio, TrueTypeGameFont.TextureSwapper swapper, boolean isStroke, boolean pixelFont
   ) {
      synchronized (this.bufferLock) {
         GameFontGlyphPositionTexture glyph = null;
         if (pixelFont && this.preDefinedChars != null && ch < this.preDefinedChars.chars.length) {
            glyph = this.preDefinedChars.chars[ch];
         }

         if (glyph != null) {
            if (isStroke) {
               return;
            }

            float startX = this.xBuffer.get(0);
            float startY = this.yBuffer.get(0);
            float width = glyph.width * ratio;
            glyph.texture.bindTexture();
            glyph.draw(startX, startY - (glyph.height + this.yDrawOffset) * ratio, width, glyph.height * ratio);
            this.xBuffer.put(0, startX + width);
         } else {
            TrueTypeGameFont.CharacterInfo info = this.charactersData.get(Integer.valueOf(ch));
            if (info == null) {
               info = this.charactersData.get(63);
            }

            info.data.position(0);
            float startX = this.xBuffer.get(0);
            float startY = this.yBuffer.get(0);
            STBTruetype.stbtt_GetPackedQuad(info.data, this.bitmapWidth, this.bitmapHeight, info.charIndex, this.xBuffer, this.yBuffer, this.alignedQuad, true);
            float advanced = (this.xBuffer.get(0) - startX) * ratio;
            this.xBuffer.put(0, startX + advanced);
            float yOffset = this.alignedQuad.y0() - startY + info.yOffset;
            float x = this.alignedQuad.x0() + info.xOffset * ratio;
            float y = startY + (yOffset + this.strokeSize) * ratio - this.strokeSize;
            float alignedWidth = this.alignedQuad.x1() - this.alignedQuad.x0();
            float alignedHeight = this.alignedQuad.y1() - this.alignedQuad.y0();
            float width = (alignedWidth + this.strokeSize) * ratio;
            float height = (alignedHeight + this.strokeSize) * ratio;
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, isStroke ? this.strokeTexture : this.fontTexture);
            GL11.glBegin(7);
            this.drawQuad(
               x,
               y,
               x + width,
               y + height,
               this.alignedQuad.s0() - textureStrokeX,
               this.alignedQuad.t0() - textureStrokeY,
               this.alignedQuad.s1() + textureStrokeX,
               this.alignedQuad.t1() + textureStrokeY
            );
            GL11.glEnd();
         }
      }
   }

   private void drawQuad(float x0, float y0, float x1, float y1, float s0, float t0, float s1, float t1) {
      GL11.glTexCoord2f(s0, t0);
      GL11.glVertex2f(x0, y0);
      GL11.glTexCoord2f(s1, t0);
      GL11.glVertex2f(x1, y0);
      GL11.glTexCoord2f(s1, t1);
      GL11.glVertex2f(x1, y1);
      GL11.glTexCoord2f(s0, t1);
      GL11.glVertex2f(x0, y1);
   }

   @Override
   public synchronized float drawChar(float x, float y, char ch, FontBasicOptions options) {
      return this.drawString(x, y, Character.toString(ch), options);
   }

   @Override
   public float drawCharShadow(float x, float y, char ch, FontBasicOptions options) {
      return this.drawStringShadow(x, y, Character.toString(ch), options);
   }

   @Override
   public synchronized float getWidth(char ch, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.getFontHeight();
      GameFontGlyphPositionTexture glyph = null;
      if (options.isPixelFont() && this.preDefinedChars != null && ch < this.preDefinedChars.chars.length) {
         glyph = this.preDefinedChars.chars[ch];
      }

      if (glyph != null) {
         return glyph.width * ratio;
      }

      synchronized (this.bufferLock) {
         TrueTypeGameFont.CharacterInfo info = this.charactersData.get(Integer.valueOf(ch));
         if (info == null) {
            info = this.charactersData.get(63);
         }

         return info.getWidth() * ratio;
      }
   }

   @Override
   public synchronized float getWidth(String str, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.getFontHeight();
      float width = 0.0F;

      for (int i = 0; i < str.length(); i++) {
         char ch = str.charAt(i);
         GameFontGlyphPositionTexture glyph = null;
         if (options.isPixelFont() && this.preDefinedChars != null && ch < this.preDefinedChars.chars.length) {
            glyph = this.preDefinedChars.chars[ch];
         }

         if (glyph != null) {
            width += glyph.width * ratio;
         } else {
            synchronized (this.bufferLock) {
               TrueTypeGameFont.CharacterInfo info = this.charactersData.get(Integer.valueOf(ch));
               if (info == null) {
                  info = this.charactersData.get(63);
               }

               float advanced = info.getWidth() * ratio;
               width += advanced;
            }
         }
      }

      return width;
   }

   @Override
   public float getHeight(char ch, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.fontSize;
      return ratio * this.fontSize;
   }

   @Override
   public float getHeight(String str, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.fontSize;
      return ratio * this.fontSize;
   }

   @Override
   public synchronized int getWidthCeil(String str, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.getFontHeight();
      float width = 0.0F;

      for (int i = 0; i < str.length(); i++) {
         char ch = str.charAt(i);
         GameFontGlyphPositionTexture glyph = null;
         if (options.isPixelFont() && this.preDefinedChars != null && ch < this.preDefinedChars.chars.length) {
            glyph = this.preDefinedChars.chars[ch];
         }

         if (glyph != null) {
            width += glyph.width * ratio;
         } else {
            synchronized (this.bufferLock) {
               TrueTypeGameFont.CharacterInfo info = this.charactersData.get(Integer.valueOf(ch));
               if (info == null) {
                  info = this.charactersData.get(63);
               }

               float advanced = info.getWidth() * ratio;
               width += advanced;
            }
         }
      }

      return (int)Math.ceil(width);
   }

   @Override
   public synchronized int getWidthCeil(char ch, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.getFontHeight();
      GameFontGlyphPositionTexture glyph = null;
      if (options.isPixelFont() && this.preDefinedChars != null && ch < this.preDefinedChars.chars.length) {
         glyph = this.preDefinedChars.chars[ch];
      }

      if (glyph != null) {
         return (int)Math.ceil(glyph.width * ratio);
      }

      synchronized (this.bufferLock) {
         TrueTypeGameFont.CharacterInfo info = this.charactersData.get(Integer.valueOf(ch));
         if (info == null) {
            info = this.charactersData.get(63);
         }

         float advanced = info.getWidth() * ratio;
         return (int)Math.ceil(advanced);
      }
   }

   @Override
   public int getFontHeight() {
      return this.fontSize;
   }

   @Override
   public int getHeightCeil(String str, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.fontSize;
      return (int)(ratio * this.fontSize);
   }

   @Override
   public int getHeightCeil(char ch, FontBasicOptions options) {
      float ratio = (float)options.getSize() / this.fontSize;
      return (int)(ratio * this.fontSize);
   }

   @Override
   public boolean canDraw(char ch) {
      return this.charactersData.containsKey(Integer.valueOf(ch));
   }

   @Override
   public void deleteTextures() {
      this.dispose();
   }

   @Override
   public GameFont updateFont(String additionalChars) {
      if (additionalChars == null) {
         return this;
      }

      HashSet<Integer> codePoints = new HashSet<>();

      for (int i = 0; i < additionalChars.length(); i++) {
         codePoints.add(additionalChars.codePointAt(i));
      }

      this.addCharacters(codePoints);
      return this;
   }

   public void dispose() {
      if (this.preDefinedChars != null) {
         this.preDefinedChars.texture.delete();
      }

      if (this.packContext != null) {
         STBTruetype.stbtt_PackEnd(this.packContext);
         this.packContext.free();
      }

      this.buffers.forEach(CustomBuffer::free);
      this.buffers.clear();
      this.packContext = null;
      this.charactersData.clear();
      if (this.fontTexture != 0) {
         GL11.glDeleteTextures(this.fontTexture);
      }

      this.fontTexture = 0;
      if (this.strokeTexture != 0) {
         GL11.glDeleteTextures(this.strokeTexture);
      }

      this.strokeTexture = 0;
      MemoryUtil.memFree(this.xBuffer);
      MemoryUtil.memFree(this.yBuffer);
      this.alignedQuad.free();
   }

   private static class CharacterInfo {
      public final int codePoint;
      public final int charIndex;
      public final Buffer data;
      public final int x0;
      public final int y0;
      public final int x1;
      public final int y1;
      public final float xOff;
      public final float yOff;
      public final float xAdvance;
      public final float xOff2;
      public final float yOff2;
      public float yOffset;
      public float xOffset;

      public CharacterInfo(int codePoint, int charIndex, Buffer data) {
         this.codePoint = codePoint;
         this.charIndex = charIndex;
         this.data = data;
         STBTTPackedchar charData = data.get(charIndex);
         this.x0 = charData.x0();
         this.y0 = charData.y0();
         this.x1 = charData.x1();
         this.y1 = charData.y1();
         this.xOff = charData.xoff();
         this.yOff = charData.yoff();
         this.xAdvance = charData.xadvance();
         this.xOff2 = charData.xoff2();
         this.yOff2 = charData.yoff2();
      }

      public float getWidth() {
         return this.xAdvance;
      }

      public int getHeight() {
         return this.y1 - this.y0;
      }
   }

   private static class TextureSwapper {
      private int currentTextureID;
      private boolean drawing;

      public TextureSwapper() {
      }

      public void useTexture(GameTexture texture) {
         if (this.currentTextureID != texture.getTextureID()) {
            if (this.drawing) {
               GL11.glEnd();
               this.drawing = false;
            }

            this.currentTextureID = texture.getTextureID();
            texture.bindTexture();
            if (!this.drawing) {
               GL11.glBegin(7);
            }

            this.drawing = true;
         }
      }

      public void useTexture(int textureID) {
         if (this.currentTextureID != textureID) {
            if (this.drawing) {
               GL11.glEnd();
               this.drawing = false;
            }

            this.currentTextureID = textureID;
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, textureID);
            if (!this.drawing) {
               GL11.glBegin(7);
            }

            this.drawing = true;
         }
      }

      public void end() {
         if (this.drawing) {
            GL11.glEnd();
            this.drawing = false;
         }

         this.currentTextureID = 0;
      }
   }
}
