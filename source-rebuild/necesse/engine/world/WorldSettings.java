package necesse.engine.world;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import necesse.engine.GameDeathPenalty;
import necesse.engine.GameDifficulty;
import necesse.engine.GameLog;
import necesse.engine.GameRaidFrequency;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.packet.PacketSettings;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.CharacterSave;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.save.WorldSave;
import necesse.engine.state.MainGame;
import necesse.engine.state.State;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;

public class WorldSettings {
   public static boolean cheatsHidden = false;
   public boolean allowCheats = false;
   public GameDifficulty difficulty = GameDifficulty.CLASSIC;
   public GameDeathPenalty deathPenalty = GameDeathPenalty.DROP_MATS;
   public GameRaidFrequency raidFrequency = GameRaidFrequency.OCCASIONALLY;
   public boolean survivalMode = true;
   public boolean playerHunger = true;
   public boolean canSettlersDie = false;
   public boolean disableMobSpawns = false;
   public boolean forcedPvP = false;
   public boolean allowOutsideCharacters = true;
   public boolean creativeMode = false;
   private boolean lastCreativeMode = false;
   public boolean disableMobAI = false;
   public float dayTimeMod = 1.0F;
   public float nightTimeMod = 1.0F;
   public int droppedItemsLifeMinutes;
   public boolean unloadSettlements;
   public int maxSettlementsPerPlayer;
   public int maxSettlersPerSettlement;
   public String gameVersion;
   private final World world;
   private Client client;

   public WorldSettings(World world) {
      this.droppedItemsLifeMinutes = Settings.droppedItemsLifeMinutes;
      this.unloadSettlements = Settings.unloadSettlements;
      this.maxSettlementsPerPlayer = Settings.maxSettlementsPerPlayer;
      this.maxSettlersPerSettlement = Settings.maxSettlersPerSettlement;
      this.gameVersion = "1.3.3";
      this.world = world;
   }

   public WorldSettings(Client client, PacketReader reader, boolean beforeConnected) {
      this.droppedItemsLifeMinutes = Settings.droppedItemsLifeMinutes;
      this.unloadSettlements = Settings.unloadSettlements;
      this.maxSettlementsPerPlayer = Settings.maxSettlementsPerPlayer;
      this.maxSettlersPerSettlement = Settings.maxSettlersPerSettlement;
      this.gameVersion = "1.3.3";
      this.world = null;
      this.client = client;
      if (beforeConnected) {
         this.applyBeforeConnectedPacket(reader);
      } else {
         this.applyContentPacket(reader);
      }
   }

   public void sendSettingsPacket() {
      if (this.world == null) {
         throw new NullPointerException("Cannot send settings packet from null world.");
      }

      this.world.server.network.sendToAllClients(new PacketSettings(this));
   }

   public void saveSettings() {
      if (this.creativeMode && !this.lastCreativeMode) {
         if (this.world.worldEntity != null && this.world.getWorldTime() != 0L) {
            this.creativeMode = false;
            Server server = this.world.server;
            server.startFullSave(true, false, null);
            server.waitForSaveComplete();

            try {
               File targetPath = WorldSave.getCreativeBackupPath(this.world.filePath);
               if (targetPath.exists()) {
                  throw new FileAlreadyExistsException(targetPath.toString());
               }

               World.copyWorld(this.world.filePath, targetPath, false);
               server.network
                  .sendToAllClients(new PacketChatMessage(new LocalMessage("ui", "creativeworldbackedupmessage", "worldname", this.world.displayName)));
            } catch (Exception e) {
               GameLog.err.println("Error creating creative backup");
               e.printStackTrace(GameLog.err);
            }

            this.creativeMode = true;
         }

         this.lastCreativeMode = true;
      }

      this.saveSettings(this.world.fileSystem.getWorldSettingsFile());
   }

   public void saveSettings(WorldFile file) {
      if (this.world == null) {
         throw new NullPointerException("Cannot save settings from null world.");
      }

      this.getSaveScript().saveScript(file);
   }

   public void loadSettings() {
      this.loadSettings(true);
   }

   public void loadSettings(boolean createFile) {
      if (this.world == null) {
         throw new NullPointerException("Cannot load settings from null world.");
      }

      WorldFile file = this.world.fileSystem.getWorldSettingsFile();
      if (file.exists() && !file.isDirectory()) {
         this.loadSaveScript(new LoadData(file));
      } else if (createFile) {
         this.saveSettings();
      }
   }

