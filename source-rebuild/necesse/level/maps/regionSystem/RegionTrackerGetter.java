package necesse.level.maps.regionSystem;

import necesse.engine.util.GameLinkedList;
import necesse.engine.util.PointSetAbstract;
import necesse.entity.manager.RegionTrackerList;

public interface RegionTrackerGetter<T> {
   RegionTracker<T> getRegionTracker();

   default PointSetAbstract<?> updateRegion(
      RegionTrackerList.RegionListGetter<T> regionsGetter, RegionTrackerList.RegionListGetter<T> saveToRegionGetter, GameLinkedList<? super T> noRegionList
   ) {
      RegionTracker<T> regionTracker = this.getRegionTracker();
      return regionTracker != null ? regionTracker.updateRegion(regionsGetter, saveToRegionGetter, noRegionList) : null;
   }
}
