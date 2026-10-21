package necesse.engine.state;

import java.awt.Point;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipError;
import necesse.engine.GameAuth;
import necesse.engine.GameCache;
import necesse.engine.GameCrashLog;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.MusicList;
import necesse.engine.Settings;
import necesse.engine.gameLoop.GameLoop;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketDisconnect;
import necesse.engine.network.server.SavedServerClientData;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.network.server.ServerHostSettings;
import necesse.engine.network.server.ServerSettings;
import necesse.engine.platforms.Platform;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.save.WorldSave;
import necesse.engine.sound.SoundEmitter;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.ObjectValue;
import necesse.engine.util.PointHashSet;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.FileSystemClosedException;
import necesse.engine.world.World;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldFile;
import necesse.entity.mobs.Mob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.camera.PanningCamera;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.MainMenuFormManager;
import necesse.gfx.forms.components.ContinueComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.ConfirmationForm;
import necesse.gfx.forms.presets.ModSaveListMismatchForm;
import necesse.gfx.forms.presets.NoticeForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.ui.ButtonColor;
import necesse.level.maps.Level;
import necesse.level.maps.SurfaceLevel;
import necesse.level.maps.biomes.MobChance;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.regionSystem.Region;
import necesse.reports.ReportUtils;

public class MainMenu extends State {
   private final Object lock = new Object();
   private Client client;
   private MainMenu.MainMenuTickThread tickThread;
   private Level level;
   private WorldEntity worldEntity;
   public MainMenu.ConnectFrom connectedFrom;
   private PanningCamera camera;
   private boolean drawPrepared;
   private InputEvent cameraInput;
   private boolean preloadInCameraDirection = true;
   private boolean preloadLargeArea = true;
   private final PointHashSet preloadedRegionPositions = new PointHashSet();
   private MainMenuFormManager formManager;
   private NoticeForm waitingForServerShutdown;
   private long completedFirstLevelDrawTime;

   public MainMenu(String notice) {
      this(notice, null);
   }

   public MainMenu(String notice, Client client) {
      this(notice == null ? null : new StaticMessage(notice), client);
   }

   public MainMenu(GameMessage notice) {
      this(notice, null);
   }

   public MainMenu(GameMessage notice, Client client) {
      this.client = client;
      this.connectedFrom = null;
      Settings.hideUI = false;
      Settings.hideCursor = false;
      this.init();
      if (notice != null) {
         this.addNotice(notice);
      }
   }

   public MainMenu(ContinueComponent continueForm) {
      this(continueForm, null);
   }

   public MainMenu(ContinueComponent continueForm, Client client) {
      this.client = client;
      this.connectedFrom = null;
      Settings.hideUI = false;
      Settings.hideCursor = false;
      this.init();
      if (continueForm != null) {
         this.addContinueForm(continueForm);
      }
   }

   public MainMenu(Client client) {
      this.client = client;
      this.init();
      this.changeLevel(client);
   }

   public void init() {
      this.setupFormManager();
      Platform.getStatsProvider().storeStatsAndAchievements();
      this.connectedFrom = null;
      this.worldEntity = WorldEntity.getDebugWorldEntity();
      this.worldEntity.keepPresetGeneratedRegionsLoaded = true;
      ControllerInput.enableLayer(ControllerInput.MENU_SET_LAYER);
      ControllerInput.setMoveAsMenuNavigation(true);
      FormManager.cleanUpLastControllerFocuses();
      if (this.client != null && !this.client.loading.isDone()) {
         this.formManager.startConnection(this.client);
      }

      this.isInitialized = true;
   }

