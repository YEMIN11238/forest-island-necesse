package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.BigCemeteryPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.FenceSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.GroundSet;
import necesse.level.maps.presets.set.HedgeSet;
import necesse.level.maps.presets.set.TreeSet;
import necesse.level.maps.presets.set.WallSet;

public class BigCemeteryGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FloorSet[] path;
   public final GroundSet[] grass;
   public final FenceSet[] fence;
   public final WallSet[] walls;
   public final TreeSet[] trees;
   public final HedgeSet[] hedges;
   public final FurnitureSet[] furniture;

   public BigCemeteryGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.path = GroundSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.sandstone,
            FloorSet.graniteBrick,
            FloorSet.deepStoneBrick,
            FloorSet.deepSnowStone,
            FloorSet.deepSwampStoneBrick,
            FloorSet.deadWood,
            FloorSet.stoneTiled,
            FloorSet.stoneBrick,
            FloorSet.snowStoneBrick
         },
         biome,
         levelIdentifier
      );
      this.grass = GroundSet.getReducedSetForBiome(
         new GroundSet[]{GroundSet.forest, GroundSet.snow, GroundSet.plains, GroundSet.swamp, GroundSet.desert}, biome, levelIdentifier
      );
      this.fence = FenceSet.getReducedSetForBiome(new FenceSet[]{FenceSet.crypt, FenceSet.iron, FenceSet.wood, FenceSet.stone}, biome, levelIdentifier);
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.willow, WallSet.deepStone, WallSet.swampStone, WallSet.wood, WallSet.dungeon}, biome, levelIdentifier
      );
      this.trees = TreeSet.getReducedSetForBiome(
         new TreeSet[]{TreeSet.willow, TreeSet.pine, TreeSet.birch, TreeSet.maple, TreeSet.deadwood, TreeSet.spruce}, biome, levelIdentifier
      );
      this.hedges = HedgeSet.getReducedSetForBiome(new HedgeSet[]{HedgeSet.forest, HedgeSet.snow, HedgeSet.plains, HedgeSet.swamp}, biome, levelIdentifier);
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.deadwood, FurnitureSet.dryad, FurnitureSet.oak, FurnitureSet.maple}, biome, levelIdentifier
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
      return new BigCemeteryPreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.path),
         random.getOneOf(this.grass),
         random.getOneOf(this.fence),
         random.getOneOf(this.walls),
         random.getOneOf(this.trees),
         random.getOneOf(this.hedges),
         random.getOneOf(this.furniture)
      );
   }
}
