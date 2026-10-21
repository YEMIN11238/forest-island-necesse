package necesse.engine.network.server;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import necesse.engine.GameAuth;
import necesse.engine.GameCache;
import necesse.engine.GameCrashLog;
import necesse.engine.GameEvents;
import necesse.engine.GameLaunch;
import necesse.engine.Settings;
import necesse.engine.commands.CommandsManager;
import necesse.engine.commands.ParsedCommand;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.events.ServerClientDisconnectEvent;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.events.ServerStopEvent;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.gameLoop.tickManager.PerformanceTimerUtils;
import necesse.engine.gameLoop.tickManager.PerformanceWrapper;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.PacketManager;
import necesse.engine.network.client.Client;
import necesse.engine.network.networkInfo.NetworkInfo;
import necesse.engine.network.packet.PacketClientInstalledDLC;
import necesse.engine.network.packet.PacketConnectApproved;
import necesse.engine.network.packet.PacketDisconnect;
import necesse.engine.network.server.network.ServerNetwork;
import necesse.engine.network.server.network.ServerSingleplayerNetwork;
import necesse.engine.platforms.Platform;
import necesse.engine.registries.PacketRegistry;
import necesse.engine.save.SaveData;
import necesse.engine.save.WorldSave;
import necesse.engine.seasons.GameSeasons;
import necesse.engine.server.ServerTickThread;
import necesse.engine.state.MainMenu;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapSet;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.FileSystemClosedException;
import necesse.engine.world.World;
import necesse.entity.manager.WorldSavedEntityComponent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.forms.components.ContinueComponent;
import necesse.gfx.forms.presets.ConfirmationContinueForm;
import necesse.gfx.forms.presets.NoticeForm;

public class Server {
   public static final int autoSaveIntervalInSec = 60;
   public static final int backupEachAutoSave = 15;
   public static int clientMoveTolerance = 30;
   public static final int migrationPrintCooldownMinutes = 1;
   public long lastMigrationTime;
   public HashMapSet<LevelIdentifier, Point> regionsMigrated = new HashMapSet<>();
   public HashSet<String> migratedNames = new HashSet<>();
   public boolean pauseForSpawnedPlayer;
   private boolean isPaused;
   private ServerClient[] clients;
   private final HashMap<Long, ServerClient> authClientMap = new HashMap<>();
   public ServerNetwork network;
   private ServerSettings settings;
   public World world;
   protected HashMap<Long, SavedServerClientData> savedClients = new HashMap<>();
   private boolean isStopped;
   private boolean stopCalled;
   private PacketDisconnect stopPacket;
   private final List<Consumer<Server>> stopEvents = new LinkedList<>();
   public List<ContinueComponent> stopErrors;
   int playersOnline;
   private int pauseCounter;
   private long saveTime;
   private int gcTickCounter;
   private TickManager tickManager;
   public final PacketManager packetManager;
   public final CommandsManager commandsManager;
   public ServerTickThread serverThread;
   private boolean hasServerStarted = false;
   protected GameMessage serverStartingMessage = null;
   protected boolean serverStartingAllowCancelling = true;
   protected Exception startupException = null;
   private Client client;
   private boolean isSinglePlayer;
   private boolean isHosted;
   private int autoSaves;
   private PerformanceTimerManager savePerformanceTimer;
   private ServerSaveHandler saveHandler;
   private long totalTimeSpentSaving;

   public void printMigrationIfShould() {
      long currentTime = System.currentTimeMillis();
      long timeSinceLastPrint = currentTime - this.lastMigrationTime;
      if (timeSinceLastPrint > 60000L) {
         this.lastMigrationTime = currentTime;
         if (!this.migratedNames.isEmpty()) {
            int totalRegions = this.regionsMigrated.entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
            String migratedNamesJoined = GameUtils.join(this.migratedNames.toArray(), ", ", " and ");
            System.out
               .println(
                  "Migrated "
                     + totalRegions
                     + " region"
                     + (totalRegions > 1 ? "s" : "")
                     + " on "
                     + this.regionsMigrated.getSize()
                     + " level"
                     + (this.regionsMigrated.getSize() > 1 ? "s" : "")
                     + " "
                     + migratedNamesJoined
                     + " within the last "
                     + 1
                     + " minutes"
               );
            this.regionsMigrated.clearAll();
            this.migratedNames.clear();
         }
      }
   }