   @Override
   public void frameTick(TickManager tickManager, GameWindow window) {
      if (this.level == null) {
         this.initLevelCameraAndThread();
      }

      if (this.client != null) {
         if (!this.client.hasDisconnected()) {
            if (tickManager.isGameTick()) {
               Performance.record(tickManager, "clientTick", this.client::tick);
            }

            Performance.record(tickManager, "clientMove", () -> {
               if (this.client != null) {
                  this.client.frameTick(tickManager);
               }
            });
            if (this.isDisposed()) {
               return;
            }
         }

         if (this.client.isDisconnected() && this.client.getLocalServer() != null) {
            if (this.client.getLocalServer().serverThread.isAlive()) {
               if (this.waitingForServerShutdown == null) {
                  this.waitingForServerShutdown = new NoticeForm("waitserver");
                  this.waitingForServerShutdown.setupNotice(new LocalMessage("ui", "waitserver"));
                  this.waitingForServerShutdown.setButtonCooldown(-2);
                  this.formManager.addContinueForm("waitserver", this.waitingForServerShutdown);
               }
            } else {
               if (this.waitingForServerShutdown != null) {
                  this.waitingForServerShutdown.applyContinue();
                  this.waitingForServerShutdown = null;
               }

               if (this.client.hasDisconnected()) {
                  if (this.client.getLocalServer().stopErrors != null) {
                     for (ContinueComponent stopError : this.client.getLocalServer().stopErrors) {
                        this.addContinueForm(stopError);
                     }
                  }

                  this.client = null;
               }
            }
         } else if (this.client.hasDisconnected() && this.client.getLocalServer() == null) {
            this.client = null;
         }
      }

      Input input = WindowManager.getWindow().getInput();
      if (GlobalData.isDevMode()) {
         InputEvent f8 = input.getEvent(297);
         InputEvent f9 = input.getEvent(298);
         if (f8 != null && f8.state || f9 != null && f9.state) {
            float[] modifiers = new float[]{0.5F, 1.0F, 2.0F, 4.0F, 8.0F, 16.0F, 32.0F};
            int index = 0;

            for (int i = 0; i < modifiers.length; i++) {
               if (TickManager.globalTimeMod == modifiers[i]) {
                  index = i;
                  break;
               }
            }

            if (f8 != null) {
               TickManager.globalTimeMod = modifiers[Math.floorMod(index - 1, modifiers.length)];
            }

            if (f9 != null) {
               TickManager.globalTimeMod = modifiers[(index + 1) % modifiers.length];
            }

            TickManager.skipDrawIfBehind = TickManager.globalTimeMod > 1.0F;
            System.out.println("Time modifier: x" + TickManager.globalTimeMod);
         }
      }

      InputEvent leftClick = input.getEvent(-100);

      try {
         input.getEvents().forEach(event -> this.formManager.submitInputEvent(event, tickManager, null));
         ControllerInput.getEvents().forEach(e -> this.formManager.submitControllerEvent(e, tickManager, null));
      } catch (ConcurrentModificationException e) {
         System.err.println("ConcurrentModificationException likely caused by opening of url/file");
         e.printStackTrace();
      }

      if (leftClick != null) {
         if (leftClick.state) {
            if (!leftClick.isUsed()) {
               leftClick.use();
               this.cameraInput = leftClick;
            }
         } else if (this.cameraInput != null) {
            leftClick.use();
            this.cameraInput = null;
         }
      }

      if (this.cameraInput != null) {
         int deltaX = input.mousePos().windowX - this.cameraInput.pos.windowX;
         int deltaY = input.mousePos().windowY - this.cameraInput.pos.windowY;
         float speed = Math.min((float)new Point(deltaX, deltaY).distance(0.0, 0.0) / 10.0F, 50.0F);
         this.camera.setDirection(deltaX, deltaY);
         this.camera.setSpeed(speed);
      } else if (this.preloadedRegionPositions.isEmpty()) {
         this.preloadInCameraDirection = true;
      }

      InputEvent escapeEvent = input.getEvent(256);
      if (escapeEvent == null) {
         ControllerEvent controllerEvent = ControllerInput.getEvent(ControllerInput.MENU_BACK);
         if (controllerEvent != null) {
            escapeEvent = InputEvent.ControllerButtonEvent(controllerEvent, tickManager);
         }
      }

      if (escapeEvent == null) {
         ControllerEvent controllerEvent = ControllerInput.getEvent(ControllerInput.MAIN_MENU);
         if (controllerEvent != null) {
            escapeEvent = InputEvent.ControllerButtonEvent(controllerEvent, tickManager);
         }
      }

      if (escapeEvent != null && !escapeEvent.isUsed() && escapeEvent.state) {
         this.formManager.submitEscapeEvent(escapeEvent);
      }

      this.formManager.frameTick(tickManager);
      if (!this.isDisposed()) {
         if (Control.SCREENSHOT.isPressed()) {
            if (window.getInput().isKeyDown(80)) {
               Renderer.takeSceneAndHudShot(tickManager, this.client == null ? null : this.client.chat);
            } else {
               Renderer.takeScreenshot(this.client == null ? null : this.client.chat);
            }
         }

         if (tickManager.isGameTick() && this.client == null) {
            SoundManager.setMusic(new MusicList(MusicRegistry.AdventureBegins), SoundManager.MusicPriority.BIOME);
         }
      }
   }

