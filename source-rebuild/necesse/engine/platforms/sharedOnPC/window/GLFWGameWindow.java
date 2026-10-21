package necesse.engine.platforms.sharedOnPC.window;

import java.awt.Dimension;
import java.awt.Point;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import necesse.engine.GameAuth;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.loading.GameNoticeException;
import necesse.engine.platforms.Platform;
import necesse.engine.util.PointerList;
import necesse.engine.window.DisplayMode;
import necesse.engine.window.GameWindow;
import necesse.engine.window.GameWindowCreationException;
import necesse.engine.window.GameWindowIcon;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.gameTexture.GameFrameBufferEXT;
import necesse.gfx.gameTexture.GameFrameBufferGL30;
import necesse.gfx.gameTexture.GameTexture;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

public class GLFWGameWindow extends GameWindow {
   private long glfwWindow = 0L;
   private boolean monitorNeedsHDRHack = false;
   private GameWindow.CURSOR cursor;

   @Override
   public void createWindow(GameWindow parent, boolean updateSize) {
      this.createWindow((GLFWGameWindow)parent, updateSize);
   }

   public void createWindow(GLFWGameWindow parent, boolean updateSize) {
      super.createWindow(parent, updateSize);
      GLFW.glfwDefaultWindowHints();
      this.setupWindowHints();
      GLFW.glfwWindowHint(131076, 0);
      String title = "Necesse v. 1.3.3";
      long authentication = GameAuth.getAuthentication();
      if (authentication != 0L) {
         title = title + " : Auth " + authentication;
      }

      this.glfwWindow = GLFW.glfwCreateWindow(1280, 720, title, 0L, parent != null ? parent.glfwWindow : 0L);
      if (this.glfwWindow == 0L) {
         Renderer.queryGLError("windowCreation");
         throw new GameWindowCreationException("Failed to create the GLFW window");
      }

      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer width = stack.mallocInt(1);
         IntBuffer height = stack.mallocInt(1);
         GLFW.glfwGetWindowSize(this.glfwWindow, width, height);
         this.windowWidth = Math.max(width.get(0), 1);
         this.windowHeight = Math.max(height.get(0), 1);
         IntBuffer frameWidth = stack.mallocInt(1);
         IntBuffer frameHeight = stack.mallocInt(1);
         GLFW.glfwGetFramebufferSize(this.glfwWindow, frameWidth, frameHeight);
         this.windowFrameWidth = Math.max(frameWidth.get(0), 1);
         this.windowFrameHeight = Math.max(frameHeight.get(0), 1);
      } catch (Throwable var12) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var11) {
               var12.addSuppressed(var11);
            }
         }

         throw var12;
      }

      if (stack != null) {
         stack.close();
      }

      GLFW.glfwSetWindowSizeCallback(this.glfwWindow, (win, widthx, heightx) -> {
         this.windowWidth = Math.max(widthx, 1);
         this.windowHeight = Math.max(heightx, 1);
         this.hasResized = true;
      });
      GLFW.glfwSetFramebufferSizeCallback(this.glfwWindow, (win, widthx, heightx) -> {
         this.windowFrameWidth = Math.max(widthx, 1);
         this.windowFrameHeight = Math.max(heightx, 1);
         this.hasResized = true;
      });
      GLFW.glfwSetWindowFocusCallback(this.glfwWindow, (window, hasFocus) -> {
         if (hasFocus) {
            this.performHDRHackIfNeeded();
         }
      });
      this.makeCurrent();
      GL.createCapabilities();
      if (GL.getCapabilities().OpenGL30) {
         this.FBO_CAPABILITIES = GameWindow.FBOType.GL30;
      } else if (GL.getCapabilities().GL_EXT_framebuffer_object) {
         this.FBO_CAPABILITIES = GameWindow.FBOType.EXT;
      } else {
         GameLog.warn.println("Could not find framebuffer capabilities, some functions may not work.");
      }

      Renderer.queryGLError("PostWindowGL");
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClearDepth(0.0);
      if (updateSize) {
         this.updateWindowSize();
      } else {
         this.updateResize();
      }

      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthFunc(519);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.setVSync(Settings.vSyncEnabled);
      this.monitorNeedsHDRHack = Platform.getWindowManager().doesMonitorNeedHDRHack();
   }

   private void setupWindowHints() {
      org.lwjgl.system.Platform platform = org.lwjgl.system.Platform.get();
      if (platform != org.lwjgl.system.Platform.MACOSX) {
         switch (Settings.displayMode) {
            case Borderless:
               GLFW.glfwWindowHint(131077, 0);
               GLFW.glfwWindowHint(131075, 0);
               break;
            case Fullscreen:
               GLFW.glfwWindowHint(131077, 1);
               GLFW.glfwWindowHint(131075, 0);
               break;
            case Windowed:
               GLFW.glfwWindowHint(131077, 1);
               GLFW.glfwWindowHint(131075, 1);
         }
      } else {
         GLFW.glfwWindowHint(131075, 1);
         GLFW.glfwWindowHint(131077, 1);
      }
   }

   private void performHDRHackIfNeeded() {
      if (this.monitorNeedsHDRHack) {
         MemoryStack stack = MemoryStack.stackPush();

         label56: {
            try {
               long monitor = GLFW.glfwGetWindowMonitor(this.glfwWindow);
               if (monitor == 0L) {
                  break label56;
               }

               GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
               int refreshRate = vidMode != null ? vidMode.refreshRate() : 60;
               IntBuffer width = stack.mallocInt(1);
               IntBuffer height = stack.mallocInt(1);
               GLFW.glfwGetWindowSize(this.glfwWindow, width, height);
               GLFW.glfwSetWindowMonitor(this.glfwWindow, monitor, 0, 0, width.get(0), height.get(0), refreshRate + 1);
               GLFW.glfwSetWindowMonitor(this.glfwWindow, monitor, 0, 0, width.get(0), height.get(0), 0);
               GLFW.glfwSetWindowMonitor(this.glfwWindow, monitor, 0, 0, width.get(0), height.get(0), refreshRate);
            } catch (Throwable var9) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (stack != null) {
               stack.close();
            }

            return;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   @Override
   public void updateWindowSize() {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         int monitorIndex = Settings.monitor;
         if (GameLaunch.launchMonitor >= 0) {
            monitorIndex = GameLaunch.launchMonitor;
         }

         long monitor = WindowManager.getMonitor(monitorIndex);
         if (monitor == 0L) {
            System.out.println("Could not find monitor from settings, falling back to primary monitor");
            monitor = GLFW.glfwGetPrimaryMonitor();
         }

         if (monitor == 0L) {
            throw new GameNoticeException("misc", "nomonitor");
         }

         GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
         if (vidMode == null) {
            throw new NullPointerException("Could not find monitor video mode");
         }

         int width = Settings.displaySize == null ? vidMode.width() : Settings.displaySize.width;
         int height = Settings.displaySize == null ? vidMode.height() : Settings.displaySize.height;
         IntBuffer x = stack.mallocInt(1);
         IntBuffer y = stack.mallocInt(1);
         GLFW.glfwGetMonitorPos(monitor, x, y);
         DisplayMode modeToApply = Settings.displayMode;
         if (modeToApply == DisplayMode.Borderless && this.monitorNeedsHDRHack) {
            modeToApply = DisplayMode.Fullscreen;
         }

         Point windowedPos = new Point(x.get(0) + (vidMode.width() - width) / 2, y.get(0) + (vidMode.height() - height) / 2);
         if (GameLaunch.launchWindowPosition != null) {
            int decorationSize = 30;
            modeToApply = DisplayMode.Windowed;
            Dimension launchWindowSize = GameLaunch.getLaunchWindowSize(vidMode.width(), vidMode.height());
            width = launchWindowSize.width;
            height = launchWindowSize.height - decorationSize;
            windowedPos = new Point(
               x.get(0) + GameLaunch.launchWindowPosition.x * width, y.get(0) + GameLaunch.launchWindowPosition.y * (height + decorationSize) + decorationSize
            );
         }

         if (org.lwjgl.system.Platform.get() != org.lwjgl.system.Platform.MACOSX) {
            switch (modeToApply) {
               case Borderless:
                  GLFW.glfwSetWindowMonitor(this.glfwWindow, 0L, x.get(0), y.get(0), vidMode.width(), vidMode.height(), vidMode.refreshRate());
                  GLFW.glfwSetWindowPos(this.glfwWindow, x.get(0), y.get(0));
                  break;
               case Fullscreen:
                  if (Platform.getWindowManager().doesMonitorNeedHDRHack()) {
                     GLFW.glfwSetWindowMonitor(this.glfwWindow, monitor, 0, 0, width, height, vidMode.refreshRate() + 1);
                     GLFW.glfwSetWindowMonitor(this.glfwWindow, monitor, 0, 0, width, height, 0);
                  }

                  GLFW.glfwSetWindowMonitor(this.glfwWindow, monitor, 0, 0, width, height, vidMode.refreshRate());
                  this.requestFocus();
                  break;
               case Windowed:
                  GLFW.glfwSetWindowMonitor(this.glfwWindow, 0L, windowedPos.x, windowedPos.y, width, height, vidMode.refreshRate());
                  GLFW.glfwSetWindowPos(this.glfwWindow, windowedPos.x, windowedPos.y);
            }
         } else {
            GLFW.glfwSetWindowMonitor(
               this.glfwWindow, 0L, x.get(0) + (vidMode.width() - width) / 2, y.get(0) + (vidMode.height() - height) / 2, width, height, vidMode.refreshRate()
            );
         }
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      this.updateResize();
   }

   @Override
   public void updateView() {
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      GL11.glOrtho(0.0, this.currentBuffer.getWidth(), this.currentBuffer.getHeight(), 0.0, 0.0, 1.0);
      GL11.glMatrixMode(5888);
      GL11.glViewport(0, 0, this.currentBuffer.getWidth(), this.currentBuffer.getHeight());
   }

   @Override
   public void update() {
      GLFW.glfwSwapBuffers(this.glfwWindow);
   }

   @Override
   public void preloadUpdate() {
      GLFW.glfwPollEvents();
      GLFW.glfwSwapBuffers(this.glfwWindow);
   }

   @Override
   public void makeCurrent() {
      GLFW.glfwMakeContextCurrent(this.glfwWindow);
   }

   @Override
   public void renderHud(float alpha) {
      if (GameResources.sharpenShader != null && Settings.sharpenInterface) {
         try {
            GameResources.sharpenShader.use(this.getFrameWidth(), this.getFrameHeight(), this.hudSize - 1.0F);
            GameFrameBuffer.draw(
               this.hudBuffer.getColorBufferTextureID(),
               0,
               0,
               this.getFrameWidth(),
               this.getFrameHeight(),
               () -> GL11.glColor4f(alpha, alpha, alpha, alpha),
               null
            );
         } catch (Exception e) {
            GameResources.sharpenShader.stop();
         }
      } else {
         GameFrameBuffer.draw(
            this.hudBuffer.getColorBufferTextureID(), 0, 0, this.getFrameWidth(), this.getFrameHeight(), () -> GL11.glColor4f(alpha, alpha, alpha, alpha), null
         );
      }
   }

   @Override
   public void show() {
      GLFW.glfwShowWindow(this.glfwWindow);
   }

   @Override
   public void requestClose() {
      GLFW.glfwSetWindowShouldClose(this.glfwWindow, true);
   }

   @Override
   public void destroy() {
      if (this.isCreated()) {
         super.destroy();
         Callbacks.glfwFreeCallbacks(this.glfwWindow);
         GLFW.glfwDestroyWindow(this.glfwWindow);
         this.glfwWindow = 0L;
      }
   }

   @Override
   public void printCapabilities(PrintStream printStream) {
      GLCapabilities capabilities = GL.getCapabilities();
      Class<? extends GLCapabilities> capClass = (Class<? extends GLCapabilities>)capabilities.getClass();
      int maxNameLength = 0;

      for (Field field : capClass.getFields()) {
         maxNameLength = Math.max(maxNameLength, field.getName().length());
      }

      for (Field field : capClass.getFields()) {
         StringBuilder builder = new StringBuilder().append(field.getName());
         int spaces = maxNameLength - field.getName().length();

         for (int i = 0; i < spaces; i++) {
            builder.append(".");
         }

         builder.append(": ");

         Object o;
         try {
            o = field.get(capabilities);
            if (o instanceof Long) {
               o = Long.toHexString((Long)o);
            } else if (o instanceof Integer) {
               o = Integer.toHexString((Integer)o);
            } else if (o instanceof Short) {
               o = Integer.toHexString((Short)o);
            } else if (o instanceof Byte) {
               o = Integer.toHexString((Byte)o);
            }
         } catch (IllegalAccessException e) {
            o = "ERR";
         }

         builder.append(o);
         printStream.println(builder);
      }
   }

   @Override
   public void requestFocus() {
      GLFW.glfwFocusWindow(this.glfwWindow);
   }

   @Override
   public boolean isCloseRequested() {
      return GLFW.glfwWindowShouldClose(this.glfwWindow);
   }

   @Override
   public boolean isFocused() {
      return GLFW.glfwGetWindowAttrib(this.glfwWindow, 131073) == 1;
   }

   @Override
   public boolean isCreated() {
      return this.glfwWindow != 0L;
   }

   @Override
   public GameFrameBuffer getNewFrameBuffer(int width, int height) {
      if (width == 0 || height == 0) {
         return new GLFWGameWindow.GlfwWindowFrameBuffer(x$0 -> this.setCurrentBuffer(x$0));
      }

      if (this.FBO_CAPABILITIES == GameWindow.FBOType.EXT) {
         return new GameFrameBufferEXT(x$0 -> this.setCurrentBuffer(x$0), width, height);
      }

      Renderer.queryGLError("PostNewEXTFrameBuffer");
      GameFrameBufferGL30 buffer = new GameFrameBufferGL30(x$0 -> this.setCurrentBuffer(x$0), width, height);
      int error = Renderer.queryGLError(null);
      if (error != 0) {
         System.out.println("Detected error trying to create GL30 frame buffers, trying EXT.");
         Renderer.printGLError(null, error);

         try {
            GameFrameBufferEXT extBuffer = new GameFrameBufferEXT(x$0 -> this.setCurrentBuffer(x$0), width, height);
            error = Renderer.queryGLError(null);
            if (error != 0) {
               System.out.println("Could not create EXT frame buffer either, some things may not work correctly :(");
               Renderer.printGLError(null, error);
               return buffer;
            }

            this.FBO_CAPABILITIES = GameWindow.FBOType.EXT;
            return extBuffer;
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      return buffer;
   }

   @Override
   public void setVSync(boolean enable) {
      GLFW.glfwSwapInterval(enable ? 1 : 0);
   }

   @Override
   public void requestAttention() {
      GLFW.glfwRequestWindowAttention(this.glfwWindow);
   }

   @Override
   public void setIcon(GameWindowIcon gameIcon) {
      if (org.lwjgl.system.Platform.get() != org.lwjgl.system.Platform.MACOSX) {
         Buffer buffer = GLFWImage.malloc(4);

         try {
            PointerList pointerList = new PointerList();
            pointerList.add(bufferAndScaleIcon(buffer, 0, gameIcon, 16));
            pointerList.add(bufferAndScaleIcon(buffer, 1, gameIcon, 32));
            pointerList.add(bufferAndScaleIcon(buffer, 2, gameIcon, 64));
            pointerList.add(bufferAndScaleIcon(buffer, 3, gameIcon, 128));
            GLFW.glfwSetWindowIcon(this.glfwWindow, buffer);
            pointerList.freeAll();
         } catch (Throwable var6) {
            if (buffer != null) {
               try {
                  buffer.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (buffer != null) {
            buffer.close();
         }
      }
   }

   public static ByteBuffer bufferAndScaleIcon(Buffer buffer, int position, GameWindowIcon icon, int resolution) {
      ByteBuffer image = GameWindowIcon.scaleIcon(icon, resolution);
      buffer.position(position).width(resolution).height(resolution).pixels(image);
      return image;
   }

   @Override
   public void setCursor(GameWindow.CURSOR cursor) {
      this.cursor = cursor;
      long cursorAddress = cursor.getCursor();
      GLFW.glfwSetCursor(this.glfwWindow, cursorAddress);
   }

   @Override
   public GameWindow.CURSOR getCursor() {
      return this.cursor;
   }

   @Override
   public void setCursorMode(int mode) {
      GLFW.glfwSetInputMode(this.glfwWindow, 208897, mode);
   }

   @Override
   public void putClipboard(String str) {
      GLFW.glfwSetClipboardString(this.glfwWindow, str);
   }

   @Override
   public String getClipboard() {
      return GLFWGameError.tryGLFWError(new GLFWGameError.Supplier<String>(65545) {
         public String run() {
            return GLFW.glfwGetClipboardString(GLFWGameWindow.this.glfwWindow);
         }

         public String onCatch(GLFWGameError error) {
            return error.errorName;
         }
      });
   }

   @Override
   public void loadCursor(GameWindow.CURSOR cursor, GameTexture texture, int size) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         GLFWImage cursorImage = GLFWImage.malloc(stack);
         int xOffset = 0;
         int yOffset = 0;
         ByteBuffer buffer;
         int resolution;
         if (cursor.spriteX < 0) {
            buffer = (new GameTexture("cursorBuffer", 1, 1)).buffer;
            resolution = 1;
         } else {
            resolution = 32;
            xOffset = cursor.xOffset;
            yOffset = cursor.yOffset;
            new GameTexture(texture, cursor.spriteX, 0, resolution);
            GameTexture lastCursor = new GameTexture(texture, cursor.spriteX, 0, resolution);
            float zoom = Renderer.getCursorSizeZoom(size);
            GameTexture sprite;
            if (zoom != 1.0F) {
               resolution = (int)(resolution * zoom);
               xOffset = (int)(xOffset * zoom);
               yOffset = (int)(yOffset * zoom);
               sprite = lastCursor.resize(resolution, resolution);
            } else {
               sprite = lastCursor;
            }

            buffer = sprite.buffer;
            cursor.setTexture(lastCursor.clamp(1).makeFinal());
         }

         buffer.position(0);
         cursorImage.width(resolution).height(resolution).pixels(buffer);
         long lastCursor = cursor.getCursor();
         if (lastCursor != 0L) {
            this.destroyCursor(lastCursor);
         }

         cursor.setCursor(GLFW.glfwCreateCursor(cursorImage, xOffset, yOffset));
      } catch (Throwable var14) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }
         }

         throw var14;
      }

      if (stack != null) {
         stack.close();
      }
   }

   @Override
   public void destroyCursor(long cursor) {
      GLFW.glfwDestroyCursor(cursor);
   }

   public long getGlfwWindow() {
      return this.glfwWindow;
   }

   private class GlfwWindowFrameBuffer extends GameFrameBuffer {
      public GlfwWindowFrameBuffer(Consumer<GameFrameBuffer> binder) {
         super(binder);
      }

      @Override
      public boolean isComplete() {
         return true;
      }

      @Override
      public int getWidth() {
         return GLFWGameWindow.this.getFrameWidth();
      }

      @Override
      public int getHeight() {
         return GLFWGameWindow.this.getFrameHeight();
      }

      @Override
      public int getColorBufferTextureID() {
         return 0;
      }

      @Override
      public int getDepthBufferTextureID() {
         return 0;
      }

      @Override
      public void glBind() {
         if (GLFWGameWindow.this.FBO_CAPABILITIES == GameWindow.FBOType.EXT) {
            EXTFramebufferObject.glBindFramebufferEXT(36160, 0);
         } else {
            GL30.glBindFramebuffer(36160, 0);
         }
      }

      @Override
      public void glUnbind() {
         this.glBind();
      }

      @Override
      public void dispose() {
      }

      @Override
      public GameTexture getTextureAndDisposeFrameBuffer(String textureDebugName) {
         throw new UnsupportedOperationException("Cannot get texture from a window frame buffer");
      }

      @Override
      public void bindTexture(int texturePos) {
         throw new UnsupportedOperationException("Cannot bind a window frame buffer as a texture");
      }
   }
}
