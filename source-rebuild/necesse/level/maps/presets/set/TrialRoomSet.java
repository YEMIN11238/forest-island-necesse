package necesse.level.maps.presets.set;

import java.util.stream.Stream;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class TrialRoomSet extends PresetSet<TrialRoomSet> {
   public static final TrialRoomSet stone = new TrialRoomSet(
      "stonefloor", "stonepressureplate", "stonepathtile", "stonedoorlocked", WallSet.stone, "stonearrowtrap", "stoneflametrap", "stonesawtrap"
   );
   public static final TrialRoomSet sandStone = new TrialRoomSet(
      "sandstonefloor",
      "sandstonepressureplate",
      "sandstonepathtile",
      "sandstonedoorlocked",
      WallSet.sandstone,
      "sandstonearrowtrap",
      "sandstoneflametrap",
      "sandstonesawtrap"
   );
   public static final TrialRoomSet snowStone = new TrialRoomSet(
      "snowstonefloor",
      "snowstonepressureplate",
      "snowstonepathtile",
      "snowstonedoorlocked",
      WallSet.snowStone,
      "snowstonearrowtrap",
      "stoneflametrap",
      "snowstonesawtrap"
   );
   public static final TrialRoomSet granite = new TrialRoomSet(
      "granitefloor", "granitepressureplate", "granitepathtile", "granitedoorlocked", WallSet.granite, "granitearrowtrap", "graniteflametrap", "granitesawtrap"
   );
   public static final TrialRoomSet swampStone = new TrialRoomSet(
      "swampstonefloor",
      "swampstonepressureplate",
      "swampstonepathtile",
      "swampstonedoorlocked",
      WallSet.swampStone,
      "swampstonearrowtrap",
      "swampstoneflametrap",
      "swampstonesawtrap"
   );
   public static final TrialRoomSet deepStone = new TrialRoomSet(
      "deepstonefloor",
      "deepstonepressureplate",
      "stonepathtile",
      "deepstonedoorlocked",
      WallSet.deepStone,
      "deepstonearrowtrap",
      "deepstoneflametrap",
      "deepstonesawtrap"
   );
   public static final TrialRoomSet deepSnowStone = new TrialRoomSet(
      "deepsnowstonefloor",
      "deepsnowstonepressureplate",
      "snowstonepathtile",
      "deepsnowstonedoorlocked",
      WallSet.deepSnowStone,
      "deepsnowstonearrowtrap",
      "deepsnowstoneflametrap",
      "deepsnowstonesawtrap"
   );
   public static final TrialRoomSet basalt = new TrialRoomSet(
      "basaltfloor", "basaltpressureplate", "basaltpathtile", "basaltdoorlocked", WallSet.basalt, "basaltarrowtrap", "basaltflametrap", "basaltsawtrap"
   );
   public static final TrialRoomSet deepSwampStone = new TrialRoomSet(
      "deepswampstonefloor",
      "deepswampstonepressureplate",
      "swampstonepathtile",
      "deepswampstonedoorlocked",
      WallSet.deepSwampStone,
      "deepswampstonearrowtrap",
      "deepswampstoneflametrap",
      "deepswampstonesawtrap"
   );
   public static final TrialRoomSet deepSandstone = new TrialRoomSet(
      "woodfloor",
      "woodpressureplate",
      "woodpathtile",
      "deepsandstonedoorlocked",
      WallSet.deepSandstone,
      "deepsandstonearrowtrap",
      "deepsandstoneflametrap",
      "deepsandstonesawtrap"
   );
   public final int floor;
   public final int pressureplate;
   public final int path;
   public final int door;
   public final WallSet wallSet;
   public final int[] traps;

   public TrialRoomSet(String floorStringID, String pressureplateStringID, String pathStringID, String doorStringID, WallSet wallSet, String... trapStringIDs) {
      this.objectArrays = new int[][]{
         {this.door = ObjectRegistry.getObjectID(doorStringID)},
         {this.pressureplate = ObjectRegistry.getObjectID(pressureplateStringID)},
         this.traps = Stream.of(trapStringIDs).mapToInt(ObjectRegistry::getObjectID).toArray()
      };
      this.tileArrays = new int[][]{{this.floor = TileRegistry.getTileID(floorStringID)}, {this.path = TileRegistry.getTileID(pathStringID)}};
      this.nestedSetArrays = new PresetSet[][]{{this.wallSet = wallSet}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(TrialRoomSet.class);
   }
}
