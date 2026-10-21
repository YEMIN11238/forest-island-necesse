package necesse.engine.state;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import necesse.engine.AbstractMusicList;
import necesse.engine.GameCrashLog;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.eventStatusBars.EventStatusBarData;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketDisconnect;
import necesse.engine.network.packet.PacketOpenJournal;
import necesse.engine.network.packet.PacketOpenPartyConfig;
import necesse.engine.network.packet.PacketSettlementOpen;
import necesse.engine.network.packet.PacketSpawnPlayer;
import necesse.engine.network.packet.PacketStartExpression;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.sound.SoundEmitter;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.GameBackground;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.camera.MainGameCamera;
import necesse.gfx.camera.MainGameFollowCamera;
import necesse.gfx.camera.MainGameMousePanningCamera;
import necesse.gfx.camera.MainGamePanningCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsBox;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.ProgressBarDrawOptions;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormTypingComponent;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.HUD;
import necesse.gfx.ui.debug.Debug;
import necesse.inventory.container.AdventureJournalContainer;
import necesse.inventory.container.AdventurePartyConfigContainer;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.travel.TravelContainer;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.reports.ReportUtils;
import org.lwjgl.opengl.GL14;

public class MainGame extends State {
   public static boolean debugFormActive = false;
   private HudDrawElement hudDraw;
   private MainGameCamera defaultCamera;
   private MainGameCamera camera;
   private final Client client;
   private boolean showMap;
   private boolean showScoreboard;
   public MainGameFormManager formManager;
   private float pauseAfterFocusLossBuffer = 0.0F;
   private long nextStatsStoreTime = System.currentTimeMillis() + 120000L;
   private boolean saveZoom;
   private float lastBlindness;
   private boolean drawPrepared = false;
   private int chatMessageRemainingMillis;
   private String chatMessage;

   public MainGame(Client client) {
      this.client = client;
      this.init();
   }

   public void init() {
      this.defaultCamera = new MainGameFollowCamera();
      this.resetCamera();
      ControllerInput.disableLayer(ControllerInput.MENU_SET_LAYER);
      FormManager.cleanUpLastControllerFocuses();
      HUD.reset();
      this.setupFormManager();
      this.setRunning(true);
      this.setShowMap(false);
      this.setShowScoreboard(false);
      Settings.hideUI = false;
      Settings.hideCursor = false;
      this.setupHudDraw();
      this.client.network.sendPacket(new PacketSpawnPlayer(this.client));
      if (GameLaunch.chatMessage != null) {
         this.chatMessage = GameLaunch.chatMessage;
         this.chatMessageRemainingMillis = GameLaunch.chatMessageDelaySeconds * 1000;
         GameLaunch.chatMessage = null;
         System.out.println("Sending chat message \"" + this.chatMessage + "\" in " + GameLaunch.chatMessageDelaySeconds + " seconds...");
      }

      this.client.spawnPacketSentTime = System.currentTimeMillis();
      this.lastBlindness = 0.0F;
      if (!this.client.worldSettings.cheatsAllowedOrHidden()) {
         System.out.println("Cleared debug menu");
         debugFormActive = false;
      }

      this.isInitialized = true;
   }

