package necesse.engine.storyObjectives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.GameState;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.registries.StoryObjectiveRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.storyObjectives.listeners.AddToSettlementStorageObjectiveListener;
import necesse.engine.storyObjectives.listeners.AssignBedObjectiveListener;
import necesse.engine.storyObjectives.listeners.ContainerOpenedObjectiveListener;
import necesse.engine.storyObjectives.listeners.IncursionCompletedObjectiveListener;
import necesse.engine.storyObjectives.listeners.MobKilledStatAddedStoryObjectiveListener;
import necesse.engine.storyObjectives.listeners.MobKilledStoryObjectiveListener;
import necesse.engine.storyObjectives.listeners.ObjectInteractedObjectiveListener;
import necesse.engine.storyObjectives.listeners.ObjectPlacedObjectiveListener;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.HashMapArrayList;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class StoryObjectiveManager implements GameState {
   public static ArrayList<Class<?>> LISTENER_CLASSES = new ArrayList<>();
   private Client client;
   private ServerClient serverClient;
   private final HashMap<String, GameLinkedList<StoryObjective>.Element> objectivesMap = new HashMap<>();
   private final GameLinkedList<StoryObjective> objectives = new GameLinkedList<>();
   private String currentObjectiveStringID;
   protected HashMapArrayList<Class<?>, StoryObjective> listenerToObjectives = new HashMapArrayList<>();
   private final HashSet<String> claimableObjectiveStringIDs = new HashSet<>();
   private final HashSet<String> dirtyObjectiveStringIDs = new HashSet<>();
   private boolean shouldUpdateFormComponents;
   public boolean hideStoryObjectives;

   public StoryObjectiveManager() {
      this.fillObjectivesLine();
   }

   public StoryObjectiveManager(ServerClient serverClient) {
      this.serverClient = serverClient;
      this.fillObjectivesLine();
   }

   @Override
   public boolean isClient() {
      return this.client != null;
   }

   @Override
   public Client getClient() {
      return this.client;
   }

   @Override
   public boolean isServer() {
      return this.serverClient != null;
   }

   public ServerClient getServerClient() {
      return this.serverClient;
   }

   @Override
   public Server getServer() {
      return this.serverClient.getServer();
   }

   public void initClient(Client client) {
      if (this.serverClient == null && this.client == null) {
         this.client = client;
      } else {
         throw new IllegalStateException("StoryObjectiveManager is already initialized");
      }
   }

   private void fillObjectivesLine() {
      this.objectivesMap.clear();
      this.objectives.clear();

      for (StoryObjective storyObjective : StoryObjectiveRegistry.createObjectiveLine(this)) {
         GameLinkedList<StoryObjective>.Element element = this.objectives.addLast(storyObjective);
         this.objectivesMap.put(storyObjective.getStringID(), element);
      }

      this.currentObjectiveStringID = this.objectives.isEmpty() ? null : this.objectives.getFirst().getStringID();
      this.updateObjectiveListeners();
   }

   public void updateObjectiveListeners() {
      this.listenerToObjectives.clearAll();
      if (this.serverClient != null) {
         StoryObjective currentObjective = this.getCurrentObjective();
         if (currentObjective != null) {
            for (GameLinkedList<StoryObjective>.Element currentElement = this.objectivesMap.get(currentObjective.getStringID());
               currentElement != null;
               currentElement = currentElement.next()
            ) {
               StoryObjective objective = (StoryObjective)currentElement.object;
               if (!objective.isCompleted()
                  && (currentObjective == currentElement.object || ((StoryObjective)currentElement.object).shouldAddListenersIfNotCurrent())) {
                  for (Class<?> listenerClass : LISTENER_CLASSES) {
                     if (listenerClass.isAssignableFrom(objective.getClass())) {
                        this.listenerToObjectives.add(listenerClass, objective);
                     }
                  }
               }
            }
         }
      }
   }

   public void updateCurrentAndClaimableObjectives() {
      this.claimableObjectiveStringIDs.clear();
      this.currentObjectiveStringID = null;

      for (StoryObjective objective : this.objectives) {
         if (this.currentObjectiveStringID == null && (!objective.isCompleted() || !objective.isClaimed())) {
            if (this.isServer() && objective.isMajorObjective()) {
               ServerClient client = this.getServerClient();
               if (client.achievementsLoaded()) {
                  client.achievements().ADVENTURE_BEGINS.markCompleted(client);
               }
            }

            this.currentObjectiveStringID = objective.getStringID();
         }

         if (objective.isCompleted() && !objective.isClaimed()) {
            this.claimableObjectiveStringIDs.add(objective.getStringID());
         }
      }
   }

   public boolean hasCompletedAnyMajorObjective() {
      for (StoryObjective objective : this.objectives) {
         if (objective.isCompleted() && objective.isMajorObjective()) {
            return true;
         }
      }

      return false;
   }

   public boolean isCurrentObjectiveMajor() {
      StoryObjective objective = this.getCurrentObjective();
      return objective != null && objective.isMajorObjective();
   }

   public <T> Stream<T> streamObjectives(Class<T> listenerClass) {
      ArrayList<StoryObjective> objectives = this.listenerToObjectives.get(listenerClass);
      return objectives.stream().filter(q -> !q.isCompleted()).map(o -> (T)o);
   }

   public <T> Iterable<T> getObjectives(Class<T> listenerClass) {
      return () -> this.streamObjectives(listenerClass).iterator();
   }

   public void clearAllProgression() {
      this.fillObjectivesLine();
   }

   public void debugSetCurrentStoryObjective(String objectiveStringID) {
      this.clearAllProgression();
      if (objectiveStringID != null) {
         GameLinkedList<StoryObjective>.Element element = this.objectivesMap.get(objectiveStringID);
         if (element != null) {
            element = element.prev();
         }

         while (element != null) {
            ((StoryObjective)element.object).debugMarkCompletedAndClaimed();
            element = element.prev();
         }
      } else {
         for (StoryObjective objective : this.objectives) {
            objective.debugMarkCompletedAndClaimed();
         }
      }

      this.updateCurrentAndClaimableObjectives();
      this.updateObjectiveListeners();
      if (this.isClient()) {
         this.updateFormComponents();
      }
   }

   public void forceUpdateSettlements() {
      if (!this.isServer()) {
         throw new IllegalStateException("Can only update settlements from the server");
      }

      ServerClient client = this.getServerClient();
      SettlementsWorldData worldData = SettlementsWorldData.getSettlementsData(client.getServer());

      for (CachedSettlementData cache : worldData.collectCachedSettlements(
         data -> data.getOwnerAuth() == client.authentication || client.isSameTeam(data.getTeamID())
      )) {
         ServerSettlementData settlement = worldData.getServerData(cache.uniqueID);
         if (settlement != null) {
            settlement.storyObjectives.forceUpdateFromCompleted(this);
         }
      }
   }

   public void addSaveData(SaveData save) {
      save.addBoolean("hideStoryObjectives", this.hideStoryObjectives);
      HashSet<String> completedObjectives = new HashSet<>();
      HashSet<String> claimedObjectives = new HashSet<>();
      SaveData objectivesSave = new SaveData("OBJECTIVES");

      for (StoryObjective objective : this.objectives) {
         if (objective.isCompleted()) {
            completedObjectives.add(objective.getStringID());
            if (objective.isClaimed()) {
               claimedObjectives.add(objective.getStringID());
            }
         } else {
            SaveData objectiveSave = new SaveData(objective.getStringID());
            objective.addSaveData(objectiveSave);
            if (!objectiveSave.isEmpty()) {
               objectivesSave.addSaveData(objectiveSave);
            }
         }
      }

      if (!completedObjectives.isEmpty()) {
         save.addStringHashSet("completedObjectives", completedObjectives);
         if (!claimedObjectives.isEmpty()) {
            save.addStringHashSet("claimedObjectives", claimedObjectives);
         }
      }

      if (!objectivesSave.isEmpty()) {
         save.addSaveData(objectivesSave);
      }
   }

   public void applyLoadData(LoadData save) {
      this.hideStoryObjectives = save.getBoolean("hideStoryObjectives", false, false);
      HashSet<String> completedObjectives = save.getStringHashSet("completedObjectives", new HashSet<>(), false);
      HashSet<String> claimedObjectives = save.getStringHashSet("claimedObjectives", new HashSet<>(), false);

      for (String objectiveStringID : completedObjectives) {
         GameLinkedList<StoryObjective>.Element element = this.objectivesMap.get(objectiveStringID);
         if (element == null) {
            GameLog.warn.println("Could not find completed story objective with stringID \"" + objectiveStringID + "\" when loading character data");
         } else {
            ((StoryObjective)element.object).markLoadDataCompleted(claimedObjectives.contains(objectiveStringID));
         }
      }

      LoadData objectivesSave = save.getFirstLoadDataByName("OBJECTIVES");
      if (objectivesSave != null) {
         for (LoadData objectiveSave : objectivesSave.getLoadData()) {
            if (objectiveSave.isArray()) {
               GameLinkedList<StoryObjective>.Element element = this.objectivesMap.get(objectiveSave.getName());
               if (element == null) {
                  GameLog.warn.println("Could not find story objective with stringID \"" + objectiveSave.getName() + "\" when loading character data");
               } else {
                  try {
                     ((StoryObjective)element.object).applyLoadData(objectiveSave);
                  } catch (Exception e) {
                     System.err.println("Error loading story objective with stringID \"" + objectiveSave.getName() + "\"");
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      this.updateCurrentAndClaimableObjectives();
      this.updateObjectiveListeners();
   }

   public ArrayList<String> migrateFromCharacterStats(Server server, PlayerStats characterStats) {
      ArrayList<String> completed = new ArrayList<>();

      for (StoryObjective objective : this.objectives) {
         objective.migrateFromCharacterStats(server, characterStats);
         if (objective.isCompleted()) {
            completed.add(objective.getStringID());
         }
      }

      this.updateCurrentAndClaimableObjectives();
      this.updateObjectiveListeners();
      return completed;
   }

   public void serverTick() {
      StoryObjective currentObjective = this.getCurrentObjective();
      if (currentObjective != null) {
         currentObjective.serverTickCurrentObjective();
      }
   }

   public void clientTick() {
      StoryObjective currentObjective = this.getCurrentObjective();
      if (currentObjective != null) {
         currentObjective.clientTickCurrentObjective();
      }
   }

   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (this.hideStoryObjectives) {
         return null;
      }

      StoryObjective currentObjective = this.getCurrentObjective();
      return currentObjective != null ? currentObjective.getCurrentObjectiveLevelHudDrawOptions(camera, perspective) : null;
   }

   public void drawOverForms(MainGame mainGame, PlayerMob perspective) {
      if (!this.hideStoryObjectives) {
         StoryObjective currentObjective = this.getCurrentObjective();
         if (currentObjective != null) {
            currentObjective.drawCurrentObjectiveOverForms(mainGame, perspective);
         }
      }
   }

   public void writeCharacterPacket(PacketWriter writer) {
      writer.putNextBoolean(this.hideStoryObjectives);

      for (StoryObjective objective : this.objectives) {
         objective.writePacket(writer);
      }
   }

   public void readCharacterPacket(PacketReader reader) {
      this.hideStoryObjectives = reader.getNextBoolean();

      for (StoryObjective objective : this.objectives) {
         objective.readPacket(reader);
      }

      this.updateCurrentAndClaimableObjectives();
      this.updateObjectiveListeners();
   }

   public void writeDirtyPacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.dirtyObjectiveStringIDs.size());

      for (String objectiveStringID : this.dirtyObjectiveStringIDs) {
         StoryObjective objective = (StoryObjective)this.objectivesMap.get(objectiveStringID).object;
         writer.putNextShortUnsigned(objective.getID());
         objective.writePacket(writer);
      }
   }

   public void readDirtyPacket(PacketReader reader) {
      boolean updatedFormComponents = false;
      int count = reader.getNextShortUnsigned();

      for (int i = 0; i < count; i++) {
         int objectiveID = reader.getNextShortUnsigned();
         String stringID = StoryObjectiveRegistry.getObjectiveStringID(objectiveID);
         if (this.currentObjectiveStringID != null && this.currentObjectiveStringID.equals(stringID)) {
            updatedFormComponents = true;
         }

         StoryObjective objective = (StoryObjective)this.objectivesMap.get(stringID).object;
         boolean lastIsCompleted = objective.isCompleted();
         objective.readPacket(reader);
         if (!lastIsCompleted && objective.isCompleted()) {
            updatedFormComponents = true;
         }
      }

      if (updatedFormComponents && this.isClient()) {
         this.updateFormComponents();
      }

      this.updateCurrentAndClaimableObjectives();
   }

   public boolean isDirty() {
      return !this.dirtyObjectiveStringIDs.isEmpty();
   }

   public void cleanAll() {
      this.dirtyObjectiveStringIDs.clear();
   }

   public StoryObjective getCurrentObjective() {
      return this.currentObjectiveStringID != null ? (StoryObjective)this.objectivesMap.get(this.currentObjectiveStringID).object : null;
   }

   public void markObjectiveDirty(StoryObjective objective) {
      if (objective.manager != this) {
         throw new IllegalArgumentException("Story objective does not belong to this manager");
      }

      this.dirtyObjectiveStringIDs.add(objective.getStringID());
   }

   public void updateFormComponents() {
      this.shouldUpdateFormComponents = true;
   }

   public boolean shouldUpdateFormComponentsAndReset() {
      boolean out = this.shouldUpdateFormComponents;
      this.shouldUpdateFormComponents = false;
      return out;
   }

   public boolean hasCompletedObjective(String objectiveStringID) {
      GameLinkedList<StoryObjective>.Element element = this.objectivesMap.get(objectiveStringID);
      return ((StoryObjective)element.object).isCompleted();
   }

   public boolean canClaimObjective(String objectiveStringID) {
      GameLinkedList<StoryObjective>.Element element = this.objectivesMap.get(objectiveStringID);
      return ((StoryObjective)element.object).isCompleted() && !((StoryObjective)element.object).isClaimed();
   }

   public StoryObjective getObjective(int id) {
      String stringID = StoryObjectiveRegistry.getObjectiveStringID(id);
      return (StoryObjective)this.objectivesMap.get(stringID).object;
   }

   public StoryObjective getNextClaimableObjective() {
      if (this.claimableObjectiveStringIDs.isEmpty()) {
         return null;
      }

      StoryObjective lowestIndexObjective = null;

      for (String stringID : this.claimableObjectiveStringIDs) {
         StoryObjective objective = (StoryObjective)this.objectivesMap.get(stringID).object;
         if (lowestIndexObjective == null || lowestIndexObjective.getSortedIndex() > objective.getSortedIndex()) {
            lowestIndexObjective = objective;
         }
      }

      return lowestIndexObjective;
   }

   public boolean addAllCompletedStringIDs(HashSet<String> storyObjectivesCompleted) {
      boolean out = false;

      for (StoryObjective objective : this.objectives) {
         if (objective.isCompleted()) {
            out |= storyObjectivesCompleted.add(objective.getStringID());
         }
      }

      return out;
   }

   public boolean hasUnclaimedObjectives() {
      return !this.claimableObjectiveStringIDs.isEmpty();
   }

   public ArrayList<StoryObjective> getVisibleObjectives() {
      ArrayList<StoryObjective> visibleObjectives = new ArrayList<>();
      StoryObjective currentObjective = this.getCurrentObjective();
      if (currentObjective != null) {
         visibleObjectives.add(currentObjective);
      }

      for (String objectiveStringID : this.claimableObjectiveStringIDs) {
         if (currentObjective == null || !currentObjective.getStringID().equals(objectiveStringID)) {
            visibleObjectives.add((StoryObjective)this.objectivesMap.get(objectiveStringID).object);
         }
      }

      return visibleObjectives;
   }

   static {
      LISTENER_CLASSES.add(MobKilledStoryObjectiveListener.class);
      LISTENER_CLASSES.add(MobKilledStatAddedStoryObjectiveListener.class);
      LISTENER_CLASSES.add(IncursionCompletedObjectiveListener.class);
      LISTENER_CLASSES.add(ObjectPlacedObjectiveListener.class);
      LISTENER_CLASSES.add(ObjectInteractedObjectiveListener.class);
      LISTENER_CLASSES.add(ContainerOpenedObjectiveListener.class);
      LISTENER_CLASSES.add(AssignBedObjectiveListener.class);
      LISTENER_CLASSES.add(AddToSettlementStorageObjectiveListener.class);
   }
}
