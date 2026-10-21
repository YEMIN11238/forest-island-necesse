package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.GuardTowerPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.GroundSet;
import necesse.level.maps.presets.set.PathSet;
import necesse.level.maps.presets.set.WallSet;

public class GuardTowerGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final WallSet[] outerWalls;
   public final WallSet[] interiorWalls;
   public final FurnitureSet[] furniture;
   public final FloorSet[] floorset;
   public final GroundSet[] gravel;
   public final PathSet[] path;

   public GuardTowerGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.outerWalls = WallSet.getReducedSetForBiome(new WallSet[]{WallSet.snowStone, WallSet.sandstone}, biome, levelIdentifier);
      this.interiorWalls = WallSet.getReducedSetForBiome(new WallSet[]{WallSet.pine, WallSet.palm, WallSet.snowStone}, biome, levelIdentifier);
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.pine, FurnitureSet.palm, FurnitureSet.oak, FurnitureSet.spruce}, biome, levelIdentifier
      );
      this.floorset = FloorSet.getReducedSetForBiome(new FloorSet[]{FloorSet.sandstoneBrick, FloorSet.snowStoneBrick}, biome, levelIdentifier);
      this.gravel = GroundSet.getReducedSetForBiome(new GroundSet[]{GroundSet.snow, GroundSet.plains}, biome, levelIdentifier);
      this.path = PathSet.getReducedSetForBiome(new PathSet[]{PathSet.sandStone, PathSet.snowStone}, biome, levelIdentifier);
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
      return new GuardTowerPreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.outerWalls),
         random.getOneOf(this.interiorWalls),
         random.getOneOf(this.furniture),
         random.getOneOf(this.floorset),
         random.getOneOf(this.gravel),
         random.getOneOf(this.path)
      );
   }
}