   @Override
   public void frameTick(TickManager tickManager, GameWindow window) {
      if (this.client == null) {
         throw new IllegalStateException("Client is not initiated");
      }

      if (GlobalData.getCurrentState() == this) {
         PlayerMob player = this.client.getPlayer();
         if (this.nextStatsStoreTime <= System.currentTimeMillis()) {
            Platform.getStatsProvider().storeStatsAndAchievements();
            this.nextStatsStoreTime = System.currentTimeMillis() + 120000L;
         }

         AtomicReference<InputEvent> escapeEventRef = new AtomicReference<>();
         Performance.record(tickManager, "controls", () -> {
            if (Control.SCREENSHOT.isPressed()) {
               if (window.getInput().isKeyDown(80)) {
                  Renderer.takeSceneAndHudShot(tickManager, this.client.chat);
               } else {
                  Renderer.takeScreenshot(this.client.chat);
               }
            }

            if (Control.ZOOM_IN.isMouseWheel()) {
               if (!this.formManager.isMouseOver()) {
                  InputEvent event = Control.ZOOM_IN.getEvent();
                  float wheelY = event != null && !event.isUsed() ? (float)event.getMouseWheelY() : 0.0F;
                  if (wheelY != 0.0F) {
                     event.use();
                     Settings.sceneSize = GameMath.limit(Settings.sceneSize + 0.05F * wheelY, GameWindow.minSceneSize, GameWindow.maxSceneSize);
                     window.updateSceneSize();
                     this.client.chat.addOrModifyMessage("zoomLevel", "Zoom: " + (int)(Settings.sceneSize * 100.0F) + "%");
                     this.saveZoom = true;
                  }
               }
            } else if (Control.ZOOM_IN.isDown()) {
               Settings.sceneSize = GameMath.limit(Settings.sceneSize + 0.001F * tickManager.getDelta(), GameWindow.minSceneSize, GameWindow.maxSceneSize);
               window.updateSceneSize();
               this.client.chat.addOrModifyMessage("zoomLevel", "Zoom: " + (int)(Settings.sceneSize * 100.0F) + "%");
               this.saveZoom = true;
            }

            if (Control.ZOOM_OUT.isMouseWheel()) {
               if (!this.formManager.isMouseOver()) {
                  InputEvent event = Control.ZOOM_OUT.getEvent();
                  float wheelY = event != null && !event.isUsed() ? (float)event.getMouseWheelY() : 0.0F;
                  if (wheelY != 0.0F) {
                     event.use();
                     Settings.sceneSize = GameMath.limit(Settings.sceneSize + 0.05F * wheelY, GameWindow.minSceneSize, GameWindow.maxSceneSize);
                     window.updateSceneSize();
                     this.client.chat.addOrModifyMessage("zoomLevel", "Zoom: " + (int)(Settings.sceneSize * 100.0F) + "%");
                     this.saveZoom = true;
                  }
               }
            } else if (Control.ZOOM_OUT.isDown()) {
               Settings.sceneSize = GameMath.limit(Settings.sceneSize - 0.001F * tickManager.getDelta(), GameWindow.minSceneSize, GameWindow.maxSceneSize);
               window.updateSceneSize();
               this.client.chat.addOrModifyMessage("zoomLevel", "Zoom: " + (int)(Settings.sceneSize * 100.0F) + "%");
               this.saveZoom = true;
            }

            if (this.saveZoom && tickManager.isFirstGameTickInSecond()) {
               Settings.saveClientSettings();
               this.saveZoom = false;
            }

            if (this.client.getPermissionLevel().getLevel() >= PermissionLevel.OWNER.getLevel()) {
               InputEvent f10Event = window.getInput().getEvent(299);
               if (f10Event != null && !f10Event.isUsed() && f10Event.state) {
                  if (this.client.worldSettings.cheatsAllowedOrHidden()) {
                     debugFormActive = !debugFormActive;
                  } else {
                     this.client.chat.addMessage(Localization.translate("misc", "allowcheats"));
                  }
               }
            } else {
               debugFormActive = false;
            }

            if (GlobalData.isDevMode()) {
               InputEvent f8 = window.getInput().getEvent(297);
               InputEvent f9 = window.getInput().getEvent(298);
               if (f8 != null && f8.state || f9 != null && f9.state) {
                  float[] modifiers = new float[]{0.5F, 1.0F, 1.5F, 2.0F, 4.0F, 8.0F, 16.0F, 32.0F};
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
                  this.client.chat.addOrModifyMessage("globalTimeMod", "Time modifier: x" + TickManager.globalTimeMod);
               }
            }

            if (this.client.getPermissionLevel().getLevel() >= PermissionLevel.ADMIN.getLevel()) {
               InputEvent f8Event = window.getInput().getEvent(296);
               if (f8Event != null && !f8Event.isUsed() && f8Event.state) {
                  if (this.client.worldSettings.cheatsAllowedOrHidden()) {
                     if (this.camera == this.defaultCamera) {
                        this.camera = new MainGameMousePanningCamera(this.camera.getX(), this.camera.getY(), 100.0F);
                     } else {
                        this.resetCamera();
                     }
                  } else {
                     this.client.chat.addMessage(Localization.translate("misc", "allowcheats"));
                  }
               }
            }

            if (Control.DEBUG_INFO.isPressed()) {
               Debug.submitChange();
            }

            if (Control.DEBUG_HUD.isPressed()) {
               HUD.debugActive = !HUD.debugActive;
            }

            if (Control.HIDE_UI.isPressed()) {
               Settings.hideUI = !Settings.hideUI;
            }

            if (Control.HIDE_CURSOR.isPressed()) {
               Settings.hideCursor = !Settings.hideCursor;
            }

            if (this.showMap() && this.client.isDead) {
               this.setShowMap(false);
            }

            if (this.showScoreboard() && this.client.isDead) {
               this.setShowScoreboard(false);
            }

            escapeEventRef.set(window.getInput().getEvent(256));
            if (escapeEventRef.get() == null) {
               ControllerEvent controllerEvent = ControllerInput.getEvent(ControllerInput.MENU_BACK);
               if (controllerEvent != null) {
                  escapeEventRef.set(InputEvent.ControllerButtonEvent(controllerEvent, tickManager));
               }
            }

            if (escapeEventRef.get() == null) {
               ControllerEvent controllerEvent = ControllerInput.getEvent(ControllerInput.MAIN_MENU);
               if (controllerEvent != null) {
                  escapeEventRef.set(InputEvent.ControllerButtonEvent(controllerEvent, tickManager));
               }
            }

            InputEvent escapeEvent = escapeEventRef.get();
            if (escapeEvent != null && !escapeEvent.isUsed() && escapeEvent.state) {
               this.formManager.chat.submitEscapeEvent(escapeEvent);
            }

            if (Settings.pauseOnFocusLoss && !window.isFocused() && this.isRunning() && !this.client.isDead) {
               this.pauseAfterFocusLossBuffer = this.pauseAfterFocusLossBuffer + tickManager.getDelta();
               if (this.pauseAfterFocusLossBuffer >= 1000.0F) {
                  if (this.formManager.hasFocusForm()) {
                     this.client.closeContainer(true);
                  }

                  this.setRunning(false);
               }
            } else {
               this.pauseAfterFocusLossBuffer = 0.0F;
            }

            if (Settings.pauseOnFocusLoss && PlatformManager.getPlatform().isRequestingPause() && this.isRunning() && !this.client.isDead) {
               this.setRunning(false);
            }

            if (this.isRunning() && ControllerInput.getEvent(ControllerInput.CONTROLLER_DISCONNECTED_EVENT) != null) {
               this.setRunning(false);
            }

            this.updateMenuLayerActive();
            ControllerInput.setMoveAsMenuNavigation(!this.isRunning());
            Performance.record(tickManager, "formInput", () -> {
               if (!Settings.hideUI) {
                  try {
                     window.getInput().getEvents().forEach(e -> {
                        if (Debug.isActive()) {
                           Debug.submitInputEvent(e, this.client);
                        }

                        this.formManager.submitInputEvent(e, tickManager, player);
                     });
                     ControllerInput.getEvents().forEach(e -> this.formManager.submitControllerEvent(e, tickManager, player));
                  } catch (ConcurrentModificationException e) {
                     System.err.println("ConcurrentModificationException likely caused by opening of url/file");
                     e.printStackTrace();
                  }
               }
            });
         });
         Performance.record(tickManager, "controls", () -> {
            InputEvent escapeEvent = escapeEventRef.get();
            if (this.isRunning() && !this.client.isDead) {
               if (!FormTypingComponent.isCurrentlyTyping()) {
                  if (escapeEvent != null && !escapeEvent.isUsed() && escapeEvent.state) {
                     if (this.showMap()) {
                        this.setShowMap(false);
                        escapeEvent.use();
                     } else if (Settings.hideUI) {
                        Settings.hideUI = false;
                        escapeEvent.use();
                     } else if (Settings.hideCursor) {
                        Settings.hideCursor = false;
                        escapeEvent.use();
                     } else if (this.camera != this.defaultCamera) {
                        this.resetCamera();
                        escapeEvent.use();
                     } else if (this.formManager.hasFocusForm()) {
                        this.client.closeContainer(true);
                        escapeEvent.use();
                     } else if (player != null && player.isInventoryExtended()) {
                        player.setInventoryExtended(false);
                        escapeEvent.use();
                     } else {
                        this.setRunning(false);
                        escapeEvent.use();
                     }
                  }

                  this.formManager.tickExpressionWheel(Control.EXPRESSION_WHEEL.isDown(), expression -> {
                     this.client.getPlayer().startExpression(expression);
                     this.client.network.sendPacket(new PacketStartExpression(this.client.getSlot(), expression));
                  });
                  if (Control.LOOT_ALL.isPressed()) {
                     this.client.getContainer().lootAllControlPressed();
                  }

                  if (Control.SORT_INVENTORY.isPressed()) {
                     this.client.getContainer().sortInventoryControlPressed();
                  }

                  if (Control.QUICK_STACK.isPressed()) {
                     this.client.getContainer().quickStackControlPressed();
                  }

                  if (Control.OPEN_ADVENTURE_PARTY.isPressed()) {
                     if (this.client.getContainer() instanceof AdventurePartyConfigContainer) {
                        this.client.closeContainer(true);
                     } else {
                        this.client.network.sendPacket(new PacketOpenPartyConfig());
                     }
                  }

                  if (Control.OPEN_ADVENTURE_JOURNAL.isPressed()) {
                     if (this.client.getContainer() instanceof AdventureJournalContainer) {
                        this.client.closeContainer(true);
                     } else {
                        this.client.network.sendPacket(new PacketOpenJournal());
                        this.client.hasNewJournalEntry = false;
                     }
                  }

                  if (Control.OPEN_SETTLEMENT.isPressed()) {
                     if (this.client.getContainer() instanceof SettlementContainer) {
                        this.client.closeContainer(true);
                     } else {
                        this.client.network.sendPacket(new PacketSettlementOpen());
                     }
                  }

                  if (Control.SHOW_MAP.isPressed()) {
                     this.setShowMap(!this.showMap());
                  }

                  if (Control.SCOREBOARD.isPressed()) {
                     this.setShowScoreboard(true);
                  }

                  if (Control.SCOREBOARD.isReleased()) {
                     this.setShowScoreboard(false);
                  }
               }

               if (player != null && player.getLevel() != null && player.getLevel() == this.client.levelManager.getDrawnLevel()) {
                  if (this.client.spawnPacketSentTime != 0L && System.currentTimeMillis() - this.client.spawnPacketSentTime >= 300L) {
                     this.client.network.sendPacket(new PacketSpawnPlayer(this.client));
                     this.client.spawnPacketSentTime = System.currentTimeMillis();
                  }

                  Performance.record(tickManager, "playerInput", () -> {
                     player.tickControls(this, tickManager.isGameTick(), this.camera);
                     window.getInput().getEvents().stream().filter(e -> !e.isUsed()).forEach(e -> player.submitInputEvent(this, e, this.camera));
                     if (!this.formManager.isMouseOver() || Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
                        if (Control.MOUSE1.isDown() && player.constantAttack) {
                           player.tryAttack(this.camera);
                        }

                        if (Control.MOUSE2.isDown() && player.constantInteract) {
                           player.runClientInteract(true, this.camera);
                        }
                     }
                  });
                  if (player.isInventoryExtended()) {
                     if (ControllerInput.isPressed(ControllerInput.MENU_NEXT)) {
                        player.setSelectedSlot((player.getSelectedSlot() + 1) % 10);
                     }

                     if (ControllerInput.isPressed(ControllerInput.MENU_PREV)) {
                        player.setSelectedSlot((player.getSelectedSlot() + 9) % 10);
                     }
                  }
               }
            } else if (escapeEvent != null && !escapeEvent.isUsed() && escapeEvent.state) {
               if (Settings.hideUI) {
                  Settings.hideUI = false;
                  escapeEvent.use();
               } else if (this.formManager.chat.isTyping()) {
                  this.formManager.chat.setTyping(false);
                  escapeEvent.use();
               } else if (this.camera != this.defaultCamera) {
                  this.resetCamera();
                  escapeEvent.use();
               } else if (!this.client.isDead) {
                  this.formManager.pauseMenu.submitEscapeEvent(escapeEvent);
               }
            }
         });
         Performance.record(tickManager, "formManagerTick", () -> this.formManager.frameTick(tickManager));
         if (tickManager.isGameTick()) {
            Performance.record(tickManager, "gameTick", this.client::tick);
            if (this.isRunning() && this.client.loading.isDone()) {
               Level level = this.client.getLevel();
               if (level != null) {
                  ClientClient me = this.client.getClient();
                  if (me != null && me.hasSpawned()) {
                     AbstractMusicList levelMusic = level.getLevelMusic(me.playerMob.getTileX(), me.playerMob.getTileY(), player);
                     if (levelMusic != null) {
                        SoundManager.setMusic(levelMusic, SoundManager.MusicPriority.BIOME);
                     }
                  }
               }
            }
         }

         Performance.record(tickManager, "frameTick", () -> this.client.frameTick(tickManager));
         if (player != null) {
            float blindness = player.buffManager.getModifier(BuffModifiers.BLINDNESS);
            float blindnessDelta = blindness - this.lastBlindness;
            float blindnessSignum = Math.signum(blindnessDelta);
            if (blindnessSignum > 0.0F) {
               this.lastBlindness = GameMath.limit(
                  this.lastBlindness + (blindnessSignum - Math.min(1.0F - blindnessDelta, 0.75F)) * tickManager.getDelta() / 500.0F, 0.0F, blindness
               );
            } else if (blindnessSignum < 0.0F) {
               this.lastBlindness = GameMath.limit(
                  this.lastBlindness + (blindnessSignum - Math.min(1.0F - blindnessDelta, 0.75F)) * tickManager.getDelta() / 500.0F, blindness, 1.0F
               );
            }
         }

         if (tickManager.isGameTick()) {
            Performance.recordConstant(tickManager, "levelEffectTick", () -> {
               if (this.client.getLevel() != null && !this.client.isPaused()) {
                  this.client.getLevel().tickEffect(this.camera, player);
               }
            });
            if (this.lastBlindness > 0.0F && player != null) {
               int minViewDistance = GameMath.lerp(this.lastBlindness, 500, 0);
               int midX = this.camera.getDrawX(player.getDrawX());
               int midY = this.camera.getDrawY(player.getDrawY()) - 16;
               PostProcessingEffects.setSceneDarkness((float)Math.pow(this.lastBlindness, 0.5), minViewDistance, minViewDistance * 3, midX, midY);
            }

            if (this.chatMessage != null) {
               this.chatMessageRemainingMillis -= 50;
               if (this.chatMessageRemainingMillis <= 0) {
                  System.out.println("Sending chat message \"" + this.chatMessage + "\" now");
                  this.formManager.chat.submitMessage(this.chatMessage);
                  this.chatMessage = null;
               }
            }
         }
      }
   }

