package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite3i extends ShaderSpriteAbstract {
   protected int x;
   protected int y;
   protected int z;

   public StaticShaderSprite3i(int pos, int x, int y, int z) {
      super(pos);
      this.x = x;
      this.y = y;
      this.z = z;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord3i(this.glPos, this.x, this.y, this.z);
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
