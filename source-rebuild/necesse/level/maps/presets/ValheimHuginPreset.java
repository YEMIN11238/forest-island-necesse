package necesse.level.maps.presets;

import java.awt.Point;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.HashMapPointEntry;
import necesse.entity.objectEntity.HuginStatueObjectEntity;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.generationModules.LinesGeneration;

public class ValheimHuginPreset extends Preset {
   public ValheimHuginPreset(GameRandom random) {
      super(HuginStatueObjectEntity.fogTileRange.maxRange * 2 + 2, HuginStatueObjectEntity.fogTileRange.maxRange * 2 + 2);
      int presetWidth = 22;
      int presetHeight = 20;
      int presetX = this.width / 2 - presetWidth / 2;
      int presetY = this.height / 2 - presetHeight / 2;
      this.applyScript(
         presetX,
         presetY,
         "PRESET = {\n\twidth = 22,\n\theight = 20,\n\ttileIDs = [83, graveltile],\n\ttiles = [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 83, 83, 83, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 83, 83, 83, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 83, 83, 83, -1, 83, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 83, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],\n\tobjectIDs = [0, air, 2, oaktree, 3, oaktreestump, 5, sprucetree, 37, deadwoodtree, 7, sprucelogbench, 8, sprucelogbench2, 296, sack, 1097, roastingstation, 10, pinetree, 408, huginstatue, 1112, surfacerock, 409, huginstatue2, 1113, surfacerockr, 892, forgottenblade],\n\tobjects = [-1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, -1, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 10, 0, 0, 0, 0, 3, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, -1, -1, -1, 0, 0, 37, 0, 0, 0, 0, 0, 0, 408, 409, 0, 0, 0, 0, 0, 0, 10, 0, 0, -1, 0, 0, 0, 0, 0, 0, 1112, 1113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1112, 1113, 0, 7, 8, 892, 0, 37, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 296, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1097, 0, 3, 0, -1, 0, 10, 0, 1112, 1113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1112, 1113, 0, 0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 37, 0, 0, 0, 0, 1112, 1113, 0, 0, 0, 1112, 1113, 0, 0, 0, 37, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, 0, 0, 5, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 2, 0, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, 0, 10, 0, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 10, 0, -1, -1, 0, 3, 0, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, -1, -1, -1, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1],\n\trotations = [3, 3, 3, 3, 3, 3, 3, 3, 0, 0, 3, 3, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 3, 3, 3, 3, 3, 0, 0, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 2, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 1, 1, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 3, 0, 3, 0, 2, 0, 1, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 3, 3, 3, 0, 0, 0, 0, 3, 0, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 3, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 3, 3, 0, 0, 0, 1, 1, 2, 3, 3, 3, 3, 0, 0, 0, 0, 3, 3, 3, 0, 3, 3, 1, 3, 3, 3, 3, 3, 2, 2],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true,\n\tclearOtherWires = false\n}"
      );
      int overgrownGrassArms = random.getIntBetween(5, 7);
      LinesGeneration overgrownGrassGeneration = this.getLinesGeneration(
         random, this.width / 2, this.height / 2, overgrownGrassArms, 35, Math.min(this.width, this.height) / 2 - 1, 2, 4, 1.5F, 2.5F
      );

      for (HashMapPointEntry<Point, LinesGeneration.PointDistance> entry : overgrownGrassGeneration.getSmoothPoints().getEntries()) {
         Point tile = entry.getKey();
         if (tile.x >= 0 && tile.x < this.width && tile.y >= 0 && tile.y < this.height) {
            double chance = entry.getValue().dist / 3.0;
            if (this.getTile(tile.x, tile.y) <= 0 && !random.getChance(chance)) {
               this.setTile(tile.x, tile.y, TileRegistry.overgrownGrassID);
            }
         }
      }

      int grassObjectID = ObjectRegistry.getObjectID("grass");

      for (Point tile : HuginStatueObjectEntity.fogTileRange.getValidTiles(this.width / 2, this.height / 2)) {
         if (tile.x >= 0 && tile.x < this.width && tile.y >= 0 && tile.y < this.height) {
            if (this.getTile(tile.x, tile.y) <= 0) {
               this.setTile(tile.x, tile.y, TileRegistry.grassID);
            }

            if (this.getObject(tile.x, tile.y) <= 0) {
               if (random.getChance(0.3F)) {
                  this.setObject(tile.x, tile.y, grassObjectID);
               } else if (random.getChance(0.1F)) {
                  this.setObject(tile.x, tile.y, ObjectRegistry.getObjectID("yellowflowerpatch"));
               }
            }
         }
      }

      this.addInventory(new LootTable(new LootItem("runeboundhornhelmet")), random, 16, 9);
   }

   public LinesGeneration getLinesGeneration(
      GameRandom random,
      int startX,
      int startY,
      int arms,
      int angleOffset,
      int maxLength,
      int armMinLength,
      int armMaxLength,
      float armStartWidth,
      float armEndWidth
   ) {
      LinesGeneration lg = new LinesGeneration(startX, startY);
      int armAngle = random.nextInt(360);
      int anglePerArm = 360 / arms;

      for (int i = 0; i < arms; i++) {
         armAngle += anglePerArm;
         lg.addMultiArm(
            random,
            armAngle,
            angleOffset,
            maxLength,
            armMinLength,
            armMaxLength,
            armStartWidth,
            armEndWidth,
            armLG -> armLG.x2 < 0 || armLG.x2 >= this.width || armLG.y2 < 0 || armLG.y2 >= this.height
         );
      }

      return lg;
   }
}
