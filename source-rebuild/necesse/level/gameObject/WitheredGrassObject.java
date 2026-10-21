package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;

public class WitheredGrassObject extends GrassObject {
   public WitheredGrassObject() {
      super("witheredgrass", 4);
      this.grassValidTileIDs.add(TileRegistry.rustGravelID);
      this.grassValidTileIDs.add(TileRegistry.dirtID);
      this.grassValidTileIDs.add(TileRegistry.gravelID);
      this.mapColor = new Color(158, 94, 49);
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }
}
