package necesse.entity.manager;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.ConcurrentHashMapQueue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.DrawOnMapEntity;
import necesse.entity.Entity;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;

public class EntityList<T extends Entity> implements Iterable<T> {
   protected final ConcurrentHashMap<Integer, T> map = new ConcurrentHashMap<>();
   protected final ConcurrentHashMapQueue<Integer, EntityList<T>.CachedEntity> cache = new ConcurrentHashMapQueue<>();
   protected int cacheTTL = 5000;
   protected final EntityManager manager;
   protected final boolean useEntityComponents;
   public final String entityName;
   protected final Function<T, Integer> uniqueIDGetter;
   protected int nextUniqueID;
   protected final Function<T, Boolean> isRemoved;
   protected final Consumer<T> dispose;
   protected final Consumer<T> onAdded;
   Consumer<T> onHiddenAdded;
   protected final Consumer<T> onRemoved;
   protected final EntityRegionList<T> regionList;

   public EntityList(
      EntityManager manager,
      boolean useEntityComponents,
      String entityName,
      Function<T, Integer> uniqueIDGetter,
      Function<T, Boolean> isRemoved,
      Consumer<T> dispose,
      Consumer<T> onAdded,
      Consumer<T> onRemoved,
      boolean trackRegions
   ) {
      this.manager = manager;
      this.useEntityComponents = useEntityComponents;
      this.entityName = entityName;
      this.uniqueIDGetter = uniqueIDGetter;
      this.isRemoved = isRemoved;
      this.dispose = dispose;
      this.onAdded = onAdded;
      this.onRemoved = onRemoved;
      if (trackRegions) {
         this.regionList = new EntityRegionList<>(this.getLevel());
      } else {
         this.regionList = null;
      }
   }

   public EntityList(EntityManager manager, boolean useEntityComponents, String entityName, Consumer<T> onAdded, Consumer<T> onRemoved, boolean trackRegions) {
      this(manager, useEntityComponents, entityName, Entity::getUniqueID, Entity::removed, e -> {
         if (!e.isDisposed()) {
            e.dispose();
         }
      }, onAdded, onRemoved, trackRegions);
   }

   protected Level getLevel() {
      return this.manager.level;
   }

   @Override
   public Iterator<T> iterator() {
      return this.map.values().iterator();
   }

   @Override
   public void forEach(Consumer<? super T> action) {
      this.map.values().forEach(action);
   }

   @Override
   public Spliterator<T> spliterator() {
      return this.map.values().spliterator();
   }

   public Stream<T> stream() {
      synchronized (this.manager.lock) {
         return this.map.values().stream();
      }
   }

   public int count() {
      return this.map.size();
   }

   public int countCache() {
      return this.cache.size();
   }

   public boolean hasUniqueID(int uniqueID, boolean searchCache) {
      if (this.uniqueIDGetter == null) {
         return false;
      }

      synchronized (this.manager.lock) {
         if (this.map.containsKey(uniqueID)) {
            return true;
         }
      }

      if (searchCache) {
         synchronized (this.manager.lock) {
            if (this.cache.containsKey(uniqueID)) {
               return true;
            }
         }
      }

      return false;
   }

   public T get(int uniqueID, boolean searchCache) {
      if (this.uniqueIDGetter == null) {
         return null;
      }

      synchronized (this.manager.lock) {
         T out = this.map.get(uniqueID);
         if (out != null) {
            return out;
         }
      }

      if (searchCache) {
         synchronized (this.manager.lock) {
            EntityList<T>.CachedEntity out = this.cache.get(uniqueID);
            if (out != null) {
               return out.entity;
            }
         }
      }

      return null;
   }

   public void add(T entity) {
      this.addHidden(entity);
      if (this.onAdded != null) {
         this.onAdded.accept(entity);
      }
   }

