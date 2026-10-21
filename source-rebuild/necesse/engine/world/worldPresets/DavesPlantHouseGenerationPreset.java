package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.DavesPlantHousePreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.CropSet;
import necesse.level.maps.presets.set.GroundSet;
import necesse.level.maps.presets.set.TreeSet;

public class DavesPlantHouseGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final GroundSet[] ground;
   public final TreeSet[] tree;
   public final CropSet[] corn;

   public DavesPlantHouseGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, false, false, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.ground = GroundSet.getReducedSetForBiome(new GroundSet[]{GroundSet.forest, GroundSet.plains, GroundSet.swamp}, biome, levelIdentifier);
      this.tree = TreeSet.getReducedSetForBiome(new TreeSet[]{TreeSet.willow, TreeSet.oak, TreeSet.maple}, biome, levelIdentifier);
      this.corn = CropSet.getReducedSetForBiome(new CropSet[]{CropSet.cabbage, CropSet.corn}, biome, levelIdentifier);
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
      return new DavesPlantHousePreset(
         this.biome, this.levelIdentifier, random, random.getOneOf(this.ground), random.getOneOf(this.tree), random.getOneOf(this.corn)
      );
   }
}
