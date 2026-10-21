package necesse.level.maps.regionSystem;

import java.awt.Point;
import necesse.entity.levelEvent.LevelEvent;

public class LevelEventRegionPositionsTracker extends AllRegionPositionsTracker<LevelEvent> {
   public LevelEventRegionPositionsTracker(LevelEvent event) {
      super(event);
   }

   @Override
   public boolean isDisposed() {
      return this.regionPositionGetter.isOver();
   }

   @Override
   public Point getSaveToRegionPos() {
      return !this.regionPositionGetter.shouldSave() ? null : this.regionPositionGetter.getSaveToRegionPos();
   }
}
