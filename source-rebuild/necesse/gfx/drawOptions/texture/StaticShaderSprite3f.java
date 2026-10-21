package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite3f extends ShaderSpriteAbstract {
   protected float x;
   protected float y;
   protected float z;

   public StaticShaderSprite3f(int pos, float x, float y, float z) {
      super(pos);
      this.x = x;
      this.y = y;
      this.z = z;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord3f(this.glPos, this.x, this.y, this.z);
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
