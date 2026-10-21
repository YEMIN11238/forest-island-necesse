package necesse.level.maps.presets.caveRooms;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class CaveRuins extends Preset {
   public static Dimension MAX_DIMENSIONS = new Dimension(10, 10);
   public static ArrayList<CaveRuins.CaveRuinGetter> caveRuinGetters = new ArrayList<>(
      Arrays.asList(
         CaveRuins1::new,
         CaveRuins2::new,
         CaveRuins3::new,
         CaveRuins4::new,
         CaveRuins5::new,
         CaveRuins6::new,
         CaveRuins7::new,
         CaveRuins8::new,
         CaveRuins9::new,
         CaveRuins10::new
      )
   );

   public CaveRuins(String script, GameRandom random, WallSet wallSet, FurnitureSet furnitureSet, String floorStringID) {
      super(script);
      if (this.width <= MAX_DIMENSIONS.width && this.height <= MAX_DIMENSIONS.height) {
         this.applyRuinsLogic(random, wallSet, furnitureSet, floorStringID);
      } else {
         throw new IllegalArgumentException("CaveRuins presets must be 10x10 or smaller, got " + this.width + "x" + this.height);
      }
   }

   public CaveRuins(int width, int height) {
      super(width, height);
   }

   public void applyRuinsLogic(GameRandom random, WallSet wallSet, FurnitureSet furnitureSet, String floorStringID) {
      applyRuinsLogic(this, random, wallSet, furnitureSet, floorStringID);
   }

   public static void applyRuinsLogic(Preset preset, GameRandom random, WallSet wallSet, FurnitureSet furnitureSet, String floorStringID) {
      FurnitureSet.oak.replaceWith(furnitureSet, preset);
      WallSet.wood.replaceWith(wallSet, preset);
      preset.replaceTile(TileRegistry.stoneFloorID, TileRegistry.getTileID(floorStringID));
      float objectChance = random.getFloatBetween(0.4F, 0.8F);
      float tileChance = random.getFloatBetween(objectChance - 0.05F, objectChance + 0.2F);

      for (int x = 0; x < preset.width; x++) {
         for (int y = 0; y < preset.height; y++) {
            boolean handleTile = true;
            int objectID = preset.getObject(x, y);
            if (objectID != -1) {
               if (objectID == furnitureSet.chest) {
                  continue;
               }

               GameObject object = ObjectRegistry.getObject(objectID);
               MultiTile multiTile = object.getMultiTile(preset.getObjectRotation(x, y));
               if (multiTile.isMaster && random.getChance(objectChance)) {
                  multiTile.streamIDs(x, y).forEach(coord -> {
                     if (preset.getObject(coord.tileX, coord.tileY) == coord.value) {
                        preset.setObject(coord.tileX, coord.tileY, -1);
                     }
                  });
               } else {
                  handleTile = false;
               }
            }

            if (handleTile) {
               int tileID = preset.getTile(x, y);
               if (tileID != -1 && random.getChance(tileChance)) {
                  preset.setTile(x, y, -1);
               }
            }
         }
      }
   }

   @FunctionalInterface
   public interface CaveRuinGetter {
      CaveRuins get(GameRandom var1, WallSet var2, FurnitureSet var3, String var4, LootTable var5, AtomicInteger var6);
   }
}
