package necesse.entity.manager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.network.packet.PacketLevelEvent;
import necesse.engine.network.packet.PacketLevelEventOver;
import necesse.engine.util.ConcurrentHashMapQueue;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobAbilityLevelEvent;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;

public class LevelEventsManager implements Iterable<LevelEvent> {
   protected final Level level;
   protected final EntityManager manager;
   protected final ConcurrentHashMap<Integer, LevelEvent> events = new ConcurrentHashMap<>();
   public int cacheTTL = 5000;
   protected final ConcurrentHashMapQueue<Integer, LevelEventsManager.CachedLevelEvent> eventsCache = new ConcurrentHashMapQueue<>();
   public final LevelEventRegionList<LevelEvent> regionList;

   public LevelEventsManager(EntityManager manager) {
      this.manager = manager;
      this.level = manager.level;
      this.regionList = new LevelEventRegionList<>(this.level);
   }

   public void add(LevelEvent event) {
      this.addHidden(event);
      if (this.level.isServer()) {
         this.level.getServer().network.sendToClientsWithEntity(new PacketLevelEvent(event), event);
      }
   }

   public void addHidden(LevelEvent event) {
      event.level = this.level;
      int uniqueID = event.getUniqueID();
      event.setUniqueID(uniqueID);
      synchronized (this.manager.lock) {
         this.events.compute(uniqueID, (uid, last) -> {
            if (last != null && last != event) {
               this.runRemoveLogic(last, false);
            }

            return event;
         });
         this.manager.componentManager.add(uniqueID, event);
         if (event instanceof LevelBuffsEntityComponent) {
            this.level.buffManager.updateBuffs();
         }
      }

      event.init();
      synchronized (this.manager.lock) {
         this.regionList.updateRegion(event);
      }

      if (this.level.isServer() && event instanceof MobAbilityLevelEvent) {
         this.manager.submittedHits.submitNewMobAbilityLevelEvent((MobAbilityLevelEvent)event);
      }
   }

   public LevelEvent get(int uniqueID, boolean searchCache) {
      synchronized (this.manager.lock) {
         LevelEvent event = this.events.get(uniqueID);
         if (event != null) {
            return event;
         }
      }

      if (searchCache) {
         synchronized (this.manager.lock) {
            LevelEventsManager.CachedLevelEvent out = this.eventsCache.get(uniqueID);
            if (out != null) {
               return out.event;
            }
         }
      }

      return null;
   }

   public boolean hasUniqueID(int uniqueID) {
      return this.events.containsKey(uniqueID);
   }

   protected void runRemoveLogic(LevelEvent event, boolean sendPacket) {
      event.onDispose();
      if (this.level.isServer() && sendPacket && event.shouldSendOverPacket()) {
         this.level.getServer().network.sendToClientsWithEntity(new PacketLevelEventOver(event.getUniqueID()), event);
      }
   }

   @Override
   public Iterator<LevelEvent> iterator() {
      return this.events.values().iterator();
   }

   public Stream<LevelEvent> stream() {
      return this.events.values().stream();
   }

   public int count() {
      return this.events.size();
   }

   public int clearLevelEvents(String stringID) {
      synchronized (this.manager.lock) {
         LinkedList<Integer> removes = new LinkedList<>();
         this.events.forEach((uniqueIDx, event) -> {
            if (event == null) {
               removes.add(uniqueIDx);
            } else if (event.getStringID().contains(stringID)) {
               removes.add(uniqueIDx);
               event.over();
               this.runRemoveLogic(event, true);
               if (this.cacheTTL > 0) {
                  this.eventsCache.addLast(uniqueIDx, new LevelEventsManager.CachedLevelEvent(uniqueIDx, event));
               }
            }
         });
         boolean updateBuffs = false;

         for (int uniqueID : removes) {
            LevelEvent remove = this.events.remove(uniqueID);
            if (remove != null) {
               this.manager.componentManager.remove(uniqueID, remove);
               if (remove instanceof LevelBuffsEntityComponent) {
                  updateBuffs = true;
               }
            }
         }

         if (updateBuffs) {
            this.level.buffManager.updateBuffs();
         }

         return removes.size();
      }
   }

   public int clearLevelEvents() {
      synchronized (this.manager.lock) {
         int amount = this.events.size();
         LinkedList<Integer> removes = new LinkedList<>();
         this.events.forEach((uniqueIDx, event) -> {
            removes.add(uniqueIDx);
            event.over();
            this.runRemoveLogic(event, true);
            if (this.cacheTTL > 0) {
               this.eventsCache.addLast(uniqueIDx, new LevelEventsManager.CachedLevelEvent(uniqueIDx, event));
            }
         });
         boolean updateBuffs = false;

         for (int uniqueID : removes) {
            LevelEvent remove = this.events.remove(uniqueID);
            if (remove != null) {
               this.manager.componentManager.remove(uniqueID, remove);
               if (remove instanceof LevelBuffsEntityComponent) {
                  updateBuffs = true;
               }
            }
         }

         if (updateBuffs) {
            this.level.buffManager.updateBuffs();
         }

         return amount;
      }
   }

