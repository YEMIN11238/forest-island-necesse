package necesse.engine;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import necesse.engine.input.Control;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Language;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.ModRuntimeException;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.EventVariable;
import necesse.engine.util.GameUtils;
import necesse.engine.window.DisplayMode;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.Renderer;
import necesse.gfx.forms.components.FormTravelContainerGrid;
import necesse.gfx.res.ResourceEncoder;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.recipe.RecipeFilter;

public class Settings {
   private static boolean loadedClientSettings;
   private static boolean firstTimeSavedSettings;
   public static float sceneSize = 1.25F;
   public static boolean adjustZoomOnHighResolution = true;
   public static boolean limitCameraToLevelBounds = true;
   public static boolean pauseOnFocusLoss = true;
   public static boolean savePerformanceOnFocusLoss = true;
   public static boolean showSettlerHeadArmor = true;
   public static boolean useTileObjectHitboxes = false;
   public static boolean loadLevelBeforeSpawn = false;
   public static boolean instantLevelChange = true;
   public static boolean smartMining = false;
   public static EventVariable<Boolean> craftingUseNearby = new EventVariable<>(true);
   public static boolean minimapHidden = false;
   public static int minimapZoomLevel = 3;
   public static int islandMapZoomLevel = 3;
   public static boolean menuCameraPan = true;
   public static boolean show1p0Notice = true;
   public static boolean showSteamDeckCursorNotice = true;
   public static int patchNotesOpenYear = 2000;
   public static int patchNotesOpenMonth = 1;
   public static int patchNotesOpenDay = 1;
   public static float cursorJoystickSensitivity = 1.0F;
   public static float interfaceSize = 1.0F;
   public static GameInterfaceStyle UI = GameInterfaceStyle.styles.get(0);
   public static boolean adjustInterfaceOnHighResolution = true;
   public static boolean sharpenInterface = true;
   public static boolean pixelFont = false;
   public static boolean showDebugInfo = false;
   public static boolean showQuestMarkers = true;
   public static boolean showTeammateMarkers = true;
   public static boolean showPickupText = true;
   public static boolean showDamageText = true;
   public static boolean showDoTText = true;
   public static boolean showMobHealthBars = true;
   public static boolean showBossHealthBars = true;
   public static boolean showControlTips = true;
   public static boolean showBasicTooltipBackground = false;
   public static boolean showItemTooltipBackground = true;
   public static boolean showLogicGateTooltips = false;
   public static boolean showIngredientsAvailable = true;
   public static boolean alwaysShowQuickbar = false;
   public static int tooltipTextSize = 16;
   public static boolean drawCursorManually = true;
   public static Color cursorColor = new Color(255, 255, 255);
   public static int cursorSize = 0;
   public static EventVariable<Boolean> trackNewQuests = new EventVariable<>(true);
   public static boolean displayJournalNotifications = true;
   public static EventVariable<Boolean> hasCraftingFilterExpanded = new EventVariable<>(false);
   public static EventVariable<Boolean> hasCraftingListExpanded = new EventVariable<>(true);
   public static EventVariable<Boolean> craftingListShowHidden = new EventVariable<>(false);
   public static EventVariable<Boolean> craftingListOnlyCraftable = new EventVariable<>(false);
   public static EventVariable<Boolean> highlightNewRecipesToWorkstation = new EventVariable<>(true);
   public static EventVariable<Boolean> hideSettlementStorage = new EventVariable<>(false);
   public static EventVariable<Boolean> hideSettlementWorkstations = new EventVariable<>(false);
   public static EventVariable<Boolean> hideSettlementForestryZones = new EventVariable<>(false);
   public static EventVariable<Boolean> hideSettlementHusbandryZones = new EventVariable<>(false);
   public static EventVariable<Boolean> hideSettlementFertilizeZones = new EventVariable<>(false);
   public static FormTravelContainerGrid.CoordinateSetting mapCoordinates = FormTravelContainerGrid.CoordinateSetting.RELATIVE_SELF;
   public static String outputDevice = null;
   public static float masterVolume = 0.5F;
   public static float effectsVolume = 1.0F;
   public static float weatherVolume = 0.2F;
   public static float UIVolume = 1.0F;
   public static float musicVolume = 0.5F;
   public static boolean muteOnFocusLoss = false;
   public static DisplayMode displayMode = DisplayMode.Borderless;
   public static int monitor = 0;
   public static Dimension displaySize = null;
   public static boolean vSyncEnabled = true;
   public static int maxFPS = 0;
   public static boolean reduceUIFramerate = true;
   public static SceneColorSetting sceneColors = SceneColorSetting.Vibrant;
   public static float brightness = 1.0F;
   public static boolean smoothLighting = true;
   public static Settings.LightSetting lights = Settings.LightSetting.Color;
   public static Settings.ParticleSetting particles = Settings.ParticleSetting.Maximum;
   public static boolean wavyGrass = true;
   public static boolean denseGrass = true;
   public static boolean windEffects = true;
   public static boolean heatDistortionEffects = true;
   public static boolean cameraShake = true;
   public static boolean alwaysLight = false;
   public static boolean alwaysRain = false;
   public static final int DEFAULT_SERVER_PORT = 14159;
   public static int serverPort = 14159;
   public static int serverSlots = 10;
   public static String serverPassword = "";
   public static int maxClientLatencySeconds = 30;
   public static boolean pauseWhenEmpty = true;
   public static boolean strictServerAuthority = false;
   public static boolean serverLogging = true;
   public static String serverMOTD = "";
   public static String serverWorld = "";
   public static String serverOwnerName = null;
   public static long serverOwnerAuth = -1L;
   public static int unloadLevelsCooldown = 30;
   public static int worldBorderSize = -1;
   public static int droppedItemsLifeMinutes = 0;
   public static boolean unloadSettlements = false;
   public static int maxSettlementsPerPlayer = -1;
   public static int maxSettlersPerSettlement = -1;
   public static boolean zipSaves = true;
   public static String language = Localization.English.stringID;
   public static ArrayList<String> banned;
   public static boolean serverPerspective = false;
   public static boolean hideUI = false;
   public static boolean hideCursor = false;
   public static boolean hideChat = false;
   protected static HashMap<Object, RecipeFilter> recipeFilters = new HashMap<>();
   protected static HashMap<Object, ItemCategoryExpandedSetting> itemCategoriesExpanded = new HashMap<>();
   public static final int[] lanPorts = new int[]{55169, 51868, 49745, 54198, 60631, 58737, 61410};
   public static String bindIP = null;

