package necesse.entity.mobs;

import necesse.entity.mobs.ai.path.SubRegionPathResult;
import necesse.level.gameObject.DoorObject;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.SubRegion;

public class BasicPathDoorOption extends PathDoorOption {
   public final boolean canBreakDown;
   public final boolean canOpen;
   public final boolean canClose;

   public BasicPathDoorOption(String debugName, Level level, boolean canBreakDown, boolean canOpen, boolean canClose) {
      super(debugName, level);
      this.canBreakDown = canBreakDown;
      this.canOpen = canOpen;
      this.canClose = canClose;
   }

   public BasicPathDoorOption(String debugName, Level level, boolean canOpen, boolean canClose) {
      this(debugName, level, false, canOpen, canClose);
   }

   @Override
   public SubRegionPathResult canPathThrough(SubRegion subregion) {
      if (this.canBreakDown) {
         return SubRegionPathResult.VALID;
      } else if (subregion.getType().isDoor) {
         return this.canOpen ? SubRegionPathResult.VALID : SubRegionPathResult.CHECK_EACH_TILE;
      } else {
         return subregion.getType().isSolid ? SubRegionPathResult.INVALID : SubRegionPathResult.VALID;
      }
   }

   @Override
   public boolean canPathThroughCheckTile(SubRegion subregion, int tileX, int tileY) {
      GameObject object = this.level.getObject(tileX, tileY);
      return !object.isDoor
         ? false
         : ((DoorObject)object).isOpen(this.level, tileX, tileY, this.level.getObjectRotation(tileX, tileY))
            || this.canOpen(tileX, tileY) && !((DoorObject)object).isForceClosed(this.level, tileX, tileY);
   }

   @Override
   public boolean canBreakDown(int tileX, int tileY) {
      return this.canBreakDown;
   }

   @Override
   public boolean canOpen(int tileX, int tileY) {
      return this.canOpen;
   }

   @Override
   public boolean canClose(int tileX, int tileY) {
      return this.canClose;
   }

   @Override
   public boolean doorChangeInvalidatesCache(DoorObject lastDoor, DoorObject newDoor, int tileX, int tileY) {
      return (!this.canOpen(tileX, tileY) || newDoor.isForceClosed(this.level, tileX, tileY)) && !this.canBreakDown(tileX, tileY);
   }
}
