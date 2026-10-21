package necesse.engine.postProcessing;

import java.util.Iterator;
import java.util.LinkedList;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.postProcessing.effects.AbstractHeatDistortionEffect;
import necesse.engine.window.GameWindow;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.ShaderBind;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.shader.HeatDistortionShader;

public class PostProcessHeatDistortionStage extends PostProcessStage {
   protected LinkedList<PostProcessHeatDistortionStage.CachedFrameBuffer> frameBuffers = new LinkedList<>();
   protected LinkedList<AbstractHeatDistortionEffect> inScreenEffects = new LinkedList<>();
   public final HeatDistortionShader shader;

   public PostProcessHeatDistortionStage(GameWindow window, HeatDistortionShader shader) {
      super(window);
      this.shader = shader;
      this.updateFrameBufferSize();
   }

   @Override
   public boolean isEnabled() {
      return Settings.heatDistortionEffects;
   }

   @Override
   public GameFrameBuffer doPostProcessing(GameFrameBuffer inputBuffer) {
      GameCamera camera = GlobalData.getCurrentState().getCamera();
      if (camera == null) {
         return inputBuffer;
      }

      this.inScreenEffects.clear();

      for (AbstractHeatDistortionEffect effect : PostProcessingEffects.heatDistortEffects) {
         if (effect.isWithinScreen(this.window, camera)) {
            this.inScreenEffects.add(effect);
         }
      }

      if (this.frameBuffers.size() < this.inScreenEffects.size()) {
         while (this.frameBuffers.size() < this.inScreenEffects.size()) {
            this.frameBuffers.add(new PostProcessHeatDistortionStage.CachedFrameBuffer());
         }
      }

      Iterator<AbstractHeatDistortionEffect> effectsIterator = this.inScreenEffects.iterator();

      for (PostProcessHeatDistortionStage.CachedFrameBuffer cachedFrameBuffer : this.frameBuffers) {
         if (effectsIterator.hasNext()) {
            cachedFrameBuffer.currentEffect = effectsIterator.next();
            cachedFrameBuffer.update(true);
         } else {
            cachedFrameBuffer.currentEffect = null;
         }
      }

      while (this.frameBuffers.size() > this.inScreenEffects.size() && this.frameBuffers.getLast().getTimeSinceLastUsed() > 60000L) {
         this.frameBuffers.removeLast().dispose();
      }

      try {
         GameResources.noiseTexture.bindTexture(ShaderBind.toGlPos(1));
         this.shader.use();

         for (PostProcessHeatDistortionStage.CachedFrameBuffer cache : this.frameBuffers) {
            if (cache.currentEffect == null) {
               break;
            }

            try {
               cache.frameBuffer.bindFrameBuffer();
               cache.frameBuffer.clearColor();
               cache.currentEffect.setupShader(this.shader, camera);
               GameFrameBuffer.draw(inputBuffer.getColorBufferTextureID(), 0, 0, cache.frameBuffer.getWidth(), cache.frameBuffer.getHeight(), null, null);
            } finally {
               cache.frameBuffer.unbindFrameBuffer();
            }

            inputBuffer = cache.frameBuffer;
         }
      } finally {
         this.shader.stop();
      }

      return inputBuffer;
   }

   @Override
   public void updateFrameBufferSize() {
      for (PostProcessHeatDistortionStage.CachedFrameBuffer frameBuffer : this.frameBuffers) {
         frameBuffer.update(false);
      }
   }

   @Override
   public void dispose() {
      for (PostProcessHeatDistortionStage.CachedFrameBuffer frameBuffer : this.frameBuffers) {
         frameBuffer.dispose();
      }

      this.frameBuffers.clear();
   }

   protected class CachedFrameBuffer {
      public long lastUsedTime;
      public GameFrameBuffer frameBuffer;
      public AbstractHeatDistortionEffect currentEffect;

      public void update(boolean updateCachedTime) {
         if (updateCachedTime) {
            this.lastUsedTime = System.currentTimeMillis();
         }

         if (this.frameBuffer == null
            || this.frameBuffer.getWidth() != PostProcessHeatDistortionStage.this.window.getSceneWidth()
            || this.frameBuffer.getHeight() != PostProcessHeatDistortionStage.this.window.getSceneHeight()) {
            if (this.frameBuffer != null) {
               this.frameBuffer.dispose();
            }

            this.frameBuffer = PostProcessHeatDistortionStage.this.window
               .getNewFrameBuffer(PostProcessHeatDistortionStage.this.window.getSceneWidth(), PostProcessHeatDistortionStage.this.window.getSceneHeight());
         }
      }

      public long getTimeSinceLastUsed() {
         return System.currentTimeMillis() - this.lastUsedTime;
      }

      public void dispose() {
         if (this.frameBuffer != null) {
            this.frameBuffer.dispose();
         }
      }
   }
}
