package necesse.engine.loading;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.zip.DataFormatException;
import javax.swing.JFrame;
import necesse.engine.GameAuth;
import necesse.engine.GameInfo;
import necesse.engine.GameLaunch;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GameLog;
import necesse.engine.GameSystemInfo;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.ClientGameLoop;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoadException;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.ModRuntimeException;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.network.server.ServerSettings;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.save.WorldSave;
import necesse.engine.state.MainMenu;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.FileSystemClosedException;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.MainMenuFormManager;
import necesse.gfx.forms.components.ContinueComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.ContinueForm;
import necesse.gfx.forms.presets.NoticeForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.reports.CrashJFrame;
import necesse.reports.CrashReportData;
import necesse.reports.GeneralModdingCrashJFrame;
import necesse.reports.ModCrashJFrame;
import necesse.reports.NoticeJFrame;
import necesse.reports.ReportUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.Configuration;

public class ClientLoader extends Loader {
   public static LinkedList<ContinueComponent> loadingNoticeForms = new LinkedList<>();
   private ClientGameLoop gameLoop;
   private boolean unloadMods = true;
   private MainMenu mainMenu;

   public static NoticeForm addLoadingNotice(String formName, GameMessage message) {
      return addLoadingNotice(formName, message, 400, 400);
   }

   public static NoticeForm addLoadingNotice(String formName, GameMessage message, int width, int maxHeight) {
      NoticeForm noticeForm = new NoticeForm(formName, width, maxHeight);
      noticeForm.setupNotice(message);
      noticeForm.setButtonCooldown(0);
      loadingNoticeForms.add(noticeForm);
      return noticeForm;
   }

