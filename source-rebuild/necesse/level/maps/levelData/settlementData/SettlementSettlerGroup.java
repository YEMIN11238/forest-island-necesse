package necesse.level.maps.levelData.settlementData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupRenameEvent;

public class SettlementSettlerGroup {
   protected final ServerSettlementData settlementData;
   public final int groupUniqueID;
   private final HashSet<Integer> mobUniqueIDs = new HashSet<>();
   protected GameMessage name = new LocalMessage("ui", "settlementgroupdefname", "number", 0);

   public SettlementSettlerGroup(ServerSettlementData settlementData, int groupUniqueID, GameMessage name) {
      this.settlementData = settlementData;
      this.groupUniqueID = groupUniqueID;
      this.name = name;
   }

   public SettlementSettlerGroup(ServerSettlementData settlementData, int groupUniqueID, LoadData save) {
      this.settlementData = settlementData;
      this.groupUniqueID = groupUniqueID;
      this.mobUniqueIDs.clear();
      this.mobUniqueIDs.addAll(save.getIntCollection("mobUniqueIDs", new ArrayList<>(), false));
   }

   public void addSaveData(SaveData save) {
      if (!this.mobUniqueIDs.isEmpty()) {
         save.addIntCollection("mobUniqueIDs", this.mobUniqueIDs);
      }
   }

   public GameMessage getName() {
      return this.name;
   }

   public void setName(GameMessage name) {
      if (!this.name.isSame(name)) {
         this.name = name;
         new SettlementSettlerGroupRenameEvent(this.settlementData, this).applyAndSendToClientsAt(this.settlementData.getLevel());
      }
   }

   public Iterable<Integer> getMobUniqueIDs() {
      return this.mobUniqueIDs;
   }

   public Stream<Integer> streamMobUniqueIDs() {
      return this.mobUniqueIDs.stream();
   }

   public int getSize() {
      return this.mobUniqueIDs.size();
   }

   public boolean addMobUniqueIDs(Collection<Integer> uniqueIDs, boolean sendEvent) {
      ArrayList<Integer> addedIDs = new ArrayList<>(uniqueIDs.size());

      for (int id : uniqueIDs) {
         if (this.mobUniqueIDs.add(id)) {
            addedIDs.add(id);
         }
      }

      if (addedIDs.isEmpty()) {
         return false;
      }

      if (sendEvent) {
         new SettlementSettlerGroupChangedEvent(this.settlementData, this.groupUniqueID, true, addedIDs)
            .applyAndSendToClientsAt(this.settlementData.getLevel());
      }

      return true;
   }

   public boolean addMobUniqueID(int uniqueID, boolean sendEvent) {
      return this.addMobUniqueIDs(Collections.singletonList(uniqueID), sendEvent);
   }

   public boolean removeMobUniqueIDs(Collection<Integer> uniqueIDs, boolean sendEvent) {
      ArrayList<Integer> removedIDs = new ArrayList<>(uniqueIDs.size());

      for (int id : uniqueIDs) {
         if (this.mobUniqueIDs.remove(id)) {
            removedIDs.add(id);
         }
      }

      if (removedIDs.isEmpty()) {
         return false;
      }

      if (sendEvent) {
         new SettlementSettlerGroupChangedEvent(this.settlementData, this.groupUniqueID, false, removedIDs)
            .applyAndSendToClientsAt(this.settlementData.getLevel());
      }

      return true;
   }

   public boolean removeMobUniqueID(int uniqueID, boolean sendEvent) {
      return this.removeMobUniqueIDs(Collections.singletonList(uniqueID), sendEvent);
   }

   public void cleanupInvalidSettlers() {
      this.mobUniqueIDs.removeIf(uniqueID -> !this.settlementData.hasSettler(uniqueID));
   }
}