   private SaveData getSaveScript() {
      SaveData save = new SaveData("WORLDSETTINGS");
      save.addBoolean("allowCheats", this.allowCheats);
      save.addEnum("difficulty", this.difficulty);
      save.addEnum("deathPenalty", this.deathPenalty);
      save.addEnum("raidFrequency", this.raidFrequency);
      save.addBoolean("survivalMode", this.survivalMode);
      save.addBoolean("playerHunger", this.playerHunger);
      save.addBoolean("disableMobSpawns", this.disableMobSpawns);
      save.addBoolean("forcedPvP", this.forcedPvP, "True = players will always have PvP enabled");
      save.addBoolean("allowOutsideCharacters", this.allowOutsideCharacters);
      save.addBoolean("creativeMode", this.creativeMode);
      save.addBoolean("disableMobAI", this.disableMobAI);
      save.addBoolean("canSettlersDie", this.canSettlersDie);
      save.addFloat("dayTimeMod", this.dayTimeMod, "Day time modifier (The higher, the longer day will last, max 10)");
      save.addFloat("nightTimeMod", this.nightTimeMod, "Night time modifier (The higher, the longer night will last, max 10)");
      save.addSafeString("gameVersion", "1.3.3");
      return save;
   }

   private void loadSaveScript(LoadData save) {
      this.allowCheats = save.getBoolean("allowCheats", this.allowCheats);
      this.difficulty = save.getEnum(GameDifficulty.class, "difficulty", this.difficulty, false);
      if ("EASY".equals(save.getUnsafeString("difficulty", null, false))) {
         this.difficulty = GameDifficulty.ADVENTURE;
      }

      this.deathPenalty = save.getEnum(GameDeathPenalty.class, "deathPenalty", this.deathPenalty, false);
      this.raidFrequency = save.getEnum(GameRaidFrequency.class, "raidFrequency", this.raidFrequency, false);
      this.survivalMode = save.getBoolean("survivalMode", this.survivalMode, false);
      this.playerHunger = save.getBoolean("playerHunger", this.playerHunger, false);
      this.disableMobSpawns = save.getBoolean("disableMobSpawns", this.disableMobSpawns);
      this.forcedPvP = save.getBoolean("forcedPvP", this.forcedPvP);
      if (save.hasLoadDataByName("allowOutsideCharacters")) {
         this.allowOutsideCharacters = save.getBoolean("allowOutsideCharacters", this.allowOutsideCharacters, false);
      } else if (this.world != null && this.world.server != null && !this.world.server.isHosted() && !this.world.server.isSingleplayer()) {
         this.allowOutsideCharacters = false;
         GameLog.warn.println("Set default allow outside characters to false for " + this.world.filePath.getName());
      }

      this.creativeMode = this.lastCreativeMode = save.getBoolean("creativeMode", this.creativeMode, false);
      this.disableMobAI = save.getBoolean("disableMobAI", this.disableMobAI, false);
      this.canSettlersDie = save.getBoolean("canSettlersDie", this.canSettlersDie, false);
      if (save.hasLoadDataByName("dayTimeMod")) {
         this.dayTimeMod = save.getFloat("dayTimeMod", this.dayTimeMod);
         this.nightTimeMod = save.getFloat("nightTimeMod", this.nightTimeMod);
         this.dayTimeMod = Math.min(Math.max(0.0F, this.dayTimeMod), 10.0F);
         this.nightTimeMod = Math.min(Math.max(0.0F, this.nightTimeMod), 10.0F);
         this.dayTimeMod = (int)(this.dayTimeMod * 10.0F) / 10.0F;
         this.nightTimeMod = (int)(this.nightTimeMod * 10.0F) / 10.0F;
         if (this.dayTimeMod == 0.0F && this.nightTimeMod == 0.0F) {
            this.dayTimeMod = 1.0F;
         }
      }

      LoadData versionSave = save.getFirstLoadDataByName("gameVersion");
      if (versionSave != null) {
         this.gameVersion = LoadData.getSafeString(versionSave);
      }
   }

