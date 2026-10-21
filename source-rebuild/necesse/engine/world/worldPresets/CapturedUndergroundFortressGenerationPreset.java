package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CapturedUndergroundFortressPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class CapturedUndergroundFortressGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FurnitureSet[] furniture;
   public final WallSet[] walls;
   public final FloorSet[] floor;
   public final ColumnSet[] column;
   public final WallSet[] wallblock;

   public CapturedUndergroundFortressGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.deadwood, FurnitureSet.dungeon}, biome, levelIdentifier
      );
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.obsidian,
            WallSet.swampStone,
            WallSet.deepSnowStone,
            WallSet.snowStone,
            WallSet.deepSwampStone,
            WallSet.ice,
            WallSet.deepSandstone,
            WallSet.sandstone,
            WallSet.spidercastle,
            WallSet.dungeon,
            WallSet.stone,
            WallSet.basalt,
            WallSet.granite,
            WallSet.deepStone
         },
         biome,
         levelIdentifier
      );
      this.floor = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.deepSnowStoneBrick,
            FloorSet.deepStoneBrick,
            FloorSet.deepSwampStoneBrick,
            FloorSet.snowStone,
            FloorSet.snowStoneBrick,
            FloorSet.deadWood,
            FloorSet.stoneTiled,
            FloorSet.sandstone,
            FloorSet.dungeon,
            FloorSet.stone,
            FloorSet.swampStoneBrick,
            FloorSet.bamboo,
            FloorSet.sandstoneBrick,
            FloorSet.basalt,
            FloorSet.graniteBrick,
            FloorSet.granite,
            FloorSet.deepStone,
            FloorSet.dryad
         },
         biome,
         levelIdentifier
      );
      this.column = ColumnSet.getReducedSetForBiome(
         new ColumnSet[]{
            ColumnSet.obsidian,
            ColumnSet.swampstone,
            ColumnSet.granite,
            ColumnSet.deepsandstone,
            ColumnSet.deepswampstone,
            ColumnSet.deepsnowstone,
            ColumnSet.wood,
            ColumnSet.snowstone,
            ColumnSet.sandstone,
            ColumnSet.stone
         },
         biome,
         levelIdentifier
      );
      this.wallblock = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.bamboo, WallSet.willow, WallSet.palm, WallSet.wood, WallSet.ice}, biome, levelIdentifier
      );
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {
         @Override
         public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            if (presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
               return !generatorStack.isSurfaceOceanOrRiver(tileX, tileY);
            } else if (presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
               return !generatorStack.isCaveRiverOrLava(tileX, tileY);
            } else {
               return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? !generatorStack.isDeepCaveRiverOrLava(tileX, tileY) : false;
            }
         }
      }));
   }

   @Override
   public Preset getPreset(GameRandom random) {
      return new CapturedUndergroundFortressPreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.furniture),
         random.getOneOf(this.walls),
         random.getOneOf(this.floor),
         random.getOneOf(this.column),
         random.getOneOf(this.wallblock)
      );
   }
}
