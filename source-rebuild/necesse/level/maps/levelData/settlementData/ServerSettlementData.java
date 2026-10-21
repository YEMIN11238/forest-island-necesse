package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.GameRaidFrequency;
import necesse.engine.GameState;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceWrapper;
import necesse.engine.journal.listeners.SettlerEquipmentChangedJournalChallengeListener;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketSpawnMob;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.storyObjectives.listeners.AssignBedObjectiveListener;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.ObjectKeyMap;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.OneWorldMigration;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldEntityGameClock;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldData.SettlersWorldData;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.AncientSkeletonRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.ChickenPeopleSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.FishianSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.FrozenDwarvesSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.HumanSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.MummiesSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.NinjaSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.PiratesSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.RogueHuntersSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.SettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.TheMafiaRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.VampiresSettlementRaidLevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.VoidApprenticesSettlementRaidLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SettlementFlagObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.container.events.ContainerEvent;
import necesse.inventory.container.settlement.events.SettlementSettlersChangedEvent;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.levelData.jobs.HasStorageLevelJob;
import necesse.level.maps.levelData.jobs.HaulFromLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.levelData.jobs.UseWorkstationLevelJob;
import necesse.level.maps.levelData.settlementData.notifications.LowFoodSettlementNotification;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecord;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;
import necesse.level.maps.regionSystem.Region;