   public static String clientDir() {
      return GlobalData.cfgPath() + "settings.cfg";
   }

   public static String clientModDir(LoadedMod mod) {
      return GlobalData.cfgPath() + "/mods/" + mod.id + ".cfg";
   }

   public static String serverDir() {
      return GlobalData.cfgPath() + "server.cfg";
   }

   public static RecipeFilter getRecipeFilterSetting(Object keyObject) {
      return recipeFilters.compute(keyObject, (key, last) -> last == null ? new RecipeFilter() : last);
   }

   public static ItemCategoryExpandedSetting getItemCategoryExpandedSetting(Object keyObject) {
      return itemCategoriesExpanded.compute(keyObject, (key, last) -> last == null ? new ItemCategoryExpandedSetting(false) : last);
   }

   public static ItemCategoryExpandedSetting getItemCategoryExpandedSetting(Object keyObject, ItemCategory masterCategory, boolean defaultExpanded) {
      return itemCategoriesExpanded.compute(keyObject, (key, last) -> last == null ? new ItemCategoryExpandedSetting(masterCategory, defaultExpanded) : last);
   }

   private static SaveData getClientSaveData() {
      SaveData settings = new SaveData("SETTINGS");
      SaveData general = new SaveData("GENERAL");
      general.addFloat("sceneSize", sceneSize, "[" + GameWindow.minSceneSize + " - " + GameWindow.maxSceneSize + "] Higher = more zoomed in");
      general.addBoolean("adjustZoomOnHighResolution", adjustZoomOnHighResolution);
      general.addBoolean("limitCameraToLevelBounds", limitCameraToLevelBounds);
      general.addBoolean("pauseOnFocusLoss", pauseOnFocusLoss);
      general.addBoolean("savePerformanceOnFocusLoss", savePerformanceOnFocusLoss);
      general.addBoolean("showSettlerHeadArmor", showSettlerHeadArmor);
      general.addBoolean("useTileObjectHitboxes", useTileObjectHitboxes);
      general.addBoolean("loadLevelBeforeSpawn", loadLevelBeforeSpawn);
      general.addBoolean("smartMining", smartMining);
      general.addBoolean("craftingUseNearby", craftingUseNearby.get());
      general.addBoolean("minimapHidden", minimapHidden);
      general.addBoolean("menuCameraPan", menuCameraPan);
      general.addInt("minimapZoomLevel", minimapZoomLevel);
      general.addInt("islandMapZoomLevel", islandMapZoomLevel);
      general.addBoolean("show1p0Notice", show1p0Notice);
      general.addBoolean("showSteamDeckCursorNotice", showSteamDeckCursorNotice);
      general.addInt("patchNotesOpenYear", patchNotesOpenYear);
      general.addInt("patchNotesOpenMonth", patchNotesOpenMonth);
      general.addInt("patchNotesOpenDay", patchNotesOpenDay);
      general.addInt("unloadLevelsCooldown", unloadLevelsCooldown);
      general.addInt("worldBorderSize", worldBorderSize);
      general.addInt("droppedItemsLifeMinutes", droppedItemsLifeMinutes);
      general.addBoolean("unloadSettlements", unloadSettlements);
      general.addInt("maxSettlementsPerPlayer", maxSettlementsPerPlayer);
      general.addInt("maxSettlersPerSettlement", maxSettlersPerSettlement);
      general.addBoolean("zipSaves", zipSaves);
      general.addSafeString("language", language);
      settings.addSaveData(general);
      if (!Control.isControlsLoaded()) {
         Control.loadControls();
      }

      SaveData controls = new SaveData("CONTROLS");
      controls.addFloat("cursorJoystickSensitivity", cursorJoystickSensitivity);

      for (Control c : Control.getControls()) {
         if (c != null && c.mod == null) {
            controls.addInt(c.id, c.getKey());
         }
      }

      SaveData controllerInputSaveData = new SaveData("CONTROLLER");
      ControllerInput.saveControllerBinds(controllerInputSaveData, null);
      if (!controllerInputSaveData.isEmpty()) {
         controls.addSaveData(controllerInputSaveData);
      }

      settings.addSaveData(controls);
      SaveData interf = new SaveData("INTERFACE");
      interf.addFloat(
         "interfaceSize",
         interfaceSize,
         "[" + GameWindow.interfaceSizes[0] + " - " + GameWindow.interfaceSizes[GameWindow.interfaceSizes.length - 1] + "] Higher = bigger"
      );
      interf.addSafeString("interfaceStyle", UI.texturesPath);
      interf.addBoolean("adjustInterfaceOnHighResolution", adjustInterfaceOnHighResolution);
      interf.addBoolean("sharpenInterface", sharpenInterface);
      interf.addBoolean("pixelFont", pixelFont);
      interf.addBoolean("showDebugInfo", showDebugInfo);
      interf.addBoolean("showQuestMarkers", showQuestMarkers);
      interf.addBoolean("showTeammateMarkers", showTeammateMarkers);
      interf.addBoolean("showPickupText", showPickupText);
      interf.addBoolean("showDamageText", showDamageText);
      interf.addBoolean("showDoTText", showDoTText);
      interf.addBoolean("showMobHealthBars", showMobHealthBars);
      interf.addBoolean("showBossHealthBars", showBossHealthBars);
      interf.addBoolean("showBasicTooltipBackground", showBasicTooltipBackground);
      interf.addBoolean("showItemTooltipBackground", showItemTooltipBackground);
      interf.addBoolean("showControlTips", showControlTips);
      interf.addBoolean("showLogicGateTooltips", showLogicGateTooltips);
      interf.addBoolean("showIngredientsAvailable", showIngredientsAvailable);
      interf.addBoolean("alwaysShowQuickbar", alwaysShowQuickbar);
      interf.addInt("tooltipTextSize", tooltipTextSize);
      interf.addBoolean("drawCursorManually", drawCursorManually);
      interf.addColor("cursorColor", cursorColor);
      interf.addInt("cursorSize", cursorSize);
      interf.addBoolean("trackNewQuests", trackNewQuests.get());
      interf.addBoolean("displayJournalNotifications", displayJournalNotifications);
      interf.addBoolean("craftingFilterExpanded", hasCraftingFilterExpanded.get());
      interf.addBoolean("craftingListExpanded", hasCraftingListExpanded.get());
      interf.addBoolean("craftingListShowHidden", craftingListShowHidden.get());
      interf.addBoolean("craftingListOnlyCraftable", craftingListOnlyCraftable.get());
      interf.addBoolean("highlightNewRecipesToWorkstation", highlightNewRecipesToWorkstation.get());
      interf.addBoolean("hideSettlementStorage", hideSettlementStorage.get());
      interf.addBoolean("hideSettlementWorkstations", hideSettlementWorkstations.get());
      interf.addBoolean("hideSettlementForestryZones", hideSettlementForestryZones.get());
      interf.addBoolean("hideSettlementHusbandryZones", hideSettlementHusbandryZones.get());
      interf.addBoolean("hideSettlementFertilizeZones", hideSettlementFertilizeZones.get());
      interf.addEnum("mapCoordinates", mapCoordinates);
      settings.addSaveData(interf);
      SaveData sound = new SaveData("SOUND");
      if (outputDevice != null) {
         sound.addSafeString("outputDevice", outputDevice);
      }

      sound.addInt("masterVolume", (int)(masterVolume * 100.0F), "[0 - 100]");
      sound.addInt("effectsVolume", (int)(effectsVolume * 100.0F), "[0 - 100]");
      sound.addInt("weatherVolume", (int)(weatherVolume * 100.0F), "[0 - 100]");
      sound.addInt("UIVolume", (int)(UIVolume * 100.0F), "[0 - 100]");
      sound.addInt("musicVolume", (int)(musicVolume * 100.0F), "[0 - 100]");
      sound.addBoolean("muteOnFocusLoss", muteOnFocusLoss);
      settings.addSaveData(sound);
      SaveData graphics = new SaveData("GRAPHICS");
      graphics.addEnum("displayMode", displayMode, Arrays.stream(DisplayMode.values()).map(Enum::toString).collect(Collectors.joining(", ")));
      if (displaySize != null) {
         graphics.addDimension("displaySize", displaySize);
      }

      graphics.addInt("monitor", monitor);
      graphics.addBoolean("vSyncEnabled", vSyncEnabled);
      graphics.addInt("maxFPS", maxFPS, "0 = uncapped");
      graphics.addBoolean("reduceUIFramerate", reduceUIFramerate);
      graphics.addEnum("sceneColors", sceneColors, Arrays.stream(SceneColorSetting.values()).map(Enum::toString).collect(Collectors.joining(", ")));
      graphics.addFloat("brightness", brightness);
      graphics.addBoolean("smoothLighting", smoothLighting);
      graphics.addEnum("lights", lights, Arrays.stream(Settings.LightSetting.values()).map(Enum::toString).collect(Collectors.joining(", ")));
      graphics.addEnum("particles", particles, Arrays.stream(Settings.ParticleSetting.values()).map(Enum::toString).collect(Collectors.joining(", ")));
      graphics.addBoolean("wavyGrass", wavyGrass);
      graphics.addBoolean("denseGrass", denseGrass);
      graphics.addBoolean("windeffects", windEffects);
      graphics.addBoolean("heatDistortionEffects", heatDistortionEffects);
      graphics.addBoolean("cameraShake", cameraShake);
      settings.addSaveData(graphics);
      return settings;
   }

