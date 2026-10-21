package necesse.engine.network.client;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import necesse.engine.CameraShake;
import necesse.engine.GameCrashLog;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.CommandsManager;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.dlc.DLCProvider;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.PacketManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.client.loading.ClientLoading;
import necesse.engine.network.client.network.ClientAddressNetwork;
import necesse.engine.network.client.network.ClientNetwork;
import necesse.engine.network.client.network.ClientSingleplayerNetwork;
import necesse.engine.network.packet.PacketClientInstalledDLC;
import necesse.engine.network.packet.PacketCloseContainer;
import necesse.engine.network.packet.PacketConnectApproved;
import necesse.engine.network.packet.PacketCraftUseNearbyInventories;
import necesse.engine.network.packet.PacketDisconnect;
import necesse.engine.network.packet.PacketPermissionUpdate;
import necesse.engine.network.packet.PacketPing;
import necesse.engine.network.packet.PacketPlayerGeneral;
import necesse.engine.network.packet.PacketPlayerInventoryAction;
import necesse.engine.network.packet.PacketPlayerMovement;
import necesse.engine.network.packet.PacketPlayerPvP;
import necesse.engine.network.packet.PacketPlayerRespawn;
import necesse.engine.network.packet.PacketSpawnPlayer;
import necesse.engine.network.packet.PacketTrackNewQuests;
import necesse.engine.network.server.AdventureParty;
import necesse.engine.network.server.Server;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.postProcessing.effects.AbstractHeatDistortionEffect;
import necesse.engine.postProcessing.effects.HeatDistortionEffect;
import necesse.engine.quest.QuestManager;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.save.CharacterSave;
import necesse.engine.seasons.GameSeasons;
import necesse.engine.sound.PrimitiveSoundEmitter;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.state.MainGame;
import necesse.engine.state.MainMenu;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldSettings;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.credits.FinalGamesCredits;
import necesse.gfx.credits.GameCreditsDrawManager;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.MainMenuFormManager;
import necesse.gfx.forms.components.ContinueComponent;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.chat.ChatMessageList;
import necesse.gfx.forms.presets.ConfirmationContinueForm;
import necesse.gfx.forms.presets.CrashDetailsContinueForm;
import necesse.gfx.forms.presets.CrashReportContinueForm;
import necesse.gfx.forms.presets.ModCrashReportContinueForm;
import necesse.gfx.forms.presets.NoticeForm;
import necesse.gfx.forms.presets.sidebar.TrackedSidebarForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.Container;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.UniqueFloatText;
import necesse.level.maps.presets.PresetUndoData;
import necesse.reports.CrashReportData;
import necesse.reports.ReportUtils;

public class Client {
   private static final int characterAutoSaveIntervalInSec = 60;
   private static final int backupEachCharacterAutoSave = 15;
   public boolean recordLoadingPerformance = false;
   private boolean isPaused;
   public boolean isDead;
   public long respawnTime;
   public ClientNetwork network;
   public GameMessage playingOnDisplayName;
   private long worldUniqueID;
   private int characterUniqueID;
   public File characterFilePath;
   private long characterSaveTime;
   private int characterSavesCounter;
   public long sessionID;
   int slot;
   int slots;
   PermissionLevel permissionLevel;
   public boolean hasNewJournalEntry;
   private long waitTimer;
   private int waitAttempts;
   public GameMessage serverRespondError;
   private long movementTimer;
   public WorldEntity worldEntity;
   public final QuestManager quests = new QuestManager(this);
   public final HashSet<Integer> trackedQuests = new HashSet<>();
   protected ClientClient[] players;
   protected int totalPlayersConnected = -1;
   private final HashMap<Long, ClientClient> authPlayerMap = new HashMap<>();
   public final ClientLevelManager levelManager = new ClientLevelManager(this);
   private Point positionUpdatePoint;
   public WorldSettings worldSettings;
   public long pvpChangeTime;
   private boolean strictServerAuthority;
   private long messageTime;
   private Color messageColor;
   private GameMessage message;
   protected GameCreditsDrawManager creditsDrawManager;
   public final ClientLoading loading;
   public ClientIslandNotes islandNotes;
   public final ChatMessageList chat;
   public ArrayList<String> chatSubmits = new ArrayList<>();
   private final LinkedList<CameraShake> cameraShakes = new LinkedList<>();
   public PlayerStats characterStats;
   public StoryObjectiveManager storyManager;
   public final AdventureParty adventureParty = new AdventureParty(this);
   private final TickManager tickManager;
   public final PacketManager packetManager;
   public final CommandsManager commandsManager;
   public long spawnPacketSentTime;
   protected boolean hasDisconnected;
   private String disconnectMessage;
   protected boolean disconnectCalled;
   private Container inventoryContainer;
   private Container openContainer;
   private boolean shouldCloseInventoryOnContainerClose;
   private long lastHungryThought;
   private long lastStarvingThought;
   private boolean isSingleplayer;
   private Server server;
   private final ExecutorService singlePlayerSocket;
   public CrashReportData serverCrashReport;
   public String overrideDisconnectMessage;
   public LinkedList<ContinueComponent> additionalDisconnectContinueForms = new LinkedList<>();
   public ClientPerformanceDumpCache performanceDumpCache = new ClientPerformanceDumpCache(this);
   private int gcTickCounter;
   public ArrayList<PresetUndoData> presetUndoData = new ArrayList<>();
   public ArrayList<Integer> presetRedoUniqueIDs = new ArrayList<>();

