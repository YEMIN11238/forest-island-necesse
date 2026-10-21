package necesse.gfx.forms.presets;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.GameLoop;
import necesse.engine.gameLoop.GameLoopListener;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.LevelRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.ObjectValue;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.gfx.GameBackground;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormButton;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentButton;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.gfx.forms.position.FormPositionDynamic;
import necesse.gfx.forms.position.FormRelativePosition;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonIcon;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.placeableItem.MobSpawnItem;
import necesse.level.gameObject.AirObject;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.furniture.SettlerBedObject;
import necesse.level.gameTile.EmptyTile;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetCopyFilter;
import necesse.level.maps.presets.PresetMirrorException;
import necesse.level.maps.presets.PresetRotateException;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.set.PresetSet;

public class PresetDebugPreviewForm extends ContinueForm {
   public static int LEVEL_PRESET_TILES_PADDING = 1;
   public static int PREVIEW_PIXELS_PADDING = 24;
   public final Client client;
   protected Preset preset;
   protected Preset presetBackup;
   protected Consumer<Preset> onCopyClicked;
   protected GameMessage errorMessage;
   protected PresetCopyFilter presetCopyFilter = new PresetCopyFilter();
   protected Level previewLevel;
   protected WorldEntity worldEntity;
   protected GameFrameBuffer frameBuffer;
   protected FormLocalLabel errorLabel;
   protected FormContentBox presetDrawContentBox;
   protected FormContentBox presetSetsContentBox;
   protected Form settingsForm;
   protected boolean preventDispose;
   protected static HashMap<Class<? extends PresetSet<?>>, ArrayList<ObjectValue<String, PresetSet<?>>>> availablePresetSets = new HashMap<>();
   protected boolean isMirroredX = false;
   protected boolean isMirroredY = false;
   protected int rotations = 0;
   protected static ArrayList<Integer> excludedTiles = new ArrayList<>();
   protected static ArrayList<ObjectValue<Integer, Integer>> excludedObjects = new ArrayList<>();
   protected static HashSet<Integer> transparentObjects = new HashSet<>();
   protected HashMap<Point, Stack<String>> humans = new HashMap<>();
   protected HashMap<Point, HashMap<String, Integer>> mobs = new HashMap<>();
   protected static ArrayList<PresetDebugPreviewForm.AppliedPresetSet<?>> presetSetsToApply = new ArrayList<>();
   protected static String presetName = "";
   protected static String presetCreator = "";
   protected static boolean allowMirrorX = false;
   protected static boolean allowMirrorY = false;
   protected static boolean allowRotation = false;
   protected static boolean replaceWater = true;
   protected static final String presetCodeTemplate = "package necesse.level.maps.presets;\n\nimport necesse.engine.util.GameRandom;\nimport necesse.engine.util.LevelIdentifier;\nimport necesse.inventory.lootTable.LootTable;\nimport necesse.inventory.lootTable.lootItem.LootItem;\nimport necesse.level.maps.biomes.Biome;\nimport necesse.level.maps.presets.set.*;\n\nimport java.awt.*;\n\n/**\n* Originally created by: %creator%\n*/\npublic class %name%Preset extends Preset {\n\n    public %name%Preset(Biome biome, LevelIdentifier levelIdentifier, GameRandom random%params%) {\n        super(\"%preset%\");\n\n%modifications%\n    }\n}\n";
   protected static final String presetWorldGenCodeTemplate = "package necesse.engine.world.worldPresets;\n\nimport necesse.engine.util.GameRandom;\nimport necesse.engine.world.biomeGenerator.BiomeGeneratorStack;\nimport necesse.engine.util.LevelIdentifier;\nimport necesse.level.maps.biomes.Biome;\nimport necesse.level.maps.presets.%name%Preset;\nimport necesse.level.maps.presets.Preset;\nimport necesse.level.maps.presets.set.*;\n\n/**\n * Originally created by: %creator%\n */\npublic class %name%GenerationPreset extends SimpleGenerationPreset {\n\n    public final Biome biome;\n    public final LevelIdentifier levelIdentifier;\n%fields%\n\n    public %name%GenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {\n        super(20, %mirrorX%, %mirrorY%, %rotation%, false, biome);\n        this.biome = biome;\n        this.levelIdentifier = levelIdentifier;\n%constructor%\n    }\n\n    @Override\n    public void setupTester(WorldPresetTester tester) {\n        tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {\n           @Override\n           public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {\n               if (presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {\n                   return !generatorStack.isSurfaceOceanOrRiver(tileX, tileY);\n               } else if (presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {\n                   return !generatorStack.isCaveRiverOrLava(tileX, tileY);\n               } else if (presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {\n                   return !generatorStack.isDeepCaveLava(tileX, tileY);\n               }\n               return false;\n           }\n       }));\n    }\n\n    @Override\n    public Preset getPreset(GameRandom random) {\n        return new %name%Preset(biome, levelIdentifier, random%args%);\n    }\n\n}\n";

   public PresetDebugPreviewForm(Client client, Preset preset, Consumer<Preset> onCopyClicked) {
      super("presetPreview", 900, 800);
      this.client = client;
      this.preset = preset;
      this.presetBackup = preset.copy();
      this.onCopyClicked = onCopyClicked;
      this.settingsForm = this.addComponent(new Form(20, 200));
      this.settingsForm.overrideUseBaseAsSize = true;
      this.settingsForm.shouldLimitDrawArea = true;
      this.settingsForm.allowResize(true, false, false, false, event -> this.updateForm());
      this.settingsForm.setMinimumResize(0, 175);
      this.settingsForm.setMaximumResize(this.getWidth(), this.getHeight() - 100);
      this.presetSetsContentBox = new FormContentBox(4, 0, this.getWidth(), 150, GameBackground.indent);
      this.presetDrawContentBox = this.addComponent(new FormContentBox(0, 0, this.getWidth(), this.getHeight() - this.settingsForm.getHeight() - 8));
      this.presetCopyFilter.acceptObjectEntities = false;
      this.presetCopyFilter.acceptWires = false;
      this.onWindowResized(WindowManager.getWindow());
   }

