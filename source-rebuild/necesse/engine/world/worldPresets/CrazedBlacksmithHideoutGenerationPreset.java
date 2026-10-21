package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CrazedBlacksmithHideoutPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class CrazedBlacksmithHideoutGenerationPreset extends SimpleGenerationPreset {
   public final WallSet[] walls;
   public final FloorSet[] floor;
   public final FurnitureSet[] furniture;
   public final ColumnSet[] columns;

   public CrazedBlacksmithHideoutGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.walls = WallSet.getReducedSetForBiome(new WallSet[]{WallSet.granite, WallSet.brick, WallSet.sandstone}, biome, levelIdentifier);
      this.floor = FloorSet.getReducedSetForBiome(
         new FloorSet[]{FloorSet.deepStoneBrick, FloorSet.graniteBrick, FloorSet.stoneBrick, FloorSet.sandstone}, biome, levelIdentifier
      );
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.palm, FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.deadwood, FurnitureSet.dungeon},
         biome,
         levelIdentifier,
         FurnitureSet.palm,
         FurnitureSet.dungeon,
         FurnitureSet.oak
      );
      this.columns = ColumnSet.getReducedSetForBiome(new ColumnSet[]{ColumnSet.granite, ColumnSet.deepstone, ColumnSet.sandstone}, biome, levelIdentifier);
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
      return new CrazedBlacksmithHideoutPreset(
         random, random.getOneOf(this.walls), random.getOneOf(this.floor), random.getOneOf(this.furniture), random.getOneOf(this.columns)
      );
   }
}