   @Override
   public boolean loadGame(String[] args, Platform platform) throws Exception {
      long startTime = System.currentTimeMillis();
      Locale.setDefault(Category.FORMAT, Locale.ENGLISH);
      GlobalData.setup(args);
      boolean detectedLocalNativesDir = false;
      if (org.lwjgl.system.Platform.get() == org.lwjgl.system.Platform.WINDOWS) {
         String appDataPath = GlobalData.appDataPath();
         if (appDataPath.contains("?") || appDataPath.contains("!") || appDataPath.contains("~")) {
            detectedLocalNativesDir = true;
         }

         if (detectedLocalNativesDir || GameLaunch.parseLaunchOptions(args).containsKey("localnatives")) {
            detectedLocalNativesDir = true;
            String nativesPath = GlobalData.rootPath() + ".\\natives";
            Configuration.SHARED_LIBRARY_EXTRACT_PATH.set(nativesPath);
            System.setProperty("com.codedisaster.steamworks.SharedLibraryExtractPath", nativesPath);
            System.setProperty("jna.tmpdir", nativesPath);
         }
      }

      boolean logDebug = GameLaunch.parseLaunchOptions(args).containsKey("log_debug_prints");
      GameLog.startLogging(logDebug, "latest-log.txt");
      GameLaunch.launchOptions = GameLaunch.parseAndHandleLaunchOptions(args);
      if (detectedLocalNativesDir) {
         System.out.println("Detected local natives dir");
      }

      GameSystemInfo.printSystemInfo(GameLog.file);
      boolean debugStartup = GameLaunch.launchOptions.containsKey("debugstartup");
      if (debugStartup) {
         System.out.println("DEBUGGING STARTUP");
      }

      if (!PlatformManager.initialize(platform)) {
         GameLog.err.println("Could not load platform");
         return false;
      }

      if (debugStartup) {
         System.out.println("STARTUP: Initialized platform " + PlatformManager.getPlatform().getPlatformDebugString());
      }

      handleLaunchArgs(GameLaunch.launchOptions);
      if (debugStartup) {
         System.out.println("STARTUP: Handled launch options");
      }

      GameAuth.loadAuth();
      GameLog.out
         .println("Started client on version " + GameInfo.getFullVersionStringAndBuild() + " with authentication " + GameAuth.getAuthentication() + ".");
      EventStatusBarManager.initialize();
      if (debugStartup) {
         System.out.println("STARTUP: Initialized EventStatusBarManager");
      }

      GameTooltipManager.initialize();
      if (debugStartup) {
         System.out.println("STARTUP: Initialized GameTooltipManager");
      }

      GameToolManager.initialize();
      if (debugStartup) {
         System.out.println("STARTUP: Initialized GameToolManager");
      }

      Renderer.initialize();
      if (debugStartup) {
         System.out.println("STARTUP: Initialized Renderer");
      }

      PostProcessingEffects.initialize();
      if (debugStartup) {
         System.out.println("STARTUP: Initialized PostProcessingEffects");
      }

      GameLoadingScreen.initLoadingScreen(() -> {
         GameWindow window = WindowManager.getWindow();
         window.tickWindowResize(false);
         window.startHudDraw();
      }, () -> {
         GameWindow window = WindowManager.getWindow();
         window.endHudDraw();
         GL11.glClear(16640);
         window.renderHud(1.0F);
         window.preloadUpdate();
      });
      if (debugStartup) {
         System.out.println("STARTUP: Initialized GameLoadingScreen");
      }

      Renderer.queryGLError("PreLoadAll");

      try {
         GlobalData.loadAll(false);
      } catch (Exception | Error var15) {
         Throwable e = var15;

         for (Throwable current = var15; current != null; current = current.getCause()) {
            if (current instanceof ModLoadException) {
               var15.printStackTrace();
               this.unloadMods = false;
               ModLoadException modException = (ModLoadException)current;
               ModCrashJFrame modCrashJFrame = new ModCrashJFrame(Collections.singletonList(modException.mod), modException);
               modCrashJFrame.setVisible(true);
               modCrashJFrame.requestFocus();
               return false;
            }

            if (current instanceof ModRuntimeException) {
               var15.printStackTrace();
               this.unloadMods = false;
               ModRuntimeException modException = (ModRuntimeException)current;
               ModCrashJFrame modCrashJFrame = new ModCrashJFrame(Collections.singletonList(modException.mod), modException);
               modCrashJFrame.setVisible(true);
               modCrashJFrame.requestFocus();
               return false;
            }

            if (current instanceof OutOfMemoryError) {
               var15.printStackTrace();
               NoticeJFrame noticeFrame = new NoticeJFrame(400, Localization.translate("misc", "outofmemory"), Localization.translate("ui", "closebutton"));
               noticeFrame.setVisible(true);
               noticeFrame.requestFocus();
               return false;
            }

            if (current instanceof DataFormatException) {
               var15.printStackTrace();
               NoticeJFrame noticeFrame = new NoticeJFrame(400, Localization.translate("misc", "datacorrupted"), Localization.translate("ui", "closebutton"));
               noticeFrame.setVisible(true);
               noticeFrame.requestFocus();
               return false;
            }

            if (current instanceof IllegalAccessError) {
               JFrame crashJFrame;
               if (!ModLoader.getEnabledMods().isEmpty()) {
                  crashJFrame = new GeneralModdingCrashJFrame(new CrashReportData(Collections.singletonList(current), null, null, "Init"));
               } else {
                  crashJFrame = new CrashJFrame(new CrashReportData(Collections.singletonList(current), null, null, "Init"));
               }

               crashJFrame.setVisible(true);
               crashJFrame.requestFocus();
            }
         }

         try {
            throw var15;
         } catch (ModLoadException ex) {
            throw new RuntimeException(ex);
         }
      }

      Renderer.queryGLError("PostLoadAll");
      WindowManager.getWindow().setupPostProcessing();
      Renderer.queryGLError("PostPostProcessingSetup");
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "finishing"));
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "finishing"));
      LinkedList<ContinueComponent> noticeForms = new LinkedList<>();
      if (Settings.showSteamDeckCursorNotice && platform.isSteamDeck()) {
         NoticeForm notice = new NoticeForm("startupsteamdeckcursornotice", 400, 400);
         notice.padding = 2;
         notice.setupNotice(
            content -> {
               FormFlow flow = new FormFlow(10);
               content.addComponent(
                  flow.nextY(new FormLocalLabel("misc", "steamdeckcursornotice", new FontOptions(20), 0, notice.getWidth() / 2, 10, notice.getWidth() - 20), 10)
               );
               content.addComponent(new FormLocalCheckBox("ui", "dontshowagain", 5, flow.next(20)))
                  .onClicked(e -> Settings.showSteamDeckCursorNotice = !e.from.checked);
            }
         );
         notice.onContinue(Settings::saveClientSettings);
         notice.setButtonCooldown(0);
         noticeForms.add(notice);
      }

      HashSet<LoadedMod> modErrors = ModLoader.modErrors.getOrDefault(ModEntry.class, new HashSet<>());
      if (!modErrors.isEmpty()) {
         for (LoadedMod mod : modErrors) {
            mod.initError = true;
            NoticeForm modError = new NoticeForm("moderrornotice", 400, 400);
            modError.setupNotice(new LocalMessage("ui", "modiniterror", "mod", mod.name));
            modError.setButtonCooldown(0);
            noticeForms.addFirst(modError);
         }
      }

      noticeForms.addAll(loadingNoticeForms);
      long loadingTime = System.currentTimeMillis() - startTime;
      ReportUtils.sendSessionStart(loadingTime);
      GlobalData.setCurrentState(this.mainMenu = new MainMenu((ContinueForm)null));

      for (ContinueComponent noticeForm : noticeForms) {
         this.mainMenu.addContinueForm(noticeForm);
      }

      this.gameLoop = new ClientGameLoop("main", Settings.maxFPS, this);
      this.gameLoop.init();
      Renderer.setCursor(GameWindow.CURSOR.DEFAULT);
      Control.resetControls();
      GameLoadingScreen.markDone();
      return true;
   }

   public static void handleLaunchArgs(HashMap<String, String> options) {
      if (options.containsKey("zipsaves")) {
         String zipSaves = options.get("zipsaves");
         Settings.zipSaves = zipSaves.equals("1") || zipSaves.equals("true");
         Settings.saveClientSettings();
      }
   }

   public void processStartGameLaunchParameters() {
      GameWindow window = WindowManager.getWindow();
      if (GameLaunch.instantContinue) {
         FormManager formManager = this.mainMenu.getFormManager();
         if (formManager instanceof MainMenuFormManager) {
            MainMenuFormManager manager = (MainMenuFormManager)formManager;
            manager.mainForm.updateContinueButton();
            manager.mainForm.continueLast();
         }

         window.requestAttention();
      } else if (GameLaunch.instantLoad != null) {
         try {
            WorldSave save;
            if (GameLaunch.instantLoad.isEmpty()) {
               save = WorldSave.getMostRecentSave(true);
            } else {
               save = WorldSave.findOrCreateNewWorld(GameLaunch.instantLoad);
            }

            if (save != null) {
               this.mainMenu.startSingleplayer(save, new ServerCreationSettings(save.filePath), null);
            }
         } catch (IOException | FileSystemClosedException e) {
            System.err.println("Could not instant load.");
            e.printStackTrace();
         }

         window.requestAttention();
      } else if (GameLaunch.instantHost != null) {
         try {
            WorldSave save;
            if (GameLaunch.instantHost.isEmpty()) {
               save = WorldSave.getMostRecentSave(true);
            } else {
               save = WorldSave.findOrCreateNewWorld(GameLaunch.instantHost);
            }

            if (save != null) {
               ServerSettings hostSettings = Platform.getNetworkManager().getDefaultHostSettings(new ServerCreationSettings(save.filePath));
               this.mainMenu.host(save, hostSettings, null, null);
            }
         } catch (IOException | FileSystemClosedException e) {
            System.err.println("Could not instant host.");
            window.requestAttention();
         }

         window.requestAttention();
      } else if (GameLaunch.instantConnect != null) {
         Platform.getNetworkManager().startupInstantConnect(GameLaunch.instantConnect, this.mainMenu);
         window.requestAttention();
      } else if (GameLaunch.instantLobbyConnect != null) {
         Platform.getNetworkManager().startupInstantConnect(GameLaunch.instantLobbyConnect, this.mainMenu);
         window.requestAttention();
      }

      GameLaunch.instantContinue = false;
      GameLaunch.instantLoad = null;
      GameLaunch.instantHost = null;
      GameLaunch.instantLobbyConnect = null;
      GameLaunch.instantConnect = null;
   }

   @Override
   public void unloadGame() {
      if (GlobalData.getCurrentState() != null) {
         GlobalData.getCurrentState().onClose();
         GlobalData.getCurrentState().dispose();
      }

      if (this.unloadMods) {
         ModLoader.getAllMods().forEach(LoadedMod::dispose);
      }

      ReportUtils.sendSessionEnd();
      GameResources.deleteShaders();
      GameTexture.deleteGeneratedTextures();
      FontManager.deleteFonts();
      if (WindowManager.getWindow() != null) {
         WindowManager.getWindow().destroy();
      }

      GL.setCapabilities(null);
      if (Settings.hasLoadedClientSettings()) {
         Settings.saveClientSettings();
      }

      if (GlobalData.stats() != null) {
         GlobalData.stats().saveStatsFile();
      }

      if (GlobalData.achievements() != null) {
         GlobalData.achievements().saveAchievementsFileSafe();
      }

      PlatformManager.dispose();
   }

   @Override
   public void startGame() {
      GlobalData.setCurrentGameLoop(this.gameLoop);
      this.gameLoop.runMainGameLoop();
   }
}
