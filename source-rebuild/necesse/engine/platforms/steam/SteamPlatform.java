package necesse.engine.platforms.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamApps;
import com.codedisaster.steamworks.SteamAppsCallback;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamNativeHandle;
import com.codedisaster.steamworks.SteamNetworkingUtils;
import com.codedisaster.steamworks.SteamUtils;
import com.codedisaster.steamworks.SteamUtilsCallback;
import com.codedisaster.steamworks.SteamNetworkingUtils.SteamNetworkingSocketsDebugOutputType;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import necesse.engine.GameCrashLog;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.CommandsManager;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModProvider;
import necesse.engine.modLoader.ModSaveInfo;
import necesse.engine.modLoader.ModsFolderModProvider;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.sharedOnPC.sound.LWJGLSoundManager;
import necesse.engine.platforms.sharedOnPC.window.GLFWWindowManager;
import necesse.engine.platforms.steam.commands.SteamNetworkSendFlagClientCommand;
import necesse.engine.platforms.steam.dlc.SteamDLCRegistry;
import necesse.engine.platforms.steam.input.SteamInputManager;
import necesse.engine.platforms.steam.modding.SteamDevModProvider;
import necesse.engine.platforms.steam.modding.SteamModProvider;
import necesse.engine.platforms.steam.modding.SteamModSaveInfo;
import necesse.engine.platforms.steam.network.SteamNetworkManager;
import necesse.engine.platforms.steam.network.client.SteamClient;
import necesse.engine.platforms.steam.stats.SteamStatsProvider;
import necesse.engine.save.LoadData;
import necesse.engine.server.ServerWindow;
import necesse.engine.state.MainGame;
import necesse.engine.state.MainMenu;
import necesse.engine.state.State;
import necesse.engine.window.DisplayMode;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.reports.NoticeJFrame;
import org.lwjgl.system.Configuration;

public class SteamPlatform extends Platform {
   private static final TicksPerSecond richPresenceUpdater = TicksPerSecond.msPerTick(4000);
   private static final TicksPerSecond dlcUpdater = TicksPerSecond.msPerTick(5000);
   private SteamUtils steamUtils;
   private SteamNetworkingUtils steamNetworkingUtils;

