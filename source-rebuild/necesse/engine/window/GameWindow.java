package necesse.engine.window;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import necesse.engine.GlobalData;
import necesse.engine.SceneColorSetting;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.platforms.Platform;
import necesse.engine.postProcessing.PostProcessHeatDistortionStage;
import necesse.engine.postProcessing.PostProcessShaderStage;
import necesse.engine.postProcessing.PostProcessShockwaveStage;
import necesse.engine.postProcessing.PostProcessStage;
import necesse.engine.state.State;
import necesse.engine.util.GameMath;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.gameTexture.GameTexture;

public abstract class GameWindow {
   public static float[] interfaceSizes = new float[]{1.0F, 1.1F, 1.2F, 1.3F, 1.4F, 1.5F, 1.6F, 1.7F, 1.8F, 1.9F, 2.0F};
   public static int minAdjustedInterfaceHeight = 1080;
   public static int minAdjustedInterfaceWidth = minAdjustedInterfaceHeight * 16 / 9;
   public static int expectedAdjustedInterfaceHeight = 1080;
   public static int expectedAdjustedInterfaceWidth = expectedAdjustedInterfaceHeight * 16 / 9;
   public static int minAdjustedSceneHeight = 1080;
   public static int minAdjustedSceneWidth = minAdjustedSceneHeight * 16 / 9;
   public static int expectedAdjustedSceneHeight = 1080;
   public static int expectedAdjustedSceneWidth = expectedAdjustedSceneHeight * 16 / 9;
   public static float minSceneSize = 1.0F;
   public static float maxSceneSize = 4.0F;
   public GameWindow.FBOType FBO_CAPABILITIES = null;
   private final Input input;
   protected int windowWidth;
   protected int windowHeight;
   protected int windowFrameWidth;
   protected int windowFrameHeight;
   protected boolean hasResized;
   protected GameFrameBuffer hudBuffer;
   protected float hudSize = 1.0F;
   private boolean hudSizeChanged = false;
   private final ArrayList<PostProcessStage> scenePostProcessing;
   private GameFrameBuffer sceneBuffer;
   private GameFrameBuffer sceneOverlayBuffer;
   protected float sceneSize = 1.0F;
   private boolean sceneSizeChanged = false;
   protected GameFrameBuffer[] applyDrawPool = new GameFrameBuffer[3];
   protected int applyDrawUse = 0;
   private final GameFrameBuffer windowBuffer;
   protected GameFrameBuffer currentBuffer;
   private float sceneRed = 1.0F;
   private float sceneGreen = 1.0F;
   private float sceneBlue = 1.0F;
   private float sceneDarkness;
   private int sceneDarknessMinViewDist = 0;
   private int sceneDarknessFadeDist = 1000;
   private boolean active;
   private boolean wasResizedThisFrame;

   public GameWindow() {
      this.scenePostProcessing = new ArrayList<>();
      this.input = new Input(this);
      this.windowBuffer = this.getNewFrameBuffer(this.getFrameWidth(), this.getFrameHeight());
      this.currentBuffer = this.windowBuffer;
   }

   public static float getClosestSize(float size, float[] sizes) {
      return IntStream.range(0, sizes.length).mapToObj(i -> sizes[i]).min(Comparator.comparingDouble(s -> Math.abs(size - s))).orElse(size);
   }

   public static float getAdjustedSize(int minWidth, int minHeight, int expectedWidth, int expectedHeight, int windowWidth, int windowHeight) {
      return windowHeight > minHeight && windowWidth > minWidth ? Math.min((float)windowHeight / expectedHeight, (float)windowWidth / expectedWidth) : 1.0F;
   }

   public void createWindow(boolean updateSize) {
      this.createWindow(null, updateSize);
   }

   public void createWindow(GameWindow parent, boolean updateSize) {
      if (parent != null) {
         this.hudSize = parent.hudSize;
         this.sceneSize = parent.sceneSize;
      }
   }

   public void initInput() {
      Platform.getInputManager().initializeInputSources(this);
   }

   public abstract void updateWindowSize();

