package necesse.level.maps.presets;

import java.awt.Point;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.HashMapPointEntry;
import necesse.level.maps.generationModules.LinesGeneration;

public class CoreKeeperCorePreset extends Preset {
   public CoreKeeperCorePreset(GameRandom random) {
      super(30, 30);
      int coreWidth = 6;
      int coreHeight = 6;
      int coreX = this.width / 2 - coreWidth / 2;
      int coreY = this.height / 2 - coreHeight / 2;

      for (int x = coreX - 1; x < coreX + coreWidth + 1; x++) {
         for (int y = coreY - 1; y < coreY + coreHeight + 1; y++) {
            this.setTile(x, y, TileRegistry.deepSandstoneID);
         }
      }

      int sandArms = random.getIntBetween(5, 8);
      LinesGeneration sandGeneration = this.getLinesGeneration(
         random, coreX + coreWidth / 2, coreY + coreHeight / 2, sandArms, 35, Math.min(this.width, this.height) / 2 - 1, 2, 4, 1.5F, 3.0F
      );

      for (HashMapPointEntry<Point, LinesGeneration.PointDistance> entry : sandGeneration.getSmoothPoints().getEntries()) {
         Point tile = entry.getKey();
         if (tile.x >= 0 && tile.x < this.width && tile.y >= 0 && tile.y < this.height) {
            double chance = entry.getValue().dist / 3.0;
            if (!random.getChance(chance)) {
               this.setTile(tile.x, tile.y, TileRegistry.deepSandstoneID);
               if (random.getChance(0.075F)) {
                  this.setObject(tile.x, tile.y, ObjectRegistry.getObjectID("swampcrate"));
               } else if (random.getChance(0.15F)) {
                  this.setObject(tile.x, tile.y, ObjectRegistry.getObjectID(random.getOneOf("seashell", "seasnail", "seastar")));
               } else if (random.getChance(0.2F)) {
                  this.setObject(tile.x, tile.y, ObjectRegistry.getObjectID(random.getOneOf("mossysteppingstones", "largemossysteppingstone")));
               } else {
                  this.setObject(tile.x, tile.y, 0);
               }
            }
         }
      }

      int rootArms = random.getIntBetween(4, 5);
      LinesGeneration rootsGeneration = this.getLinesGeneration(
         random, coreX + coreWidth / 2, coreY + coreHeight / 2, rootArms, 25, Math.min(this.width, this.height) / 2 - 1, 2, 4, 1.5F, 3.0F
      );
      int ancientRootsFlowerID = ObjectRegistry.getObjectID("wildancientroots");

      for (HashMapPointEntry<Point, LinesGeneration.PointDistance> entry : rootsGeneration.getSmoothPoints().getEntries()) {
         Point tile = entry.getKey();
         if (tile.x >= 0 && tile.x < this.width && tile.y >= 0 && tile.y < this.height) {
            double chance = entry.getValue().dist / 3.0;
            if (!random.getChance(chance)) {
               this.setTile(tile.x, tile.y, TileRegistry.ancientRootsID);
               if (random.getChance(0.3F)) {
                  this.setObject(tile.x, tile.y, ancientRootsFlowerID);
               } else if (random.getChance(0.05F)) {
                  this.setObject(tile.x, tile.y, ObjectRegistry.getObjectID("swampcrate"));
               } else if (random.getChance(0.8F)) {
                  this.setObject(tile.x, tile.y, ObjectRegistry.getObjectID("deepswamptallgrass"));
               } else {
                  this.setObject(tile.x, tile.y, 0);
               }

               if (random.getChance(0.05F)) {
                  this.addMob("firefly", tile.x, tile.y, false);
               }
            }
         }
      }

      this.applyScript(
         coreX,
         coreY,
         "PRESET = {\n\twidth = 6,\n\theight = 6,\n\tobjectIDs = [0, air, 409, thecore, 410, thecore2, 411, thecore3, 412, thecore4, 413, thecore5, 414, thecore6, 415, thecore7, 416, thecore8, 417, thecore9, 418, thecore10, 419, thecore11, 420, thecore12, 421, thecore13, 422, thecore14, 423, thecore15, 424, thecore16, 425, thecore17, 426, thecore18, 427, thecore19, 428, thecore20, 429, thecore21, 430, thecore22, 431, thecore23, 432, thecore24, 433, thecore25, 434, thecore26, 435, thecore27, 436, thecore28, 437, thecore29, 438, thecore30, 439, thecore31, 440, thecore32, 441, thecore33, 442, thecore34, 443, thecore35, 444, thecore36],\n\tobjects = [409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444],\n\trotations = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true,\n\tclearOtherWires = false\n}"
      );
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
