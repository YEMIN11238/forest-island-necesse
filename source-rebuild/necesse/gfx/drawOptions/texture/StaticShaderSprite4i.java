package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite4i extends ShaderSpriteAbstract {
   protected int x;
   protected int y;
   protected int z;
   protected int w;

   public StaticShaderSprite4i(int pos, int x, int y, int z, int w) {
      super(pos);
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord4i(this.glPos, this.x, this.y, this.z, this.w);
   }

   @Override
   public void startTopRight() {
   }

   @Override
   public void startBotRight() {
   }

   @Override
   public void startBotLeft() {
   }
}