   @Override
   public void secondTick(TickManager tickManager) {
      ReportUtils.updateSessionSeconds(this.client);
   }

   @Override
   public boolean prepareDrawScene(TickManager tickManager, boolean sceneUpdated) {
      PlayerMob perspective = this.getDrawnPlayer();
      Level level = this.getDrawnLevel();
      if (level != null) {
         level.runGLContextRunnables();
         Performance.record(tickManager, "prepareCamera", () -> {
            if (level.isServer() || level == this.client.levelManager.getLevel()) {
               int lastX = this.camera.getX();
               int lastY = this.camera.getY();
               this.camera.updateToSceneDimensions();
               this.camera.tickCamera(tickManager, this, this.client);
               if (level.shouldLimitCameraWithinBounds(perspective)) {
                  this.camera.limitToLevel(level);
               }

               if (lastX != this.camera.getX() || lastY != this.camera.getY()) {
                  WindowManager.getWindow().submitNextMoveEvent();
               }
            }
         });
         if (!level.prepareDraw(this.camera, perspective, this.client.tickManager(), sceneUpdated)) {
            return false;
         }
      }

      this.drawPrepared = true;
      return true;
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
      Level level = this.getDrawnLevel();
      if (level != null) {
         PlayerMob perspective = this.getDrawnPlayer();
         level.draw(this.camera, perspective, this.client.tickManager(), sceneUpdated);
      }

      Performance.record(tickManager, "otherDraw", () -> {
         if (Settings.hideUI || Settings.hideCursor) {
            Renderer.setCursor(GameWindow.CURSOR.INVISIBLE);
         }
      });
   }