   protected Client(TickManager tickManager) {
      this.tickManager = tickManager;
      this.singlePlayerSocket = Executors.newSingleThreadExecutor(r -> new Thread(null, r, "Singleplayer socket"));
      this.packetManager = this.newPacketManager();
      this.commandsManager = new CommandsManager(this);
      this.loading = new ClientLoading(this);
      this.chat = new ChatMessageList();
      Settings.craftingUseNearby.addChangeListener(b -> this.network.sendPacket(new PacketCraftUseNearbyInventories()), this::isDisconnected);
      Settings.trackNewQuests.addChangeListener(b -> this.network.sendPacket(new PacketTrackNewQuests()), this::isDisconnected);
      DLCProvider.onDLCInstalled
         .addListener(dlc -> this.network.sendPacket(new PacketClientInstalledDLC(this.getSlot(), DLCProvider.getInstalledDLCs())), this::isDisconnected);
   }

   public Client(TickManager tickManager, String address, int port, GameMessage playingOnDisplayName) {
      this(tickManager);
      this.network = new ClientAddressNetwork(this, address, port);
      this.playingOnDisplayName = playingOnDisplayName;
   }

   public Client(TickManager tickManager, Server server, boolean isSingleplayer) {
      this(tickManager);
      this.network = new ClientSingleplayerNetwork(this);
      this.server = server;
      this.isSingleplayer = isSingleplayer;
      this.playingOnDisplayName = new StaticMessage(server.world.displayName);
   }

   public void start() {
      if (this.network.openConnection()) {
         GameLog.debug.println("Opened connection to " + this.network.getDebugString());
         this.loading.init();
         this.slot = -1;
         this.worldUniqueID = -1L;
         this.positionUpdatePoint = new Point(0, 0);
         this.worldEntity = null;
         this.levelManager.setLevel(null);
         this.waitTimer = 0L;
         this.waitAttempts = 0;
      } else {
         System.err.println("Failed to open connection to " + this.network.getDebugString());
         this.error(Localization.translate("disconnect", "networkerror", "msg", this.network.getOpenError()), false);
      }
   }

   public void reset() {
      this.positionUpdatePoint = new Point(0, 0);
      this.loading.reset();
      this.waitTimer = 0L;
      this.waitAttempts = 0;
      this.spawnPacketSentTime = -2147483648L;
      this.closeContainer(true);
   }

   public void submitConnectionPacket(PacketConnectApproved p) {
      this.sessionID = p.sessionID;
      this.players = new ClientClient[p.slots];
      this.slots = p.slots;
      this.slot = p.slot;
      this.worldUniqueID = p.uniqueID;
      this.worldSettings = p.getWorldSettings(this);
      this.strictServerAuthority = p.strictServerAuthority;
      this.permissionLevel = p.permissionLevel;
      this.hasNewJournalEntry = p.hasNewJournalEntry;
      this.loading.submitApprovedPacket(p);
      this.islandNotes = new ClientIslandNotes(this.worldUniqueID);
      TrackedSidebarForm.loadTrackedQuests(this);
      GameSeasons.readSeasons(new PacketReader(p.activeSeasonsContent));
      this.levelManager.loadMapIcons();
   }

   public void startedHosting(Server server) {
      this.server = server;
      this.players = Arrays.copyOf(this.players, server.getSlots());
      this.slots = server.getSlots();
      this.isSingleplayer = false;
      this.resume();
   }