   public void setupPostProcessing() {
      this.scenePostProcessing.forEach(PostProcessStage::dispose);
      this.scenePostProcessing.clear();
      this.scenePostProcessing.add(new PostProcessHeatDistortionStage(this, GameResources.heatDistortShader));
      this.scenePostProcessing.add(new PostProcessShaderStage(this, GameResources.heatDistortTestShader) {
         @Override
         public boolean isEnabled() {
            return super.isEnabled() && GlobalData.isDevMode() && GlobalData.debugCheatActive();
         }

         @Override
         protected void setShaderVariables() {
            this.shader.pass1i("drawX", GameWindow.this.getSceneWidth() / 2);
            this.shader.pass1i("drawY", GameWindow.this.getSceneHeight() / 2);
         }
      });
      this.scenePostProcessing.add(new PostProcessShaderStage(this, GameResources.debugColorShader));
      this.scenePostProcessing.add(new PostProcessShaderStage(this, GameResources.colorSettingShader) {
         @Override
         public boolean isEnabled() {
            return Settings.sceneColors != SceneColorSetting.Normal;
         }

         @Override
         protected void setShaderVariables() {
            super.setShaderVariables();
            Settings.sceneColors.shaderSetup.accept(GameResources.colorSettingShader);
         }
      });
      this.scenePostProcessing
         .add(
            new PostProcessShaderStage(this, GameResources.brightnessShader) {
               @Override
               protected void setShaderVariables() {
                  super.setShaderVariables();
                  GameResources.brightnessShader
                     .pass1f("brightness", Settings.brightness > 1.0F ? GameMath.lerp(Settings.brightness - 1.0F, 1.0F, 1.5F) : Settings.brightness);
               }
            }
         );
      this.scenePostProcessing.add(new PostProcessShaderStage(this, GameResources.windowColorShader) {
         @Override
         protected void setShaderVariables() {
            GameResources.windowColorShader.pass1f("red", GameWindow.this.sceneRed);
            GameResources.windowColorShader.pass1f("green", GameWindow.this.sceneGreen);
            GameResources.windowColorShader.pass1f("blue", GameWindow.this.sceneBlue);
         }
      });
      this.scenePostProcessing.add(new PostProcessShaderStage(this, GameResources.darknessShader) {
         @Override
         public boolean isEnabled() {
            return GameWindow.this.sceneDarkness > 0.0F;
         }

         @Override
         protected void setShaderVariables() {
            GameResources.darknessShader.pass1f("intensity", GameWindow.this.sceneDarkness);
            GameResources.darknessShader.pass1i("minViewDist", GameWindow.this.sceneDarknessMinViewDist);
            GameResources.darknessShader.pass1i("fadeDist", GameWindow.this.sceneDarknessFadeDist);
         }
      });
      this.scenePostProcessing.add(new PostProcessShockwaveStage(this, GameResources.shockwaveShader));
      this.scenePostProcessing.add(new PostProcessShaderStage(this, GameResources.shockwaveTestShader) {
         @Override
         public boolean isEnabled() {
            return super.isEnabled() && GlobalData.isDevMode() && GlobalData.debugCheatActive();
         }

         @Override
         protected void setShaderVariables() {
            this.shader.pass1i("drawX", GameWindow.this.getSceneWidth() / 2);
            this.shader.pass1i("drawY", GameWindow.this.getSceneHeight() / 2);
         }
      });
      this.scenePostProcessing
         .add(
            new PostProcessShaderStage(this, GameResources.pixelScalingShader) {
               @Override
               public void updateFrameBufferSize() {
                  if (this.frameBuffer == null
                     || this.frameBuffer.getWidth() != this.window.getFrameWidth()
                     || this.frameBuffer.getHeight() != this.window.getFrameHeight()) {
                     if (this.frameBuffer != null) {
                        this.frameBuffer.dispose();
                     }

                     this.frameBuffer = this.window.getNewFrameBuffer(this.window.getFrameWidth(), this.window.getFrameHeight());
                  }
               }

               @Override
               protected void setShaderVariables() {
                  super.setShaderVariables();
                  GameResources.pixelScalingShader.pass1f("smoothing_factor", 1.0F);
                  GameResources.pixelScalingShader.pass2f("texture_pixel_size", 1.0F / this.window.getSceneWidth(), 1.0F / this.window.getSceneHeight());
               }
            }
         );
   }

   public boolean tickResize() {
      boolean out = false;
      if (this.hasResized) {
         if (this.windowWidth > 1 && this.windowHeight > 1) {
            this.updateResize();
         }

         out = true;
      }

      if (this.sceneSizeChanged) {
         this.updateSceneResize();
         out = true;
      }

      if (this.hudSizeChanged) {
         this.updateHudResize();
         out = true;
      }

      return out;
   }

