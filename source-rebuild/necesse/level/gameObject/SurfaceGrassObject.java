package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;

public class SurfaceGrassObject extends GrassObject {
   public SurfaceGrassObject() {
      super("grass", 2);
      this.mapColor = new Color(69, 105, 0);
      this.grassValidTileIDs.add(TileRegistry.overgrownGrassID);
      this.grassValidTileIDs.add(TileRegistry.grassID);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         return super.getLootTable(level, layerID, tileX, tileY);
      }

      float baitChance = 35.0F;
      if (level.weatherLayer.isRaining()) {
         baitChance = 15.0F;
      }

      String seedItem = "grassseed";
      if (level.getTileID(tileX, tileY) == TileRegistry.overgrownGrassID) {
         seedItem = "overgrown" + seedItem;
      }

      return new LootTable(new ChanceLootItem(1.0F / baitChance, "wormbait"), new ChanceLootItem(0.01F, seedItem));
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }
}