   public void frameTick(TickManager tickManager) {
      if (!this.hasDisconnected && !this.disconnectCalled) {
         Performance.record(tickManager, "packets", () -> {
            this.packetManager.tickNetworkManager();

            for (NetworkPacket p = this.packetManager.nextPacket(); p != null; p = this.packetManager.nextPacket()) {
               this.waitAttempts = 0;
               if (!GameRandom.globalRandom.getChance(this.packetManager.dropPacketChance)) {
                  NetworkPacket finalP = p;
                  Performance.record(tickManager, p.getTypePacket().getClass().getSimpleName(), () -> finalP.processClient(this));
               }

               if (this.isDisconnected()) {
                  return;
               }
            }
         });
         if (!this.isPaused() && this.worldEntity != null) {
            this.worldEntity.clientFrameTick(tickManager);
         }

         if (this.loading.isDone()) {
            if (!this.isPaused()) {
               this.getLevel().frameTick(tickManager);

               for (int i = 0; i < this.getSlots(); i++) {
                  ClientClient client = this.getClient(i);
                  if (client != null) {
                     client.tickMovement(this, tickManager.getDelta());
                  }
               }
            }
         }
      }
   }

   public void tick() {
      if (!this.hasDisconnected) {
         if (this.disconnectCalled) {
            this.privateDisconnect(true);
         } else {
            this.performanceDumpCache.tickTimeouts();
            this.loading.tick();
            this.levelManager.tick();
            if (this.server != null) {
               Exception startupException = this.server.getStartupException();
               if (startupException != null) {
                  String message = startupException.getMessage();
                  if (message == null || message.isEmpty()) {
                     message = startupException.getClass().getSimpleName();
                  }

                  this.error(
                     Localization.translate("misc", "loadworldfailed") + "\n\n\"" + message + "\"\n\n" + Localization.translate("misc", "restorebackup"), false
                  );
                  startupException.printStackTrace();
                  return;
               }

               if (!this.server.hasStarted()) {
                  return;
               }
            }

            this.waitTimer += 50L;
            if (this.waitTimer > 1000L) {
               this.waitTimer = 0L;
               this.waitAttempts++;
               if (this.getSlot() != -1 && this.waitAttempts >= 5) {
                  this.network.sendPacket(new PacketPing(-1));
               }

               if (this.waitAttempts >= 10) {
                  this.setMessage(new LocalMessage("disconnect", "notresponding", "seconds", this.waitAttempts), Color.RED, 1.0F);
                  this.serverRespondError = this.getMessage();
                  System.err.println(this.serverRespondError.translate());
               } else {
                  this.serverRespondError = null;
               }

               if (this.waitAttempts >= 60) {
                  this.error(Localization.translate("disconnect", "respondingdc"), true);
               }
            }

            if (this.loading.isDone()) {
               if (GameLaunch.runPeriodicGCRounds && this.tickManager.isGameTick() && this.server == null) {
                  this.gcTickCounter++;
                  if (this.gcTickCounter >= 1200) {
                     this.gcTickCounter = 0;
                     this.worldEntity.executor().submit(System::gc);
                  }
               }

               this.tickAutoSave();
               final PlayerMob me = this.getPlayer();
               if (me != null) {
                  me.setLevel(this.getLevel());
               }

               if (!this.isPaused()) {
                  this.cameraShakes.removeIf(c -> c.isOver(this.worldEntity.getTime()));
                  this.worldEntity.clientTick();
                  this.levelManager.clientTick(me);
                  this.totalPlayersConnected = 0;

                  for (int i = 0; i < this.getSlots(); i++) {
                     ClientClient client = this.getClient(i);
                     if (client != null) {
                        this.totalPlayersConnected++;
                        client.tick(this);
                     }
                  }

                  this.adventureParty.clientTick();
                  this.storyManager.clientTick();
                  if (me != null && !this.isDead) {
                     me.refreshClientUpdateTime();
                     if (me.removed()) {
                        me.restore();
                     }

                     me.tickOpenedDoors();
                     this.getContainer().tick();
                     if (me.buffManager.hasBuff(BuffRegistry.STARVING_BUFF)) {
                        long timeSinceLastThought = this.worldEntity.getLocalTime() - this.lastStarvingThought;
                        if (this.lastStarvingThought == 0L || timeSinceLastThought >= 60000L) {
                           UniqueFloatText text = new UniqueFloatText(
                              me.getX(), me.getY(), Localization.translate("misc", "starvingthought"), new FontOptions(16).outline(), "thought"
                           ) {
                              @Override
                              public int getAnchorX() {
                                 return me.getX();
                              }

                              @Override
                              public int getAnchorY() {
                                 return me.getY();
                              }
                           };
                           text.hoverTime = 6000;
                           this.getLevel().hudManager.addElement(text);
                           this.lastStarvingThought = this.worldEntity.getLocalTime();
                           this.lastHungryThought = this.worldEntity.getLocalTime();
                        }
                     } else if (me.buffManager.hasBuff(BuffRegistry.HUNGRY_BUFF)) {
                        long timeSinceLastThought = this.worldEntity.getLocalTime() - this.lastHungryThought;
                        if (this.lastHungryThought == 0L || timeSinceLastThought >= 60000L) {
                           UniqueFloatText text = new UniqueFloatText(
                              me.getX(), me.getY(), Localization.translate("misc", "hungrythought"), new FontOptions(16).outline(), "thought"
                           ) {
                              @Override
                              public int getAnchorX() {
                                 return me.getX();
                              }

                              @Override
                              public int getAnchorY() {
                                 return me.getY();
                              }
                           };
                           text.hoverTime = 6000;
                           this.getLevel().hudManager.addElement(text);
                           this.lastHungryThought = this.worldEntity.getLocalTime();
                        }
                     } else {
                        this.lastStarvingThought = 0L;
                        this.lastHungryThought = 0L;
                     }

                     this.movementTimer += 50L;
                     if (this.movementTimer > 2000L) {
                        this.movementTimer = 0L;
                        this.sendMovementPacket(false);
                     }

                     if (this.positionUpdatePoint.distance(me.getX(), me.getY()) > 50.0) {
                        this.sendMovementPacket(false);
                     }

                     if (me.shouldUpdateInventoryAction()) {
                        this.network.sendPacket(new PacketPlayerInventoryAction(this.getSlot(), me));
                     }
                  }
               }
            }
         }
      }
   }

