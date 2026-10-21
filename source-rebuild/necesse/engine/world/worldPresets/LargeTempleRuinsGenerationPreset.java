package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.LargeTempleRuinsPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;

public class LargeTempleRuinsGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final ChestRoomSet[] chestRoomSets;
   public final FurnitureSet[] chests;
   public final FloorSet[] floorSets;

   public LargeTempleRuinsGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.chestRoomSets = ChestRoomSet.getReducedSetForBiome(
         new ChestRoomSet[]{
            ChestRoomSet.basalt,
            ChestRoomSet.granite,
            ChestRoomSet.deepStone,
            ChestRoomSet.swampStone,
            ChestRoomSet.deepSnowStone,
            ChestRoomSet.snowStone,
            ChestRoomSet.deepSwampStone,
            ChestRoomSet.deepSandstone,
            ChestRoomSet.sandstone,
            ChestRoomSet.stone
         },
         biome,
         levelIdentifier
      );
      this.chests = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.birch, FurnitureSet.deadwood, FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.bone, FurnitureSet.dungeon},
         biome,
         levelIdentifier
      );
      this.floorSets = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.stone,
            FloorSet.stoneTiled,
            FloorSet.stoneBrick,
            FloorSet.deepStoneBrick,
            FloorSet.snowRock,
            FloorSet.snowStone,
            FloorSet.deepSnowStone,
            FloorSet.deepSnowStoneBrick,
            FloorSet.granite,
            FloorSet.graniteBrick,
            FloorSet.basalt,
            FloorSet.swampStone,
            FloorSet.swampStoneBrick,
            FloorSet.deepSwampStone,
            FloorSet.deepSwampStoneBrick,
            FloorSet.sandstone,
            FloorSet.sandstoneBrick
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
      return new LargeTempleRuinsPreset(
         random, this.biome, this.levelIdentifier, random.getOneOf(this.chestRoomSets), random.getOneOf(this.chests), random.getOneOf(this.floorSets)
      );
   }
}