   public Server(ServerSettings settings) throws IOException, FileSystemClosedException {
      this.settings = settings;
      GameSeasons.loadSeasons();
      this.serverThread = new ServerTickThread(this, "Server Thread", 60);
      this.packetManager = this.newPacketManager();
      this.commandsManager = new CommandsManager(this);
      if (settings.isSinglePlayer()) {
         this.network = new ServerSingleplayerNetwork(this);
      } else {
         this.network = Platform.getNetworkManager().createOpenServerNetwork(this, settings);
      }

      this.clients = new ServerClient[settings.slots];
      this.world = new World(this);
   }

   public void start(ServerHostSettings hostSettings, boolean concurrently) throws IOException {
      this.savedClients = this.world.getSavedClientData();
      if (concurrently) {
         this.serverThread.makeInitWorld(hostSettings);
      } else {
         try {
            this.world.init();
            this.markWorldInitialized(null, hostSettings);
         } catch (Exception e) {
            this.markWorldInitialized(e, hostSettings);
            return;
         }
      }

      this.serverThread.start();
   }

   public void markWorldInitialized(Exception exception, ServerHostSettings hostSettings) {
      try {
         if (exception != null) {
            throw exception;
         }

         if (hostSettings != null) {
            hostSettings.apply(this, false);
         }

         this.network.open();
         this.startSaveTimer();
      } catch (Exception e) {
         this.startupException = e;
         String message = e.getMessage();
         if (message == null || message.isEmpty()) {
            message = e.getClass().getSimpleName();
         }

         this.stop(PacketDisconnect.networkError(-1, message), null);
         return;
      }

      this.hasServerStarted = true;
      GameEvents.triggerEvent(new ServerStartEvent(this));
   }

   public void setStartingMessage(GameMessage message, boolean allowCancelling) {
      if (this.serverStartingMessage == null || !this.serverStartingMessage.isSame(message)) {
         System.out.println(message.translate());
      }

      this.serverStartingAllowCancelling = allowCancelling;
      this.serverStartingMessage = message;
   }

   public GameMessage getStartingMessage() {
      return this.serverStartingMessage;
   }

   public boolean startingAllowCancelling() {
      return this.serverStartingAllowCancelling;
   }

   public Exception getStartupException() {
      return this.startupException;
   }

   public boolean hasStarted() {
      return this.hasServerStarted;
   }

   public void startHostFromSingleplayer(ServerSettings serverSettings, ServerHostSettings hostSettings) {
      if (this.settings.isSinglePlayer() && this.client != null) {
         serverSettings.creationSettings = this.settings.creationSettings;
         this.settings = serverSettings;
         hostSettings.apply(this, true);
         if (!this.settings.isSinglePlayer()) {
            if (this.settings.slots < this.clients.length) {
               throw new IllegalArgumentException("Cannot decrease slots");
            }

            this.clients = Arrays.copyOf(this.clients, this.settings.slots);
            this.client.startedHosting(this);
            this.network = Platform.getNetworkManager().createOpenServerNetwork(this, this.settings);

            try {
               this.network.open();
            } catch (IOException e) {
               this.stop(PacketDisconnect.networkError(-1, e.getMessage()), null);
               e.printStackTrace();
            }

            this.resume();
            SaveData continueSave = MainMenu.getContinueCacheSaveBase(MainMenu.ContinueMode.HOST);
            continueSave.addSafeString("worldFilePath", this.settings.creationSettings.worldFilePath.getAbsolutePath());
            SaveData hostSave = new SaveData("host");
            this.settings.addSaveData(hostSave);
            continueSave.addSaveData(hostSave);
            GameCache.cacheSave(continueSave, "continueLast");
         } else {
            System.err.println("Start host did not work, because provided port is still singleplayer");
         }
      } else {
         System.err.println("Cannot start host, because world is already hosting");
      }
   }

   public void makeSingleplayer(Client client) {
      this.client = client;
      this.isSinglePlayer = true;
   }

   public void makeHosted(Client client) {
      this.client = client;
      this.isHosted = true;
   }

   public void setTickManager(TickManager tickManager) {
      this.tickManager = tickManager;
   }