   private static SaveData getServerSaveData() {
      SaveData server = new SaveData("SERVER");
      server.addInt("port", serverPort, "[0 - 65535] Server default port");
      server.addInt("slots", serverSlots, "[1 - 250] Server default slots");
      server.addSafeString("password", serverPassword, "Leave blank for no password");
      server.addInt("maxClientLatencySeconds", maxClientLatencySeconds);
      server.addBoolean("pauseWhenEmpty", pauseWhenEmpty);
      server.addBoolean(
         "strictServerAuthority",
         strictServerAuthority,
         "If true, server will be much more strict about what clients can do. It is strongly recommended to ONLY have this enabled if absolutely necessary"
      );
      server.addBoolean("logging", serverLogging, "If true, will create log files for each server start");
      server.addSafeString("language", language);
      server.addInt("unloadLevelsCooldown", unloadLevelsCooldown, "The number of seconds a level will stay loaded after the last player has left it");
      server.addInt("droppedItemsLifeMinutes", droppedItemsLifeMinutes, "Minutes that dropped items will stay in the world. 0 or less for indefinite");
      server.addBoolean("unloadSettlements", unloadSettlements, "If the server should unload player settlements or keep them loaded");
      server.addInt("maxSettlementsPerPlayer", maxSettlementsPerPlayer, "The maximum amount of settlements per player. -1 or less means infinite");
      server.addInt("maxSettlersPerSettlement", maxSettlersPerSettlement, "The maximum amount of settlers per settlement. -1 or less means infinite");
      server.addBoolean("zipSaves", zipSaves, "If true, will create new saves uncompressed");
      server.addSafeString("MOTD", serverMOTD, "Message of the day");
      return server;
   }

