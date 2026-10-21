package necesse.gfx.gameTexture;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class GameFrameBufferEXT extends GameFrameBuffer {
   public final int width;
   public final int height;
   public final int frameBufferObject;
   public final int textureID;
   private final boolean complete;

   public GameFrameBufferEXT(Consumer<GameFrameBuffer> binder, int width, int height) {
      super(binder);
      this.width = width;
      this.height = height;
      this.frameBufferObject = EXTFramebufferObject.glGenFramebuffersEXT();
      EXTFramebufferObject.glBindFramebufferEXT(36160, this.frameBufferObject);
      this.textureID = GL11.glGenTextures();
      GL11.glBindTexture(3553, this.textureID);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, 32856, Math.max(1, width), Math.max(1, height), 0, 6408, 5121, (ByteBuffer)null);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064, 3553, this.textureID, 0);
      this.complete = this.checkStatus();
      if (this.complete) {
         EXTFramebufferObject.glBindFramebufferEXT(36160, this.frameBufferObject);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClearDepth(0.0);
         GL11.glViewport(0, 0, width, height);
         GL11.glMatrixMode(5889);
         GL11.glLoadIdentity();
         GL11.glOrtho(0.0, width, height, 0.0, 0.0, 1.0);
         GL11.glMatrixMode(5888);
         GL11.glEnable(2929);
         GL11.glDepthFunc(519);
         GL11.glEnable(3553);
         GL11.glEnable(3042);
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
      }

      this.glUnbind();
   }

   private boolean checkStatus() {
      int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
      if (status != 36053) {
         System.err.println("Could not create frame buffer");
         switch (status) {
            case 36054:
               System.err.println("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT");
               break;
            case 36055:
               System.err.println("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT");
            case 36056:
            default:
               break;
            case 36057:
               System.err.println("GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT");
               break;
            case 36058:
               System.err.println("GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT");
               break;
            case 36059:
               System.err.println("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT");
               break;
            case 36060:
               System.err.println("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT");
               break;
            case 36061:
               System.err.println("GL_FRAMEBUFFER_UNSUPPORTED_EXT");
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean isComplete() {
      return this.complete;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public int getColorBufferTextureID() {
      return this.textureID;
   }

   @Override
   public int getDepthBufferTextureID() {
      return 0;
   }

   @Override
   protected void glBind() {
      EXTFramebufferObject.glBindFramebufferEXT(36160, this.frameBufferObject);
   }

   @Override
   protected void glUnbind() {
      EXTFramebufferObject.glBindFramebufferEXT(36160, 0);
   }

   @Override
   public void dispose() {
      GL11.glDeleteTextures(this.textureID);
      EXTFramebufferObject.glDeleteFramebuffersEXT(this.frameBufferObject);
   }

   @Override
   public GameTexture getTextureAndDisposeFrameBuffer(String textureDebugName) {
      GL30.glDeleteFramebuffers(this.frameBufferObject);
      return GameTexture.fromTextureID(textureDebugName, this.textureID, this.width, this.height);
   }

   @Override
   public void bindTexture(int texturePos) {
      GL13.glActiveTexture(texturePos);
      GL11.glBindTexture(3553, this.textureID);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
   }
}
