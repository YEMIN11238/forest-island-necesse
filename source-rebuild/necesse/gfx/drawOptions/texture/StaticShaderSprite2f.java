package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite2f extends ShaderSpriteAbstract {
   protected float x;
   protected float y;

   public StaticShaderSprite2f(int pos, float x, float y) {
      super(pos);
      this.x = x;
      this.y = y;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord2f(this.glPos, this.x, this.y);
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
