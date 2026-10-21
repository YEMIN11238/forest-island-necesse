package necesse.gfx.shader;

import java.util.concurrent.atomic.AtomicInteger;
import necesse.gfx.drawOptions.texture.ShaderBind;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.shaderVariable.ShaderIntVariable;

public class SceneShader extends GameShader {
   public static final int MAX_MASKS = 4;

   public SceneShader() {
      super("vertScene", "fragScene");
      this.addVariable(new ShaderIntVariable("maskDebug", 0, 5));
   }

   public void passMaskOffset(int pos, int xOffset, int yOffset) {
      this.pass2i("mask" + (pos + 1) + "Offset", xOffset, yOffset);
   }

   public void passMaskSprite(int pos, float x1, float y1, float x2, float y2) {
      this.pass4f("mask" + (pos + 1) + "Sprite", x1, y1, x2, y2);
   }

   public void passMaskSize(int pos, int width, int height) {
      this.pass2i("mask" + (pos + 1) + "Size", width, height);
   }

   public void passMaskCount(int count) {
      this.pass1i("maskCount", count);
   }

   public int[] getOffset(int pos) {
      return this.get2i("mask" + (pos + 1) + "Offset");
   }

   public void passRecolor(GameTexture texture, int colorIndex) {
      if (texture != null) {
         texture.bindTexture(ShaderBind.toGlPos(1));
         this.pass1i("recolorIndex", colorIndex);
         this.pass2i("recolorTextureSize", texture.getWidth(), texture.getHeight());
      } else {
         this.pass1i("recolorIndex", -1);
      }
   }

   public void passOverlay(GameTexture texture, float startX, float startY, float endX, float endY) {
      texture.bindTexture(ShaderBind.toGlPos(2));
      this.pass1i("overlayTextureMode", 0);
      this.pass2f("overlayTextureStart", startX, startY);
      this.pass2f("overlayTextureSize", endX - startX, endY - startY);
   }

   public void stopOverlay() {
      this.pass1i("overlayTextureMode", -1);
   }

   @Override
   public void use() {
      super.use();
      this.pass1i("recolorLookupTexture", 1);
      this.pass1i("overlayTexture", 2);

      for (int i = 0; i < 4; i++) {
         this.pass1i("mask" + (i + 1) + "Texture", i + 3);
      }
   }

   @Override
   public void stop() {
      super.stop();
   }

   public ShaderState addMaskDebug(final int debug) {
      final AtomicInteger lastDebug = new AtomicInteger();
      return new ShaderState() {
         @Override
         public void use() {
            lastDebug.set(SceneShader.this.get1i("maskDebug"));
            SceneShader.this.pass1i("maskDebug", debug);
         }

         @Override
         public void stop() {
            SceneShader.this.pass1i("maskDebug", lastDebug.get());
         }
      };
   }
}
