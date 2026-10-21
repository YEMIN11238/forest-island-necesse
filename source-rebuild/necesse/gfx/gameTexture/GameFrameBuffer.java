package necesse.gfx.gameTexture;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import necesse.engine.GameLog;
import necesse.engine.util.GameUtils;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsStart;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.stb.STBImageWrite;

public abstract class GameFrameBuffer extends AbstractGameTexture {
   private final Consumer<GameFrameBuffer> binder;

   public GameFrameBuffer(Consumer<GameFrameBuffer> binder) {
      this.binder = binder;
   }

   public abstract boolean isComplete();

   public abstract int getColorBufferTextureID();

   public abstract int getDepthBufferTextureID();

   public void clearColor() {
      GL11.glClear(16384);
   }

   public void clearDepth() {
      GL11.glClear(256);
   }

   public void bindFrameBuffer() {
      this.glBind();
      this.binder.accept(this);
   }

   public void unbindFrameBuffer() {
      this.glUnbind();
      this.binder.accept(null);
   }

   protected abstract void glBind();

   protected abstract void glUnbind();

   public abstract void dispose();

   public abstract GameTexture getTextureAndDisposeFrameBuffer(String var1);

   public TextureDrawOptionsStart initDraw() {
      TextureDrawOptionsStart drawOptions = TextureDrawOptions.initDraw(this);
      drawOptions.mirrorY();
      return drawOptions;
   }

   public static void drawSimple(int textureID, int x, int y, int width, int height, Runnable preDrawLogic, Runnable postDrawLogic) {
      GL11.glDepthMask(false);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, textureID);

      try {
         if (preDrawLogic != null) {
            preDrawLogic.run();
         }

         GL11.glBegin(7);
         GL11.glTexCoord2f(0.0F, 1.0F);
         GL11.glVertex2f(x, y);
         GL11.glTexCoord2f(1.0F, 1.0F);
         GL11.glVertex2f(x + width, y);
         GL11.glTexCoord2f(1.0F, 0.0F);
         GL11.glVertex2f(x + width, y + height);
         GL11.glTexCoord2f(0.0F, 0.0F);
         GL11.glVertex2f(x, y + height);
         GL11.glEnd();
         GL11.glDepthMask(true);
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
      } finally {
         if (postDrawLogic != null) {
            postDrawLogic.run();
         }
      }
   }

   public static void draw(
      int textureID, int x, int y, int width, int height, GameTexture.BlendQuality blendQuality, Runnable preDrawLogic, Runnable postDrawLogic
   ) {
      GL11.glBlendFunc(1, 771);
      drawSimple(textureID, x, y, width, height, () -> {
         GL11.glTexParameteri(3553, 10241, blendQuality.minFilter);
         GL11.glTexParameteri(3553, 10240, blendQuality.magFilter);
         GL11.glLoadIdentity();
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         if (preDrawLogic != null) {
            preDrawLogic.run();
         }
      }, postDrawLogic);
   }

   public static void draw(int textureID, int x, int y, int width, int height, Runnable preDrawLogic, Runnable postDrawLogic) {
      draw(textureID, x, y, width, height, GameTexture.BlendQuality.LINEAR, preDrawLogic, postDrawLogic);
   }

   public static byte[] getTextureData(int textureID, int width, int height) {
      ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, textureID);
      GL11.glGetTexImage(3553, 0, 6408, 5121, buffer);
      byte[] data = new byte[buffer.limit()];
      buffer.get(data);
      buffer.clear();
      GL11.glBindTexture(3553, 0);
      return data;
   }

   public void saveTextureImage(int textureID, int width, int height, String filePath) {
      byte[] bytes = getTextureData(textureID, width, height);
      File file = new File(filePath + ".png");
      GameUtils.mkDirs(file);
      ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
      buffer.put(bytes);
      buffer.position(0);
      STBImageWrite.stbi_write_png(file.getAbsolutePath(), width, height, 4, buffer, 0);
      GameLog.debug.println("Saved frame buffer texture image to " + file.getAbsolutePath());
   }
}