   private void updateSceneResize() {
      float sceneSizeMod = 1.0F / this.sceneSize;
      int width = (int)(this.getWidth() * sceneSizeMod);
      int height = (int)(this.getHeight() * sceneSizeMod);
      if (this.sceneBuffer == null || this.sceneBuffer.getWidth() != width || this.sceneBuffer.getHeight() != height) {
         if (this.sceneBuffer != null) {
            this.sceneBuffer.dispose();
         }

         this.sceneBuffer = this.getNewFrameBuffer(width, height);
         if (this.sceneOverlayBuffer != null) {
            this.sceneOverlayBuffer.dispose();
         }

         this.sceneOverlayBuffer = this.getNewFrameBuffer(this.sceneBuffer.getWidth(), this.sceneBuffer.getHeight());
         this.updateView();
      }

      this.scenePostProcessing.forEach(PostProcessStage::updateFrameBufferSize);
      this.sceneSizeChanged = false;
   }

   private void updateHudResize() {
      float hudSizeMod = 1.0F / this.hudSize;
      int width = (int)(this.getWidth() * hudSizeMod);
      int height = (int)(this.getHeight() * hudSizeMod);
      if (this.hudBuffer == null || this.hudBuffer.getWidth() != width || this.hudBuffer.getHeight() != height) {
         if (this.hudBuffer != null) {
            this.hudBuffer.dispose();
         }

         this.hudBuffer = this.getNewFrameBuffer(width, height);
         this.updateView();
      }

      this.hudSizeChanged = false;
   }

   private void updateFrameResize() {
      boolean changed = false;

      for (int i = 0; i < this.applyDrawPool.length; i++) {
         GameFrameBuffer current = this.applyDrawPool[i];
         if (current == null || current.getWidth() != this.getWidth() || current.getHeight() != this.getHeight()) {
            if (current != null) {
               current.dispose();
            }

            this.applyDrawPool[i] = this.getNewFrameBuffer(this.getWidth(), this.getHeight());
            changed = true;
         }
      }

      if (changed) {
         this.updateView();
      }
   }

   public void updateResize() {
      this.updateSceneResize();
      this.updateHudResize();
      this.updateFrameResize();
      this.hasResized = false;
   }

   public void updateSizeAndShow() {
      this.updateWindowSize();
      this.setVSync(Settings.vSyncEnabled);
      this.setSceneSize(Settings.sceneSize);
      this.setHudSize(Settings.interfaceSize);
      this.setIcon(GameWindowIcon.getIcon());
      this.initInput();
      this.show();
   }

   public abstract void updateView();

   public void updateHudSize() {
      float size = 1.0F;
      if (Settings.adjustInterfaceOnHighResolution) {
         size = getAdjustedSize(
            minAdjustedInterfaceWidth,
            minAdjustedInterfaceHeight,
            expectedAdjustedInterfaceWidth,
            expectedAdjustedInterfaceHeight,
            this.getFrameWidth(),
            this.getFrameHeight()
         );
      }

      this.setHudSize(size * Settings.interfaceSize);
      this.input.updateNextMousePos();
      this.input.submitNextMoveEvent();
      ControllerInput.submitNextRefreshFocusEvent();
   }

   public void updateSceneSize() {
      float size = 1.0F;
      if (Settings.adjustZoomOnHighResolution) {
         size = getAdjustedSize(
            minAdjustedSceneWidth, minAdjustedSceneHeight, expectedAdjustedSceneWidth, expectedAdjustedSceneHeight, this.getFrameWidth(), this.getFrameHeight()
         );
      }

      this.setSceneSize(size * Settings.sceneSize);
      this.input.updateNextMousePos();
      this.input.submitNextMoveEvent();
      ControllerInput.submitNextRefreshFocusEvent();
   }

   public abstract void update();

   public abstract void preloadUpdate();

   public abstract void makeCurrent();

   public void startHudDraw() {
      this.hudBuffer.bindFrameBuffer();
      this.hudBuffer.clearColor();
      this.hudBuffer.clearDepth();
   }

   public void endHudDraw() {
      this.hudBuffer.unbindFrameBuffer();
   }

   public abstract void renderHud(float var1);

   public void startSceneDraw() {
      this.sceneBuffer.bindFrameBuffer();
      this.sceneBuffer.clearColor();
      this.sceneBuffer.clearDepth();
   }

   public void endSceneDraw() {
      this.sceneBuffer.unbindFrameBuffer();
   }

   public void applyDraw(Consumer<GameFrameBuffer> drawLogic, Consumer<GameFrameBuffer> glModifyLogic, Consumer<GameFrameBuffer> glEndLogic) {
      GameFrameBuffer startBuffer = this.currentBuffer;
      this.applyDrawUse++;
      GameFrameBuffer useBuffer = this.applyDrawPool[this.applyDrawUse % this.applyDrawPool.length];

      try {
         useBuffer.bindFrameBuffer();
         useBuffer.clearColor();
         useBuffer.clearDepth();
         drawLogic.accept(useBuffer);
      } finally {
         try {
            startBuffer.bindFrameBuffer();
            Renderer.useShader(null);
            Runnable preDrawLogic = glModifyLogic == null ? null : () -> glModifyLogic.accept(useBuffer);
            Runnable postDrawLogic = glEndLogic == null ? null : () -> glEndLogic.accept(useBuffer);
            GameFrameBuffer.drawSimple(useBuffer.getColorBufferTextureID(), 0, 0, useBuffer.getWidth(), useBuffer.getHeight(), preDrawLogic, postDrawLogic);
         } finally {
            Renderer.stopShader(null);
         }
      }

      this.applyDrawUse--;
   }