   public void addHidden(T entity) {
      if (this.uniqueIDGetter != null) {
         int uniqueID = this.uniqueIDGetter.apply(entity);
         entity.setUniqueID(uniqueID);
         synchronized (this.manager.lock) {
            this.map.compute(uniqueID, (uid, last) -> {
               if (last != null && last != entity) {
                  this.runRemoveLogic((T)last);
               }

               return entity;
            });
            if (this.useEntityComponents) {
               this.manager.componentManager.add(uniqueID, entity);
               if (entity instanceof LevelBuffsEntityComponent) {
                  this.manager.level.buffManager.updateBuffs();
               }
            }
         }
      } else {
         synchronized (this.manager.lock) {
            if (this.map.containsValue(entity)) {
               return;
            }

            int uniqueID = this.nextUniqueID++;
            entity.setUniqueID(uniqueID);
            this.map.compute(uniqueID, (uid, last) -> {
               if (last != null && last != entity) {
                  this.runRemoveLogic((T)last);
               }

               return entity;
            });
            if (this.useEntityComponents) {
               this.manager.componentManager.add(uniqueID, entity);
               if (entity instanceof LevelBuffsEntityComponent) {
                  this.getLevel().buffManager.updateBuffs();
               }
            }
         }
      }

      entity.setLevel(this.getLevel());
      if (!entity.isInitialized()) {
         entity.init();
         entity.postInit();
      } else {
         entity.onLevelChanged();
      }

      if (this.onHiddenAdded != null) {
         this.onHiddenAdded.accept(entity);
      }

      if (this.regionList != null) {
         synchronized (this.manager.lock) {
            this.regionList.updateRegion(entity);
         }
      }
   }

   public EntityRegionList<T> getRegionList() {
      return this.regionList;
   }