   private void refreshLevelLoading(Level level) {
      double secondsToLoad = this.preloadLargeArea ? 120.0 : 10.0;
      double distanceToLoad = this.camera.getSpeed() > 0.0F ? this.camera.getSpeed() * secondsToLoad * 32.0 / 250.0 : 1.0;
      double distanceLoaded = 0.0;
      this.worldEntity.refreshWorldPresetCache(level.getIdentifier(), this.camera.getStartTileX(), this.camera.getStartTileY());
      this.worldEntity
         .refreshWorldPresetCache(
            level.getIdentifier(),
            (int)(this.camera.getStartTileX() + this.camera.getXDir() * distanceToLoad * 2.0),
            (int)(this.camera.getStartTileY() + this.camera.getYDir() * distanceToLoad * 2.0)
         );
      double regionTileSize = 512.0;
      double cameraX = this.camera.getX() - regionTileSize * 0.5;
      double cameraY = this.camera.getY() - regionTileSize * 0.5;
      double cameraXMod = cameraX % regionTileSize;
      double cameraYMod = cameraY % regionTileSize;
      int cameraRegionWidth = GameMath.ceil(this.camera.getWidth() / 32.0 / 16.0) + 2;
      int cameraRegionHeight = GameMath.ceil(this.camera.getHeight() / 32.0 / 16.0) + 2;
      Iterator<Point> iterator = GameUtils.streamRegionCoordinatesBetweenPoints(
            level, new Double(cameraXMod, cameraYMod), new Double(cameraXMod + this.camera.getXDir() * 100000.0, cameraYMod + this.camera.getYDir() * 100000.0)
         )
         .iterator();

      while (distanceLoaded <= (this.preloadInCameraDirection ? distanceToLoad : 1.0) && iterator.hasNext()) {
         Point point = iterator.next();
         distanceLoaded = point.distance(new Point(0, 0)) * 16.0;

         for (int x = 0; x < cameraRegionWidth; x++) {
            for (int y = 0; y < cameraRegionHeight; y++) {
               int regionX = x + point.x + GameMath.getRegionCoordByTile(GameMath.getTileCoordinate(cameraX));
               int regionY = y + point.y + GameMath.getRegionCoordByTile(GameMath.getTileCoordinate(cameraY));
               boolean isNewlyGeneratedRegion = !level.regionManager.isRegionGenerated(regionX, regionY);
               Region region = level.regionManager.getRegion(regionX, regionY, true);
               region.unloadRegionBuffer.keepLoaded();
               if (this.preloadInCameraDirection) {
                  this.preloadedRegionPositions.add(regionX, regionY);
               }

               if (isNewlyGeneratedRegion) {
                  if (!this.preloadInCameraDirection && !this.preloadedRegionPositions.isEmpty()) {
                     this.preloadedRegionPositions.clear();
                  }

                  int critters = GameRandom.globalRandom.getIntBetween(-4, 2);

                  for (int i = 0; i < critters; i++) {
                     Point spawnTile = new Point(
                        region.tileXOffset + GameRandom.globalRandom.getIntBetween(0, region.tileWidth - 1),
                        region.tileYOffset + GameRandom.globalRandom.getIntBetween(0, region.tileHeight - 1)
                     );
                     MobSpawnTable critterSpawnTable = level.getBiome(spawnTile.x, spawnTile.y).getCritterSpawnTable(level);

                     while (true) {
                        MobChance randomMob = critterSpawnTable.getRandomMob(level, null, new Point(spawnTile), GameRandom.globalRandom, "mainmenu");
                        if (randomMob == null) {
                           break;
                        }

                        Collection<Mob> spawnedMobs = randomMob.spawnMob(level, null, spawnTile, null, mob -> mob.canDespawn = false, "mainmenu");
                        if (spawnedMobs != null) {
                           break;
                        }

                        critterSpawnTable = critterSpawnTable.withoutRandomMob(randomMob);
                     }
                  }
               }
            }
         }
      }

      for (Region region : level.regionManager.tickUnloadRegions(30)) {
         if (!this.preloadedRegionPositions.contains(region.regionX, region.regionY)) {
            level.regionManager.unloadRegion(region.regionX, region.regionY);
         }
      }

      this.preloadInCameraDirection = false;
      this.preloadLargeArea = false;
   }

   @Override
   public void secondTick(TickManager tickManager) {
      ReportUtils.updateSessionSeconds(null);
   }

   public float getLoadingAlpha() {
      if (this.completedFirstLevelDrawTime == 0L) {
         return 0.0F;
      }

      long timeSinceLoaded = System.currentTimeMillis() - this.completedFirstLevelDrawTime;
      return timeSinceLoaded < 2000L ? GameMath.limit((float)timeSinceLoaded / 2000.0F, 0.0F, 1.0F) : 1.0F;
   }