   public void applyDraw(Runnable drawLogic, Runnable glModifyLogic, Runnable glEndLogic) {
      GameFrameBuffer startBuffer = this.currentBuffer;
      this.applyDrawUse++;
      GameFrameBuffer useBuffer = this.applyDrawPool[this.applyDrawUse % this.applyDrawPool.length];

      try {
         useBuffer.bindFrameBuffer();
         useBuffer.clearColor();
         useBuffer.clearDepth();
         drawLogic.run();
      } finally {
         try {
            startBuffer.bindFrameBuffer();
            Renderer.useShader(null);
            GameFrameBuffer.drawSimple(useBuffer.getColorBufferTextureID(), 0, 0, useBuffer.getWidth(), useBuffer.getHeight(), glModifyLogic, glEndLogic);
         } finally {
            Renderer.stopShader(null);
         }
      }

      this.applyDrawUse--;
   }

   public void startSceneOverlayDraw() {
      this.sceneOverlayBuffer.bindFrameBuffer();
      this.sceneOverlayBuffer.clearColor();
      this.sceneOverlayBuffer.clearDepth();
   }

   public void endSceneOverlayDraw() {
      this.sceneOverlayBuffer.unbindFrameBuffer();
   }

   public void renderScene(GameFrameBuffer buffer) {
      GameFrameBuffer finalScene = this.sceneBuffer;

      for (PostProcessStage postProcessStage : this.scenePostProcessing) {
         if (postProcessStage.isEnabled()) {
            finalScene = postProcessStage.doPostProcessing(finalScene);
         }
      }

      if (buffer != null) {
         buffer.bindFrameBuffer();
      }

      GameFrameBuffer.draw(finalScene.getColorBufferTextureID(), 0, 0, this.getFrameWidth(), this.getFrameHeight(), null, null);
      GameFrameBuffer.draw(this.sceneOverlayBuffer.getColorBufferTextureID(), 0, 0, this.getFrameWidth(), this.getFrameHeight(), null, null);
   }

   public abstract void show();

   public abstract void requestClose();

   public void destroy() {
      if (this.isCreated()) {
         for (GameFrameBuffer buffer : this.applyDrawPool) {
            if (buffer != null) {
               buffer.dispose();
            }
         }

         this.sceneBuffer.dispose();
         this.scenePostProcessing.forEach(PostProcessStage::dispose);
         this.sceneOverlayBuffer.dispose();
         this.hudBuffer.dispose();
         this.input.dispose();
      }
   }

   public abstract void printCapabilities(PrintStream var1);

   public abstract void requestFocus();

   public InputPosition mousePos() {
      return this.input == null ? InputPosition.dummyPos() : this.input.mousePos();
   }

   public void submitNextMoveEvent() {
      this.input.submitNextMoveEvent();
      ControllerInput.submitNextRefreshFocusEvent();
   }

   public void preGameTick(TickManager tickManager) {
      this.wasResizedThisFrame = Performance.record(tickManager, "other", () -> {
         int maxFPS = Settings.maxFPS;
         if (Settings.savePerformanceOnFocusLoss && !this.active) {
            maxFPS = 30;
         }

         if (tickManager.getMaxFPS() != maxFPS) {
            tickManager.setMaxFPS(maxFPS);
         }

         boolean focus = this.isFocused();
         if (focus != this.active) {
            this.active = focus;
         }

         return this.tickWindowResize(false);
      });
   }

   public boolean tickWindowResize(boolean forced) {
      if (!this.tickResize() && !forced) {
         return false;
      }

      if (Settings.adjustZoomOnHighResolution) {
         this.updateSceneSize();
      }

      if (Settings.adjustInterfaceOnHighResolution) {
         this.updateHudSize();
      }

      State state = GlobalData.getCurrentState();
      if (state != null) {
         state.onWindowResized(this);
      }

      return true;
   }

   public abstract boolean isCloseRequested();

   public abstract boolean isFocused();

   public abstract boolean isCreated();

   public boolean isKeyDown(int key) {
      return this.input == null ? false : this.input.isKeyDown(key);
   }