   public void respawn(PacketPlayerRespawn packet) {
      ClientClient client = this.getClient();
      client.respawn(packet);
      this.isDead = false;
      Level level = this.getLevel();
      if (level != null && level.getIdentifier().equals(packet.levelIdentifier)) {
         this.network.sendPacket(new PacketSpawnPlayer(this));
      } else {
         GlobalData.setCurrentState(new MainMenu(this));
      }
   }

   public long getRespawnTimeLeft() {
      return this.respawnTime - this.worldEntity.getTime();
   }

   public boolean canRespawn() {
      return this.respawnTime <= this.worldEntity.getTime();
   }

   public Level getLevel() {
      return this.levelManager.getLevel();
   }

   public boolean isSingleplayer() {
      return this.isSingleplayer;
   }

   public int getTotalPlayersConnected() {
      return this.totalPlayersConnected;
   }

   public void submitSinglePlayerPacket(PacketManager manager, NetworkPacket packet) {
      if (!this.singlePlayerSocket.isShutdown()) {
         this.singlePlayerSocket.submit(() -> manager.submitInPacket(new NetworkPacket(packet)));
      }
   }

   public Server getLocalServer() {
      return this.server;
   }

   public void pause() {
      this.isPaused = true;
      if (this.server != null) {
         this.server.pause();
      }
   }

   public void resume() {
      this.isPaused = false;
      if (this.server != null) {
         this.server.resume();
      }
   }

   public boolean isPaused() {
      return this.isPaused;
   }

   public boolean hasStrictServerAuthority() {
      return this.strictServerAuthority;
   }

   public void combineToAllStats() {
      if (this.characterStats != null && this.worldSettings != null && this.worldSettings.achievementsEnabled()) {
         GlobalData.stats().combineIdempotent(this.characterStats);
      }
   }

   protected void privateDisconnect(boolean sendPacket) {
      if (this.worldSettings != null && this.worldSettings.allowOutsideCharacters && this.loading.isDone()) {
         CharacterSave.saveCharacter(this, false);
      }

      this.combineToAllStats();
      if (this.getSlot() != -1 && sendPacket) {
         this.network.sendPacket(PacketDisconnect.clientDisconnect(this.getSlot(), this.disconnectMessage));
      }

      this.network.close();
      this.singlePlayerSocket.shutdown();
      this.levelManager.dispose();
      if (this.worldEntity != null) {
         this.worldEntity.dispose();
      }

      if (this.creditsDrawManager != null) {
         this.creditsDrawManager.dispose();
      }

      this.hasDisconnected = true;
   }

   public void instantDisconnect(String message) {
      this.disconnect(message);
      this.privateDisconnect(true);
   }

   public void disconnect(String message) {
      this.disconnectCalled = true;
      this.disconnectMessage = message;
   }

