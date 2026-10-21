package necesse.gfx.shader;

import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameSprite;

public class SpriteMaskSpriteOptions implements SpriteMaskOptions {
   public final GameSprite sprite;
   public final int xOffset;
   public final int yOffset;

   public SpriteMaskSpriteOptions(GameSprite sprite, int xOffset, int yOffset) {
      this.sprite = sprite;
      this.xOffset = xOffset;
      this.yOffset = yOffset;
   }

   @Override
   public TextureDrawOptionsEnd apply(TextureDrawOptionsEnd drawOptions, int texturePos) {
      return drawOptions.addShaderTextureFit(this.sprite, texturePos);
   }

   @Override
   public void use(SceneShader shader, int pos) {
      shader.passMaskOffset(pos, this.xOffset, this.yOffset);
      float spriteX1 = TextureDrawOptions.pixel(this.sprite.spriteX, this.sprite.spriteWidth, this.sprite.texture.getWidth());
      float spriteY1 = TextureDrawOptions.pixel(this.sprite.spriteY, this.sprite.spriteHeight, this.sprite.texture.getHeight());
      float spriteX2 = TextureDrawOptions.pixel(this.sprite.spriteX + 1, this.sprite.spriteWidth, this.sprite.texture.getWidth())
         - 1.0F / this.sprite.texture.getWidth();
      float spriteY2 = TextureDrawOptions.pixel(this.sprite.spriteY + 1, this.sprite.spriteHeight, this.sprite.texture.getHeight())
         - 1.0F / this.sprite.texture.getHeight();
      shader.passMaskSprite(pos, spriteX1, spriteY1, spriteX2, spriteY2);
      shader.passMaskSize(pos, this.sprite.texture.getWidth(), this.sprite.texture.getHeight());
   }
}
