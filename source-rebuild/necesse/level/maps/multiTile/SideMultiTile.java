package necesse.level.maps.multiTile;

import java.awt.Point;

public class SideMultiTile extends MultiTile {
   public SideMultiTile(int x, int y, int width, int height, int rotation, boolean isMaster, int... ids) {
      super(x, y, width, height, rotation, isMaster, ids);
   }

   @Override
   public Point getMirrorXPosOffset() {
      Point centerTileOffset = this.getCenterTileOffset();
      return this.rotation != 0 && this.rotation != 2 ? new Point(-centerTileOffset.x, -centerTileOffset.y) : centerTileOffset;
   }

   @Override
   public Point getMirrorYPosOffset() {
      Point centerTileOffset = this.getCenterTileOffset();
      return this.rotation != 1 && this.rotation != 3 ? new Point(-centerTileOffset.x, -centerTileOffset.y) : centerTileOffset;
   }

   @Override
   public int getXMirrorRotation() {
      if (this.rotation == 0) {
         return 2;
      } else {
         return this.rotation == 2 ? 0 : this.rotation;
      }
   }

   @Override
   public int getYMirrorRotation() {
      if (this.rotation == 1) {
         return 3;
      } else {
         return this.rotation == 3 ? 1 : this.rotation;
      }
   }
}
