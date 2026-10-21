package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.RetirementHomePreset;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.LargelPaintingSet;
import necesse.level.maps.presets.set.SmallPaintingSet;
import necesse.level.maps.presets.set.WallSet;

public class RetirementHomeGenerationPreset extends SimpleGenerationPreset {
   public final WallSet[] walls;
   public final WallSet[] doors;
   public final CarpetSet[] carpets;
   public final FloorSet[] mainFloors;
   public final FloorSet[] bathroomFloors;
   public final FurnitureSet[] furnitures;
   public final SmallPaintingSet[] kitchenPaintings;
   public final SmallPaintingSet[] entrancePaintings;
   public final SmallPaintingSet[] deskPaintings;
   public final SmallPaintingSet[] chestPaintings;
   public final LargelPaintingSet[] largePaintings;

   public RetirementHomeGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, false, false, false, biome);
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.willow,
            WallSet.basalt,
            WallSet.palm,
            WallSet.crypt,
            WallSet.deepStone,
            WallSet.dryad,
            WallSet.swampStone,
            WallSet.wood,
            WallSet.deepSandstone,
            WallSet.spidercastle,
            WallSet.dungeon,
            WallSet.stone
         },
         biome,
         levelIdentifier
      );
      this.doors = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.willow,
            WallSet.basalt,
            WallSet.pine,
            WallSet.palm,
            WallSet.granite,
            WallSet.deepStone,
            WallSet.swampStone,
            WallSet.wood,
            WallSet.sandstone,
            WallSet.stone
         },
         biome,
         levelIdentifier
      );
      this.carpets = CarpetSet.getReducedSetForBiome(
         new CarpetSet[]{CarpetSet.velour, CarpetSet.green, CarpetSet.blue, CarpetSet.purple}, biome, levelIdentifier
      );
      this.mainFloors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.deepStoneBrick,
            FloorSet.pine,
            FloorSet.palm,
            FloorSet.deepSwampStoneBrick,
            FloorSet.spiderCastle,
            FloorSet.snowStone,
            FloorSet.deadWood,
            FloorSet.dungeon,
            FloorSet.swampStoneBrick,
            FloorSet.basalt,
            FloorSet.granite,
            FloorSet.deepStone,
            FloorSet.dryad,
            FloorSet.wood
         },
         biome,
         levelIdentifier
      );
      this.bathroomFloors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.deepSnowStoneBrick,
            FloorSet.deepStoneBrick,
            FloorSet.pine,
            FloorSet.deepSnowStone,
            FloorSet.deepSwampStoneBrick,
            FloorSet.spiderCastle,
            FloorSet.snowStone,
            FloorSet.dungeon,
            FloorSet.swampStoneBrick,
            FloorSet.basalt,
            FloorSet.graniteBrick,
            FloorSet.granite,
            FloorSet.deepStone,
            FloorSet.dryad
         },
         biome,
         levelIdentifier
      );
      this.furnitures = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.palm, FurnitureSet.deadwood, FurnitureSet.dryad, FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.dungeon},
         biome,
         levelIdentifier
      );
      this.kitchenPaintings = SmallPaintingSet.getReducedSetForBiome(
         new SmallPaintingSet[]{SmallPaintingSet.commonApple, SmallPaintingSet.commonAvocado, SmallPaintingSet.commonBanana}, biome, levelIdentifier
      );
      this.entrancePaintings = SmallPaintingSet.getReducedSetForBiome(
         new SmallPaintingSet[]{
            SmallPaintingSet.rareSandstonecaveling,
            SmallPaintingSet.rareStonecaveling,
            SmallPaintingSet.rareSnowcaveling,
            SmallPaintingSet.rareSwampcaveling,
            SmallPaintingSet.abandonedMineStonecaveling
         },
         biome,
         levelIdentifier
      );
      this.deskPaintings = SmallPaintingSet.getReducedSetForBiome(
         new SmallPaintingSet[]{SmallPaintingSet.commonAbstract, SmallPaintingSet.commonRainsun}, biome, levelIdentifier
      );
      this.chestPaintings = SmallPaintingSet.getReducedSetForBiome(
         new SmallPaintingSet[]{
            SmallPaintingSet.uncommonHeart,
            SmallPaintingSet.uncommonDagger,
            SmallPaintingSet.uncommonParrot,
            SmallPaintingSet.uncommonDuck,
            SmallPaintingSet.uncommonEye,
            SmallPaintingSet.uncommonMouse,
            SmallPaintingSet.uncommonCastle
         },
         biome,
         levelIdentifier
      );
      this.largePaintings = LargelPaintingSet.getReducedSetForBiome(
         new LargelPaintingSet[]{LargelPaintingSet.rareCastle, LargelPaintingSet.rareWorldMap, LargelPaintingSet.rareShip, LargelPaintingSet.rareAbstract},
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
      return new RetirementHomePreset(
         random,
         random.getOneOf(this.walls),
         random.getOneOf(this.doors),
         random.getOneOf(this.carpets),
         random.getOneOf(this.mainFloors),
         random.getOneOf(this.bathroomFloors),
         random.getOneOf(this.furnitures),
         random.getOneOf(this.kitchenPaintings),
         random.getOneOf(this.entrancePaintings),
         random.getOneOf(this.deskPaintings),
         random.getOneOf(this.chestPaintings),
         random.getOneOf(this.largePaintings)
      );
   }
}