   public boolean hasDisconnected() {
      return this.hasDisconnected;
   }

   public boolean isDisconnected() {
      return this.disconnectCalled || this.hasDisconnected;
   }

   public void error(String message, boolean sendDisconnectPacket) {
      this.error(message, sendDisconnectPacket, null);
   }

   public void error(String message, boolean sendDisconnectPacket, ContinueComponent continueForm) {
      System.err.println(message);
      this.disconnect(message);
      this.privateDisconnect(sendDisconnectPacket);
      if (this.getLocalServer() != null) {
         this.getLocalServer().stop(PacketDisconnect.Code.SERVER_ERROR);
      }

      try {
         if (this.overrideDisconnectMessage != null) {
            message = this.overrideDisconnectMessage;
         }

         if (this.serverCrashReport != null) {
            ArrayList<LoadedMod> responsibleMods = ModLoader.getResponsibleMods(this.serverCrashReport.errors, true);
            if (!responsibleMods.isEmpty()) {
               continueForm = new ModCrashReportContinueForm(this.serverCrashReport, responsibleMods, 600, 300, reportForm -> {
                  MainMenu mainMenu = (MainMenu)GlobalData.getCurrentState();
                  FormManager formManager = mainMenu.getFormManager();
                  if (formManager instanceof MainMenuFormManager) {
                     MainMenuFormManager mainMenuFormManager = (MainMenuFormManager)formManager;
                     mainMenuFormManager.mainForm.settings.makeModsCurrent();
                  }

                  reportForm.applyContinue();
               }, reportForm -> {});
            } else if (GameCrashLog.findAnyCause(this.serverCrashReport.errors, OutOfMemoryError.class, null) != null) {
               NoticeForm noticeForm = new NoticeForm("outofmemory");
               noticeForm.setupNotice(new LocalMessage("misc", "outofmemory"));
               continueForm = noticeForm;
            } else {
               continueForm = new CrashReportContinueForm(
                  this.serverCrashReport,
                  "crashsorry",
                  600,
                  300,
                  reportForm -> {
                     MainMenu mainMenu = (MainMenu)GlobalData.getCurrentState();
                     mainMenu.addContinueForm(
                        "crashdetails",
                        new CrashDetailsContinueForm(
                           Localization.translate("ui", "crashdetailshere"), "crashgivedetails", 500, 400, 150, (detailsForm, details) -> {
                              this.sendReport(mainMenu, this.serverCrashReport, details);
                              detailsForm.applyContinue();
                           }, detailsForm -> {}
                        )
                     );
                     reportForm.applyContinue();
                  },
                  reportForm -> {}
               );
            }
         }

         MainMenu mainMenu;
         if (GlobalData.getCurrentState() instanceof MainMenu) {
            mainMenu = (MainMenu)GlobalData.getCurrentState();
            if (continueForm == null) {
               mainMenu.addNotice(message);
            } else {
               mainMenu.addContinueForm(continueForm);
            }
         } else if (continueForm == null) {
            GlobalData.setCurrentState(mainMenu = new MainMenu(message, this));
         } else {
            GlobalData.setCurrentState(mainMenu = new MainMenu(continueForm, this));
         }

         for (ContinueComponent cf : this.additionalDisconnectContinueForms) {
            mainMenu.addContinueForm(cf);
         }
      } catch (Exception e) {
         if (this.serverCrashReport == null) {
            GlobalData.getCurrentGameLoop().stopMainGameLoop();
            throw e;
         }

         GameCrashLog.openCrashFrame(this.serverCrashReport);
      }
   }

   private void sendReport(MainMenu mainMenu, CrashReportData data, String userDetails) {
      AtomicBoolean interrupted = new AtomicBoolean(false);
      NoticeForm sendingReport = new NoticeForm("sendingreport");
      sendingReport.setupNotice(new LocalMessage("ui", "sendingreport"), new LocalMessage("ui", "cancelbutton"));
      sendingReport.onContinue(() -> interrupted.set(true));
      mainMenu.addContinueForm("sendingreport", sendingReport);
      new Thread(
            () -> {
               String error = ReportUtils.sendCrashReport(data, userDetails);
               if (!interrupted.get()) {
                  if (error != null) {
                     ConfirmationContinueForm sendReportRetry = new ConfirmationContinueForm("sendreportretry", 300, 1000);
                     sendReportRetry.setupConfirmation(
                        new StaticMessage(error), new LocalMessage("ui", "sendreportretry"), new LocalMessage("ui", "continuebutton"), () -> {
                           this.sendReport(mainMenu, data, userDetails);
                           sendReportRetry.applyContinue();
                        }, sendReportRetry::applyContinue
                     );
                     mainMenu.addContinueForm("sendreportretry", sendReportRetry);
                  } else {
                     NoticeForm thankYouReport = new NoticeForm("thankyoureport");
                     thankYouReport.setupNotice(new LocalMessage("ui", "sendreportthanks"));
                     mainMenu.addContinueForm("thankyoureport", thankYouReport);
                  }
               }

               sendingReport.applyContinue();
            }
         )
         .start();
   }

