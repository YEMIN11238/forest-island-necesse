package necesse.level.maps.presets.set;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.forms.presets.PresetDebugPreviewForm;

public class FloorSet extends PresetSet<FloorSet> {
   public static final FloorSet wood = new FloorSet(TileRegistry.woodFloorID)
      .surface(BiomeRegistry.FOREST)
      .cave(BiomeRegistry.FOREST)
      .deepCave(BiomeRegistry.FOREST)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION);
   public static final FloorSet pine = new FloorSet(TileRegistry.pineFloorID)
      .surface(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .cave(BiomeRegistry.FOREST, BiomeRegistry.SNOW)
      .incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final FloorSet palm = new FloorSet(TileRegistry.palmFloorID)
      .surface(BiomeRegistry.DESERT)
      .cave(BiomeRegistry.DESERT)
      .deepCave(BiomeRegistry.DESERT)
      .incursion(BiomeRegistry.DESERT_DEEP_CAVE_INCURSION);
   public static final FloorSet willow = new FloorSet(TileRegistry.willowFloorID)
      .surface(BiomeRegistry.SWAMP)
      .cave(BiomeRegistry.SWAMP)
      .deepCave(BiomeRegistry.SWAMP)
      .incursion();
   public static final FloorSet dryad = new FloorSet(TileRegistry.dryadFloorID).surface(BiomeRegistry.PLAINS).deepCave(BiomeRegistry.PLAINS).incursion();
   public static final FloorSet stone = new FloorSet(TileRegistry.stoneFloorID).surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST);
   public static final FloorSet stoneBrick = new FloorSet(TileRegistry.stoneBrickFloorID).surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST);
   public static final FloorSet stoneTiled = new FloorSet(TileRegistry.stoneTiledFloorID).surface(BiomeRegistry.FOREST).cave(BiomeRegistry.FOREST);
   public static final FloorSet snowStone = new FloorSet(TileRegistry.snowStoneFloorID).surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW).incursion();
   public static final FloorSet snowRock = new FloorSet(TileRegistry.snowRockID);
   public static final FloorSet snowStoneBrick = new FloorSet(TileRegistry.snowStoneBrickFloorID).surface(BiomeRegistry.SNOW).cave(BiomeRegistry.SNOW);
   public static final FloorSet swampStone = new FloorSet(TileRegistry.swampStoneFloorID).surface(BiomeRegistry.SWAMP).cave(BiomeRegistry.SWAMP);
   public static final FloorSet swampStoneBrick = new FloorSet(TileRegistry.swampStoneBrickFloorID).surface(BiomeRegistry.SWAMP).cave(BiomeRegistry.SWAMP);
   public static final FloorSet sandstone = new FloorSet(TileRegistry.sandstoneFloorID).surface(BiomeRegistry.DESERT).cave(BiomeRegistry.DESERT);
   public static final FloorSet sandBrick = new FloorSet(TileRegistry.sandBrickID).surface(BiomeRegistry.DESERT).cave(BiomeRegistry.DESERT);
   public static final FloorSet sandstoneBrick = new FloorSet(TileRegistry.sandstoneBrickFloorID).surface(BiomeRegistry.DESERT).cave(BiomeRegistry.DESERT);
   public static final FloorSet granite = new FloorSet(TileRegistry.graniteFloorID).surface(BiomeRegistry.PLAINS).cave(BiomeRegistry.PLAINS);
   public static final FloorSet graniteBrick = new FloorSet(TileRegistry.graniteBrickFloorID).surface(BiomeRegistry.PLAINS).cave(BiomeRegistry.PLAINS);
   public static final FloorSet deepStone = new FloorSet(TileRegistry.deepStoneFloorID)
      .deepCave(BiomeRegistry.FOREST)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION);
   public static final FloorSet deepStoneBrick = new FloorSet(TileRegistry.deepStoneBrickFloorID)
      .deepCave(BiomeRegistry.FOREST)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION);
   public static final FloorSet deepStoneTiled = new FloorSet(TileRegistry.deepStoneTiledFloorID)
      .deepCave(BiomeRegistry.FOREST)
      .incursion(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION);
   public static final FloorSet deepSnowStone = new FloorSet(TileRegistry.deepSnowStoneFloorID)
      .deepCave(BiomeRegistry.SNOW)
      .incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final FloorSet deepSnowStoneBrick = new FloorSet(TileRegistry.deepSnowStoneBrickFloorID)
      .deepCave(BiomeRegistry.SNOW)
      .incursion(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION);
   public static final FloorSet deepSwampStone = new FloorSet(TileRegistry.deepSwampStoneFloorID)
      .deepCave(BiomeRegistry.SWAMP)
      .incursion(BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION);
   public static final FloorSet deepSwampStoneBrick = new FloorSet(TileRegistry.deepSwampStoneBrickFloorID)
      .deepCave(BiomeRegistry.SWAMP)
      .incursion(BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION);
   public static final FloorSet basalt = new FloorSet(TileRegistry.basaltFloorID).deepCave(BiomeRegistry.PLAINS).incursion();
   public static final FloorSet dungeon = new FloorSet(TileRegistry.dungeonFloorID);
   public static final FloorSet bamboo = new FloorSet(TileRegistry.bambooFloorID).deepCave(BiomeRegistry.SWAMP).incursion();
   public static final FloorSet deadWood = new FloorSet(TileRegistry.deadWoodFloorID).incursion();
   public static final FloorSet spiderCastle = new FloorSet(TileRegistry.getTileID("spidercastlefloor")).incursion(BiomeRegistry.SPIDER_CASTLE);
   public final int tile;

   protected FloorSet(int tile) {
      this.tileArrays = new int[][]{{this.tile = tile}};
   }

   static {
      PresetDebugPreviewForm.registerPresetSet(FloorSet.class);
   }
}
