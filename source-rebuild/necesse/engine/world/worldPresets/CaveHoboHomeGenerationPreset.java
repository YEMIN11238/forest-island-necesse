package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CaveHoboHomePreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.WallSet;

public class CaveHoboHomeGenerationPreset extends SimpleGenerationPreset {
   protected final Biome biome;
   protected final LevelIdentifier levelIdentifier;
   public final RockAndOreSet[] walls;
   public final FurnitureSet[] furniture;
   public final WallSet[] doors;

   public CaveHoboHomeGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.walls = RockAndOreSet.getReducedSetForBiome(new RockAndOreSet[]{RockAndOreSet.snowcave, RockAndOreSet.deepsnowcave}, biome, levelIdentifier);
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.willow, FurnitureSet.deadwood}, biome, levelIdentifier
      );
      this.doors = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.deepStone, WallSet.deepSnowStone, WallSet.snowStone, WallSet.ice}, biome, levelIdentifier
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
      return new CaveHoboHomePreset(
         this.biome, this.levelIdentifier, random, random.getOneOf(this.walls), random.getOneOf(this.furniture), random.getOneOf(this.doors)
      );
   }
}
