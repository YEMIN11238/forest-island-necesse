package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CaveFountainHallPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.WallSet;

public class CaveFountainHallGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FloorSet[] floors;
   public final WallSet[] walls;
   public final FloorSet[] centerFloor;
   public final ColumnSet[] columns;

   public CaveFountainHallGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.floors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{FloorSet.deepStoneBrick, FloorSet.graniteBrick, FloorSet.sandBrick, FloorSet.deepSwampStoneBrick, FloorSet.deepSnowStoneBrick},
         biome,
         levelIdentifier
      );
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.basalt, WallSet.granite, WallSet.deepStone, WallSet.deepSnowStone, WallSet.deepSwampStone, WallSet.deepSandstone},
         biome,
         levelIdentifier
      );
      this.centerFloor = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.sandstoneBrick, FloorSet.granite, FloorSet.deepStoneTiled, FloorSet.deepSnowStone, FloorSet.deepSwampStone, FloorSet.stoneBrick
         },
         biome,
         levelIdentifier
      );
      this.columns = ColumnSet.getReducedSetForBiome(
         new ColumnSet[]{
            ColumnSet.swampstone,
            ColumnSet.deepstone,
            ColumnSet.granite,
            ColumnSet.basalt,
            ColumnSet.deepswampstone,
            ColumnSet.deepsnowstone,
            ColumnSet.snowstone,
            ColumnSet.sandstone
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
      return new CaveFountainHallPreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.floors),
         random.getOneOf(this.walls),
         random.getOneOf(this.centerFloor),
         random.getOneOf(this.columns)
      );
   }
}
