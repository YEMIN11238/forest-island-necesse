package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;

public class CryptGrassObject extends GrassObject {
   public CryptGrassObject() {
      super("cryptgrass", 1);
      this.mapColor = new Color(61, 50, 47);
      this.grassValidTileIDs.add(TileRegistry.cryptAshID);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY) ? super.getLootTable(level, layerID, tileX, tileY) : new LootTable();
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }
}