   public int getSlot() {
      return this.slot;
   }

   public int getSlots() {
      return this.slots;
   }

   public long getWorldUniqueID() {
      return this.worldUniqueID;
   }

   public int getCharacterUniqueID() {
      return this.characterUniqueID;
   }

   public void sendMovementPacket(boolean isDirect) {
      ClientClient client = this.getClient();
      if (client != null && client.playerMob != null) {
         this.network.sendPacket(new PacketPlayerMovement(this, client, isDirect));
         this.positionUpdatePoint = new Point(client.playerMob.getX(), client.playerMob.getY());
      }
   }

   public void resetPositionPointUpdate() {
      PlayerMob me = this.getPlayer();
      if (me != null) {
         this.positionUpdatePoint = me.getPositionPoint();
      }
   }

   public Stream<ClientClient> streamClients() {
      return this.players == null ? Stream.empty() : Arrays.stream(this.players).filter(Objects::nonNull);
   }

   public ClientClient getClient(int slot) {
      return this.players != null && slot >= 0 && slot < this.players.length ? this.players[slot] : null;
   }

   public PlayerMob getPlayer(int slot) {
      ClientClient client = this.getClient(slot);
      return client != null ? client.playerMob : null;
   }

   public ClientClient getClientByAuth(long auth) {
      return this.authPlayerMap.get(auth);
   }

   public PlayerMob getPlayerByAuth(long auth) {
      ClientClient client = this.getClientByAuth(auth);
      return client != null ? client.playerMob : null;
   }

   private void setClient(int slot, ClientClient client) {
      if (this.players != null) {
         ClientClient prevClient = this.getClient(slot);
         if (prevClient != null) {
            this.authPlayerMap.remove(prevClient.authentication);
            prevClient.dispose();
         }

         this.players[slot] = client;
         if (client != null) {
            this.authPlayerMap.put(client.authentication, client);
         }

         MainGameFormManager formManager = this.getMainFormManager();
         if (formManager != null) {
            formManager.scoreboard.slotChanged(slot, client);
         }
      }
   }

   public void applyPlayerGeneralPacket(PacketPlayerGeneral packet) {
      if (!this.hasDisconnected()) {
         if (this.slot == packet.slot) {
            this.characterUniqueID = packet.characterUniqueID;
         }

         ClientClient target = this.getClient(packet.slot);
         if (target == null) {
            this.setClient(packet.slot, new ClientClient(this, packet.slot, packet));
         } else {
            target.applyGeneralPacket(packet);
         }

         this.loading.playersPhase.submitLoadedPlayer(this.slot);
      }
   }

   public void applyClientInstalledDLCPacket(PacketClientInstalledDLC packet) {
      if (!this.hasDisconnected()) {
         ClientClient target = this.getClient(packet.slot);
         if (target != null) {
            target.applyClientInstalledDLCPacket(packet);
         }
      }
   }

   public void clearClient(int slot) {
      this.setClient(slot, null);
   }

   public PlayerMob getPlayer() {
      return this.getSlot() == -1 ? null : this.getPlayer(this.getSlot());
   }

   public ClientClient getClient() {
      return this.getSlot() == -1 ? null : this.getClient(this.getSlot());
   }

   private MainGameFormManager getMainFormManager() {
      return GlobalData.getCurrentState() instanceof MainGame ? ((MainGame)GlobalData.getCurrentState()).formManager : null;
   }

   public boolean hasFocusForm() {
      MainGameFormManager formManager = this.getMainFormManager();
      return formManager != null && formManager.hasFocusForm();
   }

   public FormComponent getFocusForm() {
      MainGameFormManager formManager = this.getMainFormManager();
      return formManager != null ? formManager.getFocusForm() : null;
   }

   public void setFocusForm(ContainerComponent<?> containerComponent) {
      MainGameFormManager formManager = this.getMainFormManager();
      if (formManager != null) {
         formManager.setFocusForm(containerComponent);
      }
   }