   private static SaveData getModSaveData(LoadedMod mod, ModSettings modSettings) {
      SaveData save = new SaveData("");
      if (modSettings != null) {
         SaveData settingsData = new SaveData("SETTINGS");
         modSettings.addSaveData(settingsData);
         save.addSaveData(settingsData);
      }

      SaveData controls = new SaveData("CONTROLS");

      for (Control c : Control.getControls()) {
         if (c != null && c.mod == mod) {
            controls.addInt(c.id, c.getKey());
         }
      }

      SaveData controllerInputSaveData = new SaveData("CONTROLLER");
      ControllerInput.saveControllerBinds(controllerInputSaveData, mod);
      if (!controllerInputSaveData.isEmpty()) {
         controls.addSaveData(controllerInputSaveData);
      }

      if (!controls.isEmpty()) {
         save.addSaveData(controls);
      }

      return save;
   }

   private static void loadClientData(LoadData data) {
      LoadData general = data.getFirstLoadDataByName("GENERAL");
      if (general != null) {
         sceneSize = general.getFloat("sceneSize", sceneSize, GameWindow.minSceneSize, GameWindow.maxSceneSize);
         if (general.hasLoadDataByName("dynamicSceneSize")) {
            adjustZoomOnHighResolution = general.getBoolean("dynamicSceneSize", adjustZoomOnHighResolution);
         } else {
            adjustZoomOnHighResolution = general.getBoolean("adjustZoomOnHighResolution", adjustZoomOnHighResolution);
         }

         limitCameraToLevelBounds = general.getBoolean("limitCameraToLevelBounds", limitCameraToLevelBounds, false);
         pauseOnFocusLoss = general.getBoolean("pauseOnFocusLoss", pauseOnFocusLoss);
         savePerformanceOnFocusLoss = general.getBoolean("savePerformanceOnFocusLoss", savePerformanceOnFocusLoss);
         showSettlerHeadArmor = general.getBoolean("showSettlerHeadArmor", showSettlerHeadArmor);
         useTileObjectHitboxes = general.getBoolean("useTileObjectHitboxes", useTileObjectHitboxes, false);
         loadLevelBeforeSpawn = general.getBoolean("loadLevelBeforeSpawn", loadLevelBeforeSpawn, false);
         smartMining = general.getBoolean("smartMining", smartMining);
         craftingUseNearby.set(general.getBoolean("craftingUseNearby", craftingUseNearby.get()));
         minimapHidden = general.getBoolean("minimapHidden", minimapHidden);
         menuCameraPan = general.getBoolean("menuCameraPan", menuCameraPan);
         minimapZoomLevel = general.getInt("minimapZoomLevel", minimapZoomLevel);
         islandMapZoomLevel = general.getInt("islandMapZoomLevel", islandMapZoomLevel);
         show1p0Notice = general.getBoolean("show1p0Notice", show1p0Notice, false);
         showSteamDeckCursorNotice = general.getBoolean("showSteamDeckCursorNotice", showSteamDeckCursorNotice, false);
         patchNotesOpenYear = general.getInt("patchNotesOpenYear", patchNotesOpenYear, false);
         patchNotesOpenMonth = general.getInt("patchNotesOpenMonth", patchNotesOpenMonth, false);
         patchNotesOpenDay = general.getInt("patchNotesOpenDay", patchNotesOpenDay, false);
         unloadLevelsCooldown = general.getInt("unloadLevelsCooldown", unloadLevelsCooldown, 2, Integer.MAX_VALUE, false);
         worldBorderSize = general.getInt("worldBorderSize", worldBorderSize, -1, Integer.MAX_VALUE, false);
         droppedItemsLifeMinutes = general.getInt("droppedItemsLifeMinutes", droppedItemsLifeMinutes, false);
         unloadSettlements = general.getBoolean("unloadSettlements", unloadSettlements, false);
         maxSettlementsPerPlayer = general.getInt("maxSettlementsPerPlayer", maxSettlementsPerPlayer, false);
         maxSettlersPerSettlement = general.getInt("maxSettlersPerSettlement", maxSettlersPerSettlement, false);
         zipSaves = general.getBoolean("zipSaves", zipSaves);
         language = general.getSafeString("language", language);
         Language l = Localization.getLanguageStringID(language);
         if (l == null) {
            l = Localization.defaultLang;
         }

         l.setCurrent();
      } else {
         System.err.println("Could not load general settings.");
      }

      if (!Control.isControlsLoaded()) {
         Control.loadControls();
      }

      LoadData controls = data.getFirstLoadDataByName("CONTROLS");
      if (controls != null && Control.getControls() != null) {
         cursorJoystickSensitivity = controls.getFloat("cursorJoystickSensitivity", cursorJoystickSensitivity, false);

         for (Control c : Control.getControls()) {
            if (c != null && c.mod == null) {
               c.changeKey(controls.getInt(c.id, c.getKey()));
            }
         }

         LoadData controllerInputLoadData = controls.getFirstLoadDataByName("CONTROLLER");
         if (controllerInputLoadData != null && !controllerInputLoadData.isEmpty()) {
            ControllerInput.loadControllerBinds(controllerInputLoadData, null);
         }
      } else if (controls == null) {
         System.err.println("Could not load controls settings.");
      }

      LoadData interf = data.getFirstLoadDataByName("INTERFACE");
      if (interf != null) {
         interfaceSize = interf.getFloat("interfaceSize", interfaceSize, 1.0F, 2.0F);
         String styleString = interf.getSafeString("interfaceStyle", null, false);
         if (styleString != null) {
            UI = GameInterfaceStyle.getStyle(styleString);
         }

         if (interf.hasLoadDataByName("dynamicInterfaceSize")) {
            adjustInterfaceOnHighResolution = interf.getBoolean("dynamicInterfaceSize", adjustInterfaceOnHighResolution);
         } else {
            adjustInterfaceOnHighResolution = interf.getBoolean("adjustInterfaceOnHighResolution", adjustInterfaceOnHighResolution);
         }

         sharpenInterface = interf.getBoolean("sharpenInterface", sharpenInterface);
         pixelFont = interf.getBoolean("pixelFont", pixelFont);
         showDebugInfo = interf.getBoolean("showDebugInfo", showDebugInfo);
         showQuestMarkers = interf.getBoolean("showQuestMarkers", showQuestMarkers);
         showTeammateMarkers = interf.getBoolean("showTeammateMarkers", showTeammateMarkers);
         showPickupText = interf.getBoolean("showPickupText", showPickupText);
         showDamageText = interf.getBoolean("showDamageText", showDamageText);
         showDoTText = interf.getBoolean("showDoTText", showDoTText);
         showMobHealthBars = interf.getBoolean("showMobHealthBars", showMobHealthBars);
         showBossHealthBars = interf.getBoolean("showBossHealthBars", showBossHealthBars);
         showBasicTooltipBackground = interf.getBoolean("showBasicTooltipBackground", showBasicTooltipBackground);
         showItemTooltipBackground = interf.getBoolean("showItemTooltipBackground", showItemTooltipBackground);
         showControlTips = interf.getBoolean("showControlTips", showControlTips);
         showLogicGateTooltips = interf.getBoolean("showLogicGateTooltips", showLogicGateTooltips);
         showIngredientsAvailable = interf.getBoolean("showIngredientsAvailable", showIngredientsAvailable);
         alwaysShowQuickbar = interf.getBoolean("alwaysShowQuickbar", alwaysShowQuickbar);
         drawCursorManually = interf.getBoolean("drawCursorManually", drawCursorManually);
         tooltipTextSize = interf.getInt("tooltipTextSize", tooltipTextSize, 12, 32);
         cursorColor = interf.getColor("cursorColor", cursorColor);
         cursorSize = interf.getInt("cursorSize", cursorSize, -Renderer.cursorSizeOffset, Renderer.cursorSizes.length - Renderer.cursorSizeOffset - 1);
         trackNewQuests.set(interf.getBoolean("trackNewQuests", trackNewQuests.get()));
         displayJournalNotifications = interf.getBoolean("displayJournalNotifications", displayJournalNotifications, false);
         hasCraftingFilterExpanded.set(interf.getBoolean("craftingFilterExpanded", hasCraftingFilterExpanded.get()));
         hasCraftingListExpanded.set(interf.getBoolean("craftingListExpanded", hasCraftingListExpanded.get()));
         craftingListShowHidden.set(interf.getBoolean("craftingListShowHidden", craftingListShowHidden.get()));
         craftingListOnlyCraftable.set(interf.getBoolean("craftingListOnlyCraftable", craftingListOnlyCraftable.get()));
         highlightNewRecipesToWorkstation.set(interf.getBoolean("highlightNewRecipesToWorkstation", highlightNewRecipesToWorkstation.get()));
         hideSettlementStorage.set(interf.getBoolean("hideSettlementStorage", hideSettlementStorage.get(), false));
         hideSettlementWorkstations.set(interf.getBoolean("hideSettlementWorkstations", hideSettlementWorkstations.get(), false));
         hideSettlementForestryZones.set(interf.getBoolean("hideSettlementForestryZones", hideSettlementForestryZones.get(), false));
         hideSettlementHusbandryZones.set(interf.getBoolean("hideSettlementHusbandryZones", hideSettlementHusbandryZones.get(), false));
         hideSettlementFertilizeZones.set(interf.getBoolean("hideSettlementFertilizeZones", hideSettlementFertilizeZones.get(), false));
         mapCoordinates = interf.getEnum(FormTravelContainerGrid.CoordinateSetting.class, "mapCoordinates", mapCoordinates, false);
      } else {
         System.err.println("Could not load interface settings.");
      }

      LoadData sound = data.getFirstLoadDataByName("SOUND");
      if (sound != null) {
         outputDevice = sound.getSafeString("outputDevice", null, false);
         masterVolume = sound.getInt("masterVolume", (int)(masterVolume * 100.0F), 0, 100) / 100.0F;
         effectsVolume = sound.getInt("effectsVolume", (int)(effectsVolume * 100.0F), 0, 100) / 100.0F;
         weatherVolume = sound.getInt("weatherVolume", (int)(weatherVolume * 100.0F), 0, 100) / 100.0F;
         UIVolume = sound.getInt("UIVolume", (int)(UIVolume * 100.0F), 0, 100) / 100.0F;
         musicVolume = sound.getInt("musicVolume", (int)(musicVolume * 100.0F), 0, 100) / 100.0F;
         muteOnFocusLoss = sound.getBoolean("muteOnFocusLoss", muteOnFocusLoss);
      } else {
         System.err.println("Could not load sound settings.");
      }

      LoadData graphics = data.getFirstLoadDataByName("GRAPHICS");
      if (graphics != null) {
         displayMode = graphics.getEnum(DisplayMode.class, "displayMode", displayMode);
         displaySize = graphics.getDimension("displaySize", displaySize, false);
         monitor = graphics.getInt("monitor", monitor, 0, Integer.MAX_VALUE);
         vSyncEnabled = graphics.getBoolean("vSyncEnabled", vSyncEnabled);
         maxFPS = graphics.getInt("maxFPS", maxFPS);
         reduceUIFramerate = graphics.getBoolean("reduceUIFramerate", reduceUIFramerate);
         sceneColors = graphics.getEnum(SceneColorSetting.class, "sceneColors", sceneColors);
         brightness = graphics.getFloat("brightness", brightness, false);
         smoothLighting = graphics.getBoolean("smoothLighting", smoothLighting);
         lights = graphics.getEnum(Settings.LightSetting.class, "lights", lights);
         particles = graphics.getEnum(Settings.ParticleSetting.class, "particles", particles);
         wavyGrass = graphics.getBoolean("wavyGrass", wavyGrass);
         denseGrass = graphics.getBoolean("denseGrass", denseGrass);
         windEffects = graphics.getBoolean("windeffects", windEffects);
         heatDistortionEffects = graphics.getBoolean("heatDistortionEffects", heatDistortionEffects);
         cameraShake = graphics.getBoolean("cameraShake", cameraShake);
      } else {
         System.err.println("Could not load graphics settings.");
      }
   }

