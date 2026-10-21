package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class VillageSet extends PresetSet<VillageSet> {
   public static final VillageSet defaultSet = new VillageSet(
         FurnitureSet.spruce, WallSet.wood, WallSet.stone, "grasstile", "sandtile", "stonepathtile", "stonefloor", "woodfloor"
      )
      .all();
   public static final VillageSet oak = new VillageSet(
         FurnitureSet.oak, WallSet.wood, WallSet.stone, "grasstile", "sandtile", "stonepathtile", "stonefloor", "woodfloor"
      )
      .surface(BiomeRegistry.FOREST);
   public static final VillageSet spruce = new VillageSet(
         FurnitureSet.spruce, WallSet.wood, WallSet.stone, "grasstile", "sandtile", "stonepathtile", "stonefloor", "woodfloor"
      )
      .surface(BiomeRegistry.FOREST);
   public static final VillageSet maple = new VillageSet(
         FurnitureSet.maple, WallSet.wood, WallSet.stone, "plainsgrasstile", "sandtile", "stonepathtile", "stonefloor", "woodfloor"
      )
      .surface(BiomeRegistry.PLAINS);
   public static final VillageSet birch = new VillageSet(
         FurnitureSet.birch, WallSet.wood, WallSet.stone, "plainsgrasstile", "sandtile", "stonepathtile", "stonefloor", "woodfloor"
      )
      .surface(BiomeRegistry.PLAINS);
   public static final VillageSet pine = new VillageSet(
         FurnitureSet.pine, WallSet.pine, WallSet.snowStone, "snowtile", "icetile", "snowstonepathtile", "snowstonefloor", "pinefloor"
      )
      .surface(BiomeRegistry.SNOW);
   public static final VillageSet palm = new VillageSet(
         FurnitureSet.palm, WallSet.palm, WallSet.sandstone, "sandtile", "sandtile", "sandstonepathtile", "sandstonefloor", "palmfloor"
      )
      .surface(BiomeRegistry.DESERT);
   public final FurnitureSet furniture;
   public final WallSet woodWalls;
   public final WallSet stoneWalls;
   public final int terrainTile;
   public final int shoreTile;
   public final int pathTile;
   public final int rockTile;
   public final int woodTile;

   public VillageSet(
      FurnitureSet furniture,
      WallSet woodWalls,
      WallSet stoneWalls,
      String terrainTileStringID,
      String shoreTileStringID,
      String pathTileStringID,
      String rockTileStringID,
      String woodTileStringID
   ) {
      this.nestedSetArrays = new PresetSet[][]{{this.furniture = furniture}, {this.woodWalls = woodWalls}, {this.stoneWalls = stoneWalls}};
      this.tileArrays = new int[][]{
         {this.terrainTile = TileRegistry.getTileID(terrainTileStringID)},
         {this.shoreTile = TileRegistry.getTileID(shoreTileStringID)},
         {this.pathTile = TileRegistry.getTileID(pathTileStringID)},
         {this.rockTile = TileRegistry.getTileID(rockTileStringID)},
         {this.woodTile = TileRegistry.getTileID(woodTileStringID)}
      };
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(VillageSet.class);
   }
}
