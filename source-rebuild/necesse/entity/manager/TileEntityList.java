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
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.ConcurrentHashMapQueue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.DrawOnMapEntity;
import necesse.entity.TileEntity;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;

public class TileEntityList<T extends TileEntity> implements Iterable<T> {
   protected final ConcurrentHashMap<Long, T> map = new ConcurrentHashMap<>();
   protected final ConcurrentHashMapQueue<Long, TileEntityList<T>.CachedEntity> cache = new ConcurrentHashMapQueue<>();
   protected int cacheTTL = 5000;
   protected final EntityManager manager;
   protected final EntityComponentManager<Long> componentManager;
   public final String entityName;
   protected final Function<T, Boolean> isRemoved;
   protected final Consumer<T> dispose;
   protected final Consumer<T> onAdded;
   Consumer<T> onHiddenAdded;
   protected final Consumer<T> onRemoved;
   protected final TileEntityRegionList<T> regionList;

   public TileEntityList(
      EntityManager manager,
      EntityComponentManager<Long> componentManager,
      String entityName,
      Function<T, Boolean> isRemoved,
      Consumer<T> dispose,
      Consumer<T> onAdded,
      Consumer<T> onRemoved
   ) {
      this.manager = manager;
      this.componentManager = componentManager;
      this.entityName = entityName;
      this.isRemoved = isRemoved;
      this.dispose = dispose;
      this.onAdded = onAdded;
      this.onRemoved = onRemoved;
      this.regionList = new TileEntityRegionList<>(this.getLevel());
   }