   public void resetFocusForm() {
      MainGameFormManager formManager = this.getMainFormManager();
      if (formManager != null) {
         formManager.removeFocusForm();
      }
   }

   public void setPvP(boolean value) {
      if (!this.worldSettings.forcedPvP && this.getClient().pvpEnabled != value) {
         this.pvpChangeTime = System.currentTimeMillis();
         this.network.sendPacket(new PacketPlayerPvP(this.getSlot(), value));
      }
   }

   public boolean pvpEnabled() {
      return this.worldSettings.forcedPvP || this.getClient().pvpEnabled;
   }

   public int getTeam() {
      ClientClient client = this.getClient();
      return client != null && client.playerMob != null ? client.playerMob.getTeam() : -1;
   }

   public void setMessage(GameMessage message, Color col) {
      this.setMessage(message, col, 5.0F);
   }

   public void setMessage(String message, Color col) {
      this.setMessage(message, col, 5.0F);
   }

   public void setMessage(GameMessage message, Color col, float seconds) {
      this.messageTime = System.currentTimeMillis() + (long)(seconds * 1000.0F);
      this.message = message;
      this.messageColor = col;
   }

   public void setMessage(String message, Color col, float seconds) {
      this.setMessage(new StaticMessage(message), col, seconds);
   }

   public GameMessage getMessage() {
      return this.message;
   }

   public float getMessageAlpha() {
      float alpha = 1.0F;
      if (this.messageTime < System.currentTimeMillis()) {
         alpha = Math.abs((float)(System.currentTimeMillis() - this.messageTime) / 2000.0F - 1.0F);
      }

      return alpha;
   }

   public Color getMessageColor() {
      return new Color(this.messageColor.getRed() / 255.0F, this.messageColor.getGreen() / 255.0F, this.messageColor.getBlue() / 255.0F, this.getMessageAlpha());
   }

   public boolean messageShown() {
      return this.messageTime + 2000L > System.currentTimeMillis();
   }

   public CameraShake startCameraShake(
      PrimitiveSoundEmitter emitter, long startTime, int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff
   ) {
      CameraShake cameraShake = new CameraShake(startTime, duration, frequency, xIntensity, yIntensity, addFallOff, this.getCurrentCameraShake());
      if (Settings.cameraShake) {
         this.cameraShakes.add(cameraShake);
      }

      if (emitter != null) {
         cameraShake.from(emitter);
      }

      return cameraShake;
   }

   public CameraShake startCameraShake(PrimitiveSoundEmitter emitter, int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff) {
      return this.startCameraShake(emitter, this.worldEntity.getTime(), duration, frequency, xIntensity, yIntensity, addFallOff);
   }

   public CameraShake startCameraShake(float x, float y, int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff) {
      return this.startCameraShake(SoundPlayer.SimpleEmitter(x, y), duration, frequency, xIntensity, yIntensity, addFallOff);
   }

   public Float getCurrentCameraShake() {
      Float shake = new Float();

      for (CameraShake cameraShake : this.cameraShakes) {
         Float currentShake = cameraShake.getCurrentShake(this.worldEntity.getTime(), this.getPlayer());
         shake = new Float(shake.x + currentShake.x, shake.y + currentShake.y);
      }

      return shake;
   }

   public void addShockwaveEffect(int centerX, int centerY, float maxDistance, float fadeInDistance, float fadeOutDistance, float size, int speed) {
      PostProcessingEffects.addShockwaveEffect(this, centerX, centerY, maxDistance, fadeInDistance, fadeOutDistance, size, speed);
   }

   public <T extends AbstractHeatDistortionEffect> T addHeatDistortEffect(T heatDistortionEffect) {
      return PostProcessingEffects.addHeatDistortEffect(heatDistortionEffect);
   }

   public HeatDistortionEffect addHeatDistortEffect(int centerX, int centerY, float radiusX, float radiusY, float speed, int totalTime) {
      return PostProcessingEffects.addHeatDistortEffect(this, centerX, centerY, radiusX, radiusY, speed, totalTime);
   }

   public void startCharacterSaveTimer() {
      if (this.worldEntity != null) {
         this.characterSaveTime = this.worldEntity.getTime() + Math.max(1, 60) * 1000L;
      }
   }

   public void tickAutoSave() {
      if (this.worldSettings == null || this.worldSettings.allowOutsideCharacters || !this.loading.isDone()) {
         if (this.characterSaveTime == 0L) {
            this.startCharacterSaveTimer();
         }

         if (this.characterSaveTime > 0L && this.characterSaveTime <= this.worldEntity.getTime()) {
            CharacterSave.saveCharacter(this, this.characterSavesCounter % 15 == 0);
            this.characterSavesCounter++;
            this.startCharacterSaveTimer();
            if (this.server == null) {
               this.worldEntity.executor().submit(System::gc);
            }
         }
      }
   }

