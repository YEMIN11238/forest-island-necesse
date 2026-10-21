package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite4f extends ShaderSpriteAbstract {
   protected float x;
   protected float y;
   protected float z;
   protected float w;

   public StaticShaderSprite4f(int pos, float x, float y, float z, float w) {
      super(pos);
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord4f(this.glPos, this.x, this.y, this.z, this.w);
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
