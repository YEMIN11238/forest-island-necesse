package necesse.gfx.shader;

public class TextureOffsetShader extends GameShader {
   public TextureOffsetShader() {
      super("vertScene", "fragTextureOffset");
   }

   public void use(float xOffset, float yOffset) {
      this.use();
      this.pass1f("textureXOffset", xOffset);
      this.pass1f("textureYOffset", yOffset);
   }
}