   public boolean wasResizedThisFrame() {
      return this.wasResizedThisFrame;
   }

   public abstract GameFrameBuffer getNewFrameBuffer(int var1, int var2);

   protected void setCurrentBuffer(GameFrameBuffer frameBuffer) {
      boolean updateView = this.currentBuffer != frameBuffer;
      if (frameBuffer == null) {
         this.currentBuffer = this.windowBuffer;
      } else {
         this.currentBuffer = frameBuffer;
      }

      if (updateView) {
         this.updateView();
      }
   }

   public Input getInput() {
      return this.input;
   }

   public abstract void setVSync(boolean var1);

   public int getWidth() {
      return this.windowWidth;
   }

   public int getHeight() {
      return this.windowHeight;
   }

   public int getFrameWidth() {
      return this.windowFrameWidth;
   }

   public int getFrameHeight() {
      return this.windowFrameHeight;
   }

   public int getSceneWidth() {
      return this.sceneBuffer == null ? 1 : this.sceneBuffer.getWidth();
   }

   public int getSceneHeight() {
      return this.sceneBuffer == null ? 1 : this.sceneBuffer.getHeight();
   }

   public int getHudWidth() {
      return this.hudBuffer == null ? 1 : this.hudBuffer.getWidth();
   }

   public int getHudHeight() {
      return this.hudBuffer == null ? 1 : this.hudBuffer.getHeight();
   }

   public void setSceneSize(float size) {
      size = GameMath.toDecimals(size, 2);
      if (size != this.sceneSize) {
         this.sceneSize = size;
         this.sceneSizeChanged = true;
      }
   }

   public void setHudSize(float size) {
      size = GameMath.toDecimals(size, 2);
      if (size != this.hudSize) {
         this.hudSize = size;
         this.hudSizeChanged = true;
      }
   }

   public GameFrameBuffer getCurrentBuffer() {
      return this.currentBuffer;
   }

   public void setSceneShade(float red, float green, float blue) {
      this.sceneRed = red;
      this.sceneGreen = green;
      this.sceneBlue = blue;
   }

   public void setSceneDarkness(float percent, int minViewDist, int fadeDist) {
      this.sceneDarkness = percent;
      this.sceneDarknessMinViewDist = minViewDist;
      this.sceneDarknessFadeDist = fadeDist;
   }

   public void setSceneShade(Color color) {
      this.setSceneShade(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
   }

   public abstract void requestAttention();

   public abstract void setIcon(GameWindowIcon var1);

   public abstract void setCursor(GameWindow.CURSOR var1);

   public abstract GameWindow.CURSOR getCursor();

   public abstract void setCursorMode(int var1);

   public abstract void putClipboard(String var1);

   public abstract String getClipboard();

   public GameWindow.FBOType getFBOCapabilities() {
      return this.FBO_CAPABILITIES;
   }

   public void putClipboardDefault(String str) {
      StringSelection s = new StringSelection(str);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
   }

   public String getClipboardDefault() {
      try {
         return (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
      } catch (UnsupportedFlavorException | IOException e) {
         System.err.println("Could not get clipboard string: " + e.getMessage());
         return "";
      }
   }

   public abstract void loadCursor(GameWindow.CURSOR var1, GameTexture var2, int var3);

   public abstract void destroyCursor(long var1);

   public enum CURSOR {
      DEFAULT(0),
      INVISIBLE(-1),
      INTERACT(1),
      GRAB_OFF(2),
      GRAB_ON(3),
      LOCK(4),
      UNLOCK(5),
      CARET(6, 4, 8),
      TRASH(7),
      ARROWS_DIAGONAL1(8, 7, 7),
      ARROWS_DIAGONAL2(9, 7, 7),
      ARROWS_VERTICAL(10, 4, 8),
      ARROWS_HORIZONTAL(11, 8, 4);

      private long cursor;
      public final int spriteX;
      public final int xOffset;
      public final int yOffset;
      private GameTexture texture;

      CURSOR(int spriteX, int xOffset, int yOffset) {
         this.spriteX = spriteX;
         this.xOffset = xOffset;
         this.yOffset = yOffset;
         this.cursor = 0L;
      }

      CURSOR(int spriteX) {
         this(spriteX, 0, 0);
      }

      public long getCursor() {
         return this.cursor;
      }

      public void setCursor(long cursor) {
         this.cursor = cursor;
      }

      public GameTexture getTexture() {
         return this.texture;
      }

      public void setTexture(GameTexture texture) {
         this.texture = texture;
      }
   }

   public enum FBOType {
      GL30,
      EXT;
   }
}