   private static void loadServerData(LoadData data) {
      serverPort = data.getInt("port", serverPort, 0, 65535);
      serverSlots = data.getInt("slots", serverSlots, 1, 250);
      serverPassword = data.getSafeString("password", serverPassword);
      maxClientLatencySeconds = data.getInt("maxClientLatencySeconds", maxClientLatencySeconds);
      pauseWhenEmpty = data.getBoolean("pauseWhenEmpty", pauseWhenEmpty);
      strictServerAuthority = data.getBoolean("strictServerAuthority", strictServerAuthority, false);
      serverLogging = data.getBoolean("logging", serverLogging);
      unloadLevelsCooldown = data.getInt("unloadLevelsCooldown", unloadLevelsCooldown, 2, Integer.MAX_VALUE, false);
      worldBorderSize = data.getInt("worldBorderSize", worldBorderSize, -1, Integer.MAX_VALUE, false);
      droppedItemsLifeMinutes = data.getInt("droppedItemsLifeMinutes", droppedItemsLifeMinutes, false);
      unloadSettlements = data.getBoolean("unloadSettlements", unloadSettlements, false);
      maxSettlementsPerPlayer = data.getInt("maxSettlementsPerPlayer", maxSettlementsPerPlayer, false);
      maxSettlersPerSettlement = data.getInt("maxSettlersPerSettlement", maxSettlersPerSettlement, false);
      zipSaves = data.getBoolean("zipSaves", zipSaves);
      language = data.getSafeString("language", language);
      Language l = Localization.getLanguageStringID(language);
      if (l == null) {
         l = Localization.defaultLang;
      }

      l.setCurrent();
      serverMOTD = data.getSafeString("MOTD", serverMOTD, false);
      serverWorld = data.getSafeString("world", serverWorld, false);
   }

