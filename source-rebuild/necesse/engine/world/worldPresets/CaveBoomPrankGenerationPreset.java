package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CaveBoomPrankPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.WallSet;

public class CaveBoomPrankGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final RockAndOreSet[] rockwalls;
   public final WallSet[] wallpillars;

   public CaveBoomPrankGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.rockwalls = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{
            RockAndOreSet.forestcave,
            RockAndOreSet.plainscave,
            RockAndOreSet.swampcave,
            RockAndOreSet.snowcave,
            RockAndOreSet.desertcave,
            RockAndOreSet.deepforestcave,
            RockAndOreSet.deepplainscave,
            RockAndOreSet.deepswampcave,
            RockAndOreSet.deepsnowcave,
            RockAndOreSet.deepdesertcave
         },
         biome,
         levelIdentifier
      );
      this.wallpillars = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.obsidian,
            WallSet.swampStone,
            WallSet.deepSnowStone,
            WallSet.snowStone,
            WallSet.deepSwampStone,
            WallSet.deepSandstone,
            WallSet.sandstone,
            WallSet.dungeon,
            WallSet.stone,
            WallSet.basalt,
            WallSet.granite,
            WallSet.deepStone,
            WallSet.wood
         },
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
      return new CaveBoomPrankPreset(this.biome, this.levelIdentifier, random, random.getOneOf(this.rockwalls), random.getOneOf(this.wallpillars));
   }
}
