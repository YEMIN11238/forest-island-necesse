package necesse.engine.platforms.sharedOnPC.window;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JFrame;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.GameSystemInfo;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.window.GameWindow;
import necesse.engine.window.GameWindowCreationException;
import necesse.engine.window.WindowManager;
import necesse.reports.NoticeJFrame;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVidMode.Buffer;
import org.lwjgl.system.Configuration;
import oshi.hardware.Display;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.EdidUtil;

public class GLFWWindowManager extends WindowManager {
   private boolean glfwLoaded = false;
   private GLFWErrorCallback errorCallback = null;
   private GLFWWindowManager.WindowError windowError;
   private boolean monitorNeedsHDRHack = false;

   private void readMonitorHDRStatusOnWindows() {
      this.monitorNeedsHDRHack = false;

      try {
         if (PlatformManager.getPlatform().getOperatingSystemFamily() != Platform.OperatingSystemFamily.Windows) {
            return;
         }

         HardwareAbstractionLayer hardware = GameSystemInfo.getHardware();
         if (hardware != null) {
            try {
               if (hardware.getGraphicsCards().stream().noneMatch(x -> x.getVendor().toLowerCase().contains("nvidia"))) {
                  return;
               }
            } catch (NoClassDefFoundError e) {
               return;
            }
         }

         String[] keys;
         try {
            keys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, "SYSTEM\\CurrentControlSet\\Control\\GraphicsDrivers\\MonitorDataStore\\");
         } catch (Exception e) {
            return;
         }

         for (Display display : hardware.getDisplays()) {
            byte[] edid = display.getEdid();
            String temp = String.format("%8s%8s", Integer.toBinaryString(edid[8] & 255), Integer.toBinaryString(edid[9] & 255)).replace(' ', '0');
            String manId = String.format(
                  "%s%s%s",
                  (char)(64 + Integer.parseInt(temp.substring(1, 6), 2)),
                  (char)(64 + Integer.parseInt(temp.substring(6, 11), 2)),
                  (char)(64 + Integer.parseInt(temp.substring(11, 16), 2))
               )
               .replace("@", "");
            String productId = EdidUtil.getProductID(edid).toUpperCase();
            String path = Arrays.stream(keys).filter(x -> x.startsWith(manId + productId)).findFirst().orElse(null);
            if (path != null) {
               path = "SYSTEM\\CurrentControlSet\\Control\\GraphicsDrivers\\MonitorDataStore\\" + path;

               try {
                  if (Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, path, "HDREnabled") > 0) {
                     this.monitorNeedsHDRHack = true;
                  }
               } catch (Exception e) {
                  try {
                     if (Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, path, "AdvancedColorEnabled") > 0) {
                        this.monitorNeedsHDRHack = true;
                     }
                  } catch (Exception var13) {
                  }
               }

               if (this.monitorNeedsHDRHack) {
                  GameLog.out
                     .println(
                        "Detected HDR monitor running on Nvidia GPU. Game will use fullscreen instead of borderless and perform HDR hack. This is due to a bug with OpenGL and Nvidia on Windows."
                     );
                  return;
               }
            }
         }
      } catch (Exception e) {
         this.monitorNeedsHDRHack = false;
      }
   }

   @Override
   public long[] getMonitors() {
      PointerBuffer pointers = GLFW.glfwGetMonitors();
      if (pointers == null) {
         return new long[0];
      }

      long primary = GLFW.glfwGetPrimaryMonitor();
      long[] monitors = new long[pointers.remaining()];
      int i = 0;
      if (primary != 0L && monitors.length > 0) {
         monitors[i++] = primary;
      }

      while (pointers.hasRemaining() && i < monitors.length) {
         long next = pointers.get();
         if (next != primary) {
            monitors[i++] = next;
         }
      }

      return monitors;
   }

   @Override
   public boolean initialize() {
      boolean debugStartup = GameLaunch.launchOptions.containsKey("debugstartup");
      if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.MACOSX) {
         JFrame jFrame = new JFrame();
         jFrame.dispose();
         Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
      } else if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.LINUX) {
         String w = System.getenv("WAYLAND_DISPLAY");
         String x = System.getenv("XDG_SESSION_TYPE");
         if (w != null || "wayland".equalsIgnoreCase(x)) {
            GLFW.glfwInitHint(327683, 393220);
         }
      }

      this.errorCallback = new GLFWErrorCallback() {
         @Override
         public void invoke(int error, long description) {
            GLFWGameError.throwError(error, description);
         }
      };
      if (debugStartup) {
         System.out.println("STARTUP: Constructed GLFW error callback");
      }

      this.errorCallback.set();
      if (debugStartup) {
         System.out.println("STARTUP: Set GLFW error callback");
      }

      if (!GLFW.glfwInit()) {
         throw new IllegalStateException("Unable to initialize GLFW");
      }

      if (debugStartup) {
         System.out.println("STARTUP: Initialized GLFW");
      }

      this.glfwLoaded = true;
      this.windowError = GLFWGameError.tryGLFWError(new GLFWGameError.Supplier<GLFWWindowManager.WindowError>(65542) {
         public GLFWWindowManager.WindowError run() {
            try {
               GLFWGameWindow window = new GLFWGameWindow();
               window.createWindow(false);
               window.requestFocus();
               return new GLFWWindowManager.WindowError(window);
            } catch (GameWindowCreationException e) {
               return new GLFWWindowManager.WindowError(Localization.translate("misc", "openglfailed"));
            }
         }

         public GLFWWindowManager.WindowError onCatch(GLFWGameError error) {
            error.print(System.err);
            return new GLFWWindowManager.WindowError(Localization.translate("misc", "openglfailed"));
         }
      });
      if (this.windowError.error != null) {
         if (debugStartup) {
            System.out.println("STARTUP: Window creation error: " + this.windowError.error);
         }

         NoticeJFrame noticeFrame = new NoticeJFrame(400, this.windowError.error, "Close");
         noticeFrame.setVisible(true);
         noticeFrame.requestFocus();
         return false;
      } else {
         if (debugStartup) {
            System.out.println("STARTUP: Successfully created window");
         }

         currentWindow = this.windowError.window;
         GameSystemInfo.printGraphicsInfo(GameLog.file);
         if (debugStartup) {
            System.out.println("STARTUP: Printed graphics info");
         }

         this.readMonitorHDRStatusOnWindows();
         return super.initialize();
      }
   }

   @Override
   public GameWindow createNewWindow(boolean setAsCurrent) {
      GLFWGameWindow newWindow = new GLFWGameWindow();
      if (setAsCurrent) {
         currentWindow = newWindow;
      }

      return newWindow;
   }

   @Override
   public void dispose() {
      if (this.glfwLoaded) {
         GLFW.glfwTerminate();
      }

      if (this.errorCallback != null) {
         this.errorCallback.free();
      }
   }

   @Override
   public Dimension[] getVideoModes(long monitor) {
      Buffer modes = GLFW.glfwGetVideoModes(monitor);
      if (modes == null) {
         return new Dimension[]{new Dimension(WindowManager.getWindow().getWidth(), WindowManager.getWindow().getHeight())};
      }

      ArrayList<Dimension> sizes = new ArrayList<>();

      for (GLFWVidMode mode : modes) {
         Dimension dim = new Dimension(mode.width(), mode.height());
         if (!sizes.contains(dim)) {
            sizes.add(dim);
         }
      }

      sizes.sort((p1, p2) -> p1.height == p2.height ? Integer.compare(p1.width, p2.width) : Integer.compare(p1.height, p2.height));
      return sizes.toArray(new Dimension[0]);
   }

   @Override
   public Dimension getVideoMode(long monitor) {
      GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
      if (vidMode == null) {
         throw new NullPointerException("Could not find VidMode for monitor: " + monitor);
      } else {
         return new Dimension(vidMode.width(), vidMode.height());
      }
   }

   @Override
   public GameMessage getBorderlessDisplayMessage() {
      return this.doesMonitorNeedHDRHack() ? new LocalMessage("settingsui", "hdrhackwarning") : null;
   }

   @Override
   public boolean doesMonitorNeedHDRHack() {
      return this.monitorNeedsHDRHack;
   }

   private static class WindowError {
      public final GameWindow window;
      public final String error;

      public WindowError(GameWindow window) {
         this.window = window;
         this.error = null;
      }

      public WindowError(String error) {
         this.window = null;
         this.error = error;
      }
   }
}
