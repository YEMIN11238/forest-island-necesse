package necesse.level.maps.regionSystem;

import necesse.engine.util.GameLinkedList;
import necesse.engine.util.PointSetAbstract;
import necesse.engine.util.SingletonPointSet;
import necesse.entity.TileEntity;
import necesse.entity.manager.RegionTrackerList;
import necesse.level.maps.Level;

public abstract class TileEntityRegionPositionTracker<T extends TileEntity> implements RegionTracker<T> {
   protected final T entity;
   private final Object regionLock = new Object();
   protected GameLinkedList<? super T>.Element regionElement;

   public TileEntityRegionPositionTracker(T entity) {
      this.entity = entity;
   }

   @Override
   public PointSetAbstract<?> updateRegion(
      RegionTrackerList.RegionListGetter<T> regionsGetter, RegionTrackerList.RegionListGetter<T> saveToRegionGetter, GameLinkedList<? super T> noRegionList
   ) {
      if (this.isDisposed()) {
         return null;
      }

      Level level = this.entity.getLevel();
      if (level == null) {
         synchronized (this.regionLock) {
            if (this.regionElement != null) {
               this.regionElement.remove();
               this.regionElement = null;
            }

            return null;
         }
      } else {
         int regionX = level.regionManager.getRegionCoordByTile(this.entity.tileX);
         int regionY = level.regionManager.getRegionCoordByTile(this.entity.tileY);
         synchronized (this.regionLock) {
            if (regionsGetter != null) {
               GameLinkedList<? super T> regionList = regionsGetter.getList(regionX, regionY);
               if (this.regionElement != null) {
                  if (this.regionElement.getList() == regionList) {
                     regionList = null;
                  } else {
                     this.regionElement.remove();
                     this.regionElement = null;
                  }
               }

               if (regionList != null) {
                  this.regionElement = regionList.addLast(this.entity);
               }
            } else if (this.regionElement != null) {
               this.regionElement.remove();
               this.regionElement = null;
            }
         }

         return new SingletonPointSet(regionX, regionY);
      }
   }

   public abstract boolean isDisposed();

   @Override
   public void clearElements() {
      synchronized (this.regionLock) {
         if (this.regionElement != null) {
            this.regionElement.remove();
            this.regionElement = null;
         }
      }
   }
}