   public PermissionLevel getPermissionLevel() {
      return this.permissionLevel;
   }

   public void permissionUpdate(PacketPermissionUpdate permissionUpdate) {
      this.permissionLevel = permissionUpdate.permissionLevel;
   }

   public TickManager tickManager() {
      return this.tickManager;
   }

   public void submitPingPacket(PacketPing packet) {
      if (packet.responseKey != -1) {
         this.network.sendPacket(new PacketPing(packet.responseKey));
      }

      this.waitAttempts = 0;
   }

   private PacketManager newPacketManager() {
      return new PacketManager() {
         @Override
         public void processInstantly(NetworkPacket packet) {
            packet.getTypePacket().processClient(packet, Client.this);
         }
      };
   }

   public void reloadMap() {
      GlobalData.setCurrentState(new MainMenu(this));
   }

   public void startCreditsDraw() {
      if (this.creditsDrawManager != null) {
         this.creditsDrawManager.dispose();
      }

      this.creditsDrawManager = new GameCreditsDrawManager(WindowManager.getWindow(), new FinalGamesCredits(false));
   }

   public boolean isDrawingCredits() {
      return this.creditsDrawManager != null && !this.creditsDrawManager.isDone();
   }

   public boolean stopCreditsDraw() {
      if (this.creditsDrawManager != null) {
         this.creditsDrawManager.dispose();
         this.creditsDrawManager = null;
         return true;
      } else {
         return false;
      }
   }

   public void drawCreditsHudIfShould() {
      if (this.creditsDrawManager != null) {
         if (this.creditsDrawManager.isDone()) {
            this.creditsDrawManager.dispose();
            this.creditsDrawManager = null;
         } else {
            this.creditsDrawManager.draw(100, 100);
         }
      }
   }

   public void onWindowResized(GameWindow window) {
      if (this.creditsDrawManager != null) {
         this.creditsDrawManager.onWindowResized(window);
      }
   }

   public Container getContainer() {
      return this.openContainer != null ? this.openContainer : this.inventoryContainer;
   }

   private void openContainer(Container container, boolean openInventory, boolean closeInventory) {
      if (this.openContainer != null) {
         this.openContainer.onClose();
      }

      this.openContainer = container;
      PlayerMob player = this.getPlayer();
      this.shouldCloseInventoryOnContainerClose = false;
      if (openInventory) {
         this.shouldCloseInventoryOnContainerClose = !player.isInventoryExtended();
         player.setInventoryExtended(true);
      } else if (closeInventory) {
         player.setInventoryExtended(false);
      }
   }

   public void openContainerForm(ContainerComponent<?> containerComponent) {
      this.openContainer(containerComponent.getContainer(), containerComponent.shouldOpenInventory(), containerComponent.shouldCloseInventory());
      this.setFocusForm(containerComponent);
      this.openContainer.init();
      GlobalData.updateCraftable();
   }

   public void closeContainer(boolean sendPacket) {
      if (this.openContainer != null) {
         if (sendPacket) {
            this.network.sendPacket(new PacketCloseContainer());
         }

         if (this.shouldCloseInventoryOnContainerClose && this.getPlayer().getDraggingItem() == null) {
            this.getPlayer().setInventoryExtended(false);
         }

         this.openContainer.onClose();
         this.openContainer = null;
         this.resetFocusForm();
      }
   }

   public boolean hasOpenContainer() {
      return this.openContainer != null;
   }

   public void syncOpenContainer(int uniqueSeed) {
      if (uniqueSeed == -1) {
         if (this.openContainer != null) {
            this.closeContainer(false);
         }
      } else if (this.openContainer == null) {
         this.network.sendPacket(new PacketCloseContainer());
      } else if (this.openContainer.uniqueSeed != uniqueSeed) {
         this.closeContainer(true);
      }
   }

   public Container getInventoryContainer() {
      return this.inventoryContainer;
   }

   public void initInventoryContainer() {
      this.inventoryContainer = new Container(this.getClient(), 0);
   }

   public void saveAndClose(String disconnectMessage, PacketDisconnect.Code serverCode) {
      this.instantDisconnect(disconnectMessage);
      if (this.getLocalServer() != null) {
         this.getLocalServer().stop(serverCode);
      }
   }
}
