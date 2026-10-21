package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CircularTrapRoomDeepPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.PathSet;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.WallSet;

public class CircularTrapRoomDeepGenerationPreset extends SimpleGenerationPreset {
   public final WallSet[] walls;
   public final RockAndOreSet[] rocktiles;
   public final PathSet[] floortiles;
   public final FurnitureSet[] chest;
   public final Biome biome;

   public CircularTrapRoomDeepGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.obsidian, WallSet.basalt, WallSet.sandstone, WallSet.deepStone, WallSet.dungeon}, biome, levelIdentifier
      );
      this.rocktiles = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{RockAndOreSet.deepplainscave, RockAndOreSet.deepswampcave}, biome, levelIdentifier
      );
      this.floortiles = PathSet.getReducedSetForBiome(new PathSet[]{PathSet.basalt, PathSet.crypt, PathSet.darkFullMoon}, biome, levelIdentifier);
      this.chest = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.dryad, FurnitureSet.deadwood, FurnitureSet.oak, FurnitureSet.bone, FurnitureSet.dungeon}, biome, levelIdentifier
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
      return new CircularTrapRoomDeepPreset(
         random, this.biome, random.getOneOf(this.walls), random.getOneOf(this.rocktiles), random.getOneOf(this.floortiles), random.getOneOf(this.chest)
      );
   }
}
