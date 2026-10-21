package necesse.engine.platforms.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import necesse.engine.GameCrashLog;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.dlc.DLC;
import necesse.engine.dlc.DLCProvider;
import necesse.engine.dlc.DLCProvider.DLCStatus;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.DevModProvider;
import necesse.engine.modLoader.ModProvider;
import necesse.engine.modLoader.ModSaveInfo;
import necesse.engine.modLoader.ModsFolderModProvider;
import necesse.engine.modLoader.LoadedMod.SaveType;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.Platform.OperatingSystemFamily;
import necesse.engine.platforms.desktop.control.DesktopInputManager;
import necesse.engine.platforms.desktop.network.DesktopNetworkManager;
import necesse.engine.platforms.desktop.server.DesktopServerWindow;
import necesse.engine.platforms.desktop.stats.DesktopStatsProvider;
import necesse.engine.platforms.sharedOnPC.sound.LWJGLSoundManager;
import necesse.engine.platforms.sharedOnPC.window.GLFWWindowManager;
import necesse.engine.save.LoadData;
import necesse.engine.server.ServerWindow;
import necesse.engine.util.GameUtils;
import org.lwjgl.system.Configuration;

public class DesktopPlatform extends Platform {
   private final boolean isServer;
   private DesktopServerWindow serverGUI;

   public DesktopPlatform(boolean isServer) {
      this.isServer = isServer;
      networkManager = new DesktopNetworkManager();
      inputManager = new DesktopInputManager();
      statsProvider = new DesktopStatsProvider();
      soundManager = new LWJGLSoundManager();
      modProviders = new ArrayList<>(Arrays.asList(new DevModProvider(), new ModsFolderModProvider()));
      dlcProvider = new DLCProvider() {
         @Override
         protected DLCStatus checkDLCStatus(DLC dlc) {
            return DLCStatus.NOT_OWNED;
         }
      };
      if (isServer) {
         windowManager = null;
      } else {
         windowManager = new GLFWWindowManager();
      }
   }

   @Override
   public boolean initialize() throws Exception {
      boolean debugStartup = GameLaunch.launchOptions.containsKey("debugstartup");
      if (GameLaunch.launchOptions.containsKey("debug_lwjgl")) {
         GameLog.out.println("Enabling debugging for LWJGL");
         Configuration.DEBUG_STREAM.set(GameLog.out);
         Configuration.DEBUG.set(true);
         Configuration.DEBUG_LOADER.set(true);
         Configuration.DEBUG_FUNCTIONS.set(true);
         Configuration.DEBUG_STACK.set(true);
         Configuration.DEBUG_MEMORY_ALLOCATOR_FAST.set(true);
      }

      GameCrashLog.reportRecentJVMCrashes();

      for (ModProvider modProvider : modProviders) {
         modProvider.initialize();
      }

      statsProvider.initialize();
      this.updateLanguage();
      if (debugStartup) {
         System.out.println("STARTUP: Updated platform language");
      }

      inputManager.initialize();
      Settings.loadClientSettings();
      if (!this.isServer) {
         if (!windowManager.initialize() || !soundManager.initialize()) {
            return false;
         }

         windowManager.updateDisplayModeNow();
      }

      return true;
   }

   @Override
   public void tick(TickManager tickManager) {
   }

   @Override
   public void dispose() {
      for (ModProvider modProvider : modProviders) {
         modProvider.dispose();
      }

      inputManager.dispose();
      soundManager.dispose();
      statsProvider.dispose();
      if (!this.isServer) {
         windowManager.dispose();
      }
   }

   @Override
   public void updateLanguage() {
      Settings.language = Localization.English.stringID;
   }

   @Override
   public long getUniqueUserID() {
      File file = this.getAuthFile();
      if (!file.exists()) {
         return this.generateDefaultAuth().getMostSignificantBits();
      }

      try {
         FileInputStream readFile = new FileInputStream(file);
         ObjectInputStream read = new ObjectInputStream(readFile);
         UUID out = (UUID)read.readObject();
         read.close();
         if (out == null) {
            throw new FileNotFoundException();
         } else {
            return out.getMostSignificantBits();
         }
      } catch (Exception e) {
         GameUtils.deleteFileOrFolder(this.getAuthFile());
         return this.generateDefaultAuth().getMostSignificantBits();
      }
   }

   @Override
   public String getPlatformDebugString() {
      return "desktop";
   }

   @Override
   public int getPlatformAppBuild() {
      return -1;
   }

   @Override
   public ModSaveInfo tryGetModSaveInfo(SaveType type, LoadData save) {
      switch (type) {
         case FILE_MOD:
         case DEV_MOD:
            return new ModSaveInfo(type, save);
         default:
            return null;
      }
   }

   @Override
   public boolean isRequestingPause() {
      return false;
   }

   @Override
   public String getUserName() {
      return null;
   }

   @Override
   public void registerPlatformCommands() {
   }

   @Override
   public ServerWindow getStandaloneServerGUI() {
      if (this.serverGUI == null) {
         this.serverGUI = new DesktopServerWindow();
      }

      return this.serverGUI;
   }

   @Override
   public OperatingSystemFamily getOperatingSystemFamily() {
      switch (org.lwjgl.system.Platform.get()) {
         case LINUX:
            return OperatingSystemFamily.Linux;
         case MACOSX:
            return OperatingSystemFamily.Mac;
         case WINDOWS:
            return OperatingSystemFamily.Windows;
         default:
            return null;
      }
   }

   @Override
   public void applyDefaultMinimumSupportedPlatformSpecs() {
   }

   private UUID generateDefaultAuth() {
      UUID auth = UUID.randomUUID();
      this.saveAuth(this.getAuthFile(), auth);
      return auth;
   }

   private boolean saveAuth(File file, UUID auth) {
      if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
         System.out.print("Could not create folders for authentication");
         return false;
      }

      try {
         FileOutputStream saveFile = new FileOutputStream(file);
         ObjectOutputStream save = new ObjectOutputStream(saveFile);
         save.writeObject(auth);
         save.close();
         return true;
      } catch (Exception e) {
         System.err.print("Could not save authentication");
         e.printStackTrace();
         return false;
      }
   }

   private File getAuthFile() {
      return new File(GlobalData.cfgPath() + "auth");
   }
}