   public void frameTick(TickManager tickManager) {
      if (!this.isStopped && !this.stopCalled) {
         this.packetManager.tickNetworkManager();

         for (NetworkPacket p = this.packetManager.nextPacket(); p != null; p = this.packetManager.nextPacket()) {
            if (!GameRandom.globalRandom.getChance(this.packetManager.dropPacketChance)) {
               this.processPacket(p);
            }
         }

         if (!this.isPaused()) {
            this.world.frameTick(tickManager);

            for (int i = 0; i < this.getSlots(); i++) {
               if (this.getClient(i) != null) {
                  this.getClient(i).tickMovement(tickManager.getDelta());
               }
            }
         }
      }
   }

   public void tick() {
      if (!this.isStopped) {
         this.tickSaveHandler(this.stopCalled);
         if (this.stopCalled) {
            if (this.saveHandler == null) {
               this.privateStop();
            }
         } else {
            if (this.isSingleplayer() && this.getLocalClient().hasDisconnected() && !this.isStopped()) {
               this.stop();
            }

            if (this.isHosted() && this.getLocalClient().hasDisconnected() && !this.isStopped()) {
               this.stop();
            }

            this.tickAutoSave();
            if ((this.isDedicated() || GameLaunch.runPeriodicGCRounds) && this.tickManager.isGameTick() && this.isPaused()) {
               this.gcTickCounter++;
               if (this.gcTickCounter >= 1200) {
                  this.gcTickCounter = 0;
                  this.world.worldEntity.executor().submit(System::gc);
               }
            }

            this.network.tickUnknownPacketTimeouts();
            int playersOnline = 0;

            for (int i = 0; i < this.getSlots(); i++) {
               ServerClient client = this.getClient(i);
               if (client != null) {
                  playersOnline++;
                  client.tickTimeConnected();
               }
            }

            this.playersOnline = playersOnline;
            if (playersOnline == 0) {
               if (Settings.pauseWhenEmpty && this.pauseCounter == 300) {
                  System.out.println("Suggesting garbage collection due to empty server...");
                  this.world.worldEntity.executor().submit(System::gc);
               }

               if (this.pauseCounter == Settings.unloadLevelsCooldown * 20 + 100) {
                  System.out.println("Suggesting garbage collection due to empty server and unloaded levels...");
                  this.world.worldEntity.executor().submit(System::gc);
               }

               this.pauseCounter++;
            } else {
               this.pauseCounter = 0;
            }

            if (!this.isPaused()) {
               this.world.serverTick();

               for (int i = 0; i < this.getSlots(); i++) {
                  ServerClient client = this.getClient(i);
                  if (client != null) {
                     try {
                        client.tick();
                     } catch (Exception e) {
                        System.err.println("Error ticking client \"" + client.getName() + "\" resulted in a kick.");
                        e.printStackTrace();
                        this.disconnectClient(client.slot, PacketDisconnect.Code.INTERNAL_ERROR);
                     }
                  }
               }

               this.world
                  .levelManager
                  .getLoadedLevels()
                  .stream()
                  .filter(l -> l.unloadLevelBuffer > 20 * Math.max(2, Settings.unloadLevelsCooldown))
                  .sorted(Comparator.comparingInt(l -> l.shouldSave() ? 1 : 0))
                  .sequential()
                  .forEach(l -> {
                     this.world.levelManager.unloadLevel(l);
                     this.world.saveLevel(l);
                     System.out.println("Unloaded level " + l.getIdentifier());
                  });
            }
         }
      }
   }

   public ServerClient getPacketClient(NetworkInfo networkInfo) {
      return networkInfo == null ? this.getLocalServerClient() : networkInfo.getClient(this.streamClients());
   }

   private void processPacket(NetworkPacket packet) {
      int type = packet.type;
      ServerClient client = this.getPacketClient(packet.networkInfo);
      if (client == null && PacketRegistry.onlyConnectedClients(type)) {
         this.network.submitUnknownPacket(packet);
      } else {
         try {
            if (client != null) {
               client.submitInPacket(packet);
            }

            packet.processServer(this, client);
         } catch (Exception e) {
            System.err
               .println(
                  "Error processing client \""
                     + (client == null ? "NULL" : client.getName())
                     + "\" packet ("
                     + PacketRegistry.getPacketSimpleName(packet.type)
                     + ") resulted in a kick."
               );
            e.printStackTrace();
            Client localClient = this.getLocalClient();
            if (localClient != null && client != null && localClient.getSlot() == client.slot) {
               GameCrashLog.printCrashLog(e, localClient, this, "Server packet crash", false);
               this.stop(PacketDisconnect.Code.SERVER_ERROR);
            } else if (client != null) {
               this.disconnectClient(client.slot, PacketDisconnect.Code.INTERNAL_ERROR);
            } else {
               this.stop(PacketDisconnect.Code.INTERNAL_ERROR);
            }
         }
      }
   }

