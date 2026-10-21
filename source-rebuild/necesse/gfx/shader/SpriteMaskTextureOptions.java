package necesse.gfx.shader;

import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameTexture;

public class SpriteMaskTextureOptions implements SpriteMaskOptions {
   public final GameTexture texture;
   public final int xOffset;
   public final int yOffset;

   public SpriteMaskTextureOptions(GameTexture texture, int xOffset, int yOffset) {
      this.texture = texture;
      this.xOffset = xOffset;
      this.yOffset = yOffset;
   }

   @Override
   public TextureDrawOptionsEnd apply(TextureDrawOptionsEnd drawOptions, int texturePos) {
      return drawOptions.addShaderTextureFitCenterX(this.texture, texturePos);
   }

   @Override
   public void use(SceneShader shader, int pos) {
      shader.passMaskOffset(pos, this.xOffset, this.yOffset);
      shader.passMaskSprite(pos, 0.0F, 0.0F, 1.0F, 1.0F);
      if (this.texture != null) {
         shader.passMaskSize(pos, this.texture.getWidth(), this.texture.getHeight());
      } else {
         shader.passMaskSize(pos, 1, 1);
      }
   }
}