   public GameLinkedList<T> getSaveToRegion(int regionX, int regionY) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getSaveToRegion(regionX, regionY);
         }
      }
   }

   public GameLinkedList<T> getInRegion(int regionX, int regionY) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getInRegion(regionX, regionY);
         }
      }
   }

   public GameLinkedList<T> getInNoRegion() {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getInNoRegion();
         }
      }
   }

   public Stream<T> streamInRegionsShape(Shape shape, int extraRegionRange) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.streamInRegionsShape(shape, extraRegionRange);
         }
      }
   }

   public Stream<T> streamInRegionsInRange(float x, float y, int range) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.streamInRegionsInRange(x, y, range);
         }
      }
   }

   public Stream<T> streamInRegionsInTileRange(int x, int y, int tileRange) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.streamInRegionsInTileRange(x, y, tileRange);
         }
      }
   }

   public GameAreaStream<T> streamArea(float x, float y, int range) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.streamArea(x, y, range);
         }
      }
   }

   public GameAreaStream<T> streamAreaTileRange(int x, int y, int tileRange) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.streamAreaTileRange(x, y, tileRange);
         }
      }
   }

   public GameLinkedList<T> getInRegionTileByTile(int tileX, int tileY) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getInRegionTileByTile(tileX, tileY);
         }
      }
   }

   public ArrayList<T> getInRegionRange(int regionX, int regionY, int regionRange) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getInRegionRange(regionX, regionY, regionRange);
         }
      }
   }

   public ArrayList<T> getInRegionRangeByTile(int tileX, int tileY, int regionRange) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getInRegionRangeByTile(tileX, tileY, regionRange);
         }
      }
   }

   public ArrayList<T> getInRegionByTileRange(int tileX, int tileY, int tileRange) {
      synchronized (this.manager.lock) {
         if (this.regionList == null) {
            throw new IllegalStateException(this.entityName + " list region data not supported");
         } else {
            return this.regionList.getInRegionByTileRange(tileX, tileY, tileRange);
         }
      }
   }

   public void frameTick(TickManager tickManager, BiConsumer<T, Float> tickMovement) {
      Level level = this.getLevel();
      if (tickMovement != null) {
         float delta = tickManager.getDelta();
         Performance.record(level.tickManager(), "movement", () -> Performance.record(level.tickManager(), this.entityName, () -> {
            synchronized (this.manager.lock) {
               for (T entity : this.map.values()) {
                  if (entity != null && !this.isRemoved.apply(entity)) {
                     tickMovement.accept(entity, delta);
                  }
               }
            }
         }));
      }

      if (this.regionList != null) {
         Performance.record(level.tickManager(), "regionEnt", () -> Performance.record(level.tickManager(), this.entityName, () -> {
            synchronized (this.manager.lock) {
               for (T entity : this.map.values()) {
                  if (entity != null) {
                     this.regionList.updateRegion(entity);
                  }
               }
            }
         }));
      }
   }

   public void clientTick(Consumer<T> entityTick, List<DrawOnMapEntity> drawOnMap) {
      synchronized (this.manager.lock) {
         while (!this.cache.isEmpty()) {
            EntityList<T>.CachedEntity ce = this.cache.getFirst();
            if (!ce.shouldDie()) {
               break;
            }

            this.cache.removeFirst();
         }
      }

      synchronized (this.manager.lock) {
         LinkedList<Integer> removes = new LinkedList<>();
         this.map.forEach((uniqueIDx, entity) -> {
            if (entity == null) {
               removes.add(uniqueIDx);
            } else if (this.isRemoved.apply((T)entity)) {
               removes.add(uniqueIDx);
               this.runRemoveLogic((T)entity);
               if (this.cacheTTL > 0) {
                  this.cache.addLast(uniqueIDx, new EntityList.CachedEntity(uniqueIDx, (T)entity));
               }
            } else if (entity.getUniqueID() != uniqueIDx) {
               GameLog.warn.println(this.entityName + " has changed uniqueID from " + uniqueIDx + " to " + entity.getUniqueID() + ", removing it from level");
               removes.add(uniqueIDx);
               this.runRemoveLogic((T)entity);
            } else {
               if (entity.shouldDrawOnMap()) {
                  drawOnMap.add(entity);
               }

               entityTick.accept((T)entity);
               entity.updateRegionPos();
            }
         });
         boolean updateBuffs = false;

         for (int uniqueID : removes) {
            T remove = this.map.remove(uniqueID);
            if (this.useEntityComponents && remove != null) {
               this.manager.componentManager.remove(uniqueID, remove);
               if (remove instanceof LevelBuffsEntityComponent) {
                  updateBuffs = true;
               }
            }
         }

         if (updateBuffs) {
            this.getLevel().buffManager.updateBuffs();
         }
      }
   }

   public void serverTick(Consumer<T> entityTick, List<DrawOnMapEntity> drawOnMap) {
      synchronized (this.manager.lock) {
         while (!this.cache.isEmpty()) {
            EntityList<T>.CachedEntity ce = this.cache.getFirst();
            if (!ce.shouldDie()) {
               break;
            }

            this.cache.removeFirst();
         }
      }

      synchronized (this.manager.lock) {
         LinkedList<Integer> removes = new LinkedList<>();
         this.map.forEach((uniqueIDx, entity) -> {
            if (entity == null) {
               removes.add(uniqueIDx);
            } else if (this.isRemoved.apply((T)entity)) {
               removes.add(uniqueIDx);
               this.runRemoveLogic((T)entity);
               if (this.cacheTTL > 0) {
                  this.cache.addLast(uniqueIDx, new EntityList.CachedEntity(uniqueIDx, (T)entity));
               }
            } else if (entity.getUniqueID() != uniqueIDx) {
               GameLog.warn.println(this.entityName + " has changed uniqueID from " + uniqueIDx + " to " + entity.getUniqueID() + ", removing it from level");
               removes.add(uniqueIDx);
               this.runRemoveLogic((T)entity);
            } else {
               if (entity.shouldDrawOnMap()) {
                  drawOnMap.add(entity);
               }

               entityTick.accept((T)entity);
               entity.updateRegionPos();
            }
         });
         boolean updateBuffs = false;

         for (int uniqueID : removes) {
            T remove = this.map.remove(uniqueID);
            if (this.useEntityComponents && remove != null) {
               this.manager.componentManager.remove(uniqueID, remove);
               if (remove instanceof LevelBuffsEntityComponent) {
                  updateBuffs = true;
               }
            }
         }

         if (updateBuffs) {
            this.getLevel().buffManager.updateBuffs();
         }
      }
   }

   protected void runRemoveLogic(T entity) {
      if (this.onRemoved != null) {
         this.onRemoved.accept(entity);
      }

      if (!entity.removed()) {
         entity.remove();
      }

      entity.onRemovedFromManager();
      if (this.dispose != null) {
         this.dispose.accept(entity);
      }
   }

   public void onLoadingComplete() {
      synchronized (this.manager.lock) {
         this.map.values().forEach(Entity::onLoadingComplete);
      }
   }

   public void onUnloading() {
      synchronized (this.manager.lock) {
         this.map.values().forEach(t -> t.onUnloading(null));
      }
   }

   public void dispose() {
      synchronized (this.manager.lock) {
         if (this.dispose != null) {
            this.map.values().forEach(this.dispose);
         }
      }
   }

   protected class CachedEntity {
      public final int uniqueID;
      public final T entity;
      public final long endTime;

      public CachedEntity(int uniqueID, T entity) {
         this.uniqueID = uniqueID;
         this.entity = entity;
         this.endTime = EntityList.this.getLevel().getTime() + EntityList.this.cacheTTL;
      }

      public boolean shouldDie() {
         return this.endTime <= EntityList.this.getLevel().getTime();
      }
   }
}