   public void pause() {
      this.isPaused = true;
   }

   public void resume() {
      this.isPaused = false;
   }

   public boolean isPaused() {
      if (!this.pauseForSpawnedPlayer || !this.isSingleplayer() && !this.isHosted()) {
         return Settings.pauseWhenEmpty && this.pauseCounter >= 200 ? true : this.isPaused;
      } else {
         return true;
      }
   }

   public Stream<ServerClient> streamClients() {
      return Arrays.stream(this.clients).filter(Objects::nonNull);
   }

   public Iterable<ServerClient> getClients() {
      return () -> this.streamClients().iterator();
   }

   public ServerClient getLocalServerClient() {
      return this.streamClients().filter(c -> c.networkInfo == null).findFirst().orElse(null);
   }

   public ServerClient getClient(int slot) {
      return slot >= 0 && slot < this.clients.length ? this.clients[slot] : null;
   }

   public PlayerMob getPlayer(int slot) {
      ServerClient client = this.getClient(slot);
      return client != null ? client.playerMob : null;
   }

   public ServerClient getClientByAuth(long auth) {
      return this.authClientMap.get(auth);
   }

   public PlayerMob getPlayerByAuth(long auth) {
      ServerClient client = this.getClientByAuth(auth);
      return client != null ? client.playerMob : null;
   }

   public SavedServerClientData getSavedClientData(long auth) {
      ServerClient client = this.getClientByAuth(auth);
      return client != null ? new SavedServerClientData(client) : this.savedClients.get(auth);
   }

   public Collection<SavedServerClientData> getSavedClients() {
      return this.savedClients.values();
   }

