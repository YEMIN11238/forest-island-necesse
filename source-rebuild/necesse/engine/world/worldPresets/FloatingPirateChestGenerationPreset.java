package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.FloatingPirateChestPreset;
import necesse.level.maps.presets.Preset;

public class FloatingPirateChestGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;

   public FloatingPirateChestGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(80, false, false, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {
         @Override
         public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) ? generatorStack.isSurfaceOcean(tileX, tileY) : false;
         }
      }));
   }

   @Override
   public Preset getPreset(GameRandom random) {
      return new FloatingPirateChestPreset(this.biome, this.levelIdentifier, random);
   }
}
