package necesse.gfx.shader;

public class PixelScalingShader extends GameShader {
   public PixelScalingShader() {
      super("vertScene", "fragPixelScaling");
   }

   public void use(float smoothingFactor, float textureWidth, float textureHeight) {
      this.use();
      this.pass1f("smoothing_factor", smoothingFactor);
      this.pass2f("texture_pixel_size", 1.0F / textureWidth, 1.0F / textureHeight);
   }
}
