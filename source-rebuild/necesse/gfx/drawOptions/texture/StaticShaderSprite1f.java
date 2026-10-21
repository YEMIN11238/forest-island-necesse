package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite1f extends ShaderSpriteAbstract {
   protected float x;

   public StaticShaderSprite1f(int pos, float x) {
      super(pos);
      this.x = x;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord1f(this.glPos, this.x);
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
