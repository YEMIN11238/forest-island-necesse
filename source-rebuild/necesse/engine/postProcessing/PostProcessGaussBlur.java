package necesse.engine.postProcessing;

import necesse.engine.window.GameWindow;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameFrameBuffer;

public class PostProcessGaussBlur extends PostProcessStage {
   private float size;
   private boolean sizeChanged;
   private GameFrameBuffer horiBuffer;
   private GameFrameBuffer vertBuffer;

   public PostProcessGaussBlur(GameWindow window, float size) {
      super(window);
      this.size = size;
      this.horiBuffer = window.getNewFrameBuffer((int)(window.getSceneWidth() * size), (int)(window.getSceneHeight() * size));
      this.vertBuffer = window.getNewFrameBuffer((int)(window.getSceneWidth() * size), (int)(window.getSceneHeight() * size));
   }

   public void setSize(float size) {
      if (this.size != size) {
         this.size = size;
         this.sizeChanged = true;
      }
   }

   @Override
   public GameFrameBuffer doPostProcessing(GameFrameBuffer inputBuffer) {
      if (!this.isEnabled()) {
         return inputBuffer;
      }

      if (this.sizeChanged) {
         int width = (int)(this.window.getSceneWidth() * this.size);
         int height = (int)(this.window.getSceneHeight() * this.size);
         if (this.horiBuffer.getWidth() != width || this.horiBuffer.getHeight() != height) {
            this.horiBuffer.dispose();
            this.horiBuffer = this.window.getNewFrameBuffer(width, height);
            this.vertBuffer.dispose();
            this.vertBuffer = this.window.getNewFrameBuffer(width, height);
         }

         this.sizeChanged = false;
      }

      try {
         this.horiBuffer.bindFrameBuffer();
         this.horiBuffer.clearColor();
         GameResources.horizontalGaussShader.use();
         GameResources.horizontalGaussShader.pass1f("pixelSize", 1.0F / this.horiBuffer.getWidth());
         GameFrameBuffer.draw(inputBuffer.getColorBufferTextureID(), 0, 0, this.horiBuffer.getWidth(), this.horiBuffer.getHeight(), null, null);
      } finally {
         GameResources.horizontalGaussShader.stop();
         this.horiBuffer.unbindFrameBuffer();
      }

      try {
         this.vertBuffer.bindFrameBuffer();
         this.vertBuffer.clearColor();
         GameResources.verticalGaussShader.use();
         GameResources.verticalGaussShader.pass1f("pixelSize", 1.0F / this.vertBuffer.getHeight());
         GameFrameBuffer.draw(this.horiBuffer.getColorBufferTextureID(), 0, 0, this.vertBuffer.getWidth(), this.vertBuffer.getHeight(), null, null);
      } finally {
         GameResources.verticalGaussShader.stop();
         this.vertBuffer.unbindFrameBuffer();
      }

      return this.vertBuffer;
   }

   @Override
   public void updateFrameBufferSize() {
      this.sizeChanged = true;
   }

   @Override
   public void dispose() {
      this.horiBuffer.dispose();
      this.vertBuffer.dispose();
   }
}
