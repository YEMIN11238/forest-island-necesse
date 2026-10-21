package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.Level;

public class DeepSwampGrassObject extends GrassObject {
   public DeepSwampGrassObject() {
      super("deepswampgrass", 4);
      this.grassValidTileIDs.add(TileRegistry.deepSwampRockID);
      this.grassValidTileIDs.add(TileRegistry.ancientRootsID);
      this.mapColor = new Color(47, 73, 44);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", "swampgrass");
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

      return new LootTable(new ChanceLootItem(1.0F / baitChance, "swamplarva"));
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }
}