public class ServerSettlementData implements GameState, WorldEntityGameClock {
   public static int MIN_SECONDS_RAID_TIMER = 5400;
   public static int MAX_SECONDS_RAID_TIMER = 9000;
   public static int INCREASES_RAID_TIMER_SECONDS_PER_RAID = 1800;
   public static int UPPER_LIMIT_RAID_TIMER_SECONDS = 18000;
   public static int MIN_SECONDS_VISITOR_TIMER = 300;
   public static int MAX_SECONDS_VISITOR_TIMER = 1200;
   public static int MAX_SETTLER_GROUPS = 6;
   public static int MAX_RESTRICT_ZONES = 50;
   public static final ArrayList<SettlementVisitorOdds> visitorOdds = new ArrayList<>();
   public static SettlementVisitorOdds visitorRecruitsOdds = new SettlementVisitorOdds("recruit") {
      @Override
      public boolean canSpawn(ServerSettlementData data) {
         int totalSettlersCount = data.countTotalSettlers();
         int maxSettlersPerSettlement = data.getLevel().getWorldSettings().maxSettlersPerSettlement;
         if (maxSettlersPerSettlement >= 0 && totalSettlersCount >= maxSettlersPerSettlement) {
            return false;
         }

         float expectedSettlers = 2 + data.storyObjectives.getTotalMajorObjectivesCompleted();
         if (totalSettlersCount < expectedSettlers) {
            return true;
         }

         double averageHappiness = data.streamSettlers()
            .map(LevelSettler::getMob)
            .filter(Objects::nonNull)
            .mapToInt(SettlerMob::getSettlerTotalHappinessIncludingQualityOfLife)
            .average()
            .orElse(100.0);
         return averageHappiness >= HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD;
      }

      @Override
      public int getTickets(ServerSettlementData data) {
         return 1000;
      }

      @Override
      public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
         TicketSystemList<Supplier<HumanMob>> ticketSystem = new TicketSystemList<>();

         for (Settler settler : SettlerRegistry.getSettlers()) {
            settler.addNewRecruitSettler(data, false, ticketSystem);
         }

         while (!ticketSystem.isEmpty()) {
            Supplier<HumanMob> supplier = ticketSystem.getAndRemoveRandomObject(GameRandom.globalRandom);
            HumanMob humanMob = supplier.get();
            if (humanMob != null) {
               return new SettlementVisitorSpawner(this, humanMob);
            }
         }

         return null;
      }
   };
   public static SettlementVisitorOdds deadRecruitsOdds = new SettlementVisitorOdds("recruit") {
      @Override
      public boolean canSpawn(ServerSettlementData data) {
         return !data.diedSettlerRecruitStringIDs.isEmpty();
      }

      @Override
      public int getTickets(ServerSettlementData data) {
         return 1000;
      }

      @Override
      public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
         while (!data.diedSettlerRecruitStringIDs.isEmpty()) {
            String settlerStringID = data.diedSettlerRecruitStringIDs.remove(GameRandom.globalRandom.nextInt(data.diedSettlerRecruitStringIDs.size()));
            Settler settler = SettlerRegistry.getSettler(settlerStringID);
            if (settler != null) {
               SettlerMob settlerMob = settler.getNewSettlerMob(data);
               if (settlerMob instanceof HumanMob) {
                  return new SettlementVisitorSpawner(this, (HumanMob)settlerMob);
               }
            }
         }

         return null;
      }
   };
   public final SettlementsWorldData worldData;
   public final NetworkSettlementData networkData;
   public final int uniqueID;
   private int settlementTickBuffer = GameRandom.globalRandom.nextInt(20);
   private long settlementTicks;
   public final SettlementStats stats = new SettlementStats();
   public final ObjectKeyMap<Integer, LevelSettler> settlers = new ObjectKeyMap<>(new LinkedHashMap<>(), settler -> settler.mobUniqueID);
   private PointHashMap<SettlementBed> beds = new PointHashMap<>();
   public SettlementRoomsManager rooms = new SettlementRoomsManager(this);
   public final SettlementSettlerGroupManager groups = new SettlementSettlerGroupManager(this);
   private Point homestoneTile;
   private final ArrayList<Waystone> waystones = new ArrayList<>();
   private Point missionBoardTile;
   public final SettlementMissionBoardManager missionBoardManager = new SettlementMissionBoardManager(this);
   public final SettlementStorageManager storageManager = new SettlementStorageManager(this);
   public boolean hasTickedFirstJobs;
   public boolean hasInvalidPlacedItemsInStorage;
   private SettlementWorkZoneManager workZones = new SettlementWorkZoneManager(this);
   private final ItemCategoriesFilter newSettlerDiet = new ItemCategoriesFilter(ItemCategory.foodQualityMasterCategory, true);
   public boolean newSettlerSelfManageEquipment = true;
   public boolean newSettlerEquipmentPreferArmorSets = true;
   private final ItemCategoriesFilter newSettlerEquipmentFilter = new ItemCategoriesFilter(ItemCategory.equipmentMasterCategory, true);
   private int restrictZoneIndexCounter;
   private HashMap<Integer, RestrictZone> restrictZones = new HashMap<>();
   private int newSettlerRestrictZoneUniqueID;
   public final SettlementBoundsManager boundsManager = new SettlementBoundsManager(this);
   public SettlementStorageRecords storageRecords;
   public final SettlementStoryObjectives storyObjectives = new SettlementStoryObjectives(this);
   private long nextVisitorTimer;
   private int currentVisitorUniqueID;
   private boolean nextVisitorIsRecruit;
   private String lastVisitorIdentifier;
   private ArrayList<String> diedSettlerRecruitStringIDs = new ArrayList<>();
   private boolean hasNonAFKTeamMembers;
   private GameRaidFrequency lastRaidFrequencySetting;
   private long nextRaid;
   private int currentRaid;
   private float nextRaidDifficultyMod = 1.0F;
   private boolean lastRaidCheckWasNight;
   private int raidsCounter;
   private final HashSet<Class<? extends ContainerEvent>> eventsToSend = new HashSet<>();

   public static SettlementVisitorSpawner getNewVisitorSpawner(GameRandom random, ServerSettlementData data) {
      TicketSystemList<SettlementVisitorOdds> ticketSystem = new TicketSystemList<>();

      for (SettlementVisitorOdds odds : visitorOdds) {
         if (odds.canSpawn(data)) {
            ticketSystem.addObject(odds.getTickets(data), odds);
         }
      }

      while (!ticketSystem.isEmpty()) {
         SettlementVisitorOdds next = ticketSystem.getAndRemoveRandomObject(random);
         if (next != null) {
            SettlementVisitorSpawner nextHuman = next.getNewVisitorSpawner(data);
            if (nextHuman != null) {
               return nextHuman;
            }
         }
      }

      return null;
   }

   public ServerSettlementData(SettlementsWorldData worldData, NetworkSettlementData networkData, int uniqueID) {
      this.worldData = worldData;
      this.networkData = networkData;
      this.uniqueID = uniqueID;
      this.lastRaidFrequencySetting = this.getLevel().getWorldSettings().raidFrequency;
      this.storageRecords = new SettlementStorageRecords(this.getLevel());
      this.resetNextVisitorTime(false, false);
      this.resetNextRaidTimer(false, false);
      this.groups.createEmptyGroups();
   }

   public Level getLevel() {
      return this.networkData.level;
   }

   public void addSaveData(SaveData save) {
      save.addLong("settlementTicks", this.settlementTicks);
      this.storyObjectives.addSaveData(save);
      save.addLong("nextVisitorTimer", this.nextVisitorTimer);
      save.addInt("currentVisitorUniqueID", this.currentVisitorUniqueID);
      save.addBoolean("nextVisitorIsRecruit", this.nextVisitorIsRecruit);
      if (this.lastVisitorIdentifier != null) {
         save.addSafeString("lastVisitorIdentifier", this.lastVisitorIdentifier);
      }

      if (!this.diedSettlerRecruitStringIDs.isEmpty()) {
         save.addSafeStringCollection("diedSettlerRecruitStringIDs", this.diedSettlerRecruitStringIDs);
      }

      save.addLong("nextRaid", this.nextRaid);
      save.addInt("currentRaid", this.currentRaid);
      save.addFloat("nextRaidDifficultyMod", this.nextRaidDifficultyMod);
      save.addInt("raidsCounter", this.raidsCounter);
      if (this.homestoneTile != null) {
         save.addPoint("homestonePos", this.homestoneTile);
      }

      SaveData waystoneSave = new SaveData("waystones");

      for (Waystone waystone : this.waystones) {
         SaveData cur = new SaveData("");
         waystone.addSaveData(cur);
         waystoneSave.addSaveData(cur);
      }

      save.addSaveData(waystoneSave);
      if (this.missionBoardTile != null) {
         save.addPoint("missionBoardTile", this.missionBoardTile);
      }

      this.missionBoardManager.addSaveData(save);
      SaveData newSettlerDietSave = new SaveData("newSettlerDiet");
      this.newSettlerDiet.addSaveData(newSettlerDietSave);
      save.addSaveData(newSettlerDietSave);
      save.addBoolean("newSettlerSelfManageEquipment", this.newSettlerSelfManageEquipment);
      save.addBoolean("newSettlerEquipmentPreferArmorSets", this.newSettlerEquipmentPreferArmorSets);
      SaveData newSettlerEquipmentSave = new SaveData("newSettlerEquipmentFilter");
      this.newSettlerEquipmentFilter.addSaveData(newSettlerEquipmentSave);
      save.addSaveData(newSettlerEquipmentSave);
      if (this.newSettlerRestrictZoneUniqueID != 0) {
         save.addInt("newSettlerRestrictZoneUniqueID", this.newSettlerRestrictZoneUniqueID);
      }

      if (!this.restrictZones.isEmpty()) {
         SaveData restrictZonesSave = new SaveData("restrictZones");
         this.restrictZones.values().stream().sorted(Comparator.comparingInt(z -> z.index)).forEach(zone -> {
            SaveData restrictZoneSave = new SaveData("restrictZone");
            zone.addSaveData(restrictZoneSave);
            restrictZonesSave.addSaveData(restrictZoneSave);
         });
         save.addSaveData(restrictZonesSave);
      }

      this.boundsManager.addSaveData(save);
      SaveData settlersSave = new SaveData("SETTLERS");

      for (LevelSettler settler : this.settlers.values()) {
         SaveData settlerSave = new SaveData("SETTLER");
         settler.addSaveData(settlerSave);
         settlersSave.addSaveData(settlerSave);
      }

      save.addSaveData(settlersSave);
      SaveData bedsSave = new SaveData("BEDS");

      for (SettlementBed bed : this.beds.values()) {
         SaveData bedSave = new SaveData("BED");
         bed.addSaveData(bedSave);
         bedsSave.addSaveData(bedSave);
      }

      save.addSaveData(bedsSave);
      this.rooms.addSaveData(save);
      this.groups.addSaveData(save);
      this.storageManager.addSaveData(save);
      SaveData zonesSave = new SaveData("WORKZONES");
      this.workZones.addSaveData(zonesSave);
      save.addSaveData(zonesSave);
      SaveData statsSave = new SaveData("STATS");
      this.stats.addSaveData(statsSave);
      save.addSaveData(statsSave);
   }

   public void applyLoadData(LoadData save, OneWorldMigration migrationData, int tileXOffset, int tileYOffset) {
      this.settlementTicks = save.getLong("settlementTicks", 0L, false);
      this.storyObjectives.applySaveData(save);
      this.nextVisitorTimer = save.getLong("nextVisitorTimer", this.nextVisitorTimer, false);
      this.currentVisitorUniqueID = save.getInt("currentVisitorUniqueID", this.currentVisitorUniqueID, false);
      this.nextVisitorIsRecruit = save.getBoolean("nextVisitorIsRecruit", this.nextVisitorIsRecruit, false);
      this.lastVisitorIdentifier = save.getSafeString("lastVisitorIdentifier", this.lastVisitorIdentifier, false);
      if (save.hasLoadDataByName("nextTravelingHuman")) {
         this.nextVisitorTimer = save.getLong("nextTravelingHuman", this.nextVisitorTimer, false);
      }

      if (save.hasLoadDataByName("curTravelingHuman")) {
         this.currentVisitorUniqueID = save.getInt("curTravelingHuman", this.currentVisitorUniqueID, false);
      }

      if (save.hasLoadDataByName("nextTravelingHumanRecruit")) {
         this.nextVisitorIsRecruit = save.getBoolean("nextTravelingHumanRecruit", this.nextVisitorIsRecruit, false);
      }

      if (save.hasLoadDataByName("lastTravelingHumanIdentifier")) {
         this.lastVisitorIdentifier = save.getSafeString("lastTravelingHumanIdentifier", this.lastVisitorIdentifier, false);
      }

      this.diedSettlerRecruitStringIDs = new ArrayList<>(save.getSafeStringCollection("diedSettlerRecruitStringIDs", this.diedSettlerRecruitStringIDs, false));
      this.nextRaid = save.getLong("nextRaid", this.nextRaid);
      this.currentRaid = save.getInt("currentRaid", this.currentRaid);
      this.nextRaidDifficultyMod = save.getFloat("nextRaidDifficultyMod", this.nextRaidDifficultyMod);
      this.lastRaidCheckWasNight = this.getLevel().getWorldEntity().isNight();
      this.raidsCounter = save.getInt("raidsThisTier", this.raidsCounter, false);
      this.raidsCounter = save.getInt("raidsCounter", this.raidsCounter);
      this.homestoneTile = save.getPoint("homestonePos", null, false);
      if (this.homestoneTile != null) {
         this.homestoneTile.translate(tileXOffset, tileYOffset);
      }

      this.waystones.clear();
      LoadData waystonesSave = save.getFirstLoadDataByName("waystones");
      if (waystonesSave != null) {
         int maxWaystones = this.getMaxWaystones();

         for (LoadData waystoneSave : waystonesSave.getLoadData()) {
            if (this.waystones.size() >= maxWaystones) {
               break;
            }

            try {
               this.waystones.add(new Waystone(waystoneSave));
            } catch (Exception e) {
               GameLog.warn.println("Could not load saved waystone");
            }
         }
      }

      this.missionBoardTile = save.getPoint("missionBoardTile", null, false);
      if (this.missionBoardTile != null) {
         this.missionBoardTile.translate(tileXOffset, tileYOffset);
      }

      this.missionBoardManager.applyLoadData(save);
      LoadData newSettlerDietSave = save.getFirstLoadDataByName("newSettlerDiet");
      if (newSettlerDietSave != null) {
         this.newSettlerDiet.applyLoadData(newSettlerDietSave);
      }

      this.newSettlerSelfManageEquipment = save.getBoolean("newSettlerSelfManageEquipment", this.newSettlerSelfManageEquipment, false);
      this.newSettlerEquipmentPreferArmorSets = save.getBoolean("newSettlerEquipmentPreferArmorSets", this.newSettlerEquipmentPreferArmorSets, false);
      LoadData newSettlerEquipmentSave = save.getFirstLoadDataByName("newSettlerEquipmentFilter");
      if (newSettlerEquipmentSave != null) {
         this.newSettlerEquipmentFilter.applyLoadData(newSettlerEquipmentSave);
      }

      this.restrictZoneIndexCounter = 0;
      this.newSettlerRestrictZoneUniqueID = save.getInt("newSettlerRestrictZoneUniqueID", this.newSettlerRestrictZoneUniqueID, false);
      this.restrictZones = new HashMap<>();
      LoadData restrictZonesSave = save.getFirstLoadDataByName("restrictZones");
      if (restrictZonesSave != null) {
         for (LoadData restrictZoneSave : restrictZonesSave.getLoadDataByName("restrictZone")) {
            try {
               RestrictZone restrictZone = new RestrictZone(this, this.restrictZoneIndexCounter++, restrictZoneSave, tileXOffset, tileYOffset);
               this.restrictZones.put(restrictZone.uniqueID, restrictZone);
            } catch (LoadDataException e) {
               System.err.println("Could not load settlement restrict zone at level " + this.getLevel().getIdentifier() + ": " + e.getMessage());
            } catch (Exception e) {
               System.err.println("Unknown error loading settlement restrict zone at level " + this.getLevel().getIdentifier());
               e.printStackTrace();
            }
         }
      }

      this.boundsManager.applySaveData(save);
      this.beds = new PointHashMap<>();
      this.rooms = new SettlementRoomsManager(this);
      this.settlers.clear();
      LoadData bedsSave = save.getFirstLoadDataByName("BEDS");
      if (bedsSave != null) {
         bedsSave.getLoadDataByName("BED").stream().filter(LoadData::isArray).forEach(c -> {
            try {
               SettlementBed bed = new SettlementBed(this, c, tileXOffset, tileYOffset);
               if (bed.isValidBed()) {
                  this.beds.put(bed.tileX, bed.tileY, bed);
               }
            } catch (Exception e) {
               System.err.println("Could not load settlement bed at level " + this.getLevel().getIdentifier());
            }
         });
      }

      this.rooms.loadSaveData(save, tileXOffset, tileYOffset);
      Stream<LoadData> settlerSaves = Stream.empty();
      settlerSaves = Stream.concat(settlerSaves, save.getLoadDataByName("SETTLER").stream().filter(LoadData::isArray));
      LoadData settlersSave = save.getFirstLoadDataByName("SETTLERS");
      if (settlersSave != null) {
         settlerSaves = Stream.concat(settlerSaves, settlersSave.getLoadDataByName("SETTLER").stream().filter(LoadData::isArray));
      }

      settlerSaves.forEach(c -> {
         try {
            LevelSettler settler = new LevelSettler(this, c, tileXOffset, tileYOffset);
            this.settlers.put(settler);
         } catch (Exception e) {
            System.err.println("Could not load settlement settler at level " + this.getLevel().getIdentifier());
            e.printStackTrace();
         }
      });
      this.groups.applyLoadData(save);
      this.storageManager.applyLoadData(save, tileXOffset, tileYOffset);
      LoadData zonesSave = save.getFirstLoadDataByName("WORKZONES");
      if (zonesSave != null) {
         this.workZones = new SettlementWorkZoneManager(this, zonesSave, tileXOffset, tileYOffset);
      } else {
         this.workZones = new SettlementWorkZoneManager(this);
      }

      LoadData statsSave = save.getFirstLoadDataByName("STATS");
      if (statsSave != null) {
         this.stats.applyLoadData(statsSave);
      }
   }

   public void tickTile(int tileX, int tileY) {
      this.addOrValidateBed(tileX, tileY, true);
      this.rooms.findAndCalculateRoom(tileX, tileY);
   }

   public SettlementBed addOrValidateBed(int tileX, int tileY) {
      return this.addOrValidateBed(tileX, tileY, false);
   }

   public SettlementBed addOrValidateBed(int tileX, int tileY, boolean addOnlyPlayerPlaced) {
      if (!this.beds.containsKey(tileX, tileY)) {
         if (addOnlyPlayerPlaced && !this.getLevel().objectLayer.isPlayerPlaced(tileX, tileY)) {
            return null;
         } else {
            SettlementBed bed = new SettlementBed(this, tileX, tileY);
            if (bed.isValidBed()) {
               this.beds.put(tileX, tileY, bed);
               return bed;
            } else {
               return null;
            }
         }
      } else {
         SettlementBed bed = this.beds.get(tileX, tileY);
         if (bed.isValidBed()) {
            return bed;
         }

         LevelSettler settler = bed.getSettler();
         if (settler != null) {
            settler.assignBed(null);
         }

         this.beds.remove(tileX, tileY);
         return null;
      }
   }

   public boolean isMobPartOf(SettlerMob settler) {
      if (settler == null) {
         return false;
      }

      LevelSettler levelSettler = this.settlers.get(settler.getMob().getUniqueID());
      return levelSettler != null ? levelSettler.getMob() == settler : false;
   }

   public boolean isRaidOngoing() {
      if (this.currentRaid == 0) {
         return false;
      }

      LevelEvent lastEvent = this.getLevel().entityManager.events.get(this.currentRaid, false);
      return lastEvent != null && !lastEvent.isOver();
   }

   public Iterable<LevelSettler> getSettlers() {
      return this.settlers.values();
   }

   public Stream<LevelSettler> streamSettlers() {
      return this.settlers.values().stream();
   }

   public void serverTick() {
      Performance.record(this.getLevel().tickManager(), "settlementTick", () -> {
         this.ensureRegionsLoaded(false);
         this.boundsManager.tickTiles();

         for (LevelSettler settler : this.settlers.values()) {
            settler.serverTick();
         }

         this.settlementTickBuffer++;
         if (this.settlementTickBuffer >= 20) {
            this.settlementTickBuffer = 0;
            this.settlementTick();
         }
      });

      for (Class<? extends ContainerEvent> eventClass : this.eventsToSend) {
         try {
            Constructor<? extends ContainerEvent> constructor = eventClass.getConstructor(ServerSettlementData.class);
            ContainerEvent event = constructor.newInstance(this);
            event.applyAndSendToClientsAt(this.getLevel());
         } catch (NoSuchMethodException e) {
            System.err
               .println("Could not send " + eventClass.getSimpleName() + " from SettlementLevelData, missing constructor with SettlementLevelData parameter");
         } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            System.err
               .println(
                  "Error constructing " + eventClass.getSimpleName() + " from SettlementLevelData: " + e.getClass().getSimpleName() + ", " + e.getMessage()
               );
         }
      }

      this.eventsToSend.clear();
   }

   public void ensureRegionsLoaded(boolean forceLoad) {
      Rectangle regionRectangle = this.boundsManager.getLoadedRegionRectangle();
      if ((this.getLevel().getWorldSettings().unloadSettlements || !this.networkData.hasOwner()) && !forceLoad) {
         PointHashSet keepLoaded = new PointHashSet();
         Region firstRegion = null;

         for (int regionX = regionRectangle.x; regionX < regionRectangle.x + regionRectangle.width; regionX++) {
            for (int regionY = regionRectangle.y; regionY < regionRectangle.y + regionRectangle.height; regionY++) {
               if (this.getLevel().regionManager.isRegionWithinBounds(regionX, regionY)) {
                  if (keepLoaded == null) {
                     Region region = this.getLevel().regionManager.getRegion(regionX, regionY, true);
                     if (region != null) {
                        region.unloadRegionBuffer.keepLoaded(1);
                        region.syncSaveFileBuffer(firstRegion);
                     }
                  } else {
                     Region region = this.getLevel().regionManager.getRegion(regionX, regionY, false);
                     if (region == null) {
                        keepLoaded.add(regionX, regionY);
                     } else {
                        if (!region.unloadRegionBuffer.shouldUnload(1)) {
                           for (Point cachedRegionPosition : keepLoaded) {
                              Region cachedRegion = this.getLevel().regionManager.getRegion(cachedRegionPosition.x, cachedRegionPosition.y, true);
                              cachedRegion.unloadRegionBuffer.keepLoaded(1);
                           }

                           keepLoaded = null;
                        } else {
                           keepLoaded.add(regionX, regionY);
                        }

                        if (firstRegion == null) {
                           firstRegion = region;
                        } else {
                           region.syncSaveFileBuffer(firstRegion);
                        }
                     }
                  }
               }
            }
         }
      } else {
         this.getLevel().unloadLevelBuffer = 0;
         Region firstRegion = null;

         for (int regionX = regionRectangle.x; regionX < regionRectangle.x + regionRectangle.width; regionX++) {
            for (int regionY = regionRectangle.y; regionY < regionRectangle.y + regionRectangle.height; regionY++) {
               if (this.getLevel().regionManager.isRegionWithinBounds(regionX, regionY)) {
                  Region region = this.getLevel().regionManager.getRegion(regionX, regionY, true);
                  if (region != null) {
                     region.unloadRegionBuffer.keepLoaded();
                     if (firstRegion == null) {
                        firstRegion = region;
                     } else {
                        region.syncSaveFileBuffer(firstRegion);
                     }
                  }
               }
            }
         }
      }
   }

   public void settlementTick() {
      boolean isActive = this.networkData.hasOwner() && !this.networkData.isDisbanding();
      if (this.lastRaidFrequencySetting != this.getLevel().getWorldSettings().raidFrequency) {
         this.resetNextRaidTimer(false, true);
         this.lastRaidFrequencySetting = this.getLevel().getWorldSettings().raidFrequency;
      }

      Performance.record(this.getLevel().tickManager(), "tickSettlement", () -> {
         this.settlementTicks++;
         if (isActive) {
            this.workZones.tickSecond();
         }

         if (isActive && (this.settlementTicks + 5L) % 10L == 0L) {
            this.tickJobs();
         }

         if (this.settlementTicks % 10L == 0L) {
            this.tickBedsValid();
            if (isActive) {
               this.hasNonAFKTeamMembers = this.networkData.streamTeamMembers().anyMatch(c -> !c.isAFK());
            } else {
               this.hasNonAFKTeamMembers = false;
            }

            this.storyObjectives.fixTeamMembersCompleted();
            if (isActive && !this.networkData.isRaidActive() && GameRandom.globalRandom.getChance(0.16F)) {
               this.tickFindSettlerBeds();
               this.tickSpawnInSettlers();
               this.tickAchievements();
            }

            HashSet<Integer> removedSettlers = new HashSet<>();

            for (Entry<Integer, LevelSettler> entry : this.settlers.entrySet()) {
               LevelSettler settler = entry.getValue();
               SettlerMob settlerMob = settler.getMob();
               if (settlerMob == null) {
                  settler.notFoundBuffer++;
                  if (settler.notFoundBuffer >= 6) {
                     settler.notFoundBuffer = 0;
                     SettlementBed bed = settler.getBed();
                     if (bed != null) {
                        bed.clearSettler();
                     }

                     removedSettlers.add(entry.getKey());
                     this.sendEvent(SettlementSettlersChangedEvent.class);
                     this.groups.onSettlerRemoved(settler.mobUniqueID);
                     System.out.println("Removed not found settler " + settler.mobUniqueID);
                  } else {
                     System.out.println("Could not find settler " + settler.mobUniqueID + " in settlement. Removing soon...");
                  }
               } else {
                  settler.notFoundBuffer = 0;
                  if (!settlerMob.isSettlerWithinSettlementLoadedRegions(this.networkData)) {
                     SettlersWorldData settlersData = SettlersWorldData.getSettlersData(this.getLevel().getServer());
                     settlersData.returnIfShould(settler.mobUniqueID, this);
                  }
               }
            }

            for (int settlerUniqueID : removedSettlers) {
               this.settlers.remove(settlerUniqueID);
            }

            HashSet<Integer> processed = new HashSet<>();

            for (SettlementBed bed : this.beds.values()) {
               if (bed.getSettler() != null && processed.contains(bed.getSettler().mobUniqueID)) {
                  bed.clearSettler();
                  bed.isLocked = false;
                  this.sendEvent(SettlementSettlersChangedEvent.class);
               }

               if (bed.getSettler() != null) {
                  processed.add(bed.getSettler().mobUniqueID);
               }
            }
         }

         for (LevelSettler settler : this.settlers.values()) {
            settler.updateHome();
            settler.settlementTick();
         }

         if (isActive) {
            this.tickNextVisitor();
            this.tickNextRaid();
         }
      });
   }

   protected void tickJobs() {
      this.hasInvalidPlacedItemsInStorage = false;
      Level level = this.getLevel();
      Performance.record(
         level.tickManager(),
         "tickJobs",
         () -> {
            SettlementStorageRecords newStorageRecords = new SettlementStorageRecords(level);
            ArrayList<ServerSettlementData.TempRequest> tempRequests = new ArrayList<>();
            ArrayList<ServerSettlementData.TempStorage> tempOutputs = new ArrayList<>();
            this.storageManager.clearInvalids();
            Performance.record(level.tickManager(), "getWorkstation", () -> {
               for (SettlementWorkstation workstation : this.storageManager.getWorkstations()) {
                  workstation.updateAdjacentSolidState();
                  SettlementRequestInventory fuelInventory = workstation.getFuelInventory();
                  if (fuelInventory != null) {
                     fuelInventory.updateAdjacentSolidState();
                     if (!fuelInventory.isAllAdjacentSolid()) {
                        tempRequests.add(new ServerSettlementData.TempRequest(fuelInventory));
                     }

                     fuelInventory.removeInvalidPickups();
                     fuelInventory.dropOffSimulation.update();
                  }

                  SettlementInventory inputInventory = workstation.getProcessingInputInventory();
                  if (inputInventory != null) {
                     inputInventory.removeInvalidPickups();
                     inputInventory.dropOffSimulation.update();
                  }

                  SettlementInventory outputInventory = workstation.getProcessingOutputInventory();
                  if (outputInventory != null) {
                     tempOutputs.add(new ServerSettlementData.TempStorage(outputInventory));
                     outputInventory.removeInvalidPickups();
                     outputInventory.dropOffSimulation.update();
                  }

                  level.jobsLayer.addJob(new UseWorkstationLevelJob(workstation, () -> this.storageManager.hasWorkstation(workstation)));
               }
            });
            ArrayList<ServerSettlementData.TempStorage> tempStorage = new ArrayList<>(this.storageManager.getStorage().size());
            Performance.record(level.tickManager(), "getStorage", () -> {
               for (SettlementInventory currentStorage : this.storageManager.getStorage()) {
                  currentStorage.updateAdjacentSolidState();
                  ServerSettlementData.TempStorage temp = new ServerSettlementData.TempStorage(currentStorage);
                  SettlementRequestInventory fuelInventory = currentStorage.getFuelInventory();
                  if (fuelInventory != null) {
                     if (!currentStorage.isAllAdjacentSolid()) {
                        tempRequests.add(new ServerSettlementData.TempRequest(fuelInventory));
                     }

                     fuelInventory.removeInvalidPickups();
                     fuelInventory.dropOffSimulation.update();
                  }

                  tempStorage.add(temp);
                  temp.storage.removeInvalidPickups();
                  temp.storage.dropOffSimulation.update();
               }
            });
            Performance.record(level.tickManager(), "sortStorage", () -> tempStorage.sort(Comparator.comparingInt(e -> -e.priority)));
            Performance.record(
               level.tickManager(),
               "removeOldStorage",
               () -> {
                  for (ServerSettlementData.TempStorage storage : tempStorage) {
                     level.jobsLayer
                        .streamJobsInTile(storage.storage.tileX, storage.storage.tileY)
                        .filter(j -> j.getID() == LevelJobRegistry.haulFromID)
                        .forEach(TileLevelJob::remove);
                  }
               }
            );
            Performance.record(
               level.tickManager(),
               "removeOldOutputs",
               () -> {
                  for (ServerSettlementData.TempStorage storage : tempOutputs) {
                     level.jobsLayer
                        .streamJobsInTile(storage.storage.tileX, storage.storage.tileY)
                        .filter(j -> j.getID() == LevelJobRegistry.haulFromID)
                        .forEach(TileLevelJob::remove);
                  }
               }
            );
            Performance.record(level.tickManager(), "handleRequests", () -> {
               for (ServerSettlementData.TempRequest from : tempRequests) {
                  LinkedList<HaulFromLevelJob> jobs = new LinkedList<>();

                  for (ServerSettlementData.TempStorage to : tempStorage) {
                     this.addInvalidDropOffPositions(from, to, jobs);
                  }

                  for (ServerSettlementData.TempRequest to : tempRequests) {
                     if (from != to) {
                        this.addInvalidDropOffPositions(from, to, jobs);
                     }
                  }

                  for (HaulFromLevelJob job : jobs) {
                     level.jobsLayer.addJob(job, true);
                  }
               }
            });
            Performance.record(level.tickManager(), "handleOutputs", () -> {
               for (ServerSettlementData.TempStorage from : tempOutputs) {
                  LinkedList<HaulFromLevelJob> jobs = new LinkedList<>();
                  Performance.record(level.tickManager(), "handleStorageJobs", () -> this.handleStorageJobs(from, jobs, tempStorage, null, false));

                  for (HaulFromLevelJob job : jobs) {
                     level.jobsLayer.addJob(job, true);
                  }
               }
            });
            Performance.record(level.tickManager(), "handleStorage", () -> {
               for (int i = 0; i < tempStorage.size(); i++) {
                  ServerSettlementData.TempStorage from = tempStorage.get(i);
                  if (from.storage.isAllAdjacentSolid()) {
                     from.storage.haulFromLevelJobs = new LinkedList<>();
                  } else {
                     LinkedList<HaulFromLevelJob> jobs = new LinkedList<>();
                     Performance.record(
                        level.tickManager(), "handleStorageJobs", () -> this.handleStorageJobs(from, jobs, tempStorage, newStorageRecords, true)
                     );
                     int finalI = i;
                     Performance.record(level.tickManager(), "addDropOffs", () -> {
                        for (int j = 0; j < finalI; j++) {
                           ServerSettlementData.TempStorage to = tempStorage.get(j);
                           if (!to.storage.isAllAdjacentSolid() && to.priority > from.priority) {
                              this.addDropOffPositions(from, to, jobs);
                           }
                        }
                     });
                     Performance.record(level.tickManager(), "addJobs", () -> {
                        for (HaulFromLevelJob job : jobs) {
                           level.jobsLayer.addJob(job, true);
                        }
                     });
                     from.storage.haulFromLevelJobs = jobs;
                  }
               }
            });
            Performance.record(
               level.tickManager(),
               "setupRequestJobs",
               () -> {
                  for (SettlementInventory inventory : this.storageManager.getStorage()) {
                     if (inventory.isAllAdjacentSolid()) {
                        level.jobsLayer
                           .streamJobsInTile(inventory.tileX, inventory.tileY)
                           .filter(j -> j.getID() == LevelJobRegistry.hasStorageID)
                           .forEach(TileLevelJob::remove);
                     } else {
                        level.jobsLayer.addJob(new HasStorageLevelJob(inventory, () -> this.storageManager.hasInventory(inventory)), true);
                     }
                  }
               }
            );
            Performance.record(level.tickManager(), "tickWorkZones", () -> this.workZones.tickJobs());

            for (ServerSettlementData.TempRequest request : tempRequests) {
               request.requestInventory.addHaulJobs(level, newStorageRecords, request.priority);
            }

            this.storageRecords = newStorageRecords;
            LowFoodSettlementNotification.tickShow(this);
         }
      );
      this.hasTickedFirstJobs = true;
   }

   private void handleStorageJobs(
      ServerSettlementData.TempStorage from,
      LinkedList<HaulFromLevelJob> jobs,
      ArrayList<ServerSettlementData.TempStorage> otherStorage,
      SettlementStorageRecords records,
      boolean markInvalidPlaceItemsInStorage
   ) {
      Level level = this.getLevel();
      InventoryRange simulateRemove = new InventoryRange(from.range.inventory.copy(), from.range.startSlot, from.range.endSlot);

      for (int slot = from.range.endSlot; slot >= from.range.startSlot; slot--) {
         InventoryItem item = simulateRemove.inventory.getItem(slot);
         if (item != null) {
            int finalSlot = slot;
            Performance.record(level.tickManager(), "handlePickups", () -> {
               for (SettlementStoragePickupSlot pickup : from.storage.pickupSlots.get(finalSlot)) {
                  item.setAmount(item.getAmount() - pickup.item.getAmount());
               }
            });
            if (records != null) {
               Performance.record(level.tickManager(), "setRecords", () -> {
                  int accessibleAmount = item.getAmount();
                  if (accessibleAmount > 0) {
                     records.add(item, new SettlementStorageRecord(from.storage, finalSlot, item, accessibleAmount));
                  }
               });
            }

            if (item.getAmount() > 0) {
               int remove = Performance.record(
                  level.tickManager(),
                  "getRemoveAmount",
                  () -> Math.min(from.storage.getFilter().getRemoveAmount(level, item, simulateRemove), item.getAmount())
               );
               if (remove > 0) {
                  if (markInvalidPlaceItemsInStorage) {
                     this.hasInvalidPlacedItemsInStorage = true;
                  }

                  simulateRemove.inventory.setAmount(slot, item.getAmount() - remove);
                  HaulFromLevelJob job = jobs.stream().filter(e -> e.item.equals(level, item, true, false, "pickups")).findFirst().orElse(null);
                  if (job != null) {
                     job.item.setAmount(job.item.getAmount() + remove);
                  } else {
                     jobs.add(new HaulFromLevelJob(from.storage, item.copy(remove)));
                  }
               }
            }
         }
      }

      Performance.record(level.tickManager(), "findDropOffs", () -> Performance.record(level.tickManager(), "storage", () -> {
         for (ServerSettlementData.TempStorage to : otherStorage) {
            if (from != to) {
               for (HaulFromLevelJob jobx : jobs) {
                  int addAmount = to.storage.canAddFutureDropOff(jobx.item);
                  if (addAmount > 0) {
                     jobx.dropOffPositions.add(new HaulFromLevelJob.HaulPosition(to.storage, to.priority, addAmount));
                  }
               }
            }
         }
      }));
   }

   private void addDropOffPositions(ServerSettlementData.TempStorage from, ServerSettlementData.TempStorage to, LinkedList<HaulFromLevelJob> jobs) {
      Level level = this.getLevel();
      LinkedList<InventoryItem> possibleItems = new LinkedList<>();

      for (int slot = from.range.endSlot; slot >= from.range.startSlot; slot--) {
         InventoryItem item = from.range.inventory.getItem(slot);
         if (item != null) {
            int amountLeft = item.getAmount();
            PerformanceWrapper test0 = Performance.wrapTimer(level.tickManager(), "part1");

            for (SettlementStoragePickupSlot pickup : from.storage.pickupSlots.get(slot)) {
               amountLeft -= pickup.item.getAmount();
               if (amountLeft <= 0) {
                  break;
               }
            }

            test0.end();
            if (amountLeft > 0 && to.storage.getFilter().matchesItem(item)) {
               InventoryItem lastItem = possibleItems.stream().filter(e -> e.equals(level, item, true, false, "pickups")).findFirst().orElse(null);
               if (lastItem != null) {
                  lastItem.setAmount(lastItem.getAmount() + amountLeft);
               } else {
                  possibleItems.add(item.copy(amountLeft));
               }
            }
         }
      }

      for (InventoryItem item : possibleItems) {
         int addAmount = to.storage.canAddFutureDropOff(item);
         if (addAmount > 0) {
            HaulFromLevelJob job = jobs.stream().filter(e -> e.item.equals(level, item, true, false, "pickups")).findFirst().orElse(null);
            if (job != null) {
               if (job.dropOffPositions.stream().noneMatch(e -> e.storage == to.storage)) {
                  job.dropOffPositions.add(new HaulFromLevelJob.HaulPosition(to.storage, to.priority, addAmount));
               }
            } else {
               HaulFromLevelJob newJob = new HaulFromLevelJob(from.storage, item.copy());
               newJob.dropOffPositions.add(new HaulFromLevelJob.HaulPosition(to.storage, to.priority, addAmount));
               jobs.add(newJob);
            }
         }
      }
   }

   private void addInvalidDropOffPositions(ServerSettlementData.TempStorage from, ServerSettlementData.TempStorage to, LinkedList<HaulFromLevelJob> jobs) {
      Level level = this.getLevel();
      LinkedList<InventoryItem> possibleItems = new LinkedList<>();

      for (int slot = from.range.endSlot; slot >= from.range.startSlot; slot--) {
         InventoryItem item = from.range.inventory.getItem(slot);
         if (item != null && !from.range.inventory.isItemValid(slot, item)) {
            int amountLeft = item.getAmount();

            for (SettlementStoragePickupSlot pickup : from.storage.pickupSlots.get(slot)) {
               amountLeft -= pickup.item.getAmount();
               if (amountLeft <= 0) {
                  break;
               }
            }

            if (amountLeft > 0 && to.storage.getFilter().matchesItem(item)) {
               InventoryItem lastItem = possibleItems.stream().filter(e -> e.equals(level, item, true, false, "pickups")).findFirst().orElse(null);
               if (lastItem != null) {
                  lastItem.setAmount(lastItem.getAmount() + amountLeft);
               } else {
                  possibleItems.add(item.copy(amountLeft));
               }
            }
         }
      }

      for (InventoryItem item : possibleItems) {
         int addAmount = to.storage.canAddFutureDropOff(item);
         if (addAmount > 0) {
            HaulFromLevelJob job = jobs.stream().filter(e -> e.item.equals(level, item, true, false, "pickups")).findFirst().orElse(null);
            if (job != null) {
               if (job.dropOffPositions.stream().noneMatch(e -> e.storage == to.storage)) {
                  job.dropOffPositions.add(new HaulFromLevelJob.HaulPosition(to.storage, to.priority, addAmount));
               }
            } else {
               HaulFromLevelJob newJob = new HaulFromLevelJob(from.storage, item.copy());
               newJob.dropOffPositions.add(new HaulFromLevelJob.HaulPosition(to.storage, to.priority, addAmount));
               jobs.add(newJob);
            }
         }
      }
   }

   public void clearOutsideBounds() {
      Rectangle tileRectangle = this.boundsManager.getTileRectangle();
      PointHashSet invalidBeds = new PointHashSet();

      for (SettlementBed bed : this.beds.values()) {
         if (!tileRectangle.contains(bed.tileX, bed.tileY)) {
            bed.clearSettler();
            invalidBeds.add(bed.tileX, bed.tileY);
         }
      }

      if (!invalidBeds.isEmpty()) {
         for (Point invalidPos : invalidBeds) {
            this.beds.remove(invalidPos.x, invalidPos.y);
         }

         this.sendEvent(SettlementSettlersChangedEvent.class);
      }

      for (RestrictZone zone : this.restrictZones.values()) {
         zone.limitZoneToBounds(tileRectangle);
      }

      this.rooms.clearOutsideBounds(tileRectangle);
      this.storageManager.clearOutsideBounds(tileRectangle);
      this.workZones.clearOutsideBounds(tileRectangle);
      if (this.homestoneTile != null && tileRectangle.contains(this.homestoneTile.x, this.homestoneTile.y)) {
         this.homestoneTile = null;
      }

      if (this.missionBoardTile != null && tileRectangle.contains(this.missionBoardTile.x, this.missionBoardTile.y)) {
         this.missionBoardTile = null;
      }
   }

   public void onDisbanded() {
      for (LevelSettler settler : this.settlers.values()) {
         settler.moveOut();
      }

      this.settlers.clear();
      this.beds.clear();
      this.sendEvent(SettlementSettlersChangedEvent.class);
   }

   protected void tickBedsValid() {
      PointHashSet invalidBeds = new PointHashSet();
      PointHashSet occupiedBeds = new PointHashSet();

      for (SettlementBed bed : this.beds.values()) {
         if (!bed.isValidBed()) {
            bed.clearSettler();
            invalidBeds.add(bed.tileX, bed.tileY);
         } else if (!occupiedBeds.contains(bed.tileX, bed.tileY)) {
            if (bed.getSettler() != null) {
               if (!bed.getSettler().settler.isValidBed(bed)) {
                  bed.clearSettler();
               } else {
                  occupiedBeds.add(bed.tileX, bed.tileY);
               }
            }
         } else if (bed.getSettler() != null) {
            bed.clearSettler();
         }
      }

      if (!invalidBeds.isEmpty()) {
         for (Point invalidPos : invalidBeds) {
            this.beds.remove(invalidPos.x, invalidPos.y);
         }

         this.sendEvent(SettlementSettlersChangedEvent.class);
      }
   }

   protected void tickFindSettlerBeds() {
      for (LevelSettler settler : this.settlers.values()) {
         if (settler.getBed() == null) {
            SettlerMob mob = settler.getMob();
            if (mob != null && mob.isSettlerWithinSettlement(this.networkData)) {
               SettlementBed bed = this.findBedForSettler(settler);
               if (bed != null) {
                  this.moveSettler(settler, bed, null);
               } else {
                  mob.makeSettler(this, settler);
               }
            }
         }
      }
   }

   protected void tickSpawnInSettlers() {
      for (Settler settler : SettlerRegistry.getSettlers()) {
         if (settler.canSpawnInSettlement(this, this.networkData.getStats())) {
            SettlerMob newMob = settler.getNewSettlerMob(this);
            if (newMob != null) {
               newMob.setSettlerSeed(GameRandom.globalRandom.nextInt(), false);
               this.getLevel().entityManager.mobs.add(newMob.getMob());
               this.moveIn(new LevelSettler(this, newMob));
               System.out.println("Settler " + settler.getStringID() + " moved automatically in at " + this.getLevel().getIdentifier());
               break;
            }
         }
      }
   }

   private void tickAchievements() {
      Set<Integer> settlerIDs = SettlerRegistry.streamSettlers()
         .filter(settlerx -> settlerx.isPartOfCompleteHost)
         .map(Settler::getID)
         .collect(Collectors.toSet());

      for (LevelSettler settler : this.settlers.values()) {
         settlerIDs.remove(settler.settler.getID());
      }

      if (settlerIDs.isEmpty()) {
         this.networkData
            .streamTeamMembersAndInSettlement()
            .filter(ServerClient::achievementsLoaded)
            .forEach(c -> c.achievements().COMPLETE_HOST.markCompleted(c));
      }

      this.boundsManager.checkExpansionAchievements();
      int qualityOfLifeCount = 0;
      boolean hasRichCharacter = false;

      for (LevelSettler settler : this.settlers.values()) {
         SettlerMob settlerMob = settler.getMob();
         if (settlerMob instanceof HumanMob) {
            HumanMob humanMob = (HumanMob)settlerMob;
            if (!hasRichCharacter && humanMob.getPersonalities().size() >= 6) {
               this.networkData
                  .streamTeamMembersAndInSettlement()
                  .filter(ServerClient::achievementsLoaded)
                  .forEach(c -> c.achievements().RICH_CHARACTER.markCompleted(c));
               hasRichCharacter = true;
            }

            if (qualityOfLifeCount < 10 && humanMob.getSettlerQualityOfLife() >= 6) {
               if (++qualityOfLifeCount == 10) {
                  this.networkData
                     .streamTeamMembersAndInSettlement()
                     .filter(ServerClient::achievementsLoaded)
                     .forEach(c -> c.achievements().CLOUD_TEN.markCompleted(c));
               }
            }

            if (qualityOfLifeCount == 10 && hasRichCharacter) {
               break;
            }
         }
      }
   }

   public void onReturned(int mobUniqueID) {
      LevelSettler settler = this.getSettler(mobUniqueID);
      if (settler != null) {
         this.walkIn(settler);
         this.sendEvent(SettlementSettlersChangedEvent.class);
      }
   }

   public void walkIn(LevelSettler levelSettler) {
      SettlerMob mob = levelSettler.getMob();
      levelSettler.updateHome();
      if (mob instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)mob;
         if (humanMob.home != null) {
            humanMob.moveIn(humanMob.home.x, humanMob.home.y, true);
         }
      }
   }

   public void moveIn(LevelSettler settler) {
      SettlementBed bed = this.findBedForSettler(settler);
      if (bed == null || !this.moveSettler(settler, bed, null)) {
         this.moveSettler(settler, null, null);
      }
   }

   public boolean canMoveIn(LevelSettler settler, int maxHomelessSettlers) {
      SettlementBed bed = this.findBedForSettler(settler);
      return bed != null || maxHomelessSettlers < 0 || this.settlers.values().stream().filter(s -> s.getBed() == null).count() < maxHomelessSettlers;
   }

   public SettlementBed findBedForSettler(LevelSettler settler) {
      SettlementBed best = null;
      int bestValue = Integer.MIN_VALUE;

      for (SettlementBed bed : this.beds.values()) {
         if (bed.getSettler() == null && !bed.isLocked && settler.settler.isValidBed(bed)) {
            int happinessScore = bed.getHappinessScore();
            if (best == null || bestValue < happinessScore) {
               best = bed;
               bestValue = happinessScore;
            }
         }
      }

      return best;
   }

   public boolean moveSettler(LevelSettler settler, SettlementBed bed, ServerClient client) {
      if (settler.data != this) {
         throw new IllegalArgumentException("Settler settlement did not match this");
      }

      boolean firstAdd = false;
      if (!this.settlers.containsKey(settler.mobUniqueID)) {
         this.settlers.put(settler);
         firstAdd = true;
      }

      if (bed == null) {
         if (settler.getBed() == null && !firstAdd) {
            return false;
         }

         settler.assignBed(null);
         this.sendEvent(SettlementSettlersChangedEvent.class);
         return true;
      } else {
         if (this.beds.get(bed.tileX, bed.tileY) != bed) {
            throw new IllegalArgumentException("Bed was not in this settlement. Use SettlementLevelData.addOrValidateBed to get a bed");
         }

         if (bed.getSettler() == settler) {
            return false;
         }

         GameMessage canUse = settler.settler.canUseBed(bed);
         if (canUse != null) {
            if (client != null) {
               client.sendChatMessage(canUse);
            }

            return false;
         } else {
            LevelSettler currentSettler = bed.getSettler();
            if (currentSettler != null) {
               SettlementBed prevBed = settler.getBed();
               if (prevBed != null && prevBed.isValidBed()) {
                  GameMessage prevCanUse = currentSettler.settler.canUseBed(prevBed);
                  if (prevCanUse == null) {
                     currentSettler.assignBed(prevBed);
                  } else {
                     currentSettler.assignBed(null);
                  }
               } else {
                  currentSettler.assignBed(null);
               }
            }

            settler.assignBed(bed);
            if (client != null) {
               client.storyManager.streamObjectives(AssignBedObjectiveListener.class).forEach(AssignBedObjectiveListener::onBedAssigned);
            }

            this.sendEvent(SettlementSettlersChangedEvent.class);
            return true;
         }
      }
   }

   public boolean moveSettler(int mobUniqueID, SettlementBed bed, ServerClient client) {
      LevelSettler settler = this.getSettler(mobUniqueID);
      if (settler != null) {
         return this.moveSettler(settler, bed, client);
      }

      if (client != null) {
         client.sendChatMessage(new LocalMessage("settlement", "notsettler"));
      }

      return false;
   }

   public boolean moveSettler(int mobUniqueID, int tileX, int tileY, ServerClient client) {
      SettlementBed bed = this.addOrValidateBed(tileX, tileY);
      return bed != null ? this.moveSettler(mobUniqueID, bed, client) : false;
   }

   public boolean lockNoSettler(int tileX, int tileY, ServerClient client) {
      SettlementBed bed = this.addOrValidateBed(tileX, tileY);
      if (bed != null) {
         if (bed.getSettler() != null) {
            bed.clearSettler();
         }

         if (!bed.isLocked) {
            bed.isLocked = true;
            this.sendEvent(SettlementSettlersChangedEvent.class);
         }

         return true;
      } else {
         return false;
      }
   }

   public LevelSettler getSettler(int mobUniqueID) {
      return this.settlers.get(mobUniqueID);
   }

   public boolean hasSettler(int mobUniqueID) {
      return this.settlers.containsKey(mobUniqueID);
   }

   public boolean removeSettler(int mobUniqueID, ServerClient client) {
      LevelSettler settler = this.settlers.get(mobUniqueID);
      if (settler != null) {
         SettlerMob mob = settler.getMob();
         if (mob != null) {
            mob.moveOut();
         }

         if (settler.getBed() != null) {
            settler.getBed().clearSettler();
         }

         this.settlers.remove(mobUniqueID);
         this.groups.onSettlerRemoved(mobUniqueID);
         this.sendEvent(SettlementSettlersChangedEvent.class);
         return true;
      } else {
         if (client != null) {
            client.sendChatMessage(new LocalMessage("settlement", "notsettler"));
         }

         return false;
      }
   }

   public void renameSettler(int mobUniqueID, String name) {
      LevelSettler settler = this.settlers.get(mobUniqueID);
      if (settler != null) {
         SettlerMob mob = settler.getMob();
         if (mob != null && !mob.getSettlerName().equals(name)) {
            mob.setSettlerName(name);
            this.getLevel().getServer().network.sendToClientsWithEntity(new PacketSpawnMob(mob.getMob()), mob.getMob());
         }
      }
   }

   public boolean hasSettler(Settler settler) {
      return this.settlers.values().stream().anyMatch(s -> s.settler == settler);
   }

   public int getSettlerCount(Settler settler) {
      return (int)this.settlers.values().stream().filter(s -> s.settler == settler).count();
   }

   public int countSettlersWithBed() {
      return (int)this.settlers.values().stream().filter(s -> s.getBed() != null).count();
   }

   public int countSettlersWithoutBed() {
      return (int)this.settlers.values().stream().filter(s -> s.getBed() == null).count();
   }

   public int countTotalSettlers() {
      return this.settlers.size();
   }

   public Iterable<SettlementBed> getBeds() {
      return this.beds.values();
   }

   public Point getHomestoneTile() {
      if (this.homestoneTile != null) {
         this.getLevel().regionManager.ensureTileIsLoaded(this.homestoneTile.x, this.homestoneTile.y);
         if (!this.getLevel().getObject(this.homestoneTile.x, this.homestoneTile.y).getStringID().equals("homestone")) {
            this.homestoneTile = null;
         }
      }

      return this.homestoneTile;
   }

   public void setHomestoneTile(Point tile) {
      this.homestoneTile = tile;
   }

   public Point getMissionBoardTile() {
      if (this.missionBoardTile != null && !this.getLevel().getObject(this.missionBoardTile.x, this.missionBoardTile.y).getStringID().equals("missionboard")) {
         this.missionBoardTile = null;
      }

      return this.missionBoardTile;
   }

   public boolean hasFlag() {
      return this.networkData.hasFlag();
   }

   public SettlementFlagObjectEntity getFlagObjectEntity() {
      return this.networkData.getFlagObjectEntity();
   }

   public Point getFlagTile() {
      return new Point(this.networkData.getTileX(), this.networkData.getTileY());
   }

   public int getFlagTier() {
      return this.networkData.getFlagTier();
   }

   public int getMaxWaystones() {
      return this.storyObjectives.getTotalMajorObjectivesCompleted();
   }

   public ArrayList<Waystone> getWaystones() {
      return this.waystones;
   }

   public void setMissionBoardTile(Point tile) {
      this.missionBoardTile = tile;
   }

   public void reduceRaidCounter() {
      this.raidsCounter = Math.max(0, this.raidsCounter - 1);
      this.resetNextRaidTimer(false, true);
   }

   public GameMessage getSettlementName() {
      return this.networkData.getSettlementName();
   }

   public boolean spawnNextVisitor() {
      SettlementVisitorSpawner newArrive = null;
      if (deadRecruitsOdds != null && deadRecruitsOdds.canSpawn(this)) {
         newArrive = deadRecruitsOdds.getNewVisitorSpawner(this);
         this.nextVisitorIsRecruit = false;
      }

      if (newArrive != null || !this.nextVisitorIsRecruit) {
         this.nextVisitorIsRecruit = true;
      } else if (visitorRecruitsOdds != null && visitorRecruitsOdds.canSpawn(this)) {
         newArrive = visitorRecruitsOdds.getNewVisitorSpawner(this);
         this.nextVisitorIsRecruit = false;
      }

      if (newArrive == null) {
         newArrive = getNewVisitorSpawner(GameRandom.globalRandom, this);
      }

      if (newArrive != null && newArrive.mob != null) {
         return this.spawnVisitor(newArrive);
      }

      System.err.println("Error spawning new settlement visitor (null)");
      return false;
   }

   public boolean spawnVisitor(SettlementVisitorSpawner arrive) {
      Level level = this.getLevel();
      Point spawnPos = arrive.findRandomSpawnLocation(this);
      if (spawnPos != null) {
         arrive.mob.setLevel(level);
         arrive.mob.startVisitor(this);
         level.entityManager.addMob(arrive.mob, spawnPos.x, spawnPos.y);
         this.currentVisitorUniqueID = arrive.mob.getUniqueID();
         arrive.onSpawned(level, this, spawnPos);
         if (arrive.odds != null) {
            this.lastVisitorIdentifier = arrive.odds.identifier;
         } else {
            this.lastVisitorIdentifier = null;
         }

         this.resetNextVisitorTime(false, false);
         GameMessage arriveMessage = arrive.getArriveMessage(this);
         if (arriveMessage != null) {
            this.networkData.streamTeamMembersAndInSettlement().forEach(c -> c.sendChatMessage(arriveMessage));
         }

         return true;
      } else {
         return false;
      }
   }

   protected void tickNextVisitor() {
      this.nextVisitorTimer--;
      if (this.nextVisitorTimer <= 0L) {
         if (this.currentVisitorUniqueID != 0) {
            Mob mob = this.getLevel().entityManager.mobs.get(this.currentVisitorUniqueID, false);
            if (mob instanceof HumanMob && ((HumanMob)mob).isVisitor()) {
               this.resetNextVisitorTime(false, false);
               return;
            }
         }

         this.resetNextVisitorTime(!this.spawnNextVisitor(), false);
      }
   }

   public void onVisitorLeave(HumanMob visitor) {
      this.onVisitorLeave(visitor, new LocalMessage("settlement", "travelingleave", "mob", visitor.getDisplayName(), "settlement", this.getSettlementName()));
   }

   public void onVisitorLeave(HumanMob visitor, GameMessage leaveMessage) {
      if (leaveMessage != null) {
         this.networkData
            .streamTeamMembersAndInSettlement()
            .forEach(
               c -> c.sendChatMessage(new LocalMessage("settlement", "travelingleave", "mob", visitor.getDisplayName(), "settlement", this.getSettlementName()))
            );
      }

      this.resetNextVisitorTime(false, false);
   }

   public void resetNextVisitorTime(boolean shortened, boolean onlyIfShorter) {
      long next;
      if (!shortened && this.diedSettlerRecruitStringIDs.isEmpty()) {
         next = GameRandom.globalRandom.getIntBetween(MIN_SECONDS_VISITOR_TIMER, MAX_SECONDS_VISITOR_TIMER);
      } else {
         next = GameRandom.globalRandom.getIntBetween(300, 600);
      }

      if (onlyIfShorter) {
         this.nextVisitorTimer = Math.min(next, this.nextVisitorTimer);
      } else {
         this.nextVisitorTimer = next;
      }
   }

   public void reduceRaidTimer(int seconds) {
      this.nextRaid -= seconds;
   }

   public void tickNextRaid() {
      if (this.hasNonAFKTeamMembers && this.getLevel().getWorldSettings().raidFrequency != GameRaidFrequency.NEVER) {
         this.nextRaid--;
         boolean isNight = this.getLevel().getWorldEntity().isNight();
         if (this.nextRaid <= 0L && isNight && !this.lastRaidCheckWasNight) {
            boolean valid = this.networkData
               .streamTeamMembers()
               .anyMatch(c -> c.isAFK() ? false : c.getLevel().entityManager.mobs.stream().noneMatch(Mob::isBoss));
            if (valid) {
               this.spawnRaid();
            } else {
               this.resetNextRaidTimer(true, false);
            }
         }

         this.lastRaidCheckWasNight = isNight;
      }
   }

   public void resetNextRaidTimer(boolean shortened, boolean onlyIfShorter) {
      long next;
      if (shortened) {
         next = GameRandom.globalRandom.getIntBetween(300, 1200);
      } else {
         float modifier = 1.0F;
         switch (this.getLevel().getWorldSettings().raidFrequency) {
            case OFTEN:
               modifier = 0.5F;
               break;
            case RARELY:
               modifier = 4.0F;
         }

         next = GameRandom.globalRandom
            .getIntBetween(
               (int)(MIN_SECONDS_RAID_TIMER * modifier),
               Math.min((int)((MAX_SECONDS_RAID_TIMER + INCREASES_RAID_TIMER_SECONDS_PER_RAID * this.raidsCounter) * modifier), UPPER_LIMIT_RAID_TIMER_SECONDS)
            );
      }

      if (onlyIfShorter) {
         this.nextRaid = Math.min(next, this.nextRaid);
      } else {
         this.nextRaid = next;
      }
   }

   public boolean spawnRaid() {
      return this.spawnRaid(GameRandom.globalRandom.getOneOf(SettlementRaidLevelEvent.RaidDir.values()), false);
   }

   public boolean spawnRaid(SettlementRaidLevelEvent.RaidDir direction, boolean dontAutoAttack) {
      SettlementRaidOptions options = this.getRaidOptions(false);
      options.difficultyModifier = this.nextRaidDifficultyMod;
      options.direction = direction;
      options.dontAutoAttackSettlement = dontAutoAttack;
      return this.spawnRaid(this.getNextRaid(options), options);
   }

   public boolean spawnRaid(SettlementRaidLevelEvent raidEvent, SettlementRaidOptions options) {
      if (raidEvent != null) {
         if (this.currentRaid != 0) {
            LevelEvent lastEvent = this.getLevel().entityManager.events.get(this.currentRaid, false);
            if (lastEvent != null) {
               lastEvent.over();
            }
         }

         raidEvent.initializeFromServerData(this, options);
         this.getLevel().entityManager.events.add(raidEvent);
         this.stats.spawned_raids.addRaid(raidEvent);
         this.currentRaid = raidEvent.getUniqueID();
         this.resetNextRaidTimer(false, false);
         return true;
      } else {
         this.resetNextRaidTimer(true, false);
         return false;
      }
   }

   public void addRaidChances(TicketSystemList<Supplier<SettlementRaidLevelEvent>> raids, SettlementRaidOptions options) {
      double weaponValue = options.wealthCounter.getBestWeaponValue();
      raids.addObject(950, HumanSettlementRaidLevelEvent::new);
      if (this.stats.spawned_raids.getRaidCount("humanraid") > 0) {
         raids.addObject(this.stats.spawned_raids.getRaidCount("roguehuntersraid") > 0 ? 100 : 10000, RogueHuntersSettlementRaidLevelEvent::new);
         if (weaponValue >= 300.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("vampireraid") > 0 ? 100 : 10000, VampiresSettlementRaidLevelEvent::new);
            raids.addObject(this.stats.spawned_raids.getRaidCount("ninjasraid") > 0 ? 50 : 10000, NinjaSettlementRaidLevelEvent::new);
         }

         if (weaponValue >= 350.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("pirateraid") > 0 ? 100 : 10000, PiratesSettlementRaidLevelEvent::new);
            raids.addObject(this.stats.spawned_raids.getRaidCount("chickenpeopleraid") > 0 ? 25 : 2500, ChickenPeopleSettlementRaidLevelEvent::new);
         }

         if (weaponValue >= 500.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("frozendwarvesraid") > 0 ? 100 : 10000, FrozenDwarvesSettlementRaidLevelEvent::new);
         }

         if (weaponValue >= 550.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("voidapprenticeraid") > 0 ? 50 : 10000, VoidApprenticesSettlementRaidLevelEvent::new);
         }

         if (weaponValue >= 700.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("themafiaraid") > 0 ? 50 : 10000, TheMafiaRaidLevelEvent::new);
         }

         if (weaponValue >= 900.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("mummyraid") > 0 ? 100 : 10000, MummiesSettlementRaidLevelEvent::new);
         }

         if (weaponValue >= 1650.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("fishianraid") > 0 ? 100 : 10000, FishianSettlementRaidLevelEvent::new);
         }

         if (weaponValue >= 1750.0) {
            raids.addObject(this.stats.spawned_raids.getRaidCount("ancientskeletonraid") > 0 ? 100 : 10000, AncientSkeletonRaidLevelEvent::new);
         }
      }
   }

   public SettlementRaidLevelEvent getNextRaid(SettlementRaidOptions options) {
      if (this.countTotalSettlers() >= 3) {
         TicketSystemList<Supplier<SettlementRaidLevelEvent>> raids = new TicketSystemList<>();
         this.addRaidChances(raids, options);
         return raids.getRandomObject(GameRandom.globalRandom).get();
      } else {
         return null;
      }
   }

   public float getNextRaidDifficultyMod() {
      return this.nextRaidDifficultyMod;
   }

   public void setRaidDifficultyMod(float nextRaidDifficultyMod) {
      this.nextRaidDifficultyMod = nextRaidDifficultyMod;
   }

   public void onRaidOver(SettlementRaidLevelEvent raid, float nextRaidDifficultyMod) {
      if (raid.getUniqueID() == this.currentRaid) {
         this.nextRaidDifficultyMod = nextRaidDifficultyMod;
         this.raidsCounter++;
         this.currentRaid = 0;
         this.resetNextRaidTimer(false, false);
         if (!this.diedSettlerRecruitStringIDs.isEmpty()) {
            this.resetNextVisitorTime(true, true);
         }
      }
   }

   public int getRaidsCounter() {
      return this.raidsCounter;
   }

   public void onSettlerDeath(int mobUniqueID) {
      LevelSettler settler = this.settlers.get(mobUniqueID);
      if (settler != null) {
         float chance = settler.settler.getArriveAsRecruitAfterDeathChance(this);
         if (chance > 0.0F && GameRandom.globalRandom.getChance(chance)) {
            this.diedSettlerRecruitStringIDs.add(settler.settler.getStringID());
            if (this.diedSettlerRecruitStringIDs.size() >= 5) {
               this.diedSettlerRecruitStringIDs.remove(GameRandom.globalRandom.nextInt(this.diedSettlerRecruitStringIDs.size()));
            }
         }

         SettlementBed bed = settler.getBed();
         if (bed != null) {
            bed.clearSettler();
         }

         this.settlers.remove(mobUniqueID);
         this.sendEvent(SettlementSettlersChangedEvent.class);
      }
   }

   public void onSettlerEquipmentChanged(int mobUniqueID, InventoryRange inventoryRange, int slot, boolean isCosmetic) {
      LevelSettler settler = this.settlers.get(mobUniqueID);
      if (settler != null) {
         this.networkData
            .streamTeamMembers()
            .forEach(
               client -> JournalChallengeRegistry.handleListeners(
                  client,
                  SettlerEquipmentChangedJournalChallengeListener.class,
                  challenge -> challenge.onSettlerEquipmentChanged(client, this, settler, inventoryRange, slot, isCosmetic)
               )
            );
      }
   }

   public SettlementWorkZoneManager getWorkZones() {
      return this.workZones;
   }

   public int getNewSettlerRestrictZoneUniqueID() {
      return this.newSettlerRestrictZoneUniqueID;
   }

   public RestrictZone getNewSettlerRestrictZone() {
      return this.restrictZones.getOrDefault(this.newSettlerRestrictZoneUniqueID, null);
   }

   public void setNewSettlerRestrictZone(int uniqueID) {
      if (this.newSettlerRestrictZoneUniqueID != uniqueID) {
         this.newSettlerRestrictZoneUniqueID = uniqueID;
      }
   }

   public Collection<RestrictZone> getRestrictZones() {
      return this.restrictZones.values();
   }

   public RestrictZone getRestrictZone(int uniqueID) {
      return this.restrictZones.getOrDefault(uniqueID, null);
   }

   public RestrictZone addNewRestrictZone() {
      int uniqueID = 1;

      for (int i = 0; i < 1000; i++) {
         uniqueID = GameRandom.globalRandom.nextInt();
         if (uniqueID != 0 && uniqueID != 1 && !this.restrictZones.containsKey(uniqueID)) {
            break;
         }
      }

      AtomicInteger number = new AtomicInteger(this.restrictZones.size() + 1);
      Function<Integer, GameMessage> nameGenerator = i -> new LocalMessage("ui", "settlementareadefname", "number", i);

      while (this.restrictZones.values().stream().anyMatch(z -> z.name.translate().equals(nameGenerator.apply(number.get()).translate()))) {
         number.addAndGet(1);
      }

      RestrictZone newZone = new RestrictZone(this, uniqueID, this.restrictZoneIndexCounter++, nameGenerator.apply(number.get()));
      newZone.colorHue = this.restrictZoneIndexCounter * 3 * 36 % 360;
      this.restrictZones.put(uniqueID, newZone);
      return newZone;
   }

   public boolean deleteRestrictZone(int uniqueID) {
      return this.restrictZones.remove(uniqueID) != null;
   }

   public ItemCategoriesFilter getNewSettlerDiet() {
      return this.newSettlerDiet;
   }

   public ItemCategoriesFilter getNewSettlerEquipmentFilter() {
      return this.newSettlerEquipmentFilter;
   }

   public void sendEvent(Class<? extends ContainerEvent> eventClass) {
      this.eventsToSend.add(eventClass);
   }

   public GameTooltips getDebugTooltips() {
      StringTooltips tooltips = new StringTooltips();
      tooltips.add("Next visitor: " + GameUtils.formatSeconds(this.nextVisitorTimer));
      tooltips.add("Current visitor: " + this.currentVisitorUniqueID);
      tooltips.add("Next raid: " + GameUtils.formatSeconds(this.nextRaid));
      tooltips.add("Raid difficulty: " + this.nextRaidDifficultyMod);
      return tooltips;
   }

   private void countInventoryWealth(SettlementWealthCounter counter, int tileX, int tileY, InventoryRange range) {
      if (range != null) {
         for (int slot = range.startSlot; slot <= range.endSlot; slot++) {
            InventoryItem invItem = range.inventory.getItem(slot);
            if (invItem != null) {
               counter.addStoredItem(tileX, tileY, invItem);
            }
         }
      }
   }

   private SettlementWealthCounter countWealth(boolean checkFullLevel, boolean checkFullPlayerGear) {
      SettlementWealthCounter counter = new SettlementWealthCounter();
      if (checkFullLevel) {
         Rectangle tiles = this.boundsManager.getTileRectangle();

         for (int tileX = tiles.x; tileX < tiles.x + tiles.width; tileX++) {
            for (int tileY = tiles.y; tileY < tiles.y + tiles.height; tileY++) {
               ObjectEntity objectEntity = this.getLevel().entityManager.getObjectEntity(tileX, tileY);
               if (objectEntity instanceof OEInventory) {
                  Inventory inventory = ((OEInventory)objectEntity).getInventory();
                  if (inventory != null) {
                     this.countInventoryWealth(counter, tileX, tileY, new InventoryRange(inventory, 0, inventory.getSize() - 1));
                  }
               }
            }
         }
      } else {
         for (SettlementInventory storage : this.storageManager.getStorage()) {
            this.countInventoryWealth(counter, storage.tileX, storage.tileY, storage.getInventoryRange());
         }

         for (SettlementWorkstation workstation : this.storageManager.getWorkstations()) {
            SettlementRequestInventory fuelInventory = workstation.getFuelInventory();
            if (fuelInventory != null) {
               this.countInventoryWealth(counter, workstation.tileX, workstation.tileY, fuelInventory.getInventoryRange());
            }

            SettlementInventory outputInventory = workstation.getProcessingOutputInventory();
            if (outputInventory != null) {
               this.countInventoryWealth(counter, Integer.MIN_VALUE, Integer.MIN_VALUE, outputInventory.getInventoryRange());
            }

            SettlementInventory inputInventory = workstation.getProcessingInputInventory();
            if (inputInventory != null) {
               this.countInventoryWealth(counter, Integer.MIN_VALUE, Integer.MIN_VALUE, inputInventory.getInventoryRange());
            }
         }
      }

      for (LevelSettler settler : this.settlers.values()) {
         SettlerMob mob = settler.getMob();
         if (mob != null) {
            mob.addToWealthCounter(counter);
         }
      }

      this.networkData.streamTeamMembers().forEach(c -> {
         PlayerInventoryManager inv = c.playerMob.getInv();
         inv.streamInventorySlots(checkFullPlayerGear, true, false, false).forEach(slot -> counter.addPlayerItem(slot.getItem()));
      });
      return counter;
   }

   public SettlementRaidOptions getRaidOptions(boolean debug) {
      SettlementWealthCounter counter = this.countWealth(true, !debug);
      HashSet<String> obtainedItems = new HashSet<>();
      this.networkData.streamTeamMembers().forEach(c -> c.characterStats().items_obtained.addAllItemsToSet(obtainedItems));
      return new SettlementRaidOptions(this, obtainedItems, counter);
   }

   public static Point findRandomSpawnLevelPos(Rectangle insideTiles, Level level, Mob mob, int maxTries, int tilesFromEdge, boolean onlyIfFound) {
      Point offset = mob.getPathMoveOffset();
      Point spawnTile = findRandomSpawnTile(
         insideTiles, level, tp -> !mob.collidesWith(level, tp.tileX * 32 + offset.x, tp.tileY * 32 + offset.y), maxTries, tilesFromEdge, onlyIfFound
      );
      return spawnTile != null ? new Point(spawnTile.x * 32 + offset.x, spawnTile.y * 32 + offset.y) : null;
   }

   public static Point findRandomSpawnTile(
      Rectangle insideTiles, Level level, Predicate<TilePosition> validTileCheck, int maxTries, int tilesFromEdge, boolean onlyIfFound
   ) {
      Rectangle[] spawnTileRectangles = getOutsideRectangles(insideTiles, -tilesFromEdge, tilesFromEdge);
      Point spawnTile = null;

      for (int i = 0; i < maxTries; i++) {
         Rectangle spawnRectangle = GameRandom.globalRandom.getOneOf(spawnTileRectangles);
         spawnTile = new Point(
            level.limitTileXToBounds(spawnRectangle.x + GameRandom.globalRandom.nextInt(spawnRectangle.width), 0, 2),
            level.limitTileYToBounds(spawnRectangle.y + GameRandom.globalRandom.nextInt(spawnRectangle.height), 0, 2)
         );
         if (validTileCheck.test(new TilePosition(level, spawnTile.x, spawnTile.y))) {
            return spawnTile;
         }
      }

      return onlyIfFound ? null : spawnTile;
   }

   public Point findRandomSpawnLevelPos(Mob mob, boolean onlyIfFound) {
      return findRandomSpawnLevelPos(this.networkData.getLoadedTileRectangle(), this.getLevel(), mob, 100, 3, onlyIfFound);
   }

   public Point findRandomSpawnTile(Predicate<TilePosition> validTileCheck, boolean onlyIfFound) {
      return findRandomSpawnTile(this.networkData.getLoadedTileRectangle(), this.getLevel(), validTileCheck, 100, 3, onlyIfFound);
   }

   public static Rectangle[] getOutsideRectangles(Rectangle rectangle, int innerPadding, int size) {
      if (innerPadding != 0) {
         rectangle = new Rectangle(
            rectangle.x - innerPadding, rectangle.y - innerPadding, rectangle.width + innerPadding * 2, rectangle.height + innerPadding * 2
         );
      }

      return new Rectangle[]{
         new Rectangle(rectangle.x, rectangle.y - size, rectangle.width + size, size),
         new Rectangle(rectangle.x + rectangle.width, rectangle.y, size, rectangle.height + size),
         new Rectangle(rectangle.x - size, rectangle.y + rectangle.height, rectangle.width + size, size),
         new Rectangle(rectangle.x - size, rectangle.y - size, size, rectangle.height + size)
      };
   }

   @Override
   public boolean isClient() {
      return this.getLevel().isClient();
   }

   @Override
   public Client getClient() {
      return this.getLevel().getClient();
   }

   @Override
   public boolean isServer() {
      return this.getLevel().isServer();
   }

   @Override
   public Server getServer() {
      return this.getLevel().getServer();
   }

   @Override
   public WorldEntity getWorldEntity() {
      return this.getLevel().getWorldEntity();
   }

   public void runFinalMigration(OneWorldMigration migrationData) {
      ListIterator<Waystone> li = this.waystones.listIterator();

      while (li.hasNext()) {
         Waystone waystone = li.next();
         LevelIdentifier oldDestination = waystone.destination;
         waystone.destination = migrationData.getNewLevelIdentifier(oldDestination);
         if (waystone.destination == null) {
            GameLog.warn.println("Could not find migrated level for waystone:  " + oldDestination + ", " + waystone.tileX + "x" + waystone.tileY);
            li.remove();
         } else {
            Point destinationTileOffset = migrationData.getTilePositionOffset(oldDestination);
            waystone.tileX = waystone.tileX + destinationTileOffset.x;
            waystone.tileY = waystone.tileY + destinationTileOffset.y;
         }
      }
   }

   static {
      visitorOdds.add(new SettlementVisitorOdds("exoticmerchant") {
         @Override
         public boolean canSpawn(ServerSettlementData data) {
            return true;
         }

         @Override
         public int getTickets(ServerSettlementData data) {
            return this.identifier.equals(data.lastVisitorIdentifier) ? 25 : 100;
         }

         @Override
         public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
            return new SettlementVisitorSpawner(this, (HumanMob)MobRegistry.getMob("exoticmerchanthuman", data.getLevel()));
         }
      });
      visitorOdds.add(new SettlementVisitorOdds("pawnbroker") {
         @Override
         public boolean canSpawn(ServerSettlementData data) {
            return true;
         }

         @Override
         public int getTickets(ServerSettlementData data) {
            return this.identifier.equals(data.lastVisitorIdentifier) ? 25 : 80;
         }

         @Override
         public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
            return new SettlementVisitorSpawner(this, (HumanMob)MobRegistry.getMob("pawnbrokerhuman", data.getLevel()));
         }
      });
      visitorOdds.add(new SettlementVisitorOdds("animalmerchant") {
         @Override
         public boolean canSpawn(ServerSettlementData data) {
            return true;
         }

         @Override
         public int getTickets(ServerSettlementData data) {
            return this.identifier.equals(data.lastVisitorIdentifier) ? 10 : 60;
         }

         @Override
         public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
            if (GameRandom.globalRandom.nextBoolean()) {
               int animalCount = GameRandom.globalRandom.getIntBetween(4, 5);
               return new AnimalMerchantSettlementVisitorSpawner(this, data, "animalkeeperhuman", animalCount);
            } else {
               int animalCount = GameRandom.globalRandom.getIntBetween(1, 2);
               return new AnimalMerchantSettlementVisitorSpawner(this, data, "farmerhuman", animalCount);
            }
         }
      });
      visitorOdds.add(new SettlementVisitorOdds("recruit") {
         @Override
         public boolean canSpawn(ServerSettlementData data) {
            return true;
         }

         @Override
         public int getTickets(ServerSettlementData data) {
            return this.identifier.equals(data.lastVisitorIdentifier) ? 5 : 10;
         }

         @Override
         public SettlementVisitorSpawner getNewVisitorSpawner(ServerSettlementData data) {
            TicketSystemList<Supplier<HumanMob>> ticketSystem = new TicketSystemList<>();

            for (Settler settler : SettlerRegistry.getSettlers()) {
               settler.addNewRecruitSettler(data, true, ticketSystem);
            }

            while (!ticketSystem.isEmpty()) {
               Supplier<HumanMob> supplier = ticketSystem.getAndRemoveRandomObject(GameRandom.globalRandom);
               HumanMob humanMob = supplier.get();
               if (humanMob != null) {
                  return new SettlementVisitorSpawner(this, humanMob);
               }
            }

            return null;
         }
      });
   }

   private static class TempRequest extends ServerSettlementData.TempStorage {
      public final SettlementRequestInventory requestInventory;

      public TempRequest(SettlementRequestInventory requestInventory) {
         super(requestInventory, 0);
         this.requestInventory = requestInventory;
      }
   }

   private static class TempStorage {
      public final LevelStorage storage;
      public final InventoryRange range;
      public final int priority;

      public TempStorage(LevelStorage storage, int priority) {
         this.storage = storage;
         this.range = storage.getInventoryRange();
         this.priority = priority;
      }

      public TempStorage(SettlementInventory inventory) {
         this(inventory, inventory.priority);
      }
   }
}
