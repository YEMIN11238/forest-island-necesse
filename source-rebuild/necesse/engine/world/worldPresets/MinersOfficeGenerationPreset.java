package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.MinersOfficePreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.CrystalSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.RockAndOreSet;

public class MinersOfficeGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final RockAndOreSet[] rocksAndWalls;
   public final CrystalSet[] crystal;
   public final FloorSet[] floor;
   public final ColumnSet[] columns;
   public final FurnitureSet[] furniture;

   public MinersOfficeGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.rocksAndWalls = RockAndOreSet.getReducedSetForBiome(
         new RockAndOreSet[]{
            RockAndOreSet.forest,
            RockAndOreSet.forestcave,
            RockAndOreSet.deepforestcave,
            RockAndOreSet.desert,
            RockAndOreSet.desertcave,
            RockAndOreSet.deepdesertcave,
            RockAndOreSet.snow,
            RockAndOreSet.snowcave,
            RockAndOreSet.deepsnowcave,
            RockAndOreSet.swamp,
            RockAndOreSet.swampcave,
            RockAndOreSet.deepswampcave,
            RockAndOreSet.plains,
            RockAndOreSet.plainscave,
            RockAndOreSet.deepplainscave
         },
         biome,
         levelIdentifier
      );
      this.crystal = CrystalSet.getReducedSetForBiome(
         new CrystalSet[]{CrystalSet.topaz, CrystalSet.amethyst, CrystalSet.emerald, CrystalSet.sapphire, CrystalSet.ruby}, biome, levelIdentifier
      );
      this.floor = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.dungeon,
            FloorSet.dryad,
            FloorSet.granite,
            FloorSet.wood,
            FloorSet.deadWood,
            FloorSet.stoneTiled,
            FloorSet.deepStoneBrick,
            FloorSet.snowStoneBrick,
            FloorSet.deepSnowStone,
            FloorSet.sandstoneBrick,
            FloorSet.swampStoneBrick,
            FloorSet.deepSwampStoneBrick
         },
         biome,
         levelIdentifier
      );
      this.columns = ColumnSet.getReducedSetForBiome(
         new ColumnSet[]{
            ColumnSet.wood,
            ColumnSet.deepstone,
            ColumnSet.snowstone,
            ColumnSet.deepsnowstone,
            ColumnSet.sandstone,
            ColumnSet.deepsandstone,
            ColumnSet.swampstone,
            ColumnSet.deepswampstone
         },
         biome,
         levelIdentifier
      );
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.deadwood, FurnitureSet.dungeon}, biome, levelIdentifier
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
      return new MinersOfficePreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.rocksAndWalls),
         random.getOneOf(this.crystal),
         random.getOneOf(this.floor),
         random.getOneOf(this.columns),
         random.getOneOf(this.furniture)
      );
   }
}