   public void clientTick() {
      synchronized (this.manager.lock) {
         while (!this.eventsCache.isEmpty()) {
            LevelEventsManager.CachedLevelEvent ce = this.eventsCache.getFirst();
            if (!ce.shouldDie()) {
               break;
            }

            this.eventsCache.removeFirst();
         }

         LinkedList<Integer> removes = new LinkedList<>();
         this.events.forEach((uniqueIDx, event) -> {
            if (event == null) {
               removes.add(uniqueIDx);
            } else if (event.isOver()) {
               removes.add(uniqueIDx);
               this.runRemoveLogic(event, true);
               if (this.cacheTTL > 0) {
                  this.eventsCache.addLast(uniqueIDx, new LevelEventsManager.CachedLevelEvent(uniqueIDx, event));
               }
            } else if (event.getUniqueID() != uniqueIDx) {
               GameLog.warn.println("LevelEvent has changed uniqueID from " + uniqueIDx + " to " + event.getUniqueID() + ", removing it from level");
               removes.add(uniqueIDx);
               this.runRemoveLogic(event, true);
            } else {
               event.clientTick();
            }
         });
         boolean updateBuffs = false;

         for (int uniqueID : removes) {
            LevelEvent remove = this.events.remove(uniqueID);
            if (remove != null) {
               this.manager.componentManager.remove(uniqueID, remove);
               if (remove instanceof LevelBuffsEntityComponent) {
                  updateBuffs = true;
               }
            }
         }

         if (updateBuffs) {
            this.level.buffManager.updateBuffs();
         }
      }

      Performance.record(this.level.tickManager(), "regionEnt", () -> Performance.record(this.level.tickManager(), "events", () -> {
         synchronized (this.manager.lock) {
            this.events.values().forEach(this.regionList::updateRegion);
         }
      }));
   }

   public void serverTick() {
      synchronized (this.manager.lock) {
         while (!this.eventsCache.isEmpty()) {
            LevelEventsManager.CachedLevelEvent ce = this.eventsCache.getFirst();
            if (!ce.shouldDie()) {
               break;
            }

            this.eventsCache.removeFirst();
         }

         LinkedList<Integer> removes = new LinkedList<>();
         this.events.forEach((uniqueIDx, event) -> {
            if (event == null) {
               removes.add(uniqueIDx);
            } else if (event.isOver()) {
               removes.add(uniqueIDx);
               this.runRemoveLogic(event, true);
               if (this.cacheTTL > 0) {
                  this.eventsCache.addLast(uniqueIDx, new LevelEventsManager.CachedLevelEvent(uniqueIDx, event));
               }
            } else if (event.getUniqueID() != uniqueIDx) {
               GameLog.warn.println("LevelEvent has changed uniqueID from " + uniqueIDx + " to " + event.getUniqueID() + ", removing it from level");
               removes.add(uniqueIDx);
               this.runRemoveLogic(event, true);
            } else {
               event.serverTick();
            }
         });
         boolean updateBuffs = false;

         for (int uniqueID : removes) {
            LevelEvent remove = this.events.remove(uniqueID);
            if (remove != null) {
               this.manager.componentManager.remove(uniqueID, remove);
               if (remove instanceof LevelBuffsEntityComponent) {
                  updateBuffs = true;
               }
            }
         }

         if (updateBuffs) {
            this.level.buffManager.updateBuffs();
         }
      }

      Performance.record(this.level.tickManager(), "regionEnt", () -> Performance.record(this.level.tickManager(), "events", () -> {
         synchronized (this.manager.lock) {
            this.events.values().forEach(this.regionList::updateRegion);
         }
      }));
   }

   public void onLoadingComplete() {
      this.events.values().forEach(LevelEvent::onLoadingComplete);
   }

   public void onUnloading() {
      this.events.values().forEach(event -> event.onUnloading(null));
   }

   public void dispose() {
      this.events.values().forEach(LevelEvent::onDispose);
   }

   protected class CachedLevelEvent {
      public final int uniqueID;
      public final LevelEvent event;
      public final long endTime;

      public CachedLevelEvent(int uniqueID, LevelEvent event) {
         this.uniqueID = uniqueID;
         this.event = event;
         this.endTime = LevelEventsManager.this.level.getTime() + LevelEventsManager.this.cacheTTL;
      }

      public boolean shouldDie() {
         return this.endTime <= LevelEventsManager.this.level.getWorldEntity().getTime();
      }
   }
}