   private static void loadModSettings(LoadedMod mod, ModSettings modSettings, LoadData save) {
      if (modSettings != null) {
         LoadData settingsData = save.getFirstLoadDataByName("SETTINGS");
         if (settingsData != null && settingsData.isArray()) {
            modSettings.applyLoadData(settingsData);
         }
      }

      LoadData controlsData = save.getFirstLoadDataByName("CONTROLS");
      if (controlsData != null) {
         for (Control c : Control.getControls()) {
            if (c != null && c.mod == mod) {
               c.changeKey(controlsData.getInt(c.id, c.getKey()));
            }
         }

         LoadData controllerInputLoadData = controlsData.getFirstLoadDataByName("CONTROLLER");
         if (controllerInputLoadData != null && !controllerInputLoadData.isEmpty()) {
            ControllerInput.loadControllerBinds(controllerInputLoadData, mod);
         }
      }
   }

   public static void loadClientSettings() {
      File file = new File(clientDir());
      if (!file.exists()) {
         System.out.println("Could not load settings file, does not exist. Creating new default " + file.getName());
         firstTimeSavedSettings = true;
         saveClientSettings();
      }

      Color oldCursorColor = cursorColor;

      try {
         loadClientData(new LoadData(file));
      } catch (Exception e) {
         System.err.println("Error loading client settings, some settings may be reset");
         e.printStackTrace();
         saveClientSettings();
      }

      SoundManager.updateVolume();
      if (ResourceEncoder.isLoaded() && !cursorColor.equals(oldCursorColor)) {
         Renderer.setCursorColor(cursorColor);
      }

      GameWindow window = WindowManager.getWindow();
      if (window != null && window.isCreated()) {
         window.setVSync(vSyncEnabled);
         window.updateSceneSize();
         window.updateHudSize();
      }

      loadModSettings(true);
      loadedClientSettings = true;
      if (GameLaunch.launchOptions.containsKey("debugstartup")) {
         System.out.println("STARTUP: Loaded client settings");
      }
   }

