package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.TrainingDummyPicnicPreset;
import necesse.level.maps.presets.set.FlowerPatchSet;

public class TrainingDummyPicnicGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FlowerPatchSet[] flowers;

   public TrainingDummyPicnicGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.flowers = FlowerPatchSet.getReducedSetForBiome(
         new FlowerPatchSet[]{FlowerPatchSet.red, FlowerPatchSet.white, FlowerPatchSet.blue, FlowerPatchSet.yellow, FlowerPatchSet.purple},
         biome,
         levelIdentifier
      );
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {
         @Override
         public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) ? !generatorStack.isSurfaceOceanOrRiver(tileX, tileY) : false;
         }
      }));
   }

   @Override
   public Preset getPreset(GameRandom random) {
      return new TrainingDummyPicnicPreset(this.biome, this.levelIdentifier, random, this.flowers);
   }
}
