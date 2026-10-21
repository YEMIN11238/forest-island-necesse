package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite1i extends ShaderSpriteAbstract {
   protected int x;

   public StaticShaderSprite1i(int pos, int x) {
      super(pos);
      this.x = x;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord1i(this.glPos, this.x);
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