   public static boolean hasLoadedClientSettings() {
      return loadedClientSettings;
   }

   public static boolean isFirstTimeSavedSettings() {
      return firstTimeSavedSettings;
   }

   public static boolean loadServerSettings(File file, boolean saveIfNotExists) {
      boolean load = file.exists();
      if (!load && saveIfNotExists) {
         System.out.println("Could not load server settings file, does not exist. Creating new default " + file.getName());
         saveServerSettings();
         load = true;
      }

      if (load) {
         loadServerData(new LoadData(file));
      }

      return load;
   }

   public static void loadServerSettings() {
      File file = new File(serverDir());
      loadServerSettings(file, true);
      loadModSettings(true);
   }

   public static void loadModSettings(boolean saveIfNotExists) {
      if (ModLoader.hasLoadedMods()) {
         for (LoadedMod mod : ModLoader.getEnabledMods()) {
            ModSettings settings = mod.getSettings();

            try {
               File modFile = new File(clientModDir(mod));
               boolean load = modFile.exists();
               if (!load && saveIfNotExists) {
                  load = saveModSettings(mod);
               }

               if (load) {
                  loadModSettings(mod, settings, new LoadData(modFile));
               }
            } catch (Exception e) {
               throw new ModRuntimeException(mod, "Error loading mod settings", e);
            }
         }
      }
   }

