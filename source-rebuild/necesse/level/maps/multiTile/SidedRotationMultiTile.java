package necesse.level.maps.multiTile;

import java.awt.Point;

public class SidedRotationMultiTile extends MultiTile {
   public SidedRotationMultiTile(int x, int y, int width, int height, int rotation, boolean isMaster, int... ids) {
      super(x, y, width, height, rotation, isMaster, ids);
   }

   @Override
   public Point getMirrorXPosOffset() {
      Point centerTileOffset = this.getCenterTileOffset();
      return this.rotation != 1 && this.rotation != 3 ? new Point(-centerTileOffset.x, 0) : new Point(0, centerTileOffset.y);
   }

   @Override
   public Point getMirrorYPosOffset() {
      Point centerTileOffset = this.getCenterTileOffset();
      return this.rotation != 0 && this.rotation != 2 ? new Point(0, -centerTileOffset.y) : new Point(centerTileOffset.x, 0);
   }
}
