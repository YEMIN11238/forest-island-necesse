package necesse.inventory.container.settlement.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameLinkedList;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.SettlementContainerSettlerGroup;
import necesse.inventory.container.settlement.actions.RequestSettlerDataAbstractAction;
import necesse.inventory.container.settlement.actions.SettlementDataSubscriptionHandler;
import necesse.inventory.container.settlement.events.SettlementSettlerActivityDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerCommandDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerDataAbstractEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGearDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupDeletedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupFullEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerLooksDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlersChangedEvent;

public class SettlementClientDataManager {
   public final SettlementContainer container;
   public final Client client;
   public int settlerBasicsSubscription = -1;
   protected HashMap<Integer, GameLinkedList<SettlementSettlerData>.Element> settlersMap = new HashMap<>();
   protected GameLinkedList<SettlementSettlerData> settlers = new GameLinkedList<>();
   public ArrayList<SettlementLockedBedData> lockedBeds = new ArrayList<>();
   public LinkedHashMap<Integer, SettlementContainerSettlerGroup> settlerGroups = new LinkedHashMap<>();

   public SettlementClientDataManager(SettlementContainer container, Client client) {
      this.container = container;
      this.client = client;
   }

   public void init() {
      this.container.onEvent(SettlementSettlersChangedEvent.class, event -> {
         if (this.container.hasSettlementAccess(this.client)) {
            this.container.requestSettlerBasics.runAndSend();
         }
      });
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         HashSet<Integer> removedSettlers = new HashSet<>(this.settlersMap.keySet());

         for (SettlementSettlerData settler : event.settlers) {
            removedSettlers.remove(settler.mobUniqueID);
            GameLinkedList<SettlementSettlerData>.Element element = this.settlersMap.get(settler.mobUniqueID);
            if (element == null) {
               element = this.settlers.addLast(settler);
               this.settlersMap.put(settler.mobUniqueID, element);
            } else {
               ((SettlementSettlerData)element.object).updateFromOtherData(settler);
            }
         }

         for (int mobUniqueID : removedSettlers) {
            GameLinkedList<SettlementSettlerData>.Element element = this.settlersMap.remove(mobUniqueID);
            if (element != null && !element.isRemoved()) {
               element.remove();
            }
         }

         Comparator<SettlementSettlerData> comparing = Comparator.comparing(s -> s.settler.getID());
         comparing = comparing.thenComparing(s -> s.mobUniqueID);
         this.settlers.sort(comparing);

         for (SettlementSettlerData settler : this.settlers) {
            settler.updateFromClient(this.client.getLevel());
         }

         this.requestSettlerData(this.container.requestSettlerLooksData, s -> s.looksData == null);
         this.requestSettlerData(this.container.requestSettlerActivityData, s -> s.activityData == null);
         this.lockedBeds = event.lockedBeds;
         this.settlerGroups = event.settlerGroups;
      });
      this.container.onEvent(SettlementSettlerGroupChangedEvent.class, event -> {
         SettlementContainerSettlerGroup group = this.settlerGroups.get(event.groupUniqueID);
         if (group == null) {
            this.container.requestSettlersGroupAction.runAndSend(event.groupUniqueID);
         } else if (event.addedToGroup) {
            group.mobUniqueIDs.addAll(event.mobUniqueIDs);
         } else {
            event.mobUniqueIDs.forEach(group.mobUniqueIDs::remove);
         }
      });
      this.container.onEvent(SettlementSettlerGroupFullEvent.class, event -> this.settlerGroups.put(event.group.groupUniqueID, event.group));
      this.container.onEvent(SettlementSettlerGroupDeletedEvent.class, event -> this.settlerGroups.remove(event.groupUniqueID));
      this.container.onEvent(SettlementSettlerLooksDataEvent.class, event -> this.applySettlerData(event, (data, addon) -> data.looksData = addon));
      this.container.onEvent(SettlementSettlerActivityDataEvent.class, event -> this.applySettlerData(event, (data, addon) -> data.activityData = addon));
      this.container.onEvent(SettlementSettlerCommandDataEvent.class, event -> this.applySettlerData(event, (data, addon) -> data.commandData = addon));
      this.container.onEvent(SettlementSettlerGearDataEvent.class, event -> this.applySettlerData(event, (data, addon) -> data.gearData = addon));
   }

   public void requestSettlerData(RequestSettlerDataAbstractAction action, Predicate<SettlementSettlerData> filter) {
      HashSet<Integer> mobUniqueIDs = new HashSet<>();

      for (SettlementSettlerData settler : this.settlers) {
         if (filter.test(settler)) {
            mobUniqueIDs.add(settler.mobUniqueID);
         }
      }

      if (!mobUniqueIDs.isEmpty()) {
         action.runAndSend(mobUniqueIDs);
      }
   }

   public void requestSettlerData(SettlementDataSubscriptionHandler handler, Predicate<SettlementSettlerData> filter) {
      HashSet<Integer> mobUniqueIDs = new HashSet<>();

      for (SettlementSettlerData settler : this.settlers) {
         if (filter.test(settler)) {
            mobUniqueIDs.add(settler.mobUniqueID);
         }
      }

      if (!mobUniqueIDs.isEmpty()) {
         handler.requestData(mobUniqueIDs);
      }
   }

   public <T> void applySettlerData(SettlementSettlerDataAbstractEvent<T> event, BiConsumer<SettlementSettlerData, T> applier) {
      boolean foundMissingSettler = false;

      for (Entry<Integer, T> entry : event.setterData.entrySet()) {
         int mobUniqueID = entry.getKey();
         GameLinkedList<SettlementSettlerData>.Element element = this.settlersMap.get(mobUniqueID);
         if (element != null) {
            T value = entry.getValue();
            if (value != null) {
               applier.accept((SettlementSettlerData)element.object, value);
            }
         } else {
            foundMissingSettler = true;
         }
      }

      if (foundMissingSettler) {
         this.container.requestSettlerBasics.runAndSend();
      }
   }

   public Stream<SettlementSettlerData> streamSettlers() {
      return this.settlers.stream();
   }

   public Iterable<SettlementSettlerData> getSettlers() {
      return this.settlers;
   }

   public SettlementSettlerData getSettler(int mobUniqueID) {
      GameLinkedList<SettlementSettlerData>.Element element = this.settlersMap.get(mobUniqueID);
      return element != null ? (SettlementSettlerData)element.object : null;
   }

   public int getTotalSettlers() {
      return this.settlers.size();
   }

   public void updateSubscriptions(boolean hasAccess) {
      if (hasAccess) {
         if (this.settlerBasicsSubscription == -1) {
            this.settlerBasicsSubscription = this.container.subscribeSettlerBasics.subscribe();
            this.container.requestSettlerBasics.runAndSend();
         }
      } else {
         if (this.settlerBasicsSubscription != -1) {
            this.container.subscribeSettlerBasics.unsubscribe(this.settlerBasicsSubscription);
         }

         this.settlerBasicsSubscription = -1;
      }
   }
}