   public static void saveClientSettings() {
      File file = new File(clientDir());
      getClientSaveData().saveScript(file);
      saveModSettings();
   }

   public static void saveServerSettings() {
      File file = new File(serverDir());
      getServerSaveData().saveScript(file);
      saveModSettings();
   }

   private static void saveModSettings() {
      if (ModLoader.hasLoadedMods()) {
         for (LoadedMod mod : ModLoader.getEnabledMods()) {
            saveModSettings(mod);
         }
      }
   }

   private static boolean saveModSettings(LoadedMod mod) {
      ModSettings settings = mod.getSettings();

      try {
         File modFile = new File(clientModDir(mod));
         SaveData data = getModSaveData(mod, settings);
         if (!data.isEmpty()) {
            data.saveScript(modFile);
            return true;
         } else {
            return false;
         }
      } catch (Exception e) {
         throw new ModRuntimeException(mod, "Error saving mod settings", e);
      }
   }

   public static void loadBanned() {
      banned = new ArrayList<>();
      File file = new File(GlobalData.cfgPath() + "banned.cfg");

      try {
         if (!file.exists()) {
            if (GameUtils.mkDirs(file)) {
               OutputStream make = new FileOutputStream(file);
               make.write(new byte[0]);
               make.close();
            } else {
               GameLog.warn.println("Could not create folders for file: " + file.getAbsolutePath());
            }
         }

         BufferedReader reader = new BufferedReader(new FileReader(file));

         String admin;
         while ((admin = reader.readLine()) != null) {
            banned.add(admin.trim().toLowerCase());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static boolean isBanned(String auth) {
      return banned.contains(auth.toLowerCase());
   }

   public static boolean isBanned(long auth) {
      return isBanned("" + auth);
   }

   public static void addBanned(String auth) {
      if (!banned.contains(auth.trim().toLowerCase())) {
         banned.add(auth.trim().toLowerCase());
         File file = new File(GlobalData.cfgPath() + "banned.cfg");

         try {
            OutputStream make = new FileOutputStream(file);

            for (String s : banned) {
               make.write(GameUtils.getStringBytes(s + "\n"));
            }

            make.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   public static boolean removeBanned(String auth) {
      if (banned.contains(auth.trim().toLowerCase())) {
         banned.remove(auth.trim().toLowerCase());
         File file = new File(GlobalData.cfgPath() + "banned.cfg");

         try {
            OutputStream make = new FileOutputStream(file);

            for (String s : banned) {
               make.write(GameUtils.getStringBytes(s + "\n"));
            }

            make.close();
         } catch (IOException e) {
            e.printStackTrace();
         }

         return true;
      } else {
         return false;
      }
   }

   public enum LightSetting {
      Color(new LocalMessage("settingsui", "lightscolor")),
      White(new LocalMessage("settingsui", "lightswhite"));

      public final GameMessage displayName;

      LightSetting(GameMessage displayName) {
         this.displayName = displayName;
      }
   }

   public enum ParticleSetting {
      Minimal(new LocalMessage("settingsui", "particlesmin")),
      Decreased(new LocalMessage("settingsui", "particlesdec")),
      Maximum(new LocalMessage("settingsui", "particlesmax"));

      public final GameMessage displayName;

      ParticleSetting(GameMessage displayName) {
         this.displayName = displayName;
      }
   }
}