   @Override
   protected void init() {
      super.init();
      this.updatePreview();
      this.updateForm();
      this.onWindowResized(WindowManager.getWindow());
      ((GameLoop)this.client.tickManager()).addGameLoopListener(new GameLoopListener() {
         @Override
         public void frameTick(TickManager tickManager, GameWindow window) {
            PresetDebugPreviewForm.this.frameTickPreview(tickManager);
         }

         @Override
         public void drawTick(TickManager tickManager) {
            Performance.record(tickManager, "presetPreviewDraw", () -> PresetDebugPreviewForm.this.renderPreview(tickManager));
         }

         @Override
         public boolean isDisposed() {
            return PresetDebugPreviewForm.this.isDisposed();
         }
      });
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isUsed() || !event.state || event.getID() != 256 && event.getID() != Control.INVENTORY.getKey()) {
         this.handleRightClickMenu(event);
         super.handleInputEvent(event, tickManager, perspective);
      } else {
         this.applyContinue();
         event.use();
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isUsed()
         || !event.buttonState
         || event.getState() != ControllerInput.MENU_BACK && event.getState() != ControllerInput.MAIN_MENU && event.getState() != ControllerInput.INVENTORY) {
         super.handleControllerEvent(event, tickManager, perspective);
      } else {
         this.applyContinue();
         event.use();
      }
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.setHeight(window.getHudHeight() - 50);
      this.setWidth(window.getHudWidth() - 50);
      this.settingsForm.setHeight(GameMath.limit(this.settingsForm.getHeight(), 200, this.getHeight() - 100));
      this.settingsForm.setMaximumResize(this.getWidth(), this.getHeight() - 100);
      this.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
      this.updateForm();
   }

   @Override
   public void dispose() {
      if (!this.preventDispose) {
         super.dispose();
         if (this.frameBuffer != null) {
            this.frameBuffer.dispose();
            this.frameBuffer = null;
         }

         if (this.previewLevel != null) {
            this.previewLevel.dispose();
            this.previewLevel = null;
         }
      }
   }

   protected void handleRightClickMenu(InputEvent event) {
      if (!event.isUsed() && event.isMouseClickEvent() && event.getID() == -99 && !event.state) {
         int presetDrawWidth = this.preset.width * 32 + PREVIEW_PIXELS_PADDING * 2;
         int presetDrawHeight = this.preset.height * 32 + PREVIEW_PIXELS_PADDING;
         int xOffset = 0;
         if (this.presetDrawContentBox.getWidth() > presetDrawWidth) {
            xOffset = (this.presetDrawContentBox.getWidth() - presetDrawWidth) / 2;
         }

         int deltaWidth = this.frameBuffer.getWidth() - presetDrawWidth;
         int deltaHeight = this.frameBuffer.getHeight() - presetDrawHeight;
         int xOffsetBuffer = -deltaWidth / 2;
         int yOffsetBuffer = -deltaHeight / 2;
         int formX = event.pos.hudX - this.getX();
         int formY = event.pos.hudY - this.getY();
         int x = formX - this.presetDrawContentBox.getX() + this.presetDrawContentBox.getScrollX() - xOffset - xOffsetBuffer;
         int y = formY - this.presetDrawContentBox.getY() + this.presetDrawContentBox.getScrollY() - yOffsetBuffer;
         int tileX = GameMath.getTileCoordinate(x);
         int tileY = GameMath.getTileCoordinate(y);
         SelectionFloatMenu.SelectionFloatMenuStyle menuStyle = SelectionFloatMenu.Solid(new FontOptions(16));
         if (this.previewLevel != null
            && formX > this.presetDrawContentBox.getX()
            && formX < this.presetDrawContentBox.getX() + this.presetDrawContentBox.getWidth()
            && formY > this.presetDrawContentBox.getY()
            && formY < this.presetDrawContentBox.getY() + this.presetDrawContentBox.getHeight()) {
            GameTile tile = this.previewLevel.getTile(tileX, tileY);
            GameObject[] objects = new GameObject[ObjectLayerRegistry.getTotalLayers()];

            for (Integer layerID : ObjectLayerRegistry.getLayerIDs()) {
               GameObject object = this.previewLevel.getObject(layerID, tileX, tileY);
               if (object.isMultiTile()) {
                  object = object.getMultiTile(0).getMasterObject();
               }

               objects[layerID] = object;
            }

            SelectionFloatMenu menu = new SelectionFloatMenu(this, menuStyle);
            SelectionFloatMenu excludeMenu = new SelectionFloatMenu(this, menuStyle);
            if (!(tile instanceof EmptyTile)) {
               excludeMenu.add(tile.getStringID() + " tiles", () -> {
                  excludedTiles.add(tile.getID());
                  this.updatePreview();
                  menu.remove();
               });
            }

            for (int layer = 0; layer < objects.length; layer++) {
               GameObject object = objects[layer];
               if (!(object instanceof AirObject)) {
                  int finalLayer = layer;
                  excludeMenu.add(object.getStringID() + " objects", () -> {
                     excludedObjects.add(new ObjectValue<>(finalLayer, object.getID()));
                     this.updatePreview();
                     menu.remove();
                  });
               }
            }

            if (!excludeMenu.isEmpty()) {
               menu.add("Exclude all", excludeMenu, false);
            }

            SelectionFloatMenu transparentMenu = new SelectionFloatMenu(this, menuStyle);
            if (!(objects[0] instanceof AirObject)) {
               transparentMenu.add(objects[0].getStringID() + " objects", () -> {
                  transparentObjects.add(objects[0].getID());
                  this.updatePreview();
                  menu.remove();
               });
            }

            if (!transparentMenu.isEmpty()) {
               menu.add("Make transparent", transparentMenu, false);
            }

            SelectionFloatMenu includeMenu = new SelectionFloatMenu(this, menuStyle);
            SelectionFloatMenu includeTilesMenu = new SelectionFloatMenu(this, menuStyle);

            for (Integer excludedTile : excludedTiles) {
               includeTilesMenu.add(TileRegistry.getTileStringID(excludedTile) + " tiles", () -> {
                  excludedTiles.remove(excludedTile);
                  this.updatePreview();
                  menu.remove();
               });
            }

            if (!includeTilesMenu.isEmpty()) {
               includeMenu.add("Tiles", includeTilesMenu, false);
            }

            SelectionFloatMenu includeObjectsMenu = new SelectionFloatMenu(this, menuStyle);

            for (ObjectValue<Integer, Integer> excludedObject : excludedObjects) {
               includeObjectsMenu.add(ObjectRegistry.getObjectStringID(excludedObject.value) + " objects", () -> {
                  excludedObjects.remove(excludedObject);
                  this.updatePreview();
                  menu.remove();
               });
            }

            for (Integer transparentObject : transparentObjects) {
               includeObjectsMenu.add(ObjectRegistry.getObjectStringID(transparentObject) + " objects (transparent)", () -> {
                  transparentObjects.remove(transparentObject);
                  this.updatePreview();
                  menu.remove();
               });
            }

            if (!includeObjectsMenu.isEmpty()) {
               includeMenu.add("Objects", includeObjectsMenu, false);
            }

            if (!includeMenu.isEmpty()) {
               menu.add("Include", includeMenu, false);
            }

            SelectionFloatMenu humanMenu = new SelectionFloatMenu(this, menuStyle);
            if (objects[0] instanceof SettlerBedObject) {
               SettlerBedObject bedObject = (SettlerBedObject)objects[0];
               LevelObject masterBed = bedObject.getSettlerBedMasterLevelObject(this.previewLevel, tileX, tileY);
               int bedX = masterBed.tileX;
               int bedY = masterBed.tileY;
               Point location = this.translatePointToPreset(new Point(bedX, bedY));
               Stack<String> currentHumans = this.humans.computeIfAbsent(location, loc -> new Stack<>());
               if (!currentHumans.isEmpty()) {
                  humanMenu.add("Remove last human", () -> {
                     currentHumans.pop();
                     menu.remove();
                  });
               }

               MobRegistry.getMobs().forEach(mob -> {
                  String mobID = mob.getStringID();
                  if (mobID.endsWith("human")) {
                     humanMenu.add("Add " + mobID, () -> {
                        currentHumans.push(mobID);
                        menu.remove();
                     });
                  }
               });
            }

            if (!humanMenu.isEmpty()) {
               menu.add("Humans", humanMenu, false);
            }

            SelectionFloatMenu mobsMenu = new SelectionFloatMenu(this, menuStyle);
            Point location = this.translatePointToPreset(new Point(tileX, tileY));
            SelectionFloatMenu removeMobsMenu = new SelectionFloatMenu(this, menuStyle);
            HashMap<String, Integer> currentMobs = this.mobs.computeIfAbsent(location, loc -> new HashMap<>());
            currentMobs.forEach((mobID, count) -> removeMobsMenu.add("Remove " + count + "x " + mobID, () -> {
               currentMobs.remove(mobID);
               menu.remove();
            }));
            if (!removeMobsMenu.isEmpty()) {
               mobsMenu.add("Remove mobs", removeMobsMenu, false);
            }

            SelectionFloatMenu uncategorizedMobsMenu = new SelectionFloatMenu(this, menuStyle);
            HashSet<String> remainingMobs = MobRegistry.getMobs().stream().map(IDDataContainer::getStringID).collect(Collectors.toCollection(HashSet::new));
            this.addMobItemCategoryChildren(mobsMenu, ItemCategory.getCategory("mobs"), remainingMobs, mobID -> {
               HashMap<String, Integer> mobLocation = this.mobs.get(location);
               int mobCount = mobLocation.computeIfAbsent(mobID, s -> 0);
               mobLocation.put(mobID, mobCount + 1);
            });
            remainingMobs.stream().sorted().forEach(mobID -> uncategorizedMobsMenu.add(mobID, () -> {
               HashMap<String, Integer> mobLocation = this.mobs.get(location);
               int mobCount = mobLocation.computeIfAbsent(mobID, s -> 0);
               mobLocation.put(mobID, mobCount + 1);
            }));
            if (!uncategorizedMobsMenu.isEmpty()) {
               mobsMenu.add("Uncategorized", uncategorizedMobsMenu, false);
            }

            if (!mobsMenu.isEmpty()) {
               menu.add("Mobs", mobsMenu, false);
            }

            SelectionFloatMenu presetSetsMenu = new SelectionFloatMenu(this, menuStyle);
            HashSet<Class<? extends PresetSet<?>>> addedPresetSets = new HashSet<>();
            availablePresetSets.forEach((presetSetClass, presetSets) -> presetSets.forEach(presetSet -> {
               boolean canReplace = false;

               for (GameObject object : objects) {
                  if (presetSet.value.canReplaceObject(object.getID())) {
                     canReplace = true;
                     break;
                  }
               }

               if (!canReplace) {
                  canReplace = presetSet.value.canReplaceTile(tile.getID());
               }

               if (canReplace && addedPresetSets.add(presetSetClass)) {
                  presetSetsMenu.add(presetSetClass.getSimpleName(), () -> {
                     presetSetsToApply.add(new PresetDebugPreviewForm.AppliedPresetSet(presetSetClass, presetSet, presetSet));
                     this.updatePresetSetSection();
                     menu.remove();
                  });
               }
            }));
            if (!presetSetsMenu.isEmpty()) {
               menu.add("Apply preset sets", presetSetsMenu, false);
            }

            if (!menu.isEmpty()) {
               this.getManager().openFloatMenu(menu);
               event.use();
            }
         }
      }
   }

   protected void addMobItemCategoryChildren(SelectionFloatMenu menu, ItemCategory category, HashSet<String> remainingMobs, Consumer<String> mobConsumer) {
      category.getChildren().forEach(childCategory -> {
         SelectionFloatMenu childMenu = new SelectionFloatMenu(this, SelectionFloatMenu.Solid(new FontOptions(16)));
         menu.add(childCategory.stringID, childMenu, false);
         this.addMobItemCategoryChildren(childMenu, childCategory, remainingMobs, mobConsumer);
      });
      remainingMobs.stream().sorted().filter(mobID -> {
         MobSpawnItem spawnItem = MobRegistry.getMobSpawnItemID(mobID);
         if (spawnItem != null && ItemCategory.getItemsCategory(spawnItem) == category) {
            menu.add(mobID, () -> mobConsumer.accept(mobID));
            return true;
         } else {
            return false;
         }
      }).collect(Collectors.toList()).forEach(remainingMobs::remove);
   }

   public Preset getConfiguredPreset(boolean applyPresetSets) {
      Preset configuredPreset = this.preset;
      configuredPreset = configuredPreset.copy(this.presetCopyFilter);

      for (Integer excludedTile : excludedTiles) {
         configuredPreset.replaceTile(excludedTile, -1);
      }

      for (ObjectValue<Integer, Integer> excludedObject : excludedObjects) {
         GameObject object = ObjectRegistry.getObject(excludedObject.value);
         if (object.isMultiTile()) {
            for (int id : object.getMultiTile(0).ids) {
               configuredPreset.replaceObjectLayer(excludedObject.object, id, 0, 0);
            }
         } else {
            configuredPreset.replaceObjectLayer(excludedObject.object, excludedObject.value, 0, 0);
         }
      }

      for (Integer id : transparentObjects) {
         GameObject object = ObjectRegistry.getObject(id);
         if (object.isMultiTile()) {
            for (int multiID : object.getMultiTile(0).ids) {
               configuredPreset.replaceObjectLayer(0, multiID, -1, 0);
            }
         } else {
            configuredPreset.replaceObjectLayer(0, id, -1, 0);
         }
      }

      if (applyPresetSets) {
         for (PresetDebugPreviewForm.AppliedPresetSet<?> presetToApply : presetSetsToApply) {
            presetToApply.apply(configuredPreset);
         }
      }

      return configuredPreset;
   }

   protected void updatePreview() {
      if (this.frameBuffer != null) {
         this.frameBuffer.dispose();
         this.frameBuffer = null;
      }

      if (this.previewLevel != null) {
         this.previewLevel.dispose();
         this.previewLevel = null;
      }

      try {
         this.worldEntity = WorldEntity.getDebugWorldEntity(this.client.worldEntity);
         this.previewLevel = LevelRegistry.getNewLevel(
            0,
            new LevelIdentifier("preset-preview"),
            this.preset.width + LEVEL_PRESET_TILES_PADDING * 2,
            this.preset.height + LEVEL_PRESET_TILES_PADDING * 2,
            this.worldEntity
         );
         this.previewLevel.alwaysDrawWire = true;
         this.previewLevel.baseBiome = BiomeRegistry.FOREST;

         for (int tileX = 0; tileX < this.previewLevel.tileWidth; tileX++) {
            for (int tileY = 0; tileY < this.previewLevel.tileHeight; tileY++) {
               this.previewLevel.setTile(tileX, tileY, TileRegistry.emptyID);
            }
         }

         this.previewLevel.lightManager.ambientLightOverride = new GameLight(150.0F);
         this.previewLevel.lightManager.ambientLight = this.previewLevel.lightManager.ambientLightOverride;
         Preset preset = this.getConfiguredPreset(true);
         preset.applyToLevel(this.previewLevel, LEVEL_PRESET_TILES_PADDING, LEVEL_PRESET_TILES_PADDING, new GameBlackboard());
         this.previewLevel.onLoadingComplete();
         this.frameBuffer = Renderer.getNewFrameBuffer(this.previewLevel.tileWidth * 32, this.previewLevel.tileHeight * 32);
         this.renderPreview(null);
      } catch (Exception e) {
         e.printStackTrace(GameLog.err);
         if (this.frameBuffer != null) {
            this.frameBuffer.dispose();
            this.frameBuffer = null;
         }

         if (this.previewLevel != null) {
            this.previewLevel.dispose();
            this.previewLevel = null;
         }
      }
   }

   protected void frameTickPreview(TickManager tickManager) {
      if (this.previewLevel != null) {
         Performance.record(tickManager, "presetPreviewTick", () -> {
            this.worldEntity.serverFrameTick(tickManager);
            this.previewLevel.frameTick(tickManager);
            if (tickManager.isGameTick()) {
               this.worldEntity.serverTick();
               this.previewLevel.clientTick();
               this.previewLevel.serverTick();
               GameCamera camera = new GameCamera(0, 0, this.previewLevel.tileWidth * 32, this.previewLevel.tileHeight * 32);
               this.previewLevel.tickEffect(camera, this.client.getPlayer());
            }
         });
      }
   }

   protected void renderPreview(TickManager tickManager) {
      if (this.frameBuffer != null && this.previewLevel != null) {
         GameCamera camera = new GameCamera(0, 0, this.frameBuffer.getWidth(), this.frameBuffer.getHeight());
         Renderer.renderLevelToBuffer(this.frameBuffer, this.previewLevel, this.client.getPlayer(), camera, tickManager);
         this.frameBuffer.bindFrameBuffer();

         for (int y = 0; y < this.preset.height; y++) {
            for (int x = 0; x < this.preset.width; x++) {
               int objectID = this.preset.getObject(0, x, y);
               if (transparentObjects.contains(objectID)) {
                  Renderer.drawShape(
                     new Rectangle(camera.getTileDrawX(x + LEVEL_PRESET_TILES_PADDING), camera.getTileDrawY(y + LEVEL_PRESET_TILES_PADDING), 32, 32),
                     true,
                     1.0F,
                     0.0F,
                     0.0F,
                     0.5F
                  );
               }
            }
         }

         this.humans.forEach((location, mobs) -> {
            if (!mobs.isEmpty()) {
               location = this.translatePointToPreset(location);
               int drawX = camera.getTileDrawX(location.x);
               int drawY = camera.getTileDrawY(location.y);
               String mobID = mobs.peek();
               MobRegistry.getMobIcon(mobID).initDraw().pos(drawX, drawY).draw();
               FontManager.bit.drawString(drawX + 10, drawY + 10, "" + mobs.size(), new FontOptions(12).outline());
            }
         });
         this.mobs.forEach((location, mobs) -> {
            int mobCount = mobs.size();
            int split;
            int fontSize;
            if (mobCount > 1 && mobCount <= 4) {
               split = 2;
               fontSize = 10;
            } else if (mobCount > 4) {
               split = 3;
               fontSize = 8;
            } else {
               split = 1;
               fontSize = 12;
            }

            if (!mobs.isEmpty()) {
               location = this.translatePointToPreset(location);
               int drawX = camera.getTileDrawX(location.x);
               int drawY = camera.getTileDrawY(location.y);
               int currentMobIndex = 0;

               for (Entry<String, Integer> entry : mobs.entrySet()) {
                  String mobID = entry.getKey();
                  int size = 32 / split;
                  int xx = drawX + currentMobIndex % split * size;
                  int yx = drawY + currentMobIndex / split * size;
                  MobRegistry.getMobIcon(mobID).initDraw().size(size).pos(xx, yx).draw();
                  FontManager.bit.drawString(xx + size / 2.0F, yx + size / 2.0F, "" + entry.getValue(), new FontOptions(fontSize).outline());
                  currentMobIndex++;
               }
            }
         });
         this.frameBuffer.unbindFrameBuffer();
      }
   }

   protected void updateForm() {
      this.presetDrawContentBox.clearComponents();
      final int presetDrawWidth = this.preset.width * 32 + PREVIEW_PIXELS_PADDING * 2;
      final int presetDrawHeight = this.preset.height * 32 + PREVIEW_PIXELS_PADDING;
      this.presetDrawContentBox.addComponent(new FormCustomDraw(0, 0, presetDrawWidth, presetDrawHeight) {
         @Override
         public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
            int xOffset = 0;
            if (PresetDebugPreviewForm.this.presetDrawContentBox.getWidth() > presetDrawWidth) {
               xOffset = (PresetDebugPreviewForm.this.presetDrawContentBox.getWidth() - presetDrawWidth) / 2;
            }

            if (PresetDebugPreviewForm.this.frameBuffer == null) {
               GameResources.error.initDraw().size(this.width, this.height).draw(this.getX() + xOffset, this.getY());
            } else {
               int deltaWidth = PresetDebugPreviewForm.this.frameBuffer.getWidth() - presetDrawWidth;
               int deltaHeight = PresetDebugPreviewForm.this.frameBuffer.getHeight() - presetDrawHeight;
               int xOffsetBuffer = -deltaWidth / 2;
               int yOffsetBuffer = -deltaHeight / 2;
               PresetDebugPreviewForm.this.frameBuffer.initDraw().draw(this.getX() + xOffset + xOffsetBuffer, this.getY() + yOffsetBuffer);
            }
         }
      });
      this.presetDrawContentBox.setWidth(this.getWidth());
      this.setWidth(this.presetDrawContentBox.getWidth());
      this.presetDrawContentBox.setHeight(this.getHeight() - this.settingsForm.getHeight() - 15);
      this.presetDrawContentBox.setContentBox(new Rectangle(presetDrawWidth, presetDrawHeight));
      FormFlow flow = new FormFlow(5);
      flow.nextY(this.presetDrawContentBox, 10);
      this.updateSettingsForm();
      this.settingsForm.setPosition(0, this.getHeight() - this.settingsForm.getHeight());
      WindowManager.getWindow().submitNextMoveEvent();
   }

   protected void updateSettingsForm() {
      this.settingsForm.setWidth(this.getWidth());
      this.settingsForm.clearComponents();
      FormFlow settingsFlow = new FormFlow(4);
      this.addConfigButtons(settingsFlow);
      if (this.errorLabel != null) {
         this.settingsForm.removeComponent(this.errorLabel);
         this.errorLabel = null;
      }

      if (this.errorMessage != null) {
         settingsFlow.next(4);
         FontOptions fontOptions = new FontOptions(16).color(Settings.UI.errorTextColor);
         this.errorLabel = this.settingsForm
            .addComponent(settingsFlow.nextY(new FormLocalLabel(this.errorMessage, fontOptions, 0, this.getWidth() / 2, 0, this.getWidth() - 20), 4));
      }

      settingsFlow.next(4);
      this.settingsForm.addComponent(settingsFlow.nextY(this.presetSetsContentBox, 10));
      this.updatePresetSetSection();
      int endButtonsWidth = this.settingsForm.getWidth() / 3 - 8 - 2;
      FormTextButton bottomButton = this.settingsForm
         .addComponent(new FormLocalTextButton("ui", "presettoclipboard", 0, 0, endButtonsWidth, FormInputSize.SIZE_32, ButtonColor.BASE));
      bottomButton.setPosition(new FormPositionDynamic(() -> 4, () -> this.settingsForm.getHeight() - 40));
      bottomButton.onClicked(e -> {
         GameWindow window = WindowManager.getWindow();
         Preset newPreset = this.getConfiguredPreset(false);
         window.putClipboardDefault(newPreset.getScript());
         this.applyContinue();
         this.onCopyClicked.accept(newPreset);
      });
      bottomButton = this.settingsForm.addComponent(new FormTextButton("Create worldgen file", 0, 0, endButtonsWidth, FormInputSize.SIZE_32, ButtonColor.BASE));
      bottomButton.setPosition(new FormPositionDynamic(() -> 4 + endButtonsWidth + 4, () -> this.settingsForm.getHeight() - 40));
      bottomButton.onClicked(this::showWorldGenFileDialog);
      bottomButton = this.settingsForm
         .addComponent(new FormLocalTextButton("ui", "closebutton", 4 + (endButtonsWidth + 4) * 2, 0, endButtonsWidth, FormInputSize.SIZE_32, ButtonColor.BASE));
      bottomButton.setPosition(new FormPositionDynamic(() -> 4 + (endButtonsWidth + 4) * 2, () -> this.settingsForm.getHeight() - 40));
      bottomButton.onClicked(e -> this.applyContinue());
   }

   protected void addConfigButtons(FormFlow yFlow) {
      ArrayList<ArrayList<PresetDebugPreviewForm.ConfigButton>> lines = new ArrayList<>();
      ArrayList<PresetDebugPreviewForm.ConfigButton> configButtons = new ArrayList<>();
      lines.add(configButtons);
      configButtons.add(new PresetDebugPreviewForm.ConfigButton(Settings.UI.button_reset_20, new StaticMessage("Reset rotation and mirror")) {
         @Override
         public void onClick() {
            PresetDebugPreviewForm.this.rotations = 0;
            PresetDebugPreviewForm.this.isMirroredX = false;
            PresetDebugPreviewForm.this.isMirroredY = false;
            PresetDebugPreviewForm.this.preset = PresetDebugPreviewForm.this.presetBackup.copy();
            PresetDebugPreviewForm.this.updatePreview();
            PresetDebugPreviewForm.this.clearErrorMessage();
         }
      });
      configButtons.add(new PresetDebugPreviewForm.ConfigButton(Settings.UI.mirror_horizontal_32, new LocalMessage("ui", "presetmirrorx")) {
         @Override
         public void onClick() {
            try {
               PresetDebugPreviewForm.this.preset = PresetDebugPreviewForm.this.preset.mirrorX();
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.clearErrorMessage();
               PresetDebugPreviewForm.this.isMirroredX = !PresetDebugPreviewForm.this.isMirroredX;
            } catch (PresetMirrorException ex) {
               PresetDebugPreviewForm.this.setErrorMessage(ex.getGameMessage());
            }
         }
      });
      configButtons.add(new PresetDebugPreviewForm.ConfigButton(Settings.UI.mirror_vertical_32, new LocalMessage("ui", "presetmirrory")) {
         @Override
         public void onClick() {
            try {
               PresetDebugPreviewForm.this.preset = PresetDebugPreviewForm.this.preset.mirrorY();
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.clearErrorMessage();
               PresetDebugPreviewForm.this.isMirroredY = !PresetDebugPreviewForm.this.isMirroredY;
            } catch (PresetMirrorException ex) {
               PresetDebugPreviewForm.this.setErrorMessage(ex.getGameMessage());
            }
         }
      });
      configButtons.add(new PresetDebugPreviewForm.ConfigButton(Settings.UI.rotate_clockwise_32, new LocalMessage("ui", "presetrotateclockwise")) {
         @Override
         public void onClick() {
            try {
               PresetDebugPreviewForm.this.preset = PresetDebugPreviewForm.this.preset.rotate(PresetRotation.CLOCKWISE);
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.updateForm();
               PresetDebugPreviewForm.this.clearErrorMessage();
               PresetDebugPreviewForm.this.rotations++;
            } catch (PresetRotateException ex) {
               PresetDebugPreviewForm.this.setErrorMessage(ex.getGameMessage());
            }
         }
      });
      configButtons.add(
         new PresetDebugPreviewForm.ConfigButton(Settings.UI.rotate_counterclockwise_32, new LocalMessage("ui", "presetrotatecounterclockwise")) {
            @Override
            public void onClick() {
               try {
                  PresetDebugPreviewForm.this.preset = PresetDebugPreviewForm.this.preset.rotate(PresetRotation.ANTI_CLOCKWISE);
                  PresetDebugPreviewForm.this.updatePreview();
                  PresetDebugPreviewForm.this.updateForm();
                  PresetDebugPreviewForm.this.clearErrorMessage();
                  PresetDebugPreviewForm.this.rotations--;
               } catch (PresetRotateException ex) {
                  PresetDebugPreviewForm.this.setErrorMessage(ex.getGameMessage());
               }
            }
         }
      );
      configButtons = new ArrayList<>();
      lines.add(configButtons);
      configButtons.add(
         new PresetDebugPreviewForm.ConfigButton(
            () -> this.presetCopyFilter.acceptTiles ? Settings.UI.toggle_tiles_on_32 : Settings.UI.toggle_tiles_off_32,
            () -> new LocalMessage("ui", "presettoggletiles")
         ) {
            @Override
            public void onClick() {
               PresetDebugPreviewForm.this.presetCopyFilter.acceptTiles = !PresetDebugPreviewForm.this.presetCopyFilter.acceptTiles;
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.clearErrorMessage();
            }
         }
      );
      configButtons.add(
         new PresetDebugPreviewForm.ConfigButton(
            () -> this.presetCopyFilter.acceptObjects ? Settings.UI.toggle_objects_on_32 : Settings.UI.toggle_objects_off_32,
            () -> new StaticMessage("Toogle objects")
         ) {
            @Override
            public void onClick() {
               PresetDebugPreviewForm.this.presetCopyFilter.acceptObjects = !PresetDebugPreviewForm.this.presetCopyFilter.acceptObjects;
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.clearErrorMessage();
            }
         }
      );
      configButtons.add(
         new PresetDebugPreviewForm.ConfigButton(
            () -> this.presetCopyFilter.acceptObjectEntities ? Settings.UI.toggle_object_entities_on_32 : Settings.UI.toggle_object_entities_off_32,
            () -> new StaticMessage("Toogle object entities")
         ) {
            @Override
            public void onClick() {
               PresetDebugPreviewForm.this.presetCopyFilter.acceptObjectEntities = !PresetDebugPreviewForm.this.presetCopyFilter.acceptObjectEntities;
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.clearErrorMessage();
            }
         }
      );
      configButtons.add(
         new PresetDebugPreviewForm.ConfigButton(
            () -> this.presetCopyFilter.acceptWires ? Settings.UI.toggle_wires_on_32 : Settings.UI.toggle_wires_off_32, () -> new StaticMessage("Toogle wires")
         ) {
            @Override
            public void onClick() {
               PresetDebugPreviewForm.this.presetCopyFilter.acceptWires = !PresetDebugPreviewForm.this.presetCopyFilter.acceptWires;
               PresetDebugPreviewForm.this.updatePreview();
               PresetDebugPreviewForm.this.clearErrorMessage();
            }
         }
      );
      configButtons.add(
         new PresetDebugPreviewForm.ConfigButton(
            () -> this.preset.clearOtherWires ? Settings.UI.toggle_clear_wires_on_32 : Settings.UI.toggle_clear_wires_off_32,
            () -> this.preset.clearOtherWires ? new LocalMessage("ui", "presetclearwiresontip") : new LocalMessage("ui", "presetclearwiresofftip")
         ) {
            @Override
            public void onClick() {
               PresetDebugPreviewForm.this.preset.clearOtherWires = !PresetDebugPreviewForm.this.preset.clearOtherWires;
               PresetDebugPreviewForm.this.clearErrorMessage();
            }

            @Override
            public boolean isActive() {
               return PresetDebugPreviewForm.this.presetCopyFilter.acceptWires;
            }
         }
      );
      int buttonPadding = 2;
      int totalWidth = this.settingsForm.getWidth() - (8 - buttonPadding * 2);
      FormInputSize buttonSize = FormInputSize.SIZE_32;
      int buttonsWidth = buttonSize.height + buttonPadding * 2;
      int buttonsHeight = buttonSize.height + buttonPadding * 2;
      int buttonsStartY = yFlow.next(buttonsHeight * lines.size());

      for (int y = 0; y < lines.size(); y++) {
         configButtons = lines.get(y);
         int buttonsXOffset = (totalWidth - configButtons.size() * buttonsWidth) / 2;

         for (int x = 0; x < configButtons.size(); x++) {
            final PresetDebugPreviewForm.ConfigButton configButton = configButtons.get(x);
            int buttonX = 4 + buttonsXOffset + x * buttonsWidth + buttonPadding;
            int buttonY = buttonsStartY + y * buttonsHeight + buttonPadding;
            FormContentButton formButton = this.settingsForm
               .addComponent(new FormContentButton(buttonX, buttonY, buttonSize.height, buttonSize, ButtonColor.BASE) {
                  @Override
                  protected void drawContent(int x, int yx, int width, int height) {
                     ButtonIcon buttonIcon = configButton.iconGetter.get();
                     GameTexture texture = buttonIcon.texture;
                     int drawXOffset = (width - texture.getWidth()) / 2;
                     int drawYOffset = (height - texture.getHeight()) / 2;
                     texture.initDraw().color(buttonIcon.colorGetter.apply(this.getButtonState())).draw(x + drawXOffset, yx + drawYOffset);
                  }

                  @Override
                  protected void addTooltips(PlayerMob perspective) {
                     if (configButton.tooltip != null) {
                        GameMessage tooltip = configButton.tooltip.get();
                        if (tooltip != null) {
                           GameTooltipManager.addTooltip(new StringTooltips(tooltip.translate(), 400), TooltipLocation.FORM_FOCUS);
                        }
                     }
                  }

                  @Override
                  public boolean isActive() {
                     return configButton.isActive();
                  }
               });
            formButton.onClicked(e -> configButton.onClick());
         }
      }
   }

   protected void showWorldGenFileDialog(FormInputEvent<FormButton> e) {
      int width = 600;
      ConfirmationContinueForm dialog = new ConfirmationContinueForm("Create world generation file", width, 400);
      FormTextInput nameInput = new FormTextInput(4, 4, FormInputSize.SIZE_32, width - 4, 50);
      FormTextInput creatorInput = new FormTextInput(4, 50, FormInputSize.SIZE_32, width - 4, 50);
      FormCheckBox randomizeMirrorX = new FormCheckBox("Randomize mirror x", 4, 100, allowMirrorX);
      randomizeMirrorX.onClicked(event -> {
         allowMirrorX = !allowMirrorX;
         randomizeMirrorX.checked = allowMirrorX;
      });
      FormCheckBox randomizeMirrorY = new FormCheckBox("Randomize mirror y", 204, 100, allowMirrorY);
      randomizeMirrorY.onClicked(event -> {
         allowMirrorY = !allowMirrorY;
         randomizeMirrorY.checked = allowMirrorY;
      });
      FormCheckBox randomizeRotation = new FormCheckBox("Randomize rotation", 404, 100, allowRotation);
      randomizeRotation.onClicked(event -> {
         allowRotation = !allowRotation;
         randomizeRotation.checked = allowRotation;
      });
      FormCheckBox replaceLiquidTiles = new FormCheckBox("Replace liquid tiles", 4, 130, replaceWater) {
         @Override
         public GameTooltips getTooltip() {
            return new StringTooltips(
               "Replaces all generated liquid tiles within the preset area with shore representing the biome.\nPrevents empty tiles from being liquid."
            );
         }
      };
      replaceLiquidTiles.onClicked(event -> {
         replaceWater = !replaceWater;
         replaceLiquidTiles.checked = replaceWater;
      });
      PresetDebugPreviewForm.LevelGenerationSetup surfaceSetup = new PresetDebugPreviewForm.LevelGenerationSetup(
         "src/main/java/necesse/engine/world/worldPresets/SurfacePresetsWorldPreset.java", "SURFACE_IDENTIFIER", "Surface", 175
      );
      PresetDebugPreviewForm.LevelGenerationSetup caveSetup = new PresetDebugPreviewForm.LevelGenerationSetup(
         "src/main/java/necesse/engine/world/worldPresets/CavePresetsWorldPreset.java", "CAVE_IDENTIFIER", "Cave", 235
      );
      PresetDebugPreviewForm.LevelGenerationSetup deepCaveSetup = new PresetDebugPreviewForm.LevelGenerationSetup(
         "src/main/java/necesse/engine/world/worldPresets/DeepCavePresetsWorldPreset.java", "DEEP_CAVE_IDENTIFIER", "Deep cave", 295
      );
      nameInput.onChange(event -> {
         String trimmedName = nameInput.getText().replace(" ", "").trim();
         surfaceSetup.onPresetNameChanged(trimmedName);
         caveSetup.onPresetNameChanged(trimmedName);
         deepCaveSetup.onPresetNameChanged(trimmedName);
      });
      MainGame mainGame = (MainGame)GlobalData.getCurrentState();
      dialog.setupConfirmation(setup -> {
         setup.addComponent(nameInput);
         nameInput.setText(presetName);
         nameInput.placeHolder = new StaticMessage("Name");
         nameInput.tooltip = new StaticMessage("Name of the preset");
         nameInput.onChange(event -> presetName = nameInput.getText());
         setup.addComponent(creatorInput);
         creatorInput.setText(presetCreator);
         creatorInput.placeHolder = new StaticMessage("Original creator");
         creatorInput.tooltip = new StaticMessage("Original creator of the preset");
         creatorInput.onChange(event -> presetCreator = creatorInput.getText());
         setup.addComponent(randomizeMirrorX);
         setup.addComponent(randomizeMirrorY);
         setup.addComponent(randomizeRotation);
         setup.addComponent(replaceLiquidTiles);
         surfaceSetup.addComponentsToWorldGenerationDialogue(setup);
         caveSetup.addComponentsToWorldGenerationDialogue(setup);
         deepCaveSetup.addComponentsToWorldGenerationDialogue(setup);
      }, () -> {
         String name = nameInput.getText().replace(" ", "");
         if (name.isEmpty()) {
            name = nameInput.placeHolder.translate();
         }

         String creator = creatorInput.getText().replace(" ", "");
         if (creator.isEmpty()) {
            creator = creatorInput.placeHolder.translate();
         }

         this.generateWorldGenFile(name, creator, randomizeMirrorX.checked, randomizeMirrorY.checked, randomizeRotation.checked, replaceLiquidTiles.checked);
         surfaceSetup.saveLevelSetupToFile();
         caveSetup.saveLevelSetupToFile();
         deepCaveSetup.saveLevelSetupToFile();
         this.preventDispose = false;
         mainGame.formManager.addContinueForm("presetPreview", this);
      }, () -> {
         this.preventDispose = false;
         mainGame.formManager.addContinueForm("presetPreview", this);
      });
      ContainerComponent.setPosMiddle(dialog);
      this.preventDispose = true;
      mainGame.formManager.addContinueForm("presetPreview", dialog);
   }

   protected void updatePresetSetSection() {
      this.presetSetsContentBox.setHeight(this.settingsForm.getHeight() - this.presetSetsContentBox.getY() - 50);
      this.presetSetsContentBox.setWidth(this.settingsForm.getWidth() - 8);
      this.presetSetsContentBox.clearComponents();
      FormFlow yFlow = new FormFlow(4);

      for (PresetDebugPreviewForm.AppliedPresetSet<?> appliedPresetSetRaw : presetSetsToApply) {
         this.addPresetSetForms(appliedPresetSetRaw, yFlow, this.presetSetsContentBox.getWidth() - 8);
      }

      this.presetSetsContentBox.fitContentBoxToComponents();
      this.updatePreview();
   }

   protected <T extends PresetSet<T>> void addPresetSetForms(final PresetDebugPreviewForm.AppliedPresetSet<T> appliedPresetSet, FormFlow yFlow, int width) {
      int y = yFlow.next(28);
      FormContentIconButton removeButton = this.presetSetsContentBox
         .addComponent(
            new FormContentIconButton(
               4, y, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.container_storage_remove, new StaticMessage("Remove PresetSet")
            )
         );
      removeButton.onClicked(e -> {
         presetSetsToApply.remove(appliedPresetSet);
         this.updatePresetSetSection();
      });
      FormLabel label = this.presetSetsContentBox.addComponent(new FormLabel("", new FontOptions(16), -1, 0, y + 4) {
         boolean isHovering = false;

         @Override
         public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
            super.handleInputEvent(event, tickManager, perspective);
            this.isHovering = this.isMouseOver(event);
         }

         @Override
         public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
            super.draw(tickManager, perspective, renderBox);
            if (this.isHovering) {
               GameTooltipManager.addTooltip(new StringTooltips(appliedPresetSet.presetSetClass.getSimpleName(), 400), TooltipLocation.FORM_FOCUS);
            }
         }
      });
      label.setPosition(new FormRelativePosition(removeButton, removeButton.getWidth() + 4, 4));
      FormDropdownSelectionButton<ObjectValue<String, T>> toReplaceDropDown = this.presetSetsContentBox
         .addComponent(new FormDropdownSelectionButton<>(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, 150));
      toReplaceDropDown.setPosition(new FormRelativePosition(label, 4, -4));
      FormDropdownSelectionButton<ObjectValue<String, T>> replacementDropDown = this.presetSetsContentBox
         .addComponent(new FormDropdownSelectionButton<>(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, 150));
      replacementDropDown.setPosition(new FormRelativePosition(toReplaceDropDown, toReplaceDropDown.getWidth() + 4, 0));
      ArrayList<ObjectValue<String, PresetSet<?>>> presetSets = availablePresetSets.get(appliedPresetSet.presetSetClass);
      presetSets.forEach(availablePresetSet -> {
         ObjectValue<String, T> presetSet = (ObjectValue<String, T>)availablePresetSet;
         toReplaceDropDown.options.add(presetSet, new StaticMessage(availablePresetSet.object));
         replacementDropDown.options.add(presetSet, new StaticMessage(availablePresetSet.object));
      });
      toReplaceDropDown.setSelected(appliedPresetSet.toReplace, new StaticMessage(appliedPresetSet.toReplace.object));
      replacementDropDown.setSelected(appliedPresetSet.replacement, new StaticMessage(appliedPresetSet.replacement.object));
      toReplaceDropDown.onSelected(selected -> {
         appliedPresetSet.toReplace = selected.value;
         this.updatePreview();
      });
      replacementDropDown.onSelected(selected -> {
         appliedPresetSet.replacement = selected.value;
         this.updatePreview();
      });
      FormContentIconButton nextButton = this.presetSetsContentBox
         .addComponent(new FormContentIconButton(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.next_song, new StaticMessage("Next set")));
      nextButton.onClicked(event -> {
         int index = presetSets.indexOf(replacementDropDown.getSelected()) + 1;
         if (index >= presetSets.size()) {
            index = 0;
         }

         ObjectValue<String, T> nextPresetSet = (ObjectValue)presetSets.get(index);
         replacementDropDown.setSelected(nextPresetSet, new StaticMessage(nextPresetSet.object));
         appliedPresetSet.replacement = nextPresetSet;
         this.updatePreview();
      });
      nextButton.setPosition(new FormRelativePosition(replacementDropDown, replacementDropDown.getWidth() + 4, 0));
      FormContentIconButton approveButton = this.presetSetsContentBox
         .addComponent(
            new FormContentIconButton(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.button_checked_20, new StaticMessage("Approve replacement"))
         );
      approveButton.onClicked(event -> {
         appliedPresetSet.approved.put((String)replacementDropDown.getSelected().object, (T)replacementDropDown.getSelected().value);
         this.updatePresetSetSection();
      });
      approveButton.setPosition(new FormRelativePosition(nextButton, nextButton.getWidth() + 4, 0));
      FormDropdownSelectionButton<ObjectValue<String, PresetSet<?>>> approvedDropDown = this.presetSetsContentBox
         .addComponent(new FormDropdownSelectionButton<>(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, 175));
      approvedDropDown.onSelected(selected -> {
         appliedPresetSet.approved.remove(selected.value.object);
         this.updatePresetSetSection();
      });
      approvedDropDown.setSelected(null, new StaticMessage("Approved sets"));
      approvedDropDown.setPosition(new FormRelativePosition(approveButton, approveButton.getWidth() + 4, 0));
      appliedPresetSet.approved
         .forEach(
            (approvedPresetSet, approvedPresetSetObject) -> approvedDropDown.options
               .add(new ObjectValue<>(approvedPresetSet, approvedPresetSetObject), new StaticMessage(approvedPresetSet))
         );
      if (approvedDropDown.options.isEmpty()) {
         approvedDropDown.setActive(false);
      }

      ButtonIcon randomizeIcon = appliedPresetSet.applyRandomly ? Settings.UI.firework_random : Settings.UI.priority_normal;
      FormContentIconButton randomizeButton = this.presetSetsContentBox
         .addComponent(new FormContentIconButton(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, randomizeIcon, new StaticMessage("Randomize from approved")));
      randomizeButton.onClicked(event -> {
         appliedPresetSet.applyRandomly = !appliedPresetSet.applyRandomly;
         this.updatePresetSetSection();
         this.updatePreview();
      });
      randomizeButton.setPosition(new FormRelativePosition(approvedDropDown, approvedDropDown.getWidth() + 4, 0));
      int widthLeft = width - (randomizeButton.getX() + randomizeButton.getWidth() + 4) - (FormInputSize.SIZE_24.height + 8);
      label.setText(GameUtils.maxString(appliedPresetSet.presetSetClass.getSimpleName(), new FontOptions(16), widthLeft / 2));
      toReplaceDropDown.setPosition(new FormRelativePosition(label, 4 + widthLeft / 2, -4));
      FormTextInput nameInput = this.presetSetsContentBox.addComponent(new FormTextInput(0, y, FormInputSize.SIZE_24, widthLeft / 2, 50));
      nameInput.setText(appliedPresetSet.fieldName);
      nameInput.tooltip = new StaticMessage("Field name");
      nameInput.onChange(event -> appliedPresetSet.fieldName = nameInput.getText().replace(" ", ""));
      nameInput.setPosition(new FormRelativePosition(randomizeButton, randomizeButton.getWidth() + 4, 0));
      FormContentIconButton duplicateButton = this.presetSetsContentBox
         .addComponent(new FormContentIconButton(0, y, FormInputSize.SIZE_24, ButtonColor.BASE, Settings.UI.copy_button, new StaticMessage("Duplicate set")));
      duplicateButton.onClicked(event -> {
         presetSetsToApply.add(new PresetDebugPreviewForm.AppliedPresetSet<>(appliedPresetSet));
         this.updatePresetSetSection();
      });
      duplicateButton.setPosition(new FormRelativePosition(nameInput, nameInput.width + 4, 0));
   }

   protected Point translatePointToPreset(Point point) {
      point = this.preset.getMirroredPoint(point.x, point.y, this.isMirroredX, this.isMirroredY);
      return PresetUtils.getRotatedPointInSpace(
         point.x, point.y, this.presetBackup.width, this.presetBackup.height, PresetRotation.toRotationAngle(this.rotations)
      );
   }

   public void setErrorMessage(GameMessage errorMessage) {
      this.errorMessage = errorMessage;
      this.updateForm();
   }

   public void clearErrorMessage() {
      if (this.errorMessage != null) {
         this.errorMessage = null;
         this.updateForm();
      }
   }

   protected void generateWorldGenFile(
      String name, String originalCreator, boolean randomizeMirrorX, boolean randomizeMirrorY, boolean randomizeRotation, boolean replaceLiquidTiles
   ) {
      File presetFile = new File("src/main/java/necesse/level/maps/presets/" + name + "Preset.java");
      File presetGenerationFile = new File("src/main/java/necesse/engine/world/worldPresets/" + name + "GenerationPreset.java");
      GameLog.debug.println("Creating preset file: " + presetFile.getAbsolutePath());
      GameLog.debug.println("Creating preset generation file: " + presetGenerationFile.getAbsolutePath());
      String presetScript = this.getConfiguredPreset(false).getScript();
      presetScript = presetScript.replace("\n", "\\n\" + \n\t\t\t\t\"");
      String presetCode = "package necesse.level.maps.presets;\n\nimport necesse.engine.util.GameRandom;\nimport necesse.engine.util.LevelIdentifier;\nimport necesse.inventory.lootTable.LootTable;\nimport necesse.inventory.lootTable.lootItem.LootItem;\nimport necesse.level.maps.biomes.Biome;\nimport necesse.level.maps.presets.set.*;\n\nimport java.awt.*;\n\n/**\n* Originally created by: %creator%\n*/\npublic class %name%Preset extends Preset {\n\n    public %name%Preset(Biome biome, LevelIdentifier levelIdentifier, GameRandom random%params%) {\n        super(\"%preset%\");\n\n%modifications%\n    }\n}\n"
         .replace("%name%", name)
         .replace("%preset%", presetScript);
      presetCode = presetCode.replace("%creator%", originalCreator);
      String presetGenerationCode = "package necesse.engine.world.worldPresets;\n\nimport necesse.engine.util.GameRandom;\nimport necesse.engine.world.biomeGenerator.BiomeGeneratorStack;\nimport necesse.engine.util.LevelIdentifier;\nimport necesse.level.maps.biomes.Biome;\nimport necesse.level.maps.presets.%name%Preset;\nimport necesse.level.maps.presets.Preset;\nimport necesse.level.maps.presets.set.*;\n\n/**\n * Originally created by: %creator%\n */\npublic class %name%GenerationPreset extends SimpleGenerationPreset {\n\n    public final Biome biome;\n    public final LevelIdentifier levelIdentifier;\n%fields%\n\n    public %name%GenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {\n        super(20, %mirrorX%, %mirrorY%, %rotation%, false, biome);\n        this.biome = biome;\n        this.levelIdentifier = levelIdentifier;\n%constructor%\n    }\n\n    @Override\n    public void setupTester(WorldPresetTester tester) {\n        tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {\n           @Override\n           public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {\n               if (presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {\n                   return !generatorStack.isSurfaceOceanOrRiver(tileX, tileY);\n               } else if (presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {\n                   return !generatorStack.isCaveRiverOrLava(tileX, tileY);\n               } else if (presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {\n                   return !generatorStack.isDeepCaveLava(tileX, tileY);\n               }\n               return false;\n           }\n       }));\n    }\n\n    @Override\n    public Preset getPreset(GameRandom random) {\n        return new %name%Preset(biome, levelIdentifier, random%args%);\n    }\n\n}\n"
         .replace("%name%", name);
      presetGenerationCode = presetGenerationCode.replace("%creator%", originalCreator);
      HashMap<String, Integer> variableIndexes = new HashMap<>();
      presetSetsToApply.forEach(presetSet -> presetSet.updateEqualPresetSets(presetSetsToApply));
      HashSet<PresetDebugPreviewForm.AppliedPresetSet<?>> equalSetAlreadyHandled = new HashSet<>();
      StringBuilder parameters = new StringBuilder();
      StringBuilder modifications = new StringBuilder();
      StringBuilder genFields = new StringBuilder();
      StringBuilder genConstructor = new StringBuilder();
      StringBuilder genArgs = new StringBuilder();
      presetSetsToApply.forEach(
         presetSet -> {
            String className = presetSet.presetSetClass.getSimpleName();
            String variableName = presetSet.getName();
            if (variableIndexes.containsKey(variableName)) {
               int index = variableIndexes.get(variableName) + 1;
               variableName = variableName + index;
               variableIndexes.put(variableName, index);
            } else {
               variableIndexes.put(variableName, 1);
            }

            if (presetSet.applyRandomly) {
               parameters.append(", ").append(className).append("[] ").append(variableName);
               modifications.append("\t\t")
                  .append(className)
                  .append(".")
                  .append(presetSet.toReplace.object)
                  .append(".replaceWithRandomly(random, ")
                  .append(variableName)
                  .append(", this);\n");
            } else {
               parameters.append(", ").append(className).append(" ").append(variableName);
               modifications.append("\t\t")
                  .append(className)
                  .append(".")
                  .append(presetSet.toReplace.object)
                  .append(".replaceWith(")
                  .append(variableName)
                  .append(", this);\n");
            }

            genFields.append("\n\t").append("public final ").append(className).append("[] ").append(variableName).append(";");
            if (!equalSetAlreadyHandled.contains(presetSet)) {
               genConstructor.append("\n\t\tthis.").append(variableName).append(" = ");
               presetSet.equalToOtherPresets.forEach(equalPresetSet -> {
                  genConstructor.append(equalPresetSet.getName()).append(" = ");
                  equalSetAlreadyHandled.add((PresetDebugPreviewForm.AppliedPresetSet<?>)equalPresetSet);
               });
               genConstructor.append(className).append(".getReducedSetForBiome(").append("new ").append(className).append("[]{");
               presetSet.approved
                  .forEach(
                     (approvedPresetSet, approvedPresetSetObject) -> genConstructor.append("\n\t\t\t\t")
                        .append(className)
                        .append(".")
                        .append(approvedPresetSet)
                        .append(",")
                  );
               genConstructor.append("\n\t\t}, biome, levelIdentifier);\n");
            }

            if (presetSet.applyRandomly) {
               genArgs.append(", ").append(variableName);
            } else {
               genArgs.append(", random.getOneOf(").append(variableName).append(")");
            }
         }
      );
      modifications.append("\n");
      this.humans
         .forEach(
            (point, mobs) -> {
               Point configuredPoint = this.translatePointToPreset(point);
               mobs.forEach(
                  mobID -> modifications.append("\n\t\taddHuman(\"")
                     .append(mobID)
                     .append("\", ")
                     .append(configuredPoint.x - LEVEL_PRESET_TILES_PADDING)
                     .append(", ")
                     .append(configuredPoint.y - LEVEL_PRESET_TILES_PADDING)
                     .append(", human -> {}, random);")
               );
            }
         );
      modifications.append("\n");
      this.mobs
         .forEach(
            (point, mobs) -> {
               Point configuredPoint = this.translatePointToPreset(point);
               if (!mobs.isEmpty()) {
                  modifications.append("\n\t\taddMobs(")
                     .append(configuredPoint.x - LEVEL_PRESET_TILES_PADDING)
                     .append(", ")
                     .append(configuredPoint.y - LEVEL_PRESET_TILES_PADDING)
                     .append(", false, ");
               }

               boolean first = true;

               for (Entry<String, Integer> entry : mobs.entrySet()) {
                  String mobID = entry.getKey();
                  Integer count = entry.getValue();

                  for (int i = 0; i < count; i++) {
                     if (!first) {
                        modifications.append(", ");
                     }

                     first = false;
                     modifications.append("\"").append(mobID).append("\"");
                  }
               }

               if (!mobs.isEmpty()) {
                  modifications.append(");");
               }
            }
         );
      modifications.append("\n");
      modifications.append("\n\t\t// TODO: Update loot tables");

      for (int y = 0; y < this.previewLevel.tileHeight; y++) {
         for (int x = 0; x < this.previewLevel.tileWidth; x++) {
            ObjectEntity objectEntity = this.previewLevel.entityManager.getObjectEntity(x, y);
            if (objectEntity != null && objectEntity.implementsOEInventory()) {
               String objectStringID = objectEntity.getObject().getStringID();
               String itemToAdd = "wheat";
               if (objectStringID.contains("cooking")) {
                  itemToAdd = "oaklog";
               } else if (objectStringID.contains("coolingbox")) {
                  itemToAdd = "iceblossom";
               } else if (objectStringID.contains("dresser")) {
                  itemToAdd = "hardhat";
               } else if (objectStringID.contains("cabinet")) {
                  itemToAdd = "witchbroom";
               }

               modifications.append("\n\t\taddInventory(new LootTable(LootItem.between(\"")
                  .append(itemToAdd)
                  .append("\", 2, 5)), random, ")
                  .append(x - LEVEL_PRESET_TILES_PADDING)
                  .append(", ")
                  .append(y - LEVEL_PRESET_TILES_PADDING)
                  .append("); // ")
                  .append(objectStringID);
            }
         }
      }

      modifications.append("\n");

      for (int y = 0; y < this.previewLevel.tileHeight; y++) {
         for (int x = 0; x < this.previewLevel.tileWidth; x++) {
            ObjectEntity objectEntity = this.previewLevel.entityManager.getObjectEntity(x, y);
            if (objectEntity instanceof SignObjectEntity) {
               SignObjectEntity signObjectEntity = (SignObjectEntity)objectEntity;
               modifications.append("\n\t\taddSign(\"")
                  .append(signObjectEntity.getSignMessage().translate())
                  .append("\", ")
                  .append(x - LEVEL_PRESET_TILES_PADDING)
                  .append(", ")
                  .append(y - LEVEL_PRESET_TILES_PADDING)
                  .append(");");
            }
         }
      }

      if (replaceLiquidTiles) {
         modifications.append("\n\t\tPresetUtils.addShoreTiles(this, -1, -1, width + 2, height + 2);");
      }

      presetCode = presetCode.replace("%modifications%", modifications.toString());
      presetCode = presetCode.replace("%params%", parameters.toString());
      presetGenerationCode = presetGenerationCode.replace("%fields%", genFields.toString());
      presetGenerationCode = presetGenerationCode.replace("%mirrorX%", randomizeMirrorX ? "true" : "false");
      presetGenerationCode = presetGenerationCode.replace("%mirrorY%", randomizeMirrorY ? "true" : "false");
      presetGenerationCode = presetGenerationCode.replace("%rotation%", randomizeRotation ? "true" : "false");
      presetGenerationCode = presetGenerationCode.replace("%constructor%", genConstructor.toString());
      presetGenerationCode = presetGenerationCode.replace("%args%", genArgs.toString());

      try {
         Files.write(presetFile.toPath(), GameUtils.getStringBytes(presetCode));
         Files.write(presetGenerationFile.toPath(), GameUtils.getStringBytes(presetGenerationCode));
      } catch (IOException e) {
         e.printStackTrace(GameLog.err);
      }
   }

   public static void registerPresetSet(Class<? extends PresetSet<?>> presetSetClass) {
      ArrayList<ObjectValue<String, PresetSet<?>>> sets = availablePresetSets.computeIfAbsent(presetSetClass, k -> new ArrayList<>());

      for (Field field : presetSetClass.getFields()) {
         if (field.getType() == presetSetClass) {
            try {
               sets.add(new ObjectValue<>(field.getName(), (PresetSet<?>)field.get(presetSetClass)));
            } catch (IllegalAccessException var7) {
            }
         }
      }
   }

   protected static class AppliedPresetSet<T extends PresetSet<T>> {
      public Class<T> presetSetClass;
      public ObjectValue<String, T> toReplace;
      public ObjectValue<String, T> replacement;
      public final HashMap<String, T> approved = new HashMap<>();
      public String fieldName = "";
      public HashSet<PresetDebugPreviewForm.AppliedPresetSet<T>> equalToOtherPresets = new HashSet<>();
      public boolean applyRandomly = false;

      public AppliedPresetSet(Class<T> presetSetClass, ObjectValue<String, T> toReplace, ObjectValue<String, T> replacement) {
         this.presetSetClass = presetSetClass;
         this.toReplace = toReplace;
         this.replacement = replacement;
      }

      public AppliedPresetSet(PresetDebugPreviewForm.AppliedPresetSet<T> copy) {
         this.presetSetClass = copy.presetSetClass;
         this.toReplace = copy.toReplace;
         this.replacement = copy.replacement;
         this.approved.putAll(copy.approved);
         this.fieldName = copy.fieldName;
         this.equalToOtherPresets.addAll(copy.equalToOtherPresets);
      }

      public void apply(Preset preset) {
         if (this.applyRandomly) {
            this.toReplace.value.replaceWithRandomly(GameRandom.globalRandom, (T[])this.approved.values().toArray(new PresetSet[0]), preset);
         } else {
            this.toReplace.value.replaceWith(this.replacement.value, preset);
         }
      }

      public void updateEqualPresetSets(List<PresetDebugPreviewForm.AppliedPresetSet<?>> appliedSets) {
         this.equalToOtherPresets.clear();
         appliedSets.forEach(
            otherSet -> {
               if (otherSet != this
                  && otherSet.presetSetClass == this.presetSetClass
                  && otherSet.toReplace.object.equals(this.toReplace.object)
                  && ((PresetSet)otherSet.toReplace.value).equals(this.toReplace.value)
                  && otherSet.approved.size() == this.approved.size()
                  && otherSet.approved.keySet().containsAll(this.approved.keySet())) {
                  this.equalToOtherPresets.add((PresetDebugPreviewForm.AppliedPresetSet<T>)otherSet);
               }
            }
         );
      }

      public String getName() {
         if (!this.fieldName.isEmpty()) {
            return this.fieldName;
         }

         String className = this.presetSetClass.getSimpleName();
         return className.substring(0, 1).toLowerCase() + className.substring(1);
      }
   }

   protected abstract static class ConfigButton {
      public Supplier<ButtonIcon> iconGetter;
      public Supplier<GameMessage> tooltip;

      public ConfigButton(Supplier<ButtonIcon> iconGetter, Supplier<GameMessage> tooltip) {
         this.iconGetter = iconGetter;
         this.tooltip = tooltip;
      }

      public ConfigButton(ButtonIcon icon, GameMessage tooltip) {
         this(() -> icon, () -> tooltip);
      }

      public abstract void onClick();

      public boolean isActive() {
         return true;
      }
   }

   public static class LevelGenerationSetup {
      public FormTextInput forestTickets;
      public FormTextInput snowTickets;
      public FormTextInput plainsTickets;
      public FormTextInput swampTickets;
      public FormTextInput desertTickets;
      public String presetGenerationName;
      public String worldPresetPath;
      public String levelIdentifierName;
      public GameLinkedList<String> code;
      public ArrayList<FormComponent> components = new ArrayList<>();

      public LevelGenerationSetup(String path, String levelIdentifierName, String friendlyName, int y) {
         this.worldPresetPath = path;
         this.levelIdentifierName = levelIdentifierName;
         FormFlow xFlow = new FormFlow(4);
         this.components.add(new FormLabel(friendlyName + " Tickets: ", new FontOptions(16), -1, 4, y));
         y += 20;
         this.forestTickets = this.createTicketInputForm("Forest", xFlow, y);
         this.snowTickets = this.createTicketInputForm("Snow", xFlow, y);
         this.plainsTickets = this.createTicketInputForm("Plains", xFlow, y);
         this.swampTickets = this.createTicketInputForm("Swamp", xFlow, y);
         this.desertTickets = this.createTicketInputForm("Desert", xFlow, y);

         try {
            this.code = new GameLinkedList<>();
            this.code.addAll(Files.readAllLines(Paths.get(this.worldPresetPath)));
         } catch (IOException e) {
            e.printStackTrace(GameLog.err);
         }
      }

      private FormTextInput createTicketInputForm(String name, FormFlow xFlow, int y) {
         this.components.add(xFlow.nextX(new FormLabel(name, new FontOptions(16), -1, 0, y), 8));
         FormTextInput inputForm = xFlow.nextX(new FormTextInput(0, y, FormInputSize.SIZE_24, 50, 5), 8);
         this.components.add(inputForm);
         inputForm.setRegexMatchFull("^(0|[1-9][0-9]*)$");
         return inputForm;
      }

      public void addComponentsToWorldGenerationDialogue(ComponentListContainer<FormComponent> parent) {
         for (FormComponent component : this.components) {
            parent.addComponent(component);
         }
      }

      public void onPresetNameChanged(String presetName) {
         this.presetGenerationName = presetName + "GenerationPreset";
         this.forestTickets.placeHolder = new StaticMessage(this.getTickets(BiomeRegistry.FOREST));
         this.snowTickets.placeHolder = new StaticMessage(this.getTickets(BiomeRegistry.SNOW));
         this.plainsTickets.placeHolder = new StaticMessage(this.getTickets(BiomeRegistry.PLAINS));
         this.swampTickets.placeHolder = new StaticMessage(this.getTickets(BiomeRegistry.SWAMP));
         this.desertTickets.placeHolder = new StaticMessage(this.getTickets(BiomeRegistry.DESERT));
      }

      private String getTickets(Biome biome) {
         String lineToLookFor = "new "
            + this.presetGenerationName
            + "(LevelIdentifier."
            + this.levelIdentifierName
            + ", BiomeRegistry."
            + biome.getStringID().toUpperCase()
            + "))";

         for (String line : this.code) {
            if (line.contains(lineToLookFor)) {
               int startIndex = line.indexOf(40) + 1;
               int endIndex = line.indexOf(44);
               return line.substring(startIndex, endIndex);
            }
         }

         return "";
      }

      private int getTicketsFromInputForm(FormTextInput input) {
         if (!input.getText().isEmpty()) {
            return Integer.parseInt(input.getText());
         } else {
            return input.placeHolder != null && !input.placeHolder.isEmpty() ? Integer.parseInt(input.placeHolder.translate()) : 0;
         }
      }

      public void saveLevelSetupToFile() {
         this.updateTicketsInCode(BiomeRegistry.FOREST, this.getTicketsFromInputForm(this.forestTickets));
         this.updateTicketsInCode(BiomeRegistry.SNOW, this.getTicketsFromInputForm(this.snowTickets));
         this.updateTicketsInCode(BiomeRegistry.PLAINS, this.getTicketsFromInputForm(this.plainsTickets));
         this.updateTicketsInCode(BiomeRegistry.SWAMP, this.getTicketsFromInputForm(this.swampTickets));
         this.updateTicketsInCode(BiomeRegistry.DESERT, this.getTicketsFromInputForm(this.desertTickets));

         try {
            Files.write(Paths.get(this.worldPresetPath), this.code, GlobalData.charset);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }

      private void updateTicketsInCode(Biome biome, int tickets) {
         String biomeName = biome.getStringID();
         String biomeLineToLookFor = "add" + biomeName.substring(0, 1).toUpperCase() + biomeName.substring(1).toLowerCase() + "Presets() {";
         String presetLineToLookFor = "new "
            + this.presetGenerationName
            + "(LevelIdentifier."
            + this.levelIdentifierName
            + ", BiomeRegistry."
            + biomeName.toUpperCase()
            + "))";
         String updatedLine = "\t\taddPreset("
            + tickets
            + ", new "
            + this.presetGenerationName
            + "(LevelIdentifier."
            + this.levelIdentifierName
            + ", BiomeRegistry."
            + biomeName.toUpperCase()
            + "));";
         GameLinkedList<String>.Element biomeMethodLineElement = null;

         for (GameLinkedList<String>.Element lineElement : this.code.elements()) {
            String line = (String)lineElement.object;
            if (biomeMethodLineElement == null) {
               if (line.contains(biomeLineToLookFor)) {
                  biomeMethodLineElement = lineElement;
               }
            } else if (line.contains(presetLineToLookFor)) {
               if (tickets <= 0) {
                  lineElement.remove();
               } else {
                  lineElement.replace(updatedLine);
               }

               return;
            }
         }

         if (tickets > 0) {
            if (biomeMethodLineElement != null) {
               biomeMethodLineElement.insertAfter(updatedLine);
            } else {
               throw new RuntimeException("Could not find biome method " + biomeLineToLookFor + " line in preset generation file: " + this.worldPresetPath);
            }
         }
      }
   }
}