   public TileEntityList(EntityManager manager, EntityComponentManager<Long> componentManager, String entityName, Consumer<T> onAdded, Consumer<T> onRemoved) {
      this(manager, componentManager, entityName, TileEntity::removed, e -> {
         if (!e.isDisposed()) {
            e.dispose();
         }
      }, onAdded, onRemoved);
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

   protected long getUniqueID(int tileX, int tileY) {
      return GameMath.getUniqueLongKey(tileX, tileY);
   }

   public boolean hasEntityID(int tileX, int tileY, boolean searchCache) {
      long uniqueID = this.getUniqueID(tileX, tileY);
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

   public T get(int tileX, int tileY, boolean searchCache) {
      long uniqueID = this.getUniqueID(tileX, tileY);
      synchronized (this.manager.lock) {
         T out = this.map.get(uniqueID);
         if (out != null) {
            return out;
         }
      }

      if (searchCache) {
         synchronized (this.manager.lock) {
            TileEntityList<T>.CachedEntity out = this.cache.get(uniqueID);
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
      long uniqueID = this.getUniqueID(entity.tileX, entity.tileY);
      synchronized (this.manager.lock) {
         this.map.compute(uniqueID, (uid, last) -> {
            if (last != null && last != entity) {
               this.runRemoveLogic((T)last);
            }

            return entity;
         });
         if (this.componentManager != null) {
            this.componentManager.add(uniqueID, entity);
            if (entity instanceof LevelBuffsEntityComponent) {
               this.manager.level.buffManager.updateBuffs();
            }
         }
      }

      if (!entity.isInitialized()) {
         entity.init();
         entity.postInit();
      } else {
         entity.onLevelChanged();
      }

      if (this.onHiddenAdded != null) {
         this.onHiddenAdded.accept(entity);
      }

      synchronized (this.manager.lock) {
         this.regionList.updateRegion(entity);
      }
   }

   public TileEntityRegionList<T> getRegionList() {
      return this.regionList;
   }

   public GameLinkedList<T> getInRegion(int regionX, int regionY) {
      synchronized (this.manager.lock) {
         return this.regionList.getInRegion(regionX, regionY);
      }
   }

   public Stream<T> streamInRegionsShape(Shape shape, int extraRegionRange) {
      synchronized (this.manager.lock) {
         return this.regionList.streamInRegionsShape(shape, extraRegionRange);
      }
   }

   public Stream<T> streamInRegionsInTileRange(int tileX, int tileY, int tileRange) {
      synchronized (this.manager.lock) {
         return this.regionList.streamInRegionsInTileRange(tileX, tileY, tileRange);
      }
   }

   public GameAreaStream<T> streamAreaTileRange(int tileX, int tileY, int tileRange) {
      synchronized (this.manager.lock) {
         return this.regionList.streamAreaTileRange(tileX, tileY, tileRange);
      }
   }

   public GameLinkedList<T> getInRegionTileByTile(int tileX, int tileY) {
      synchronized (this.manager.lock) {
         return this.regionList.getInRegionTileByTile(tileX, tileY);
      }
   }

   public ArrayList<T> getInRegionRange(int regionX, int regionY, int regionRange) {
      synchronized (this.manager.lock) {
         return this.regionList.getInRegionRange(regionX, regionY, regionRange);
      }
   }

   public ArrayList<T> getInRegionRangeByTile(int tileX, int tileY, int regionRange) {
      synchronized (this.manager.lock) {
         return this.regionList.getInRegionRangeByTile(tileX, tileY, regionRange);
      }
   }

   public ArrayList<T> getInRegionByTileRange(int tileX, int tileY, int tileRange) {
      synchronized (this.manager.lock) {
         return this.regionList.getInRegionByTileRange(tileX, tileY, tileRange);
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
   }

   public void clientTick(Consumer<T> entityTick, List<DrawOnMapEntity> drawOnMap) {
      synchronized (this.manager.lock) {
         while (!this.cache.isEmpty()) {
            TileEntityList<T>.CachedEntity ce = this.cache.getFirst();
            if (!ce.shouldDie()) {
               break;
            }

            this.cache.removeFirst();
         }
      }

      synchronized (this.manager.lock) {
         LinkedList<Long> removes = new LinkedList<>();
         this.map.forEach((uniqueIDx, entity) -> {
            if (entity == null) {
               removes.add(uniqueIDx);
            } else if (this.isRemoved.apply((T)entity)) {
               removes.add(uniqueIDx);
               this.runRemoveLogic((T)entity);
               if (this.cacheTTL > 0) {
                  this.cache.addLast(uniqueIDx, new TileEntityList.CachedEntity(uniqueIDx, (T)entity));
               }
            } else {
               if (entity.shouldDrawOnMap()) {
                  drawOnMap.add(entity);
               }

               entityTick.accept((T)entity);
            }
         });
         boolean updateBuffs = false;

         for (long uniqueID : removes) {
            T remove = this.map.remove(uniqueID);
            if (this.componentManager != null && remove != null) {
               this.componentManager.remove(uniqueID, remove);
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
            TileEntityList<T>.CachedEntity ce = this.cache.getFirst();
            if (!ce.shouldDie()) {
               break;
            }

            this.cache.removeFirst();
         }
      }

      synchronized (this.manager.lock) {
         LinkedList<Long> removes = new LinkedList<>();
         this.map.forEach((uniqueIDx, entity) -> {
            if (entity == null) {
               removes.add(uniqueIDx);
            } else if (this.isRemoved.apply((T)entity)) {
               removes.add(uniqueIDx);
               this.runRemoveLogic((T)entity);
               if (this.cacheTTL > 0) {
                  this.cache.addLast(uniqueIDx, new TileEntityList.CachedEntity(uniqueIDx, (T)entity));
               }
            } else {
               if (entity.shouldDrawOnMap()) {
                  drawOnMap.add(entity);
               }

               entityTick.accept((T)entity);
            }
         });
         boolean updateBuffs = false;

         for (long uniqueID : removes) {
            T remove = this.map.remove(uniqueID);
            if (this.componentManager != null && remove != null) {
               this.componentManager.remove(uniqueID, remove);
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
         this.map.values().forEach(TileEntity::onLoadingComplete);
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
      public final long uniqueID;
      public final T entity;
      public final long endTime;

      public CachedEntity(long uniqueID, T entity) {
         this.uniqueID = uniqueID;
         this.entity = entity;
         this.endTime = TileEntityList.this.getLevel().getTime() + TileEntityList.this.cacheTTL;
      }

      public boolean shouldDie() {
         return this.endTime <= TileEntityList.this.getLevel().getTime();
      }
   }
}
