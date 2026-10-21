package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.SmallMineEntrancePreset;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.GroundSet;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.WallSet;

public class SmallMineEntranceGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final RockAndOreSet[] rockwalls;
   public final WallSet[] wall;
   public final FloorSet[] cavefloor;
   public final ColumnSet[] columns;
   public final GroundSet[] gravel;

   public SmallMineEntranceGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.rockwalls = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{RockAndOreSet.forest, RockAndOreSet.snow, RockAndOreSet.plains, RockAndOreSet.swamp, RockAndOreSet.desert}, biome, levelIdentifier
      );
      this.wall = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.granite, WallSet.swampStone, WallSet.snowStone, WallSet.sandstone, WallSet.stone}, biome, levelIdentifier
      );
      this.cavefloor = FloorSet.getReducedSetForBiome(
         new FloorSet[]{FloorSet.snowStone, FloorSet.graniteBrick, FloorSet.swampStone, FloorSet.sandstone, FloorSet.stone}, biome, levelIdentifier
      );
      this.columns = ColumnSet.getReducedSetForBiome(
         new ColumnSet[]{ColumnSet.swampstone, ColumnSet.granite, ColumnSet.snowstone, ColumnSet.sandstone, ColumnSet.stone}, biome, levelIdentifier
      );
      this.gravel = GroundSet.getReducedSetForBiome(
         new GroundSet[]{GroundSet.forest, GroundSet.plains, GroundSet.snow, GroundSet.swamp, GroundSet.desert}, biome, levelIdentifier
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
      return new SmallMineEntrancePreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.rockwalls),
         random.getOneOf(this.wall),
         random.getOneOf(this.cavefloor),
         random.getOneOf(this.columns),
         random.getOneOf(this.gravel)
      );
   }
}
