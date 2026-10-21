package necesse.gfx;

import necesse.gfx.gameTexture.GameTexture;

public interface TextureBinder {
   TextureBinder NO_TEXTURE = GameTexture::unbindTexture;

   default void bindTexture() {
      this.bindTexture(33984);
   }

   void bindTexture(int var1);
}
