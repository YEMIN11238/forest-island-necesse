package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.SmallCaveLabyrinthPreset;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.WallSet;

public class SmallCaveLabyrinthGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final RockAndOreSet[] ground;
   public final WallSet[] walls;

   public SmallCaveLabyrinthGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.ground = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{RockAndOreSet.swampcave, RockAndOreSet.forestcave, RockAndOreSet.desertcave, RockAndOreSet.snowcave, RockAndOreSet.plainscave},
         biome,
         levelIdentifier
      );
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.basalt, WallSet.deepStone, WallSet.deepSnowStone, WallSet.deepSwampStone, WallSet.deepSandstone},
         biome,
         LevelIdentifier.DEEP_CAVE_IDENTIFIER
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
      return new SmallCaveLabyrinthPreset(this.biome, this.levelIdentifier, random, random.getOneOf(this.ground), random.getOneOf(this.walls));
   }
}