   @Override
   public void drawSceneOverlay(TickManager tickManager) {
      PlayerMob perspective = this.getDrawnPlayer();
      Level level = this.getDrawnLevel();
      if (level != null) {
         level.drawHud(this.camera, perspective, this.client.tickManager());
      }

      if (perspective != null) {
         Rectangle selectBox = perspective.getSelectBox();
         GameTooltipManager.setTooltipsPlayer(
            InputPosition.fromScenePos(WindowManager.getWindow().getInput(), this.camera.getDrawX(selectBox.x), this.camera.getDrawY(selectBox.y))
         );
      }
   }

   @Override
   public void drawHud(TickManager tickManager) {
      if (!Settings.hideUI) {
         GameWindow window = WindowManager.getWindow();
         GameMessage loadingMessage = this.client.loading.getLoadingMessage();
         if (loadingMessage != null) {
            int startY = window.getHudHeight() / 2 + 100;
            FontOptions fontOptions = new FontOptions(16).color(Color.WHITE);
            String message = loadingMessage.translate();
            int maxWidth = 400;
            ArrayList<String> lines = GameUtils.breakString(message, fontOptions, maxWidth);
            if (lines.size() <= 1) {
               maxWidth = FontManager.bit.getWidthCeil(message, fontOptions);
            }

            int height = lines.size() * fontOptions.getSize();
            startY = GameMath.limit(startY, 0, window.getHudHeight() - height);
            GameBackground.itemTooltip.getDrawOptions(window.getHudWidth() / 2 - maxWidth / 2 - 4, startY - 4, maxWidth + 8, height + 8).draw();

            for (int i = 0; i < lines.size(); i++) {
               String line = lines.get(i);
               int lineWidth = FontManager.bit.getWidthCeil(line, fontOptions);
               int y = startY + i * fontOptions.getSize();
               int x = window.getHudWidth() / 2 - lineWidth / 2;
               FontManager.bit.drawString(x, y, line, fontOptions);
            }
         }

         PlayerMob perspective = this.getDrawnPlayer();
         if (Settings.serverPerspective) {
            int width = FontManager.bit.getWidthCeil("VIEWING SERVER PERSPECTIVE", new FontOptions(20));
            FontManager.bit.drawString(window.getHudWidth() / 2 - width / 2, 130.0F, "VIEWING SERVER PERSPECTIVE", new FontOptions(20));
         }

         if (this.formManager != null) {
            if (this.client.messageShown()) {
               FontOptions options = new FontOptions(20).outline().color(this.client.getMessageColor());
               ArrayList<GameMessage> lines = this.client.getMessage().breakMessage(options, window.getHudWidth() / 2);

               for (int i = 0; i < lines.size(); i++) {
                  GameMessage msg = lines.get(i);
                  int width = FontManager.bit.getWidthCeil(msg.translate(), options);
                  FontManager.bit
                     .drawString(
                        window.getHudWidth() / 2 - width / 2,
                        this.formManager.importantBuffs.getHeight() + this.formManager.importantBuffs.getY() + 10 + options.getSize() * i,
                        msg.translate(),
                        options
                     );
               }
            }

            this.formManager.unimportantBuffs.setPosition(this.getStatusBarWidth() + 6, this.getStatusDrawBox(perspective).getBoundingBox().y - 6);
            if (this.formManager.minimap.isMinimized()) {
               this.formManager.unimportantBuffs.columns = (window.getHudWidth() - this.getStatusBarWidth() + 6 - 40) / 40;
            } else {
               this.formManager.unimportantBuffs.columns = (window.getHudWidth() - this.getStatusBarWidth() - this.formManager.minimap.getWidth() + 6 - 40)
                  / 40;
            }

            this.formManager.unimportantBuffs.columns = Math.max(this.formManager.unimportantBuffs.columns, 2);
            this.formManager.importantBuffs.setPosition(window.getHudWidth() / 2, this.getStatusBarHeight());
            int travelY = Math.max(this.getStatusBarHeight() + this.formManager.importantBuffs.getHeight() + 10, window.getHudHeight() / 4);
            this.formManager.travel.setPosition((window.getHudWidth() - this.formManager.travel.getWidth()) / 2, travelY);
            this.formManager.chat.setHidden(false);
            if (this.isRunning()) {
               this.drawStatusBar();
               this.drawEventStatusBars();
            }

            if (ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER)) {
               GameTooltipManager.addControllerGlyph(
                  Localization.translate("controls", "navigatetip"),
                  ControllerInput.MENU_UP,
                  ControllerInput.MENU_RIGHT,
                  ControllerInput.MENU_DOWN,
                  ControllerInput.MENU_LEFT
               );
               GameTooltipManager.addControllerGlyph(Localization.translate("ui", "backbutton"), ControllerInput.MENU_BACK);
            } else {
               GameTooltipManager.addControllerGlyph(Localization.translate("controls", "movetip"), ControllerInput.MOVE);
               GameTooltipManager.addControllerGlyph(Localization.translate("controls", "aimtip"), ControllerInput.AIM);
               GameTooltipManager.addControllerGlyph(Localization.translate("controls", "hotbartip"), ControllerInput.PREV_HOTBAR, ControllerInput.NEXT_HOTBAR);
               if (this.isRunning() && perspective.getSelectedItem() != null) {
                  GameTooltipManager.addControllerGlyph(Localization.translate("controls", "mouse1"), ControllerInput.ATTACK);
               }

               if (perspective.isRiding()) {
                  GameTooltipManager.addControllerGlyph(Localization.translate("controls", "usemount"), ControllerInput.USE_MOUNT);
               }
            }

            GameTooltipManager.addControllerGlyph(Localization.translate("controls", "inventory"), ControllerInput.INVENTORY);
            GameTooltipManager.addControllerGlyph(Localization.translate("controls", "togglecursor"), ControllerInput.TOGGLE_AIM);
            Performance.record(tickManager, "formsDraw", () -> this.formManager.draw(tickManager, perspective));
            if (this.isRunning()) {
               this.client.storyManager.drawOverForms(this, perspective);
            }

            Performance.record(tickManager, "debugDraw", () -> Debug.draw(this.client));
            this.client.drawCreditsHudIfShould();
         }
      }
   }

   private Level getDrawnLevel() {
      Level level = this.client.levelManager.getDrawnLevel();
      if (Settings.serverPerspective && this.client.getLocalServer() != null) {
         level = this.client.getLocalServer().world.getLevel(level.getIdentifier());
      }

      return level;
   }

   private PlayerMob getDrawnPlayer() {
      PlayerMob player = this.client.getPlayer();
      if (Settings.serverPerspective && this.client.getLocalServer() != null) {
         player = this.client.getLocalServer().getPlayer(this.client.getSlot());
      }

      return player;
   }

   public void setupHudDraw() {
      if (this.hudDraw != null) {
         this.hudDraw.remove();
      }

      this.hudDraw = new HudDrawElement() {
         @Override
         public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
            if (Settings.showTeammateMarkers) {
               LinkedList<Rectangle> occupied = new LinkedList<>();
               ClientClient me = MainGame.this.client.getClient();
               FontOptions fontOptions = new FontOptions(16).outline().color(200, 200, 200);
               int screenSize = Math.min(camera.getWidth(), camera.getHeight());

               for (int i = 0; i < MainGame.this.client.getSlots(); i++) {
                  ClientClient cl = MainGame.this.client.getClient(i);
                  if (cl != null && cl != me && cl.loadedPlayer && cl.hasSpawned() && !cl.isDead() && (cl.isSameTeam(me) || GlobalData.debugCheatActive())) {
                     String name = cl.getName();
                     final DrawOptionsList drawOptions = new DrawOptionsList();
                     LevelIdentifier meLevel = me.getLevelIdentifier();
                     LevelIdentifier clLevel = cl.getLevelIdentifier();
                     if ((!meLevel.isOneWorldDimension() || !clLevel.isOneWorldDimension()) && !meLevel.equals(clLevel)) {
                        GameWindow window = WindowManager.getWindow();
                        int drawX = window.getSceneWidth() / 2;
                        int drawY = window.getSceneHeight() / 2 - screenSize / 3;
                        int width = FontManager.bit.getWidthCeil(name, fontOptions);
                        Rectangle drawBounds = new Rectangle(drawX - width / 2, drawY, width, 20);

                        while (true) {
                           Rectangle finalBounding = new Rectangle(drawBounds);
                           Rectangle collision = occupied.stream().filter(r -> r.intersects(finalBounding)).findFirst().orElse(null);
                           if (collision == null) {
                              drawOptions.add(new StringDrawOptions(fontOptions, name).pos(drawX - width / 2, drawBounds.y));
                              occupied.add(drawBounds);
                              break;
                           }

                           drawBounds = new Rectangle(drawX - width / 2, collision.y + collision.height, width, 20);
                        }
                     } else {
                        ArrayList<String> lines = new ArrayList<>();
                        lines.add(name);
                        int dir = meLevel.getOneWorldDimensionDelta(clLevel);
                        if (dir != 0) {
                           if (dir < 0) {
                              lines.add("(" + Localization.translate("ui", "scoredirdown") + ")");
                           } else {
                              lines.add("(" + Localization.translate("ui", "scoredirup") + ")");
                           }
                        }

                        drawOptions.add(HUD.getDirectionIndicator(me.playerMob.x, me.playerMob.y, cl.playerMob, 0, 5.0F, lines, fontOptions, camera));
                     }

                     list.add(new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return Integer.MAX_VALUE;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           drawOptions.draw();
                        }
                     });
                  }
               }
            }
         }
      };
      this.client.getLevel().hudManager.addElement(this.hudDraw);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      if (this.formManager != null) {
         this.formManager.onWindowResized(window);
      }

      this.client.onWindowResized(window);
   }

   public void setupFormManager() {
      if (this.formManager != null) {
         this.formManager.dispose();
      }

      this.formManager = new MainGameFormManager(this, this.client);
      this.formManager.setup();
   }

   @Override
   public FormManager getFormManager() {
      return this.formManager;
   }

   public void updateMenuLayerActive() {
      PlayerMob player = this.client.getPlayer();
      boolean menuLayerActive = player != null && player.isInventoryExtended()
         || this.client.hasFocusForm()
         || this.client.isDead
         || this.formManager.hasFocusForm()
         || this.formManager.isControllerKeyboardOpen()
         || this.formManager.hasFloatMenu()
         || !this.formManager.travel.isHidden()
         || this.formManager.hasContinueForms()
         || this.showMap()
         || !this.isRunning()
         || GameToolManager.doesToolForceMenuLayer();
      if (menuLayerActive) {
         if (!ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER)) {
            ControllerInput.enableLayer(ControllerInput.MENU_SET_LAYER);
         }
      } else if (ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER)) {
         ControllerInput.disableLayer(ControllerInput.MENU_SET_LAYER);
      }
   }

   @Override
   public void onClientDrawnLevelChanged() {
      super.onClientDrawnLevelChanged();
      this.setupFormManager();
      this.formManager.updateActive(true);
      this.formManager.fixSidebar();
      this.formManager.chat.refreshBoundingBoxes();
      this.setupHudDraw();
   }

   @Override
   public Stream<Rectangle> streamHudHitboxes() {
      PlayerMob player = this.client.getPlayer();
      if (player == null) {
         return super.streamHudHitboxes();
      }

      Rectangle boundingBox = this.getStatusDrawBox(player).getBoundingBox();
      return Stream.concat(super.streamHudHitboxes(), Stream.of(new Rectangle(boundingBox.x, boundingBox.y - 32, boundingBox.width, boundingBox.height + 32)));
   }

   @Override
   public void reloadInterfaceFromSettings(boolean makeInterfaceCurrent) {
      this.formManager = new MainGameFormManager(this, this.client);
      this.formManager.setup();
      this.formManager.pauseMenu.setHidden(false);
      this.formManager.pauseMenu.makeCurrent(this.formManager.pauseMenu.settings);
      if (makeInterfaceCurrent) {
         this.formManager.pauseMenu.settings.makeInterfaceCurrent();
         this.formManager.pauseMenu.settings.setSaveActive(true);
         this.formManager.pauseMenu.settings.reloadedInterface = true;
      }

      this.formManager.updateActive(true);
      this.formManager.chat.refreshBoundingBoxes();
   }

   @Override
   public void setRunning(boolean running) {
      boolean before = this.isRunning();
      super.setRunning(running);
      if (before != this.isRunning()) {
         if (this.client.isSingleplayer()) {
            if (this.isRunning()) {
               this.client.resume();
            } else {
               this.client.pause();
            }
         }

         if (!this.isRunning()) {
            this.setShowScoreboard(false);
         }

         this.formManager.pauseMenu.setHidden(this.isRunning());
      }
   }

   public void drawEventStatusBars() {
      int midY = WindowManager.getWindow().getHudHeight() / 2;
      int width = this.formManager.toolbar.getWidth();
      int drawX = this.formManager.toolbar.getX();
      int drawY = this.formManager.toolbar.getY() - 10;
      if (!this.formManager.inventory.isHidden()) {
         drawX = this.formManager.inventory.getX();
         drawY = this.formManager.inventory.getY() - 10;
      }

      for (EventStatusBarData statusBar : EventStatusBarManager.getStatusBars()) {
         if (Settings.showBossHealthBars || statusBar.category != EventStatusBarData.BarCategory.boss) {
            FontOptions fontOptions = new FontOptions(16).outline();
            EventStatusBarData.StatusAtTime latest = statusBar.getLatest();
            boolean draw = false;
            ProgressBarDrawOptions drawOptions = new ProgressBarDrawOptions(Settings.UI.healthbar_big, width);
            Color bufferColor = statusBar.getBufferColor();
            if (bufferColor != null) {
               EventStatusBarData.StatusAtTime buffered = statusBar.getBuffered();
               float bufferedPerc = buffered.getPercent();
               drawOptions = drawOptions.addBar(bufferedPerc).color(bufferColor).end();
               draw = true;
            }

            Color fillColor = statusBar.getFillColor();
            if (fillColor != null) {
               float perc = latest.getPercent();
               drawOptions.addBar(perc).color(fillColor).end();
               draw = true;
            }

            GameMessage statusText = statusBar.getStatusText(latest);
            if (statusText != null) {
               drawOptions.text(statusText.translate()).fontOptions(fontOptions);
            }

            if (draw) {
               drawY -= drawOptions.getHeight();
               drawOptions.draw(drawX, drawY);
            }

            FairTypeDrawOptions displayNameDrawOptions = statusBar.getDisplayNameDrawOptions();
            if (displayNameDrawOptions != null) {
               Rectangle boundingBox = displayNameDrawOptions.getBoundingBox();
               drawY -= boundingBox.height + 2;
               displayNameDrawOptions.draw(drawX + width / 2, drawY);
            }

            if (drawY < midY + 100) {
               break;
            }
         }
      }
   }

   public int getStatusBarHeight() {
      if (this.client == null) {
         return 0;
      }

      PlayerMob player = this.client.getPlayer();
      if (player == null) {
         return 0;
      }

      Rectangle box = this.getStatusDrawBox(player).getBoundingBox();
      return box.y + box.height;
   }

   public int getStatusBarWidth() {
      if (this.client == null) {
         return 0;
      }

      PlayerMob player = this.client.getPlayer();
      if (player == null) {
         return 0;
      }

      Rectangle box = this.getStatusDrawBox(player).getBoundingBox();
      return box.x + box.width;
   }

   public void drawStatusBar() {
      if (this.client != null) {
         PlayerMob player = this.client.getPlayer();
         if (player != null) {
            FontOptions options = new FontOptions(16).outline();
            String str = player.getHealth() + "/" + player.getMaxHealth();
            int width = FontManager.bit.getWidthCeil(str, options);
            GameWindow window = WindowManager.getWindow();
            FontManager.bit.drawString(window.getHudWidth() / 2 - width / 2, 16.0F, str, options);
            if (player.getMaxResilience() > 0) {
               FontOptions resOptions = new FontOptions(16).outline().color(new Color(255, 233, 73));
               String resStr = (int)player.getResilience() + "/" + player.getMaxResilience();
               int resWidth = FontManager.bit.getWidthCeil(resStr, resOptions);
               FontManager.bit.drawString(window.getHudWidth() / 2 - resWidth / 2 + width + 16, 16.0F, resStr, resOptions);
            }

            DrawOptionsBox drawBox = this.getStatusDrawBox(player);
            drawBox.draw();
         }
      }
   }

   public DrawOptionsBox getStatusDrawBox(PlayerMob player) {
      GameWindow window = WindowManager.getWindow();
      int playerHealth = player.getMaxHealthFlat() + player.getMaxHealthUpgrade();
      int maxRows = GameMath.limit((int)Math.ceil(playerHealth / 200.0F), 1, 3);
      if (200 >= playerHealth && playerHealth > 100) {
         maxRows = 2;
      }

      int iconsPerRow = GameMath.limit(GameMath.ceilToNearest(playerHealth, 200 > playerHealth ? 100 : 200) / maxRows / 10, 10, 20);
      int rowWidth = Settings.UI.heart_outline.getWidth() * 10 + 36;
      int rowHeight = Settings.UI.heart_outline.getHeight() / (maxRows == 3 ? 2 : 1) + 4;
      DrawOptionsBox drawBox = drawStatusIconsCentered(
         window.getHudWidth() / 2,
         32,
         Settings.UI.heart_outline,
         Settings.UI.heart_fill,
         player.getHealth(),
         player.getMaxHealth(),
         10.0F,
         iconsPerRow,
         maxRows,
         0,
         rowWidth,
         rowHeight,
         new StringTooltips(Localization.translate("ui", "healthbartip", "value", player.getHealth() + "/" + player.getMaxHealth()))
      );
      if (player.getMaxResilience() > 0) {
         Rectangle currentBox = drawBox.getBoundingBox();
         drawBox = DrawOptionsBox.concat(
            drawBox,
            drawStatusIconsCentered(
               window.getHudWidth() / 2,
               currentBox.y + currentBox.height + (maxRows == 3 ? 10 : 0),
               Settings.UI.resilience_outline,
               Settings.UI.resilience_fill,
               player.getResilience(),
               player.getMaxResilience(),
               10.0F,
               10,
               1,
               0,
               rowWidth,
               Settings.UI.resilience_outline.getHeight() + 4,
               new StringTooltips(Localization.translate("ui", "resiliencebartip", "value", (int)player.getResilience() + "/" + player.getMaxResilience()))
            )
         );
      }

      if (player.getLevel() != null && player.getLevel().getWorldSettings() != null && player.getLevel().getWorldSettings().playerHunger()) {
         float hungerLevel = Math.min(1.0F, player.hungerLevel);
         Rectangle currentBox = drawBox.getBoundingBox();
         drawBox = DrawOptionsBox.concat(
            drawBox,
            drawStatusIconsCentered(
               window.getHudWidth() / 2,
               currentBox.y + currentBox.height,
               Settings.UI.food_outline,
               Settings.UI.food_fill,
               hungerLevel,
               1.0F,
               0.1F,
               10,
               1,
               0,
               new StringTooltips(Localization.translate("ui", "hungerbartip", "value", (int)Math.ceil(hungerLevel * 100.0F) + "%"))
            )
         );
      }

      if (player.getLevel() != null && player.usesMana() && player.isManaBarVisible()) {
         final float mana = player.getMana();
         final int maxMana = player.getMaxMana();
         Rectangle currentBox = drawBox.getBoundingBox();
         String manaStr = Math.round(mana) + " / " + maxMana;
         FontOptions fontOptions = new FontOptions(16).outline();
         float manaPerc = GameMath.limit(mana / maxMana, 0.0F, 1.0F);
         int manaDrawX = window.getHudWidth() / 2 - rowWidth / 2;
         int manaDrawY = currentBox.y + currentBox.height;
         ProgressBarDrawOptions manaBar = new ProgressBarDrawOptions(Settings.UI.healthbar_big, rowWidth)
            .addBar(manaPerc)
            .color(new Color(51, 133, 224))
            .end()
            .text(manaStr)
            .fontOptions(fontOptions);
         final DrawOptions manaDrawOptions = manaBar.pos(manaDrawX, manaDrawY);
         final Rectangle manaBounds = new Rectangle(manaDrawX, manaDrawY, rowWidth, manaBar.getHeight() + 4);
         DrawOptionsBox manaDrawOptionsBox = new DrawOptionsBox() {
            @Override
            public void draw() {
               manaDrawOptions.draw();
               if (this.getBoundingBox().contains(WindowManager.getWindow().mousePos().hudX, WindowManager.getWindow().mousePos().hudY)) {
                  GameTooltipManager.addTooltip(
                     new StringTooltips(Localization.translate("ui", "manabartip", "value", (int)Math.ceil(mana) + "/" + maxMana)), TooltipLocation.PLAYER
                  );
               }
            }

            @Override
            public Rectangle getBoundingBox() {
               return manaBounds;
            }
         };
         drawBox = DrawOptionsBox.concat(manaDrawOptionsBox, drawBox);
      }

      return drawBox;
   }

   public static DrawOptionsBox drawStatusIconsCentered(
      int drawX,
      int drawY,
      GameTexture outline,
      GameTexture fill,
      float value,
      float maxValue,
      float valuePerIcon,
      int iconsPerRow,
      int maxRows,
      int bounceOffset,
      GameTooltips tooltips
   ) {
      return drawStatusIconsCentered(
         drawX,
         drawY,
         outline,
         fill,
         value,
         maxValue,
         valuePerIcon,
         iconsPerRow,
         maxRows,
         bounceOffset,
         outline.getWidth() * iconsPerRow + 4 * (iconsPerRow - 1),
         outline.getHeight() + 4,
         tooltips
      );
   }

   public static DrawOptionsBox drawStatusIconsCentered(
      int drawX,
      int drawY,
      GameTexture outline,
      GameTexture fill,
      float value,
      float maxValue,
      float valuePerIcon,
      int iconsPerRow,
      int maxRows,
      int bounceOffset,
      int rowWidth,
      int rowHeight,
      GameTooltips tooltips
   ) {
      return drawStatusIcons(
         drawX - rowWidth / 2, drawY, outline, fill, value, maxValue, valuePerIcon, iconsPerRow, maxRows, bounceOffset, rowWidth, rowHeight, tooltips
      );
   }

   public static DrawOptionsBox drawStatusIcons(
      int drawX,
      int drawY,
      GameTexture outline,
      GameTexture fill,
      float value,
      float maxValue,
      float valuePerIcon,
      int iconsPerRow,
      int maxRows,
      int bounceOffset,
      GameTooltips tooltips
   ) {
      return drawStatusIcons(
         drawX,
         drawY,
         outline,
         fill,
         value,
         maxValue,
         valuePerIcon,
         iconsPerRow,
         maxRows,
         bounceOffset,
         outline.getWidth() * iconsPerRow + 4 * (iconsPerRow - 1),
         outline.getHeight() + 4,
         tooltips
      );
   }

   public static DrawOptionsBox drawStatusIcons(
      final int drawX,
      final int drawY,
      GameTexture outline,
      GameTexture fill,
      float value,
      float maxValue,
      float valuePerIcon,
      int iconsPerRow,
      int maxRows,
      int bounceOffset,
      final int rowWidth,
      int rowHeight,
      final GameTooltips tooltips
   ) {
      boolean bounce = value / maxValue < 0.2F;
      int bounceAmount = 0;
      if (bounce) {
         int bounceInterval = 5000;
         int totalBounceTime = 3000;
         int firstBounceTime = 600;
         long time = (System.currentTimeMillis() + bounceOffset) % bounceInterval;
         if (time <= totalBounceTime) {
            int bounceTime = firstBounceTime;
            int bounceTimeOffset = 0;

            while (bounceTime > 0) {
               float bouncePerc = (float)(time - bounceTimeOffset) / bounceTime;
               if (bouncePerc < 0.0F) {
                  break;
               }

               float amountMod = (float)bounceTime / firstBounceTime;
               bounceAmount = (int)(GameMath.sin(bouncePerc * 180.0F) * 10.0F * amountMod);
               bounceTimeOffset += bounceTime;
               bounceTime /= 2;
            }
         }
      }

      int totalIcons = Math.min(iconsPerRow * maxRows, (int)Math.ceil(maxValue / valuePerIcon));
      int outlineWidth = (outline.getWidth() - fill.getWidth()) / 2;
      int outlineHeight = (outline.getHeight() - fill.getHeight()) / 2;
      float finalValuePerIcon = maxValue / totalIcons;
      int emptySpace = rowWidth - outline.getWidth() * iconsPerRow;
      float emptySpacePerIcon = (float)emptySpace / (iconsPerRow - 1);
      int iconWidth = (rowWidth + fill.getWidth() / 2) / iconsPerRow;
      int rowTotalFilledWidth = rowWidth - outlineWidth * 2;
      float currentX = 0.0F;
      float currentWidth = 0.0F;
      int fillMaxWidthPerRow = Math.min(rowWidth - outlineWidth * 2, fill.getWidth() * iconsPerRow);
      float fillPerIcon = (fillMaxWidthPerRow + Math.min(0.0F, emptySpacePerIcon + outlineWidth * 2)) / iconsPerRow;
      final DrawOptionsList drawOptions = new DrawOptionsList();

      for (int i = 0; i < totalIcons; i++) {
         int column = i % iconsPerRow;
         int row = i / iconsPerRow;
         currentX += outline.getWidth() + emptySpacePerIcon;
         currentWidth += fillPerIcon;
         if (column == 0) {
            currentX = 0.0F;
            currentWidth = 0.0F;
         }

         int x = drawX + (int)currentX;
         int y = drawY + rowHeight * row;
         int minWidth = Math.min(iconWidth, fill.getWidth());
         int iconsThisRow = Math.min(totalIcons - row * iconsPerRow, iconsPerRow);
         int totalFilledWidth = Math.min(iconsThisRow * fill.getWidth(), rowWidth - outlineWidth * 2);
         float valueThisRow = value - row * finalValuePerIcon * iconsPerRow;
         float totalValueThisRow = iconsThisRow * finalValuePerIcon;
         int rowFilledWidth = Math.min((int)(valueThisRow / totalValueThisRow * totalFilledWidth), totalFilledWidth);
         int startWidth = (int)currentWidth;
         int currentFilled = rowFilledWidth - startWidth;
         boolean hasFilling = currentFilled > 0;
         drawOptions.add(outline.initDraw().pos(x, y - (hasFilling ? bounceAmount : 0)));
         if (hasFilling) {
            int widthFilled = Math.min(currentFilled, fill.getWidth());
            drawOptions.add(fill.initDraw().section(0, widthFilled, 0, fill.getHeight()).pos(x + outlineWidth, y + outlineHeight - bounceAmount));
         }
      }

      final int height = (int)Math.ceil((float)totalIcons / iconsPerRow) * rowHeight;
      return new DrawOptionsBox() {
         @Override
         public void draw() {
            GameWindow window = WindowManager.getWindow();
            GL14.glBlendFuncSeparate(770, 771, 770, 771);
            drawOptions.draw();
            GL14.glBlendFuncSeparate(770, 771, 1, 771);
            if (tooltips != null && this.getBoundingBox().contains(window.mousePos().hudX, window.mousePos().hudY)) {
               GameTooltipManager.addTooltip(tooltips, TooltipLocation.PLAYER);
            }
         }

         @Override
         public Rectangle getBoundingBox() {
            return new Rectangle(drawX, drawY, rowWidth, height);
         }
      };
   }

   public boolean canTravel(Client client, PlayerMob player) {
      return player != null
            && player.getLevel() != null
            && client.getLevel() != null
            && client.getLevel().isIslandPosition()
            && client.getLevel().getIslandDimension() == 0
            && this.isRunning()
            && !client.isDead
         ? TravelContainer.getTravelDir(player) != null
         : false;
   }

   public MainGameCamera getCamera() {
      return this.camera;
   }

   public void setCamera(MainGameCamera camera) {
      this.camera = camera;
   }

   public void resetCamera() {
      this.camera = this.defaultCamera;
   }

   public Client getClient() {
      return this.client;
   }

   public boolean showMap() {
      return this.showMap;
   }

   public void setShowMap(boolean showMap) {
      this.showMap = showMap;
      this.formManager.updateActive(false);
   }

   public boolean showScoreboard() {
      return this.showScoreboard;
   }

   public void setShowScoreboard(boolean showScoreboard) {
      this.showScoreboard = showScoreboard;
      this.formManager.scoreboard.setHidden(!showScoreboard);
   }

   public void checkDebugHotkey(GameWindow window, int key, Runnable runnable) {
      InputEvent event = window.getInput().getEvent(key);
      if (event != null && !event.isUsed() && event.state) {
         runnable.run();
      }
   }

   public void checkCameraPanHotkey(GameWindow window, int key, float xDir, float yDir, float speed) {
      this.checkDebugHotkey(window, key, () -> {
         MainGameCamera oldCamera = this.getCamera();
         MainGamePanningCamera newCamera = new MainGamePanningCamera(oldCamera.getX(), oldCamera.getY());
         newCamera.setDirection(xDir, yDir);
         newCamera.setSpeed(speed);
         this.setCamera(newCamera);
      });
   }

   public void disconnect(String message) {
      if (this.client.getLocalServer() != null) {
         this.client.getLocalServer().stop();
      }

      this.client.disconnect(message);
      GlobalData.setCurrentState(new MainMenu((String)null, this.client));
   }

   @Override
   public void dispose() {
      super.dispose();
      this.formManager.dispose();
      if (this.hudDraw != null) {
         this.hudDraw.remove();
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
      GameCrashLog.printCrashLog(errors, this.client, this.client == null ? null : this.client.getLocalServer(), "MainGame", this.client == null);
      if (this.client != null) {
         this.client.error(Localization.translate("disconnect", "clienterror"), true);
      } else {
         GlobalData.getCurrentGameLoop().stopMainGameLoop();
      }
   }

   @Override
   public SoundEmitter getALListener() {
      return this.camera != this.defaultCamera ? this.camera : this.client.getPlayer();
   }
}