   private static void onNetworkDebugOutput(int type, String message) {
      type = type == Integer.MAX_VALUE ? 9 : type;
      SteamNetworkingSocketsDebugOutputType[] enumValues = SteamNetworkingSocketsDebugOutputType.values();
      if (type >= enumValues.length) {
         GameLog.err.println("Steam Networking [Unsupported enum type (" + type + ")]: " + message);
      } else {
         SteamNetworkingSocketsDebugOutputType enumType = enumValues[type];
         switch (enumType) {
            case Msg:
            case Verbose:
            case Debug:
               GameLog.out.println("Steam Networking [" + enumType + "]: " + message);
               break;
            case Important:
            case Warning:
               GameLog.warn.println("Steam Networking [" + enumType + "]: " + message);
               break;
            case Bug:
            case Error:
               GameLog.err.println("Steam Networking [" + enumType + "]: " + message);
               break;
            case Everything:
            case None:
            case _Force32Bit:
               GameLog.err.println("Steam Networking [Unsupported enum type (" + enumType + ")]: " + message);
         }
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

      Platform.inputManager = new SteamInputManager();
      Platform.networkManager = new SteamNetworkManager();
      Platform.windowManager = new GLFWWindowManager();
      Platform.soundManager = new LWJGLSoundManager();
      Platform.statsProvider = new SteamStatsProvider();
      Platform.modProviders = new ArrayList<>(Arrays.asList(new SteamDevModProvider(), new ModsFolderModProvider(), new SteamModProvider()));
      Platform.dlcProvider = new SteamDLCRegistry();
      if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.WINDOWS) {
         System.setProperty("com.codedisaster.steamworks.SharedLibraryExtractPath", GlobalData.rootPath() + ".\\natives");
      }

      SteamAPI.loadLibraries(new GameSteamLibraryLoader());
      if (SteamAPI.restartAppIfNecessary(1169040)) {
         return false;
      }

      if (!SteamAPI.init()) {
         NoticeJFrame noticeFrame = new NoticeJFrame(400, Localization.translate("misc", "steamfailed"), Localization.translate("ui", "closebutton"));
         noticeFrame.setVisible(true);
         noticeFrame.requestFocus();
         return false;
      }

      this.steamNetworkingUtils = new SteamNetworkingUtils();
      SteamNetworkingSocketsDebugOutputType debugFlags = SteamNetworkingSocketsDebugOutputType.Error;
      if (GameLaunch.launchOptions.containsKey("debug_steam_network")) {
         debugFlags = SteamNetworkingSocketsDebugOutputType.Everything;
      }

      this.steamNetworkingUtils.setDebugOutputHook(debugFlags, SteamPlatform::onNetworkDebugOutput);
      this.steamUtils = new SteamUtils(new SteamUtilsCallback() {});
      if (this.steamUtils.isSteamRunningOnSteamDeck()) {
         System.out.println("Detected running on Steam Deck");
         Settings.sceneSize = 1.5F;
         Settings.interfaceSize = 1.3F;
      }

      SteamData.init();
      GameCrashLog.reportRecentJVMCrashes();
      Platform.statsProvider.initialize();
      dlcProvider.checkForNewlyInstalledDLCs();

      for (ModProvider modProvider : Platform.modProviders) {
         modProvider.initialize();
      }

      this.updateLanguage();
      if (debugStartup) {
         System.out.println("STARTUP: Updated platform language");
      }

      if (Platform.windowManager.initialize() && Platform.soundManager.initialize()) {
         Platform.inputManager.initialize();
         Settings.loadClientSettings();
         Platform.windowManager.updateDisplayModeNow();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void tick(TickManager tickManager) {
      Performance.record(tickManager, "steamCallbacks", () -> {
         if (SteamData.isCreated()) {
            try {
               SteamAPI.runCallbacks();
               Platform.statsProvider.tick();
            } catch (Exception e) {
               if (!(e instanceof SteamException)) {
                  throw e;
               }

               if (!SteamData.isOnCallbackErrorCooldown()) {
                  GameLog.warn.println("Error running Steam callbacks");
                  SteamData.resetCallbackErrorCooldown();
               }
            }
         }
      });
      richPresenceUpdater.gameTick();
      if (richPresenceUpdater.shouldTick()) {
         this.updateSteamRichPresence();
      }

      dlcUpdater.gameTick();
      if (dlcUpdater.shouldTick()) {
         dlcProvider.checkForNewlyInstalledDLCs();
      }

      SteamData.ConnectInfo connectInfo = SteamData.tickLobbyConnectRequested();
      if (connectInfo != null) {
         System.out.println("Got Steam request to connect to " + connectInfo);
         State currentState = GlobalData.getCurrentState();
         if (currentState instanceof MainMenu) {
            connectInfo.startConnectionClient((MainMenu)currentState);
            WindowManager.getWindow().requestAttention();
         } else if (currentState instanceof MainGame) {
            ((MainGame)currentState).getClient().instantDisconnect("Quit");
            MainMenu menu;
            GlobalData.setCurrentState(menu = new MainMenu((String)null));
            connectInfo.startConnectionClient(menu);
            WindowManager.getWindow().requestAttention();
         }
      }
   }

   private void updateSteamRichPresence() {
      if (GlobalData.getCurrentState() instanceof MainMenu) {
         MainMenu mainMenu = (MainMenu)GlobalData.getCurrentState();
         SteamClient client = (SteamClient)mainMenu.getClient();
         if (client != null) {
            client.updateSteamRichPresence();
         } else {
            SteamData.setRichPresence("steam_display", "#richpresence_atmainmenu");
         }
      } else {
         MainGame mainGame = (MainGame)GlobalData.getCurrentState();
         SteamClient client = (SteamClient)mainGame.getClient();
         client.updateSteamRichPresence();
      }
   }

   @Override
   public void dispose() {
      for (ModProvider modProvider : Platform.modProviders) {
         modProvider.dispose();
      }

      Platform.inputManager.dispose();
      Platform.soundManager.dispose();
      Platform.statsProvider.dispose();
      Platform.windowManager.dispose();
      if (this.steamNetworkingUtils != null) {
         this.steamNetworkingUtils.dispose();
      }

      try {
         Thread.sleep(1000L);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }

      SteamData.dispose();
      if (this.steamUtils != null) {
         this.steamUtils.dispose();
      }

      if (SteamAPI.isSteamRunning()) {
         SteamAPI.shutdown();
      }
   }

   @Override
   public void updateLanguage() {
      SteamApps steamApps = new SteamApps(new SteamAppsCallback() {});
      String language = steamApps.getCurrentGameLanguage();
      switch (language) {
         case "russian":
            Settings.language = Localization.Russian.stringID;
            break;
         case "portuguese":
         case "brazilian":
            Settings.language = Localization.BrazilianPortuguese.stringID;
            break;
         case "spanish":
         case "latam":
            Settings.language = Localization.Spanish.stringID;
            break;
         case "german":
            Settings.language = Localization.German.stringID;
            break;
         case "schinese":
            Settings.language = Localization.ChineseSimplified.stringID;
            break;
         case "tchinese":
            Settings.language = Localization.ChineseTraditional.stringID;
            break;
         case "czech":
            Settings.language = Localization.Czech.stringID;
            break;
         case "japanese":
            Settings.language = Localization.Japanese.stringID;
            break;
         case "swedish":
            Settings.language = Localization.Swedish.stringID;
            break;
         case "french":
            Settings.language = Localization.French.stringID;
            break;
         case "hungarian":
            Settings.language = Localization.Hungarian.stringID;
            break;
         case "turkish":
            Settings.language = Localization.Turkish.stringID;
            break;
         case "italian":
            Settings.language = Localization.Italian.stringID;
            break;
         case "koreana":
            Settings.language = Localization.Korean.stringID;
            break;
         case "polish":
            Settings.language = Localization.Polish.stringID;
            break;
         case "ukrainian":
            Settings.language = Localization.Ukrainian.stringID;
            break;
         case "thai":
            Settings.language = Localization.Thai.stringID;
            break;
         case "indonesian":
            Settings.language = Localization.Indonesian.stringID;
            break;
         case "lithuanian":
            Settings.language = Localization.Lithuanian.stringID;
            break;
         case "dutch":
            Settings.language = Localization.Dutch.stringID;
            break;
         case "finnish":
            Settings.language = Localization.Finnish.stringID;
            break;
         case "vietnamese":
            Settings.language = Localization.Vietnamese.stringID;
            break;
         case "norwegian":
            Settings.language = Localization.Norwegian.stringID;
            break;
         case "danish":
            Settings.language = Localization.Danish.stringID;
            break;
         case "croatian":
            Settings.language = Localization.Croatian.stringID;
            break;
         case "arabic":
            Settings.language = Localization.Arabic.stringID;
            break;
         case "bulgarian":
            Settings.language = Localization.Bulgarian.stringID;
            break;
         case "greek":
         case "romanian":
         case "english":
            Settings.language = Localization.English.stringID;
      }

      steamApps.dispose();
   }

   @Override
   public long getUniqueUserID() {
      return SteamData.isCreated() ? SteamNativeHandle.getNativeHandle(SteamData.getSteamID()) : 0L;
   }

   @Override
   public String getPlatformDebugString() {
      return "steam";
   }

   @Override
   public boolean isSteamDeck() {
      return this.steamUtils.isSteamRunningOnSteamDeck();
   }

   @Override
   public void applyDefaultMinimumSupportedPlatformSpecs() {
      Settings.sceneSize = 1.5F;
      Settings.interfaceSize = 1.3F;
      Settings.displaySize = new Dimension(1280, 800);
      Settings.displayMode = DisplayMode.Windowed;
      GameWindow window = WindowManager.getWindow();
      window.updateSceneSize();
      window.updateHudSize();
      getWindowManager().updateDisplayModeAfterTick();
   }

   @Override
   public int getPlatformAppBuild() {
      return SteamData.getApps().getAppBuildId();
   }

   @Override
   public ModSaveInfo tryGetModSaveInfo(LoadedMod.SaveType type, LoadData save) {
      switch (type) {
         case FILE_MOD:
         case DEV_MOD:
            return new ModSaveInfo(type, save);
         case STEAM_MOD:
            return new SteamModSaveInfo(type, save);
         default:
            return null;
      }
   }

   @Override
   public boolean isRequestingPause() {
      return SteamData.isOverlayActive();
   }

   @Override
   public String getUserName() {
      return Objects.toString(SteamData.getSteamName());
   }

   @Override
   public void registerPlatformCommands() {
      CommandsManager.registerClientCommand(new SteamNetworkSendFlagClientCommand());
   }

   @Override
   public ServerWindow getStandaloneServerGUI() {
      return null;
   }

   @Override
   public Platform.OperatingSystemFamily getOperatingSystemFamily() {
      switch (org.lwjgl.system.Platform.get()) {
         case LINUX:
            return Platform.OperatingSystemFamily.Linux;
         case MACOSX:
            return Platform.OperatingSystemFamily.Mac;
         case WINDOWS:
            return Platform.OperatingSystemFamily.Windows;
         default:
            return null;
      }
   }
}