   @Override
   public boolean prepareDrawScene(TickManager tickManager, boolean sceneUpdated) {
      if (this.level != null && this.level.isLoadingComplete()) {
         this.level.runGLContextRunnables();
         this.camera.updateToSceneDimensions();
         this.camera.tickMovement(tickManager);
         if (!this.level.prepareDraw(this.camera, null, tickManager, sceneUpdated)) {
            return false;
         }

         this.drawPrepared = true;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void drawScene(TickManager tickManager, boolean sceneUpdated) {
      if (!this.drawPrepared) {
         GameLog.warn.println("Scene draw was not prepared before calling draw(...). Call prepareDrawScene() first.");
         if (!this.prepareDrawScene(tickManager, sceneUpdated)) {
            return;
         }
      }

      this.drawPrepared = false;
      this.level.draw(this.camera, null, tickManager, sceneUpdated);
      if (this.completedFirstLevelDrawTime == 0L) {
         this.completedFirstLevelDrawTime = System.currentTimeMillis();
      }
   }

   @Override
   public void drawSceneOverlay(TickManager tickManager) {
      Level level = this.level;
      if (level != null && level.isLoadingComplete()) {
         level.drawHud(this.camera, null, tickManager);
      }
   }

   @Override
   public void drawHud(TickManager tickManager) {
      float loadingAlpha = this.getLoadingAlpha();
      GameLoadingScreen.drawKeyArt(WindowManager.getWindow(), 1.0F - loadingAlpha);
      this.formManager.draw(tickManager, null);
      GameTooltipManager.addControllerGlyph(
         Localization.translate("controls", "navigatetip"),
         ControllerInput.MENU_UP,
         ControllerInput.MENU_RIGHT,
         ControllerInput.MENU_DOWN,
         ControllerInput.MENU_LEFT
      );
      GameTooltipManager.addControllerGlyph(Localization.translate("ui", "backbutton"), ControllerInput.MENU_BACK);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      if (this.formManager != null) {
         this.formManager.onWindowResized(window);
      }

      synchronized (this.lock) {
         this.preloadInCameraDirection = true;
         this.preloadLargeArea = true;
      }
   }

   public void setupFormManager() {
      this.formManager = new MainMenuFormManager(this);
      this.formManager.setup();
   }

   public MainMenuFormManager getFormManager() {
      return this.formManager;
   }

   @Override
   public void reloadInterfaceFromSettings(boolean makeInterfaceCurrent) {
      this.formManager = new MainMenuFormManager(this);
      this.formManager.setup();
      this.formManager.mainForm.makeCurrent(this.formManager.mainForm.settings);
      if (makeInterfaceCurrent) {
         this.formManager.mainForm.settings.makeInterfaceCurrent();
         this.formManager.mainForm.settings.setSaveActive(true);
         this.formManager.mainForm.settings.reloadedInterface = true;
      }
   }

   public void toggleCameraPanSetting() {
      this.initCamera();
   }

   @Override
   public GameCamera getCamera() {
      return this.camera;
   }

   protected boolean setupLoadSaveConfirmations(WorldSave worldSave, final GameBlackboard confirms, final Runnable confirmed) {
      if (worldSave != null && confirms != null) {
         WorldEntity worldEntity = worldSave.getWorldEntity();
         if (!confirms.containsKey("transferlastplayer") && worldEntity != null) {
            World world = worldSave.getWorld();
            if (world != null) {
               boolean shouldCloseFileSystem = !world.fileSystem.isOpen();

               try {
                  if (shouldCloseFileSystem) {
                     world.reloadFileSystem();
                  }

                  if (!world.fileSystem.playerFileExists(GameAuth.getAuthentication())) {
                     HashMap<Long, SavedServerClientData> savedClients = world.getSavedClientData();
                     world.closeFileSystem();
                     if (savedClients.size() == 1) {
                        SavedServerClientData client = savedClients.values().stream().findFirst().orElse(null);
                        LocalMessage message = new LocalMessage("misc", "differentlastplayer", "name", client.name);
                        ConfirmationForm confirmationForm = new ConfirmationForm("inheritlastplayer", 500, 600);
                        confirmationForm.setupConfirmation(
                           message, new LocalMessage("misc", "inheritplayeryes"), new LocalMessage("misc", "inheritplayerno"), () -> {
                              confirms.set("transferlastplayer", true);
                              confirms.set("inheritlastplayer", client.authentication);
                              this.formManager.mainForm.makeCurrent(this.formManager.mainForm.main);
                              confirmed.run();
                           }, () -> {
                              confirms.set("transferlastplayer", true);
                              this.formManager.mainForm.makeCurrent(this.formManager.mainForm.main);
                              confirmed.run();
                           }
                        );
                        this.formManager.mainForm.addComponent(confirmationForm, (form, active) -> {
                           if (!active) {
                              this.formManager.mainForm.removeComponent(form);
                           }
                        });
                        confirmationForm.onWindowResized(WindowManager.getWindow());
                        this.formManager.mainForm.makeCurrent(confirmationForm);
                        return true;
                     }

                     if (savedClients.size() > 1) {
                        NoticeForm noticeForm = new NoticeForm("inheritplayer", 500, 600);
                        noticeForm.setButtonCooldown(-2);
                        noticeForm.setupNotice(
                           content -> {
                              FormFlow flow = new FormFlow(10);
                              content.addComponent(
                                 flow.nextY(
                                    new FormLocalLabel(
                                       "misc", "differentlastplayers", new FontOptions(20), 0, content.getWidth() / 2, 0, content.getWidth() - 20
                                    ),
                                    10
                                 )
                              );
                              int buttonWidth = Math.min(300, content.getWidth() - 8);
                              int buttonX = (content.getWidth() - buttonWidth) / 2;
                              content.addComponent(
                                    flow.nextY(
                                       new FormLocalTextButton("misc", "inheritplayerno", buttonX, 0, buttonWidth, FormInputSize.SIZE_24, ButtonColor.BASE), 4
                                    )
                                 )
                                 .onClicked(ex -> {
                                    confirms.set("transferlastplayer", true);
                                    this.formManager.mainForm.makeCurrent(this.formManager.mainForm.main);
                                    confirmed.run();
                                 });

                              for (SavedServerClientData savedClient : savedClients.values()) {
                                 FormTextButton button = content.addComponent(
                                    flow.nextY(new FormTextButton(savedClient.name, buttonX, 0, buttonWidth, FormInputSize.SIZE_24, ButtonColor.BASE), 4)
                                 );
                                 LocalMessage playTime = new LocalMessage(
                                    "ui",
                                    "characterplaytime",
                                    "time",
                                    savedClient.characterStats == null ? "N/A" : GameUtils.formatSeconds(savedClient.characterStats.time_played.get())
                                 );
                                 button.setTooltip(playTime.translate());
                                 button.onClicked(ex -> {
                                    confirms.set("transferlastplayer", true);
                                    confirms.set("inheritlastplayer", savedClient.authentication);
                                    this.formManager.mainForm.makeCurrent(this.formManager.mainForm.main);
                                    confirmed.run();
                                 });
                              }
                           }
                        );
                        this.formManager.mainForm.addComponent(noticeForm, (form, active) -> {
                           if (!active) {
                              this.formManager.mainForm.removeComponent(form);
                           }
                        });
                        noticeForm.onWindowResized(WindowManager.getWindow());
                        this.formManager.mainForm.makeCurrent(noticeForm);
                        return true;
                     }
                  }
               } catch (IOException | FileSystemClosedException e) {
                  System.err.println("Error trying to reload world file system to check for player transfer...");
                  e.printStackTrace();
               } finally {
                  if (shouldCloseFileSystem && world.fileSystem.isOpen()) {
                     try {
                        world.closeFileSystem();
                     } catch (IOException e) {
                        System.err.println("Error closing file system to check for player transfer...");
                     }
                  }
               }
            }
         }

         if (!confirms.containsKey("modmismatch") && !ModLoader.matchesCurrentMods(worldSave)) {
            ModSaveListMismatchForm mismatchForm = new ModSaveListMismatchForm() {
               @Override
               public void loadAnywayPressed() {
                  confirms.set("modmismatch", true);
                  MainMenu.this.formManager.mainForm.makeCurrent(MainMenu.this.formManager.mainForm.main);
                  confirmed.run();
               }

               @Override
               public void backPressed() {
                  MainMenu.this.formManager.mainForm.setConnectedFromCurrent();
               }
            };
            this.formManager.mainForm.addComponent(mismatchForm, (form, active) -> {
               if (!active) {
                  this.formManager.mainForm.removeComponent(form);
               }
            });
            mismatchForm.setup(worldEntity.lastMods);
            mismatchForm.onWindowResized(WindowManager.getWindow());
            this.formManager.mainForm.makeCurrent(mismatchForm);
            return true;
         }

         if (worldSave.isEarlierThanOneWorld()) {
            if (!confirms.containsKey("convertearlyaccessworld1")) {
               ConfirmationForm confirmationForm = new ConfirmationForm("convertearlyaccessworld", 500, 600);
               GameMessageBuilder builder = new GameMessageBuilder().append("misc", "converteaworld1").append("\n").append("misc", "converteaworld2");
               confirmationForm.setupConfirmation(builder, new LocalMessage("misc", "converteaworldreadmore"), new LocalMessage("ui", "backbutton"), () -> {
                  confirms.set("convertearlyaccessworld1", true);
                  this.formManager.mainForm.makeCurrent(this.formManager.mainForm.main);
                  confirmed.run();
               }, () -> this.formManager.mainForm.setConnectedFromCurrent());
               this.formManager.mainForm.addComponent(confirmationForm, (form, active) -> {
                  if (!active) {
                     this.formManager.mainForm.removeComponent(form);
                  }
               });
               confirmationForm.onWindowResized(WindowManager.getWindow());
               this.formManager.mainForm.makeCurrent(confirmationForm);
               return true;
            }

            if (!confirms.containsKey("convertearlyaccessworld2")) {
               ConfirmationForm confirmationForm = new ConfirmationForm("convertearlyaccessworld", 500, 600);
               GameMessageBuilder builder = new GameMessageBuilder()
                  .append("misc", "converteaworld3")
                  .append("\n\n")
                  .append("misc", "converteaworld4")
                  .append("\n\n")
                  .append("misc", "converteaworld5")
                  .append("\n\n")
                  .append("misc", "converteaworld6")
                  .append("\n\n")
                  .append("misc", "converteaworld7");
               confirmationForm.setupConfirmation(builder, new LocalMessage("misc", "converteaworldaccept"), new LocalMessage("ui", "backbutton"), () -> {
                  confirms.set("convertearlyaccessworld2", true);
                  this.formManager.mainForm.makeCurrent(this.formManager.mainForm.main);
                  confirms.set("createmigrationbackup", true);
                  confirmed.run();
               }, () -> this.formManager.mainForm.setConnectedFromCurrent());
               this.formManager.mainForm.addComponent(confirmationForm, (form, active) -> {
                  if (!active) {
                     this.formManager.mainForm.removeComponent(form);
                  }
               });
               confirmationForm.onWindowResized(WindowManager.getWindow());
               this.formManager.mainForm.makeCurrent(confirmationForm);
               return true;
            }
         }
      }

      return false;
   }

   public void startSingleplayer(WorldSave worldSave, ServerCreationSettings serverCreationSettings, MainMenu.ConnectFrom connectedFrom) {
      this.startSingleplayer(worldSave, serverCreationSettings, connectedFrom, new GameBlackboard());
   }

   private void startSingleplayer(
      WorldSave worldSave, ServerCreationSettings serverCreationSettings, MainMenu.ConnectFrom connectedFrom, GameBlackboard confirms
   ) {
      this.connectedFrom = connectedFrom;
      if (!this.setupLoadSaveConfirmations(worldSave, confirms, () -> this.startSingleplayer(worldSave, serverCreationSettings, connectedFrom, confirms))) {
         if (!this.runConfirmChecks(worldSave, confirms)) {
            try {
               this.startConnection(Platform.getNetworkManager().startSingleplayerClient(serverCreationSettings), connectedFrom);
               SaveData continueSave = getContinueCacheSaveBase(MainMenu.ContinueMode.OPEN);
               continueSave.addSafeString("worldFilePath", serverCreationSettings.worldFilePath.getAbsolutePath());
               GameCache.cacheSave(continueSave, "continueLast");
            } catch (IOException | ZipError ex) {
               this.addNotice(
                  Localization.translate("misc", "loadworldfailed") + "\n\n\"" + ex.getMessage() + "\"\n\n" + Localization.translate("misc", "restorebackup")
               );
               ex.printStackTrace();
            } catch (FileSystemClosedException ex) {
               this.addNotice(Localization.translate("misc", "loadworldfailed") + "\n\n" + Localization.translate("misc", "savenotclosed"));
               ex.printStackTrace();
            }
         }
      }
   }

   public void host(WorldSave worldSave, ServerSettings serverSettings, ServerHostSettings serverHostSettings, MainMenu.ConnectFrom connectedFrom) {
      this.host(worldSave, serverSettings, serverHostSettings, connectedFrom, new GameBlackboard());
   }

   private void host(
      WorldSave worldSave, ServerSettings serverSettings, ServerHostSettings serverHostSettings, MainMenu.ConnectFrom connectedFrom, GameBlackboard confirms
   ) {
      this.connectedFrom = connectedFrom;
      if (!this.setupLoadSaveConfirmations(worldSave, confirms, () -> this.host(worldSave, serverSettings, serverHostSettings, connectedFrom, confirms))) {
         if (!this.runConfirmChecks(worldSave, confirms)) {
            try {
               this.startConnection(Platform.getNetworkManager().startHostClient(serverSettings, serverHostSettings), connectedFrom);
               SaveData continueSave = getContinueCacheSaveBase(MainMenu.ContinueMode.HOST);
               continueSave.addSafeString("worldFilePath", serverSettings.creationSettings.worldFilePath.getAbsolutePath());
               SaveData hostSave = new SaveData("host");
               serverSettings.addSaveData(hostSave);
               continueSave.addSaveData(hostSave);
               GameCache.cacheSave(continueSave, "continueLast");
            } catch (IOException | ZipError ex) {
               this.addNotice(
                  Localization.translate("misc", "loadworldfailed") + "\n\n\"" + ex.getMessage() + "\"\n\n" + Localization.translate("misc", "restorebackup")
               );
               ex.printStackTrace();
            } catch (FileSystemClosedException ex) {
               this.addNotice(Localization.translate("misc", "loadworldfailed") + "\n\n" + Localization.translate("misc", "savenotclosed"));
               ex.printStackTrace();
            }
         }
      }
   }

   protected boolean runConfirmChecks(WorldSave worldSave, GameBlackboard confirms) {
      if (confirms.containsKey("inheritlastplayer")) {
         long auth = confirms.getLong("inheritlastplayer");
         World world = worldSave.getWorld();
         boolean shouldCloseFileSystem = !world.fileSystem.isOpen();

         try {
            if (shouldCloseFileSystem) {
               world.reloadFileSystem();
            }

            WorldFile file = world.fileSystem.getPlayerFile(auth);
            if (file.exists()) {
               file.moveTo(world.fileSystem.getPlayerPath(GameAuth.getAuthentication()), StandardCopyOption.REPLACE_EXISTING);
            } else {
               System.err.println("Could not find last player file to transfer: " + file.toAbsolutePath());
            }
         } catch (IOException | FileSystemClosedException e) {
            System.err.println("Failed to transfer last player data...");
            e.printStackTrace();
         } finally {
            if (shouldCloseFileSystem && world.fileSystem.isOpen()) {
               try {
                  world.closeFileSystem();
               } catch (IOException e) {
                  System.err.println("Failed to close file system for transfer last player data...");
               }
            }
         }
      }

      if (confirms.containsKey("createmigrationbackup")) {
         File targetPath = WorldSave.getPreMigrationBackupPath(worldSave.filePath);

         try {
            World.copyWorld(worldSave.filePath, targetPath, false);
         } catch (IOException e) {
            this.addNotice("Failed to create a backup of the world before migration: " + e.getMessage());
            e.printStackTrace();
            return true;
         }
      }

      return false;
   }

   public ObjectValue<GameMessage, Runnable> loadContinueCacheSave() {
      try {
         LoadData continueSave = GameCache.getSave("continueLast");
         if (continueSave != null) {
            MainMenu.ContinueMode mode = continueSave.getEnum(MainMenu.ContinueMode.class, "mode", null);
            if (mode != null) {
               switch (mode) {
                  case OPEN:
                     String worldFilePathx = continueSave.getSafeString("worldFilePath", continueSave.getSafeString("value", null, false));
                     if (worldFilePathx != null) {
                        File filePath = new File(worldFilePathx);
                        if (!World.worldExists(filePath)) {
                           return null;
                        }

                        return new ObjectValue<>(
                           new LocalMessage("ui", "continuetip", "world", World.getWorldDisplayName(filePath.getName())),
                           () -> {
                              try {
                                 WorldSave worldSave = new WorldSave(filePath, true, true, true);
                                 this.startSingleplayer(worldSave, new ServerCreationSettings(filePath), null);
                              } catch (IOException | ZipError ex) {
                                 this.addNotice(
                                    Localization.translate("misc", "loadworldfailed")
                                       + "\n\n\""
                                       + ex.getMessage()
                                       + "\"\n\n"
                                       + Localization.translate("misc", "restorebackup")
                                 );
                                 ex.printStackTrace();
                              } catch (FileSystemClosedException ex) {
                                 this.addNotice(Localization.translate("misc", "loadworldfailed") + "\n\n" + Localization.translate("misc", "savenotclosed"));
                                 ex.printStackTrace();
                              }
                           }
                        );
                     }
                     break;
                  case HOST:
                     String worldFilePath = continueSave.getSafeString("worldFilePath", continueSave.getSafeString("value", null, false));
                     if (worldFilePath != null) {
                        File filePath = new File(worldFilePath);
                        if (!World.worldExists(filePath)) {
                           return null;
                        }

                        LoadData hostSave = continueSave.getFirstLoadDataByName("host");
                        if (hostSave != null) {
                           ServerSettings settings = Platform.getNetworkManager().getServerSettingsFromSave(filePath, hostSave);
                           return new ObjectValue<>(
                              new LocalMessage("ui", "continuehost", "world", World.getWorldDisplayName(filePath.getName())),
                              () -> {
                                 try {
                                    WorldSave worldSave = new WorldSave(filePath, true, true, true);
                                    this.host(worldSave, settings, null, null);
                                 } catch (IOException | ZipError ex) {
                                    this.addNotice(
                                       Localization.translate("misc", "loadworldfailed")
                                          + "\n\n\""
                                          + ex.getMessage()
                                          + "\"\n\n"
                                          + Localization.translate("misc", "restorebackup")
                                    );
                                    ex.printStackTrace();
                                 } catch (FileSystemClosedException ex) {
                                    this.addNotice(Localization.translate("misc", "loadworldfailed") + "\n\n" + Localization.translate("misc", "savenotclosed"));
                                    ex.printStackTrace();
                                 }
                              }
                           );
                        }
                     }
                     break;
                  case JOIN:
                     return Platform.getNetworkManager().getMainMenuContinueButtonForJoining(continueSave, this);
               }
            }
         }
      } catch (Exception var7) {
      }

      return null;
   }

   public static SaveData getContinueCacheSaveBase(MainMenu.ContinueMode mode) {
      SaveData save = new SaveData("");
      save.addEnum("mode", mode);
      return save;
   }

   public void startConnection(Client client, MainMenu.ConnectFrom connectedFrom) {
      if (this.client != null) {
         client.instantDisconnect("Client error");
      }

      this.client = client;
      this.connectedFrom = connectedFrom;
      GlobalData.resetDebug();
      this.formManager.startConnection(client);
   }

   public void changeLevel(Client client) {
      client.reset();
      this.formManager.startConnection(client);
   }

   public Client getClient() {
      return this.client;
   }

   public void initLevelCameraAndThread() {
      synchronized (this.lock) {
         if (this.level != null && !this.level.isDisposed()) {
            this.level.dispose();
            this.level.runGLContextRunnables();
         }

         this.level = new SurfaceLevel(LevelIdentifier.SURFACE_IDENTIFIER, 0, 0, this.worldEntity);
         this.level.lightManager.ambientLightOverride = this.level.lightManager.newLight(150.0F);
         this.initCamera();
      }

      if (this.tickThread == null) {
         this.tickThread = new MainMenu.MainMenuTickThread("main-menu-level", 60);
         this.level.overrideTickManager(this.tickThread.gameLoop);
         this.tickThread.start();
      }
   }

   public void initCamera() {
      PanningCamera lastCamera = this.camera;
      this.camera = new PanningCamera();
      if (lastCamera != null) {
         this.camera.setPosition(lastCamera.getX(), lastCamera.getY());
         this.camera.setDirection(lastCamera.getXDir(), lastCamera.getYDir());
      } else {
         int dirX = GameRandom.globalRandom.getOneOf(-1, 1);
         int dirY = GameRandom.globalRandom.getOneOf(-1, 1);
         this.camera.setDirection(dirX, dirY);
         this.camera.centerCamera(16384, 16384);
      }

      if (lastCamera != null) {
         Settings.menuCameraPan = lastCamera.getSpeed() == 0.0F;
      }

      if (Settings.menuCameraPan) {
         this.camera.setSpeed(10.0F);
      } else {
         this.camera.setSpeed(0.0F);
      }
   }

   public void addNotice(String notice) {
      this.addNotice(notice == null ? null : new StaticMessage(notice));
   }

   public void addNotice(GameMessage notice) {
      NoticeForm noticeForm = new NoticeForm("notice");
      noticeForm.setupNotice(notice);
      this.addContinueForm(null, noticeForm);
   }

   public void addContinueForm(String key, ContinueComponent form) {
      if (form != null) {
         this.formManager.addContinueForm(key, form);
      }
   }

   public void addContinueForm(ContinueComponent form) {
      if (form != null) {
         this.formManager.addContinueForm(null, form);
      }
   }

   public void cancelConnection() {
      this.formManager.cancelConnection();
   }

   @Override
   public void dispose() {
      super.dispose();
      Level level = this.level;
      this.level = null;
      synchronized (this.lock) {
         this.formManager.dispose();
         if (level != null) {
            level.dispose();
            level.runGLContextRunnables();
         }

         if (this.worldEntity != null) {
            this.worldEntity.dispose();
         }

         this.worldEntity = null;
         if (this.tickThread != null) {
            this.tickThread.gameLoop.stopMainGameLoop();
         }

         this.tickThread = null;
      }
   }

   @Override
   public void onClose() {
      if (this.client != null) {
         this.client.saveAndClose("Closed client", PacketDisconnect.Code.SERVER_STOPPED);
      }
   }

   @Override
   public void onCrash(List<Throwable> errors) {
      GameCrashLog.printCrashLog(errors, this.client, this.client == null ? null : this.client.getLocalServer(), "MainMenu", this.client == null);
      if (this.client != null) {
         this.client.error(Localization.translate("disconnect", "clienterror"), true);
      } else {
         GlobalData.getCurrentGameLoop().stopMainGameLoop();
      }
   }

   @Override
   public SoundEmitter getALListener() {
      return SoundPlayer.SimpleEmitter(0.0F, 0.0F);
   }

   public enum ConnectFrom {
      SingleplayerMenu,
      SinglePlayerLoadWorld,
      MultiplayerMenu,
      MultiplayerHostWorld,
      MultiplayerJoinFriend,
      MultiplayerJoinServer;
   }

   public enum ContinueMode {
      OPEN,
      HOST,
      JOIN;
   }

   public class MainMenuGameLoop extends GameLoop {
      public MainMenuGameLoop(String name, int maxFPS) {
         super(name, maxFPS);
      }

      @Override
      public void update() {
         super.update();
         if (!MainMenu.this.isDisposed()) {
            Level level = MainMenu.this.level;
            PanningCamera camera = MainMenu.this.camera;
            if (level != null && camera != null) {
               synchronized (MainMenu.this.lock) {
                  Performance.recordConstant(this, "refreshLevelLoading", () -> MainMenu.this.refreshLevelLoading(level));
                  if (!level.isLoadingComplete()) {
                     level.onLoadingComplete();
                  }

                  MainMenu.this.worldEntity.serverFrameTick(this);
                  if (this.isGameTick()) {
                     MainMenu.this.worldEntity.serverTick();
                     level.clientTick();
                     level.serverTick();
                  }

                  level.frameTick(this);
                  if (this.isGameTick()) {
                     Performance.recordConstant(this, "levelEffectTick", () -> level.tickEffect(camera, null));
                  }
               }
            }
         }
      }
   }

   public class MainMenuTickThread extends Thread {
      public MainMenu.MainMenuGameLoop gameLoop;

      public MainMenuTickThread(String name, int maxFPS) {
         super(name);
         this.gameLoop = MainMenu.this.new MainMenuGameLoop(name, maxFPS);
      }

      @Override
      public void run() {
         this.gameLoop.runMainGameLoop();
      }
   }
}
