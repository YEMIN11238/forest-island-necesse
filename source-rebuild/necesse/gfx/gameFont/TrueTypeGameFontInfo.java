package necesse.gfx.gameFont;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import necesse.engine.GlobalData;
import necesse.engine.util.GameUtils;
import necesse.gfx.res.ResourceEncoder;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

public class TrueTypeGameFontInfo {
   private STBTTFontinfo info;
   private ByteBuffer ttfBuffer;
   public final String file;
   private final int ascent;
   private final int descent;
   private final int lineGap;

   public TrueTypeGameFontInfo(String file) throws IOException {
      this.file = file;
      File outsideFile = new File(GlobalData.rootPath() + "res/fonts/" + file + ".ttf");
      byte[] bytes;
      if (outsideFile.exists()) {
         bytes = GameUtils.loadByteFile(outsideFile);
      } else {
         try {
            bytes = ResourceEncoder.getResourceBytes("fonts/" + file + ".ttf");
         } catch (FileNotFoundException e) {
            bytes = GameUtils.loadByteFile(outsideFile);
         }
      }

      this.ttfBuffer = BufferUtils.createByteBuffer(bytes.length);
      this.ttfBuffer.put(bytes);
      this.ttfBuffer.flip();
      this.info = STBTTFontinfo.malloc();
      if (!STBTruetype.stbtt_InitFont(this.info, this.ttfBuffer)) {
         throw new IOException("Could not load font " + file);
      }

      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer ascent = stack.mallocInt(1);
         IntBuffer descent = stack.mallocInt(1);
         IntBuffer lineGap = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(this.info, ascent, descent, lineGap);
         this.ascent = ascent.get(0);
         this.descent = descent.get(0);
         this.lineGap = lineGap.get(0);
      } catch (Throwable var10) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var8) {
               var10.addSuppressed(var8);
            }
         }

         throw var10;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public synchronized float getFontSize(int size) {
      return STBTruetype.stbtt_ScaleForMappingEmToPixels(this.info, size) * (this.ascent - this.descent);
   }

   public synchronized float getLineGap(int size) {
      return STBTruetype.stbtt_ScaleForMappingEmToPixels(this.info, size) * this.lineGap;
   }

   public synchronized boolean canDisplay(int codePoint) {
      int index = STBTruetype.stbtt_FindGlyphIndex(this.info, codePoint);
      return index != 0;
   }

   public synchronized ByteBuffer getTTFBuffer() {
      return this.ttfBuffer;
   }

   public synchronized void dispose() {
      if (this.info != null) {
         this.info.free();
      }

      this.info = null;
   }
}
