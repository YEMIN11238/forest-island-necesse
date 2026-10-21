package necesse.engine;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.util.GameUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Platform;
import oshi.SystemInfo;
import oshi.hardware.Display;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.EdidUtil;

public class GameSystemInfo {
   private static boolean initialized;
   private static SystemInfo info;
   private static HardwareAbstractionLayer hardware;
   private static OperatingSystem os;
   private static String graphicsCardString;
   private static String openGLVersionString;

   private static void initialize() {
      try {
         info = new SystemInfo();
      } catch (Exception | Error e) {
         System.err.println("Could not initialize system info:");
         e.printStackTrace();
      }

      if (info != null) {
         try {
            hardware = info.getHardware();
         } catch (Exception | Error e) {
            System.err.println("Could not initialize system hardware:");
            e.printStackTrace();
         }

         try {
            os = info.getOperatingSystem();
         } catch (Exception | Error e) {
            System.err.println("Could not initialize system OS:");
            e.printStackTrace();
         }
      }

      initialized = true;
   }

   public static SystemInfo getInfo() {
      if (!initialized) {
         initialize();
      }

      return info;
   }

   public static HardwareAbstractionLayer getHardware() {
      if (!initialized) {
         initialize();
      }

      return hardware;
   }

   public static OperatingSystem getOS() {
      if (!initialized) {
         initialize();
      }

      return os;
   }

   public static void printSystemInfo(PrintStream printStream) {
      try {
         printStream.println("SYSTEM INFO:");
         printSystemInfo(printStream, "\t");
      } catch (Exception | Error e) {
         printStream.println("Error getting system info:");
         e.printStackTrace(printStream);
      }
   }

   public static void printSystemInfo(PrintStream printStream, String prefix) {
      printStream.println(prefix + "Username: " + System.getProperty("user.name"));
      printStream.println(prefix + "OS: " + getOSName());
      printStream.println(prefix + "CPU: " + getCPUName());
      printStream.println(prefix + "OS arch: " + System.getProperty("os.arch"));
      printStream.println(prefix + "Memory total: " + getTotalMemoryString());
      printStream.println(prefix + "Memory max: " + getSafeString(() -> GameUtils.getByteString(Runtime.getRuntime().maxMemory())));
      printStream.println(prefix + "Java path: " + System.getProperty("java.home"));
      printStream.println(prefix + "Java version: " + System.getProperty("java.version"));

      try {
         printStream.println(prefix + "JVM arguments:");

         for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            printStream.println(prefix + "\t" + arg);
         }
      } catch (Exception e) {
         printStream.println(prefix + "\tERR: " + e.getMessage());
      }

      printStream.println(prefix + "LWJGL version: " + Version.getVersion());
      printStream.println(prefix + "Root path: " + GlobalData.rootPath());
      printStream.println(prefix + "AppData path: " + GlobalData.appDataPath());
      printStream.println(prefix + "Working dir: " + System.getProperty("user.dir"));
      String nativesPath = System.getProperty("org.lwjgl.librarypath");
      if (nativesPath == null) {
         nativesPath = "INTERNAL";
      }

      printStream.println(prefix + "Natives path: " + nativesPath);
   }

   public static void printGraphicsInfo(PrintStream printStream) {
      try {
         printStream.println("GRAPHICS INFO:");
         printGraphicsInfo(printStream, "\t");
      } catch (Exception | Error e) {
         printStream.println("Error getting system info:");
         e.printStackTrace(printStream);
      }
   }

   public static void printGraphicsInfo(PrintStream printStream, String prefix) {
      printStream.println(prefix + "Graphics card: " + getGraphicsCard());
      printStream.println(prefix + "OpenGL version: " + getOpenGLVersion());

      try {
         ArrayList<String> displays = getDisplays();

         for (int i = 0; i < displays.size(); i++) {
            String str = displays.get(i);
            printStream.println(prefix + "Display " + i + ": " + str);
         }
      } catch (Exception | Error var5) {
      }
   }

   public static String getGraphicsCard() {
      return getSafeString(() -> {
         if (graphicsCardString == null) {
            if (GLFW.glfwGetCurrentContext() == 0L) {
               return "NO_CONTEXT";
            }

            graphicsCardString = GL11.glGetString(7936) + ", " + GL11.glGetString(7937);
         }

         return graphicsCardString;
      });
   }

   public static String getOpenGLVersion() {
      return getSafeString(() -> {
         if (openGLVersionString == null) {
            if (GLFW.glfwGetCurrentContext() == 0L) {
               return "NO_CONTEXT";
            }

            openGLVersionString = GL11.glGetString(7938);
         }

         return openGLVersionString;
      });
   }

   public static ArrayList<String> getDisplays() {
      if (Platform.get() == Platform.MACOSX) {
         return new ArrayList<>(Collections.singletonList("MacOS display"));
      }

      HardwareAbstractionLayer hardware = getHardware();
      if (hardware == null) {
         return new ArrayList<>(Collections.singletonList("NULL HARDWARE"));
      }

      List<Display> displays = hardware.getDisplays();
      ArrayList<String> out = new ArrayList<>(displays.size());

      for (Display display : displays) {
         byte[] edid = display.getEdid();
         byte[][] desc = EdidUtil.getDescriptors(edid);

         for (byte[] d : desc) {
            if (EdidUtil.getDescriptorType(d) == 252) {
               out.add(getSafeString(() -> EdidUtil.getDescriptorText(d) + " (" + EdidUtil.getManufacturerID(edid) + ", " + EdidUtil.getProductID(edid) + ")"));
            }
         }
      }

      return out;
   }

   public static String getOSName() {
      return getSafeString(() -> getOS().toString());
   }

   public static String getCPUName() {
      return getSafeString(() -> getHardware().getProcessor().getProcessorIdentifier().getName());
   }

   public static long getTotalMemory() {
      return getHardware().getMemory().getTotal();
   }

   public static long getAvailableMemory() {
      return getHardware().getMemory().getAvailable();
   }

   public static long getUsedMemory() {
      return getTotalMemory() - getAvailableMemory();
   }

   public static String getTotalMemoryString() {
      return getSafeString(() -> GameUtils.getByteString(getTotalMemory()));
   }

   public static String getAvailableMemoryString() {
      return getSafeString(() -> GameUtils.getByteString(getAvailableMemory()));
   }

   public static String getUsedMemoryString() {
      return getSafeString(() -> GameUtils.getByteString(getTotalMemory() - getAvailableMemory()));
   }

   private static String getSafeString(Supplier<String> supplier) {
      try {
         return supplier.get();
      } catch (Exception | Error e) {
         return "ERR: " + e.getClass().getSimpleName();
      }
   }
}