   public boolean isNameAlreadyInUse(String name, long ignoreAuth) {
      for (SavedServerClientData value : this.savedClients.values()) {
         if (value.authentication != ignoreAuth && value.name.equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   public void updateSavedClientData(ServerClient client) {
      SavedServerClientData data = this.savedClients.get(client.authentication);
      if (data == null) {
         this.savedClients.put(client.authentication, new SavedServerClientData(client));
      } else {
         data.update(client);
      }
   }

   public void deleteSavedClientData(long authentication) {
      this.savedClients.remove(authentication);
   }

   public String getNameByAuth(long auth, String defaultValue) {
      ServerClient client = this.getClientByAuth(auth);
      return client != null ? client.getName() : this.getSavedClientName(auth, defaultValue);
   }

   public String getSavedClientName(long auth, String defaultValue) {
      SavedServerClientData savedClientData = this.getSavedClientData(auth);
      return savedClientData == null ? defaultValue : savedClientData.name;
   }

   public boolean addClient(
      NetworkInfo networkInfo,
      long authentication,
      String version,
      boolean craftingUsesNearbyInventories,
      boolean trackNewQuests,
      PacketClientInstalledDLC installedDLC
   ) {
      if (!this.isSingleplayer()) {
         System.out
            .println(
               "Client \""
                  + authentication
                  + "\" with address "
                  + (networkInfo == null ? "LOCAL" : networkInfo.getDisplayName())
                  + " is connecting with version "
                  + version
                  + "."
            );
      }

      boolean hasClient = this.world.hasClient(authentication);
      SavedServerClientData savedClientData = this.getSavedClientData(authentication);
      String clientName = savedClientData == null ? "N/A" : savedClientData.name;
      if (Settings.isBanned(authentication) || hasClient && Settings.isBanned(clientName)) {
         if (!this.isSingleplayer() && !this.isHosted() || authentication != GameAuth.getAuthentication()) {
            this.network.sendPacket(new NetworkPacket(new PacketDisconnect(-1, PacketDisconnect.Code.BANNED_CLIENT), networkInfo));
            String addition = hasClient ? " (" + clientName + ")." : ".";
            System.out.println("Client " + authentication + " is banned" + addition);
            return false;
         }

         System.out.println("The singleplayer/host client was banned, unbanning now.");
         Settings.removeBanned(String.valueOf(authentication));
         if (hasClient) {
            Settings.removeBanned(clientName);
         }
      }

      String consoleName = hasClient ? "\"" + clientName + "\"" : "\"" + authentication + "\"";
      if (!version.equals("1.3.3")) {
         System.out.println("Client " + consoleName + " had wrong version (" + version + ").");
         this.network.sendPacket(new NetworkPacket(new PacketDisconnect(-1, PacketDisconnect.Code.WRONG_VERSION), networkInfo));
         return false;
      }

      for (int i = 0; i < this.getSlots(); i++) {
         ServerClient client = this.getClient(i);
         if (client != null && client.authentication == authentication) {
            this.packetManager.startVerboseLogging(networkInfo, 10000);
            if (Objects.equals(client.networkInfo, networkInfo)) {
               System.out.println("Client " + consoleName + " is already connected. Sending another approved packet...");
               client.sendPacket(new PacketConnectApproved(this, client));
               return false;
            }

            System.out.println("Client " + consoleName + " was already playing.");
            this.network.sendPacket(new NetworkPacket(new PacketDisconnect(-1, PacketDisconnect.Code.ALREADY_PLAYING), networkInfo));
            return false;
         }
      }

      int slotOffset = 0;

      for (int i = 0; i < this.getSlots(); i++) {
         int actualSlot = (i + slotOffset) % this.getSlots();
         if (this.getClient(actualSlot) == null) {
            System.out.println("Client " + consoleName + " connected on slot " + (actualSlot + 1) + "/" + this.getSlots() + ".");
            long sessionID = GameRandom.globalRandom.nextLong();
            if (!hasClient) {
               System.out.println("Creating new player: " + authentication);
               this.clients[actualSlot] = ServerClient.getNewPlayerClient(this, sessionID, networkInfo, actualSlot, authentication);
               this.clients[actualSlot].saveClient();
            } else {
               this.clients[actualSlot] = this.world.loadClient(sessionID, authentication, networkInfo, actualSlot);
               System.out.println("Loaded player: " + authentication);
               if (!this.clients[actualSlot].needAppearance()) {
                  this.clients[actualSlot].onFirstConnecting();
               }
            }

            this.authClientMap.put(authentication, this.clients[actualSlot]);
            this.clients[actualSlot].sendPacket(new PacketConnectApproved(this, this.clients[actualSlot]));
            this.clients[actualSlot].craftingUsesNearbyInventories = craftingUsesNearbyInventories;
            this.clients[actualSlot].trackNewQuests = trackNewQuests;
            this.clients[actualSlot].applyClientInstalledDLCPacket(installedDLC);
            GameEvents.triggerEvent(new ServerClientConnectedEvent(this.clients[actualSlot]));
            return true;
         }
      }

      System.out.println("Could not find a slot for client \"" + authentication + "\".");
      this.network.sendPacket(new NetworkPacket(new PacketDisconnect(-1, PacketDisconnect.Code.SERVER_FULL), networkInfo));
      return false;
   }

   public int getSlots() {
      return this.settings.slots;
   }

   public boolean isSingleplayer() {
      return this.isSinglePlayer;
   }

   public boolean isHosted() {
      return this.isHosted;
   }

   public boolean isDedicated() {
      return this.client == null;
   }

   public Client getLocalClient() {
      return this.client;
   }

   public void sendCommand(String command, ServerClient client) {
      if (this.commandsManager.runServerCommand(new ParsedCommand(command), client)) {
         if (client != null) {
            System.out.println(client.getName() + " issued command: /" + command);
         }
      } else if (client != null) {
         System.out.println(client.getName() + " tried to issue command: /" + command);
      }
   }

   public void startSaveTimer() {
      this.saveTime = this.world.worldEntity.getTime() + Math.max(1, 60) * 1000L;
   }

   public void tickAutoSave() {
      if (this.saveTime <= this.world.worldEntity.getTime() && this.saveHandler == null) {
         boolean shouldBackup = this.autoSaves % 15 == 0;
         if (GameLaunch.launchOptions.containsKey("nobackups")) {
            shouldBackup = false;
         }

         try {
            if (shouldBackup) {
               System.out.println("Running an auto save with backup...");
               this.startFullSave(true, false, new ServerSaveHandler.SaveInterruptedOrFinishedEventHandler() {
                  @Override
                  public void onSaveInterrupted(ServerSaveHandler newSaveHandler) {
                     if (!Server.this.stopCalled) {
                        newSaveHandler.saveEvents.add(this);
                     }
                  }

                  @Override
                  public void onSaveFinished() {
                     long startTime = System.nanoTime();

                     try {
                        Server.this.reloadFileSystem();
                     } catch (IOException | FileSystemClosedException e) {
                        Server.this.handleSaveException(e, false);
                     }

                     System.out.println("Reloaded file system in " + GameUtils.getTimeStringNano(System.nanoTime() - startTime));
                     boolean isDirectory = World.isWorldADirectory(Server.this.world.filePath);
                     File targetPath = WorldSave.getNextBackupPath(isDirectory);
                     System.out.println("Copying backup to path: " + targetPath);
                     Thread thread = new Thread(() -> {
                        try {
                           if (targetPath.exists()) {
                              GameUtils.deleteFileOrFolder(targetPath);
                           }

                           World.copyWorld(Server.this.world.filePath, targetPath, false);
                           File deprecatedFile = new File(World.getSavesPath() + targetPath.getName());
                           if (deprecatedFile.exists()) {
                              World.deleteWorld(deprecatedFile);
                           }

                           System.out.println("Finished backup");
                           if (Server.this.isDedicated() || GameLaunch.runPeriodicGCRounds) {
                              System.gc();
                           }
                        } catch (Exception e) {
                           System.err.println("Error saving latest backup");
                           e.printStackTrace();
                        }
                     }, "Latest backup");
                     thread.start();
                     Server.this.startSaveTimer();
                  }
               });
            } else {
               this.world.saveWorldEntity();
               this.world.settings.saveSettings();
               this.world
                  .worldEntity
                  .dataComponentManager
                  .<WorldSavedEntityComponent, Class<WorldSavedEntityComponent>>streamAll(WorldSavedEntityComponent.class)
                  .forEach(WorldSavedEntityComponent::onWorldSaved);

               for (int i = 0; i < this.getSlots(); i++) {
                  ServerClient client = this.getClient(i);
                  if (client != null) {
                     client.saveClient();
                  }
               }

               long startTime = System.nanoTime();
               this.reloadFileSystem();
               System.out.println("Auto save: Reloaded file system in " + GameUtils.getTimeStringNano(System.nanoTime() - startTime));
            }
         } catch (Exception e) {
            this.handleSaveException(e, false);
         }

         if (this.isDedicated() || GameLaunch.runPeriodicGCRounds) {
            this.world.worldEntity.executor().submit(System::gc);
         }

         this.autoSaves++;
         this.startSaveTimer();
      }
   }

   public boolean startFullSave(boolean forced, boolean printPerformance, ServerSaveHandler.SaveInterruptedOrFinishedEventHandler eventHandler) {
      if (this.saveHandler != null && !forced) {
         return false;
      }

      System.out.println("Starting world save");
      PerformanceTimerManager timer;
      if (printPerformance) {
         this.savePerformanceTimer = new PerformanceTimerManager(false);
         timer = this.savePerformanceTimer;
      } else {
         this.savePerformanceTimer = null;
         timer = this.tickManager();
      }

      ServerSaveHandler oldHandler = this.saveHandler;
      this.saveHandler = new ServerSaveHandler(timer, this);
      if (eventHandler != null) {
         this.saveHandler.saveEvents.add(eventHandler);
      }

      if (oldHandler != null) {
         oldHandler.saveEvents.forEach(e -> e.onSaveInterrupted(this.saveHandler));
      }

      return true;
   }

   public void tickSaveHandler(boolean closeFileSystem) {
      if (this.saveHandler != null) {
         PerformanceWrapper wrapper = null;

         try {
            long time = System.nanoTime();
            wrapper = Performance.wrapTimer(this.tickManager(), "worldSaving");
            boolean success = this.saveHandler.tick();
            this.totalTimeSpentSaving = this.totalTimeSpentSaving + (System.nanoTime() - time);
            wrapper.end();
            wrapper = null;
            if (success) {
               time = System.nanoTime();
               if (closeFileSystem) {
                  wrapper = Performance.wrapTimer(this.tickManager(), "closeFileSystem");
                  this.closeFileSystem();
               } else {
                  wrapper = Performance.wrapTimer(this.tickManager(), "reloadFileSystem");
                  this.reloadFileSystem();
               }

               this.totalTimeSpentSaving = this.totalTimeSpentSaving + (System.nanoTime() - time);
               if (this.savePerformanceTimer != null) {
                  System.out.println("World save performance results: ");
                  PerformanceTimerUtils.printPerformanceTimer(this.savePerformanceTimer.getCurrentRootPerformanceTimer());
                  this.savePerformanceTimer = null;
               }

               if (this.saveHandler.saveEvents.isEmpty()) {
                  System.out.println("Completed world save");
               } else {
                  this.saveHandler.saveEvents.forEach(ServerSaveHandler.SaveInterruptedOrFinishedEventHandler::onSaveFinished);
               }

               if ((this.isDedicated() || GameLaunch.runPeriodicGCRounds) && !this.isStopped()) {
                  this.world.worldEntity.executor().submit(System::gc);
               }

               this.saveHandler = null;
            }
         } catch (Exception e) {
            this.saveHandler = null;
            this.handleSaveException(e, closeFileSystem);
         } finally {
            if (wrapper != null) {
               wrapper.end();
            }
         }
      }
   }

   private void handleSaveException(Exception e, boolean closeFileSystem) {
      if (closeFileSystem) {
         System.err.println(Localization.translate("misc", "savefailed1"));
         e.printStackTrace();
         GameCache.removeCache("continueLast");
         this.stopErrors = new LinkedList<>();
         NoticeForm saveFailedForm = new NoticeForm("notice");
         saveFailedForm.setupNotice(new GameMessageBuilder().append("misc", "savefailed1").append("\n\n" + e.getMessage()));
         this.stopErrors.add(saveFailedForm);
         if (!this.world.fileSystem.isOpen()) {
            ConfirmationContinueForm continueZipForm = new ConfirmationContinueForm("");
            continueZipForm.setupConfirmation(
               new LocalMessage("misc", "savecompressfailed"), new LocalMessage("ui", "acceptbutton"), new LocalMessage("ui", "declinebutton"), () -> {
                  Settings.zipSaves = false;
                  continueZipForm.applyContinue();
               }, continueZipForm::applyContinue
            );
            this.stopErrors.add(continueZipForm);
         }
      } else {
         Client client = this.getLocalClient();
         if (client != null) {
            e.printStackTrace();
            if (!this.world.fileSystem.isOpen()) {
               client.overrideDisconnectMessage = Localization.translate("misc", "savefailed1") + "\n\n" + e.getMessage();
               this.stop(PacketDisconnect.Code.SERVER_ERROR);
               this.privateStop();
               if (this.world.filePath.getName().endsWith("zip")) {
                  GameCache.removeCache("continueLast");
                  ConfirmationContinueForm continueZipForm = new ConfirmationContinueForm("");
                  continueZipForm.setupConfirmation(
                     new LocalMessage("misc", "savecompressfailed"), new LocalMessage("ui", "acceptbutton"), new LocalMessage("ui", "declinebutton"), () -> {
                        Settings.zipSaves = false;
                        continueZipForm.applyContinue();
                     }, continueZipForm::applyContinue
                  );
                  client.additionalDisconnectContinueForms.add(continueZipForm);
               }
            } else {
               client.chat.addMessage(GameColor.RED.getColorCode() + Localization.translate("misc", "savefailed1"));
               client.chat.addMessage(GameColor.RED.getColorCode() + e.getMessage());
               client.chat.addMessage(GameColor.RED.getColorCode() + Localization.translate("misc", "savefailed2"));
            }
         } else {
            System.err.println(Localization.translate("misc", "savefailed1"));
            e.printStackTrace();
            System.err.println(Localization.translate("misc", "savefailed2"));
            if (!this.world.fileSystem.isOpen()) {
               this.stop(PacketDisconnect.Code.SERVER_ERROR);
               this.privateStop();
            }
         }
      }
   }

   public void waitForSaveComplete() {
      while (this.saveHandler != null) {
         this.tickSaveHandler(false);
      }
   }

   private void closeFileSystem() throws IOException {
      this.world.closeFileSystem();
   }

   private void reloadFileSystem() throws IOException, FileSystemClosedException {
      this.world.reloadFileSystem();
   }

   public int getSlot(PlayerMob player) {
      for (int i = 0; i < this.getSlots(); i++) {
         ServerClient client = this.getClient(i);
         if (client != null && player == client.playerMob) {
            return i;
         }
      }

      return -1;
   }

   public boolean disconnectClient(int slot, PacketDisconnect.Code code) {
      return this.disconnectClient(this.getClient(slot), code);
   }

   public boolean disconnectClient(ServerClient client, PacketDisconnect.Code code) {
      return this.disconnectClient(client, new PacketDisconnect(client.slot, code));
   }

   public boolean disconnectClient(int slot, PacketDisconnect packet) {
      return this.disconnectClient(this.getClient(slot), packet);
   }

   public boolean disconnectClient(ServerClient client, PacketDisconnect packet) {
      if (client != null) {
         this.network.sendToAllClients(packet);
         this.disconnectClient(client, true);
         return true;
      } else {
         return false;
      }
   }

   private void disconnectClient(ServerClient client, boolean save) {
      client.onUnloading();
      if (save) {
         client.saveClient();
      }

      GameEvents.triggerEvent(new ServerClientDisconnectEvent(client));
      client.dispose();
      this.clients[client.slot] = null;
      this.authClientMap.remove(client.authentication);
   }

   public int getPlayersOnline() {
      return this.playersOnline;
   }

   private void privateStop() {
      this.world.levelManager.getLoadedLevels().stream().sorted(Comparator.comparingInt(l -> l.shouldSave() ? 1 : 0)).forEach(level -> {
         level.onUnloading();
         level.dispose();
      });
      System.out.println("Stopped server on " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + " with code: " + this.stopPacket.code);
      System.out.println("World time: " + this.world.worldEntity.getDayTimeInt() + ", day " + this.world.worldEntity.getDay());
      System.out.println("Game time: " + this.world.worldEntity.getTime());
      System.out.println("Total ticks: " + this.tickManager().getTotalTicks());
      System.out.println("Received: " + this.packetManager.getTotalIn() + " (" + this.packetManager.getTotalInPackets() + " packets)");
      System.out.println("Sent: " + this.packetManager.getTotalOut() + " (" + this.packetManager.getTotalOutPackets() + " packets)");

      for (int i = 0; i < this.getSlots(); i++) {
         ServerClient client = this.getClient(i);
         if (client != null) {
            client.dispose();
         }
      }

      GameEvents.triggerEvent(new ServerStopEvent(this));
      this.network.close();
      this.world.dispose();
      this.isStopped = true;
      this.stopEvents.forEach(c -> c.accept(this));
   }

   public boolean isStopped() {
      return this.stopCalled || this.isStopped;
   }

   public boolean hasClosed() {
      return this.isStopped;
   }

   public void stop() {
      this.stop((PacketDisconnect)null, null);
   }

   public void stop(Consumer<Server> onStop) {
      this.stop((PacketDisconnect)null, onStop);
   }

   public void stop(PacketDisconnect.Code code) {
      this.stop(code, null);
   }

   public void stop(PacketDisconnect.Code code, Consumer<Server> onStop) {
      this.stop(code == null ? null : new PacketDisconnect(0, code), onStop);
   }

   public void stop(PacketDisconnect stopPacket, Consumer<Server> onStop) {
      if (stopPacket == null) {
         stopPacket = new PacketDisconnect(0, PacketDisconnect.Code.SERVER_STOPPED);
      }

      this.stopPacket = stopPacket;
      if (onStop != null) {
         this.stopEvents.add(onStop);
      }

      this.stopCalled = true;
      this.world
         .levelManager
         .getLoadedLevels()
         .stream()
         .filter(l -> !l.shouldSave())
         .forEach(
            level -> this.streamClients()
               .filter(c -> c.getLevelIdentifier().equals(level.getIdentifier()))
               .forEach(c -> c.changeToFallbackLevel(level.getIdentifier(), false))
         );

      for (int i = 0; i < this.getSlots(); i++) {
         ServerClient client = this.getClient(i);
         if (client != null) {
            client.sendPacket(new PacketDisconnect(stopPacket, i));
            client.onUnloading();
         }
      }

      this.startFullSave(true, false, new ServerSaveHandler.SaveInterruptedOrFinishedEventHandler() {
         @Override
         public void onSaveFinished() {
            System.out.println("Completed world save before stopping server");
         }
      });
   }

   public TickManager tickManager() {
      return this.tickManager;
   }

   private PacketManager newPacketManager() {
      return new PacketManager() {
         @Override
         public void processInstantly(NetworkPacket packet) {
            Server.this.processPacket(packet);
         }
      };
   }

   public ServerSettings getSettings() {
      return this.settings;
   }
}
