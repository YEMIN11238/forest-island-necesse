package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;

public class PlainsGrassObject extends GrassObject {
   public PlainsGrassObject() {
      super("plainsgrass", 4);
      this.grassValidTileIDs.add(TileRegistry.overgrownPlainsGrassID);
      this.grassValidTileIDs.add(TileRegistry.plainsGrassID);
      this.mapColor = new Color(184, 131, 21);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(layerID, tileX, tileY)) {
         return super.getLootTable(level, layerID, tileX, tileY);
      }

      float baitChance = 30.0F;
      if (level.weatherLayer.isRaining()) {
         baitChance = 10.0F;
      }

      if (level.isCave) {
         baitChance = 25.0F;
      }

      String seedItem = "plainsgrassseed";
      if (level.getTileID(tileX, tileY) == TileRegistry.overgrownPlainsGrassID) {
         seedItem = "overgrown" + seedItem;
      }

      return new LootTable(new ChanceLootItem(1.0F / baitChance, "wormbait"), new ChanceLootItem(0.01F, seedItem));
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }
}
