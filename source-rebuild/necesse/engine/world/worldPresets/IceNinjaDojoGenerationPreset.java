package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.IceNinjaDojoPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.VillageSet;

public class IceNinjaDojoGenerationPreset extends SimpleGenerationPreset {
   public final VillageSet[] wallsandfloor;
   public final CarpetSet[] carpets;

   public IceNinjaDojoGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.wallsandfloor = VillageSet.getReducedSetForBiome(new VillageSet[]{VillageSet.palm, VillageSet.maple}, biome, levelIdentifier);
      this.carpets = CarpetSet.getReducedSetForBiome(
         new CarpetSet[]{CarpetSet.velour, CarpetSet.brownbear, CarpetSet.green, CarpetSet.blue, CarpetSet.purple, CarpetSet.steelgrey, CarpetSet.leather},
         biome,
         levelIdentifier
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
      return new IceNinjaDojoPreset(random, random.getOneOf(this.wallsandfloor), random.getOneOf(this.carpets));
   }
}
