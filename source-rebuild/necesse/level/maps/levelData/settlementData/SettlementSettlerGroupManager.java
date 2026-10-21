package necesse.level.maps.levelData.settlementData;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupDeletedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupFullEvent;

public class SettlementSettlerGroupManager {
   protected final ServerSettlementData settlementData;
   protected final LinkedHashMap<Integer, SettlementSettlerGroup> groups = new LinkedHashMap<>();

   public SettlementSettlerGroupManager(ServerSettlementData settlementData) {
      this.settlementData = settlementData;
   }

   public void createEmptyGroups() {
      while (this.groups.size() < ServerSettlementData.MAX_SETTLER_GROUPS) {
         this.addNewGroup();
      }
   }

   public void addSaveData(SaveData save) {
      SaveData groupsSave = new SaveData("SETTLER_GROUPS");

      for (SettlementSettlerGroup group : this.groups.values()) {
         SaveData groupSave = new SaveData("SETTLER_GROUP");
         groupSave.addInt("uniqueID", group.groupUniqueID);
         group.addSaveData(groupSave);
         groupsSave.addSaveData(groupSave);
      }

      if (!groupsSave.isEmpty()) {
         save.addSaveData(groupsSave);
      }
   }

   public void applyLoadData(LoadData save) {
      this.groups.clear();
      LoadData groupsSave = save.getFirstLoadDataByName("SETTLER_GROUPS");
      if (groupsSave != null) {
         for (LoadData groupSave : groupsSave.getLoadDataByName("SETTLER_GROUP")) {
            int uniqueID = groupSave.getInt("uniqueID", 0, false);
            if (!this.isValidUniqueID(uniqueID)) {
               uniqueID = this.findNewUniqueID();
            }

            SettlementSettlerGroup group = new SettlementSettlerGroup(this.settlementData, uniqueID, groupSave);
            this.groups.put(uniqueID, group);
         }
      }

      this.createEmptyGroups();
   }

   protected int findNewUniqueID() {
      for (int i = 0; i < 25; i++) {
         int uniqueID = GameRandom.globalRandom.nextInt();
         if (this.isValidUniqueID(uniqueID)) {
            return uniqueID;
         }
      }

      return GameRandom.globalRandom.nextInt();
   }

   protected boolean isValidUniqueID(int uniqueID) {
      return uniqueID != 0 && this.groups.containsKey(uniqueID);
   }

   public void onSettlerRemoved(int mobUniqueID) {
      for (SettlementSettlerGroup group : this.groups.values()) {
         group.removeMobUniqueID(mobUniqueID, false);
      }
   }

   public void cleanupInvalidSettlers() {
      for (SettlementSettlerGroup group : this.groups.values()) {
         group.cleanupInvalidSettlers();
      }
   }

   public int getGroupCount() {
      return this.groups.size();
   }

   public Iterable<SettlementSettlerGroup> getGroups() {
      return this.groups.values();
   }

   public SettlementSettlerGroup getGroup(int uniqueID) {
      return this.groups.getOrDefault(uniqueID, null);
   }

   public SettlementSettlerGroup addNewGroup() {
      int uniqueID = this.findNewUniqueID();
      AtomicInteger number = new AtomicInteger(this.groups.size() + 1);
      Function<Integer, GameMessage> nameGenerator = i -> new LocalMessage("ui", "settlementgroupdefname", "number", i);

      while (this.groups.values().stream().anyMatch(group -> group.name.translate().equals(nameGenerator.apply(number.get()).translate()))) {
         number.addAndGet(1);
      }

      SettlementSettlerGroup newGroup = new SettlementSettlerGroup(this.settlementData, uniqueID, nameGenerator.apply(number.get()));
      this.groups.put(uniqueID, newGroup);
      new SettlementSettlerGroupFullEvent(this.settlementData, newGroup).applyAndSendToClientsAt(this.settlementData.getLevel());
      return newGroup;
   }

   public boolean deleteGroup(int uniqueID) {
      if (this.groups.remove(uniqueID) != null) {
         new SettlementSettlerGroupDeletedEvent(this.settlementData, uniqueID).applyAndSendToClientsAt(this.settlementData.getLevel());
         return true;
      } else {
         return false;
      }
   }
}
