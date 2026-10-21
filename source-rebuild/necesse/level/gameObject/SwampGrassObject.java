package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;

public class SwampGrassObject extends GrassObject {
   public SwampGrassObject() {
      super("swampgrass", 4);
      this.mapColor = new Color(88, 103, 51);
      this.grassValidTileIDs.add(TileRegistry.swampRockID);
      this.grassValidTileIDs.add(TileRegistry.overgrownSwampGrassID);
      this.grassValidTileIDs.add(TileRegistry.swampGrassID);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         return super.getLootTable(level, layerID, tileX, tileY);
      }

      float baitChance = 30.0F;
      if (level.weatherLayer.isRaining()) {
         baitChance = 10.0F;
      }

      if (level.isCave) {
         baitChance = 25.0F;
      }

      String seedItem = "swampgrassseed";
      if (level.getTileID(tileX, tileY) == TileRegistry.overgrownSwampGrassID) {
         seedItem = "overgrown" + seedItem;
      }

      return new LootTable(new ChanceLootItem(1.0F / baitChance, "swamplarva"), new ChanceLootItem(0.01F, seedItem));
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }
}