   public void setupBeforeConnectedPacket(PacketWriter writer) {
      writer.putNextBoolean(this.allowCheats);
      writer.putNextByteUnsigned(this.difficulty.ordinal());
      writer.putNextByteUnsigned(this.deathPenalty.ordinal());
      writer.putNextByteUnsigned(this.raidFrequency.ordinal());
      writer.putNextBoolean(this.survivalMode);
      writer.putNextBoolean(this.playerHunger);
      writer.putNextBoolean(this.disableMobSpawns);
      writer.putNextBoolean(this.forcedPvP);
      writer.putNextBoolean(this.allowOutsideCharacters);
      writer.putNextBoolean(this.creativeMode);
      writer.putNextBoolean(this.disableMobAI);
      writer.putNextBoolean(this.canSettlersDie);
      writer.putNextFloat(this.dayTimeMod);
      writer.putNextFloat(this.nightTimeMod);
      writer.putNextInt(this.droppedItemsLifeMinutes);
      writer.putNextBoolean(this.unloadSettlements);
      writer.putNextInt(this.maxSettlementsPerPlayer);
      writer.putNextInt(this.maxSettlersPerSettlement);
   }

   public void applyBeforeConnectedPacket(PacketReader reader) {
      this.allowCheats = reader.getNextBoolean();
      this.difficulty = GameDifficulty.values()[reader.getNextByteUnsigned()];
      this.deathPenalty = GameDeathPenalty.values()[reader.getNextByteUnsigned()];
      this.raidFrequency = GameRaidFrequency.values()[reader.getNextByteUnsigned()];
      this.survivalMode = reader.getNextBoolean();
      this.playerHunger = reader.getNextBoolean();
      this.disableMobSpawns = reader.getNextBoolean();
      this.forcedPvP = reader.getNextBoolean();
      this.allowOutsideCharacters = reader.getNextBoolean();
      boolean newCreativeMode = reader.getNextBoolean();
      if (this.client != null && this.client.characterFilePath != null && newCreativeMode) {
         CharacterSave.backupCharacterForCreativeIfNecessary(this.client);
      }

      this.creativeMode = newCreativeMode;
      this.disableMobAI = reader.getNextBoolean();
      this.canSettlersDie = reader.getNextBoolean();
      this.dayTimeMod = reader.getNextFloat();
      this.nightTimeMod = reader.getNextFloat();
      if (this.world != null) {
         this.world.worldEntity.calculateAmbientLightValues();
      }

      if (this.client != null && this.client.worldEntity != null) {
         this.client.worldEntity.calculateAmbientLightValues();
      }

      this.droppedItemsLifeMinutes = reader.getNextInt();
      this.unloadSettlements = reader.getNextBoolean();
      this.maxSettlementsPerPlayer = reader.getNextInt();
      this.maxSettlersPerSettlement = reader.getNextInt();
      State state = GlobalData.getCurrentState();
      if (state instanceof MainGame) {
         ((MainGame)state).formManager.updateActive(true);
      }
   }

   public void setupContentPacket(PacketWriter writer) {
      this.setupBeforeConnectedPacket(writer);
   }

   public void applyContentPacket(PacketReader reader) {
      this.applyBeforeConnectedPacket(reader);
   }

   public void setupCreativePacket(PacketWriter writer) {
      writer.putNextByteUnsigned(this.difficulty.ordinal());
      writer.putNextByteUnsigned(this.deathPenalty.ordinal());
      writer.putNextByteUnsigned(this.raidFrequency.ordinal());
      writer.putNextBoolean(this.survivalMode);
      writer.putNextBoolean(this.playerHunger);
      writer.putNextBoolean(this.disableMobSpawns);
      writer.putNextBoolean(this.disableMobAI);
      writer.putNextBoolean(this.canSettlersDie);
   }

   public void applyCreativePacket(PacketReader reader, ServerClient client) {
      LocalMessage enabled = new LocalMessage("ui", "creativemsgenabled");
      LocalMessage disabled = new LocalMessage("ui", "creativemsgdisabled");
      GameDifficulty newDifficulty = GameDifficulty.values()[reader.getNextByteUnsigned()];
      if (newDifficulty != this.difficulty) {
         this.difficulty = newDifficulty;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(new LocalMessage("ui", "creativedifficultymsg", "player", client.getName(), "difficulty", this.difficulty.displayName))
            );
      }

