package necesse.level.maps.presets.set;

import java.util.Arrays;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class ChestRoomSet extends PresetSet<ChestRoomSet> {
   public static final ChestRoomSet wood = new ChestRoomSet("stonefloor", "stonepressureplate", WallSet.wood, ColumnSet.wood, "storagebox", "woodarrowtrap")
      .surface(BiomeRegistry.FOREST, BiomeRegistry.PLAINS)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS)
      .deepCave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS)
      .incursion();
   public static final ChestRoomSet dryad = new ChestRoomSet(
         "granitefloor", "granitepressureplate", WallSet.dryad, ColumnSet.sandstone, "storagebox", "dryadarrowtrap"
      )
      .surface(BiomeRegistry.PLAINS)
      .cave(BiomeRegistry.PLAINS);
   public static final ChestRoomSet stone = new ChestRoomSet(
         "stonefloor", "stonepressureplate", WallSet.stone, ColumnSet.stone, "storagebox", "stoneflametrap", "stonearrowtrap"
      )
      .surface(BiomeRegistry.FOREST)
      .cave(BiomeRegistry.FOREST)
      .incursion(BiomeRegistry.FOREST);
   public static final ChestRoomSet sandstone = new ChestRoomSet(
         "sandstonefloor", "sandstonepressureplate", WallSet.sandstone, ColumnSet.sandstone, "storagebox", "sandstoneflametrap", "sandstonearrowtrap"
      )
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT);
   public static final ChestRoomSet snowStone = new ChestRoomSet(
         "snowstonefloor", "snowstonepressureplate", WallSet.snowStone, ColumnSet.snowstone, "storagebox", "snowstonearrowtrap"
      )
      .surface(BiomeRegistry.SNOW)
      .cave(BiomeRegistry.SNOW);
   public static final ChestRoomSet ice = new ChestRoomSet(
         "snowstonefloor", "snowstonepressureplate", WallSet.ice, ColumnSet.deepstone, "storagebox", "icearrowtrap"
      )
      .surface(BiomeRegistry.SNOW)
      .cave(BiomeRegistry.SNOW);
   public static final ChestRoomSet granite = new ChestRoomSet(
         "granitefloor", "granitepressureplate", WallSet.granite, ColumnSet.sandstone, "storagebox", "graniteflametrap", "granitearrowtrap"
      )
      .surface(BiomeRegistry.PLAINS)
      .cave(BiomeRegistry.PLAINS);
   public static final ChestRoomSet swampStone = new ChestRoomSet(
         "swampstonefloor", "swampstonepressureplate", WallSet.swampStone, ColumnSet.swampstone, "storagebox", "swampstoneflametrap", "swampstonearrowtrap"
      )
      .surface(BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SWAMP);
   public static final ChestRoomSet deepStone = new ChestRoomSet(
         "deepstonefloor", "deepstonepressureplate", WallSet.deepStone, ColumnSet.deepstone, "storagebox", "deepstoneflametrap", "deepstonearrowtrap"
      )
      .deepCave(BiomeRegistry.FOREST);
   public static final ChestRoomSet obsidian = new ChestRoomSet(
         "deepstonefloor", "deepstonepressureplate", WallSet.obsidian, ColumnSet.obsidian, "storagebox", "obsidianflametrap", "obsidianarrowtrap"
      )
      .deepCave(BiomeRegistry.PLAINS, BiomeRegistry.SWAMP, BiomeRegistry.FOREST);
   public static final ChestRoomSet deepSnowStone = new ChestRoomSet(
         "deepsnowstonefloor",
         "deepsnowstonepressureplate",
         WallSet.deepSnowStone,
         ColumnSet.deepsnowstone,
         "storagebox",
         "deepsnowstoneflametrap",
         "deepsnowstonearrowtrap"
      )
      .deepCave(BiomeRegistry.SNOW);
   public static final ChestRoomSet basalt = new ChestRoomSet(
         "basaltfloor", "basaltpressureplate", WallSet.basalt, ColumnSet.basalt, "storagebox", "basaltflametrap", "basaltarrowtrap"
      )
      .deepCave(BiomeRegistry.PLAINS);
   public static final ChestRoomSet deepSwampStone = new ChestRoomSet(
         "deepswampstonefloor",
         "deepswampstonepressureplate",
         WallSet.deepSwampStone,
         ColumnSet.deepswampstone,
         "storagebox",
         "deepswampstoneflametrap",
         "deepswampstonearrowtrap"
      )
      .deepCave(BiomeRegistry.SWAMP);
   public static final ChestRoomSet deepSandstone = new ChestRoomSet(
         "woodfloor", "woodpressureplate", WallSet.deepSandstone, ColumnSet.deepsandstone, "storagebox", "deepsandstoneflametrap", "deepsandstonearrowtrap"
      )
      .deepCave(BiomeRegistry.DESERT);
   public final int floor;
   public final int pressureplate;
   public final WallSet wallSet;
   public final ColumnSet columnSet;
   public final int inventoryObject;
   public final int[] traps;

   public ChestRoomSet(
      String floorStringID, String pressureplateStringID, WallSet wallSet, ColumnSet columnSet, String inventoryObjectStringID, String... trapsStringIDs
   ) {
      this.objectArrays = new int[][]{
         {this.floor = TileRegistry.getTileID(floorStringID)},
         {this.pressureplate = ObjectRegistry.getObjectID(pressureplateStringID)},
         {this.inventoryObject = ObjectRegistry.getObjectID(inventoryObjectStringID)},
         this.traps = Arrays.stream(trapsStringIDs).mapToInt(ObjectRegistry::getObjectID).toArray()
      };
      this.nestedSetArrays = new PresetSet[][]{{this.wallSet = wallSet}, {this.columnSet = columnSet}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(ChestRoomSet.class);
   }
}
