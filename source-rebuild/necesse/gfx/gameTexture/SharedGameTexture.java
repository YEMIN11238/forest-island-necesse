package necesse.gfx.gameTexture;

import java.awt.Color;
import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class SharedGameTexture {
   private String debugName;
   private ArrayList<SharedGameTexture.AddedTexture> textures = new ArrayList<>();

   public SharedGameTexture(String debugName) {
      this.debugName = debugName;
   }

   public GameTexture generate() {
      return this.generate(false);
   }

   public GameTexture generate(boolean drawDebugOutlines) {
      int area = 0;
      int maxWidth = 0;

      for (SharedGameTexture.AddedTexture at : this.textures) {
         area += at.w * at.h;
         maxWidth = Math.max(maxWidth, at.w);
      }

      Comparator<SharedGameTexture.AddedTexture> byHeight = Comparator.comparingInt(t -> t.h);
      this.textures.sort(byHeight.reversed());
      int startWidth = (int)Math.max(Math.ceil(Math.sqrt(area / 0.95)), maxWidth);
      ArrayList<Rectangle> spaces = new ArrayList<>();
      spaces.add(new Rectangle(0, 0, startWidth, Integer.MAX_VALUE));
      ArrayList<SharedGameTexture.PackedTexture> packed = new ArrayList<>();

      for (SharedGameTexture.AddedTexture at : this.textures) {
         for (int i = spaces.size() - 1; i >= 0; i--) {
            Rectangle space = spaces.get(i);
            if (at.w <= space.width && at.h <= space.height) {
               packed.add(new SharedGameTexture.PackedTexture(space.x, space.y, at));
               if (at.w == space.width && at.h == space.height) {
                  Rectangle last = spaces.remove(spaces.size() - 1);
                  if (i < spaces.size()) {
                     spaces.remove(i);
                     spaces.add(i, last);
                  }
               } else if (at.h == space.height) {
                  space.x = space.x + at.w;
                  space.width = space.width - at.w;
               } else if (at.w == space.width) {
                  space.y = space.y + at.h;
                  space.height = space.height - at.h;
               } else {
                  spaces.add(new Rectangle(space.x + at.w, space.y, space.width - at.w, at.h));
                  space.y = space.y + at.h;
                  space.height = space.height - at.h;
               }
               break;
            }
         }
      }

      int width = 0;
      int height = 0;

      for (SharedGameTexture.PackedTexture pt : packed) {
         width = Math.max(width, pt.x + pt.texture.w);
         height = Math.max(height, pt.y + pt.texture.h);
      }

      for (Rectangle space : spaces) {
         width = Math.max(width, space.x + space.width);
      }

      if (width == 0 && height == 0) {
         return null;
      }

      GameTexture texture = new GameTexture(this.debugName, width, height);

      for (SharedGameTexture.PackedTexture pt : packed) {
         int startX = pt.x;
         int startY = pt.y;
         GameTexture currentTexture = pt.texture.texture;
         int currentHeight = currentTexture.getHeight();
         int currentWidth = currentTexture.getWidth();
         ByteBuffer currentBuffer;
         if (!currentTexture.isFinal()) {
            currentBuffer = currentTexture.buffer;
         } else {
            currentBuffer = BufferUtils.createByteBuffer(currentWidth * currentHeight * 4);
            currentTexture.bindTexture();
            GL11.glGetTexImage(3553, 0, 6408, 5121, currentBuffer);
         }

         for (int j = 0; j < currentHeight; j++) {
            int myIndex = (startX + (startY + j) * width) * 4;
            int theirIndex = j * currentWidth * 4;
            texture.buffer.position(myIndex);
            currentBuffer.position(theirIndex);

            for (int i = 0; i < currentWidth * 4; i++) {
               texture.buffer.put(currentBuffer.get());
            }
         }

         GameTextureSection section = new GameTextureSection(texture, startX, startX + currentWidth, startY, startY + currentHeight);
         pt.texture.section.texture = texture;
         pt.texture.section.startX = startX;
         pt.texture.section.endX = startX + currentWidth;
         pt.texture.section.startY = startY;
         pt.texture.section.endY = startY + currentHeight;
         if (drawDebugOutlines) {
            for (int x = section.startX; x < section.endX; x++) {
               texture.setPixel(x, section.startY, new Color(255, 100, 255));
               texture.setPixel(x, section.endY - 1, new Color(255, 100, 255));
            }

            for (int y = section.startY; y < section.endY; y++) {
               texture.setPixel(section.startX, y, new Color(255, 100, 255));
               texture.setPixel(section.endX - 1, y, new Color(255, 100, 255));
            }
         }
      }

      if (drawDebugOutlines) {
         for (Rectangle space : spaces) {
            int startX = space.x;
            int endX = startX + space.width;
            int startY = space.y;
            if (startY < texture.getHeight()) {
               int endY = startY + space.height;
               if (endY < texture.getHeight()) {
                  for (int x = startX; x < endX; x++) {
                     texture.setPixel(x, startY, new Color(255, 100, 100, 150));
                     texture.setPixel(x, endY - 1, new Color(255, 100, 100, 150));
                  }

                  for (int y = startY; y < endY; y++) {
                     texture.setPixel(startX, y, new Color(255, 100, 100, 150));
                     texture.setPixel(endX - 1, y, new Color(255, 100, 100, 150));
                  }
               }
            }
         }
      }

      texture.makeFinal();
      return texture;
   }

   public GameTextureSection addTexture(GameTexture texture) {
      if (this.textures == null) {
         throw new IllegalStateException("Shared texture closed");
      }

      GameTextureSection section = new GameTextureSection();
      this.textures.add(new SharedGameTexture.AddedTexture(texture, section));
      texture.makeFinal();
      return section;
   }

   public GameTextureSection addBlankQuad(int width, int height) {
      GameTexture texture = new GameTexture(this.debugName + " blank" + width + "x" + height, width, height);
      texture.fill(255, 255, 255, 255);
      return this.addTexture(texture);
   }

   public void close() {
      this.textures = null;
   }

   protected static class AddedTexture {
      public int w;
      public int h;
      public final GameTexture texture;
      public final GameTextureSection section;

      public AddedTexture(GameTexture texture, GameTextureSection section) {
         this.w = texture.getWidth();
         this.h = texture.getHeight();
         this.texture = texture;
         this.section = section;
      }
   }

   protected static class PackedTexture {
      public int x;
      public int y;
      public final SharedGameTexture.AddedTexture texture;

      public PackedTexture(int x, int y, SharedGameTexture.AddedTexture texture) {
         this.x = x;
         this.y = y;
         this.texture = texture;
      }
   }
}
