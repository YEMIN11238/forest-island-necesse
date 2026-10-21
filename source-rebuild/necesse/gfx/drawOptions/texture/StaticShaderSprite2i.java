package necesse.gfx.drawOptions.texture;

import org.lwjgl.opengl.GL13;

public class StaticShaderSprite2i extends ShaderSpriteAbstract {
   protected int x;
   protected int y;

   public StaticShaderSprite2i(int pos, int x, int y) {
      super(pos);
      this.x = x;
      this.y = y;
   }

   @Override
   public void startTopLeft() {
      GL13.glMultiTexCoord2i(this.glPos, this.x, this.y);
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