      GameDeathPenalty newDeathPenalty = GameDeathPenalty.values()[reader.getNextByteUnsigned()];
      if (newDeathPenalty != this.deathPenalty) {
         this.deathPenalty = newDeathPenalty;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(new LocalMessage("ui", "creativedeathpenaltymsg", "player", client.getName(), "penalty", this.deathPenalty.displayName))
            );
      }

      GameRaidFrequency newRaidFrequency = GameRaidFrequency.values()[reader.getNextByteUnsigned()];
      if (newRaidFrequency != this.raidFrequency) {
         this.raidFrequency = newRaidFrequency;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(
                  new LocalMessage("ui", "creativeraidfrequencymsg", "player", client.getName(), "frequency", this.raidFrequency.displayName)
               )
            );
      }

      boolean newSurvivalMode = reader.getNextBoolean();
      if (newSurvivalMode != this.survivalMode) {
         this.survivalMode = newSurvivalMode;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(
                  new LocalMessage("ui", "creativesurvivalmodemsg", "player", client.getName(), "action", this.survivalMode ? enabled : disabled)
               )
            );
      }

      boolean newPlayerHunger = reader.getNextBoolean();
      if (newPlayerHunger != this.playerHunger) {
         this.playerHunger = newPlayerHunger;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(
                  new LocalMessage("ui", "creativeplayerhungermsg", "player", client.getName(), "action", this.playerHunger ? enabled : disabled)
               )
            );
      }

      boolean newDisableMobSpawns = reader.getNextBoolean();
      if (newDisableMobSpawns != this.disableMobSpawns) {
         this.disableMobSpawns = newDisableMobSpawns;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(
                  new LocalMessage("ui", "creativehostilemobspawningmsg", "player", client.getName(), "action", this.disableMobSpawns ? disabled : enabled)
               )
            );
      }

      boolean newMobAiDisabled = reader.getNextBoolean();
      if (newMobAiDisabled != this.disableMobAI) {
         this.disableMobAI = newMobAiDisabled;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(new LocalMessage("ui", "creativemobaimsg", "player", client.getName(), "action", this.disableMobAI ? disabled : enabled))
            );
      }

      boolean newCanSettlersDie = reader.getNextBoolean();
      if (newCanSettlersDie != this.canSettlersDie) {
         this.canSettlersDie = newCanSettlersDie;
         client.getServer()
            .network
            .sendToAllClients(
               new PacketChatMessage(
                  new LocalMessage("ui", "creativecansettlersdiemsg", "player", client.getName(), "action", this.canSettlersDie ? enabled : disabled)
               )
            );
      }
   }

   public GameTooltips getTooltips(LocalMessage title) {
      StringTooltips tooltips = new StringTooltips(title.translate());
      tooltips.add(new LocalMessage("ui", "difficultytip", "difficulty", this.difficulty.displayName).translate());
      tooltips.add(new LocalMessage("ui", "dptip", "penalty", this.deathPenalty.displayName).translate());
      if (this.survivalMode) {
         tooltips.add(Localization.translate("ui", "survivalmode"));
      }

      if (this.creativeMode) {
         tooltips.add(Localization.translate("ui", "creativemode"));
      }

      if (this.disableMobAI) {
         tooltips.add(Localization.translate("ui", "creativemobaiisdisabled"));
      } else {
         tooltips.add(Localization.translate("ui", this.playerHunger ? "hungerenabled" : "hungerdisabled"));
      }

      tooltips.add(Localization.translate("ui", this.disableMobSpawns ? "mobsdisabled" : "mobsenabled"));
      tooltips.add(Localization.translate("ui", this.forcedPvP ? "forcepvpon" : "forcepvpoff"));
      tooltips.add(Localization.translate("ui", this.forcedPvP ? "forcepvpon" : "forcepvpoff"));
      GameWindow window = WindowManager.getWindow();
      if (!window.isKeyDown(340) && !window.isKeyDown(344)) {
         tooltips.add(Localization.translate("ui", "showadvanced"), GameColor.LIGHT_GRAY);
      } else {
         tooltips.add(Localization.translate("ui", "daymod", "mod", (int)(this.dayTimeMod * 100.0F) + "%"));
         tooltips.add(Localization.translate("ui", "nightmod", "mod", (int)(this.nightTimeMod * 100.0F) + "%"));
      }

      return tooltips;
   }

   public boolean playerHunger() {
      return this.survivalMode || this.playerHunger;
   }

   public void enableCheats() {
      if (!this.allowCheats) {
         this.allowCheats = true;
         this.saveSettings();
         this.sendSettingsPacket();
      }
   }

   public void enableCreativeMode(boolean saveAndSync) {
      if (!this.creativeMode) {
         if (this.client != null && this.client.getClient() != null && this.client.getClient().isClient()) {
            CharacterSave.backupCharacterForCreativeIfNecessary(this.client);
         }

         this.creativeMode = true;
         if (saveAndSync) {
            this.saveSettings();
            this.sendSettingsPacket();
         }
      }
   }

   public boolean achievementsEnabled() {
      return !this.allowCheats && !this.creativeMode;
   }

   public boolean cheatsAllowedOrHidden() {
      return this.allowCheats || cheatsHidden;
   }
}
