package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.SwampSettlerRuinsPreset;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.RockAndOreSet;
import necesse.level.maps.presets.set.WallSet;

public class SwampSettlerRuinsGenerationPreset extends SimpleGenerationPreset {
   protected LevelIdentifier levelIdentifier;
   public final CarpetSet[] carpets;
   public final FurnitureSet[] furnitures;
   public final WallSet[] walls;
   public final RockAndOreSet[] rocks;
   public final FurnitureSet[] chests;
   public final ColumnSet[] columns;

   public SwampSettlerRuinsGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.levelIdentifier = levelIdentifier;
      this.carpets = CarpetSet.getReducedSetForBiome(new CarpetSet[]{CarpetSet.velour, CarpetSet.green, CarpetSet.leather}, biome, levelIdentifier);
      this.furnitures = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.willow, FurnitureSet.oak, FurnitureSet.dungeon}, biome, levelIdentifier
      );
      this.walls = WallSet.getReducedSetForBiome(new WallSet[]{WallSet.swampStone, WallSet.deepSwampStone}, biome, levelIdentifier);
      this.rocks = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{RockAndOreSet.swampcave, RockAndOreSet.swamp, RockAndOreSet.deepswampcave}, biome, levelIdentifier
      );
      this.chests = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.willow, FurnitureSet.oak, FurnitureSet.dungeon}, biome, levelIdentifier
      );
      this.columns = ColumnSet.getReducedSetForBiome(new ColumnSet[]{ColumnSet.swampstone, ColumnSet.deepswampstone}, biome, levelIdentifier);
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
      return new SwampSettlerRuinsPreset(
         random,
         this.levelIdentifier,
         random.getOneOf(this.carpets),
         random.getOneOf(this.furnitures),
         random.getOneOf(this.walls),
         random.getOneOf(this.rocks),
         random.getOneOf(this.chests),
         random.getOneOf(this.columns)
      );
   }
}
