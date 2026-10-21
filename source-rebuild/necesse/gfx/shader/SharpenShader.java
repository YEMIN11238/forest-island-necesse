package necesse.gfx.shader;

public class SharpenShader extends GameShader {
   public SharpenShader() {
      super("vertScene", "fragSharpen");
   }

   public void use(int width, int height, float amount) {
      this.use();
      this.pass1i("renderWidth", width);
      this.pass1i("renderHeight", height);
      this.pass1f("amount", amount);
   }
}
