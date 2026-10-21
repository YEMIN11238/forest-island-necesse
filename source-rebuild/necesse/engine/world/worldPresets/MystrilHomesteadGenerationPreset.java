package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.MystrilHomesteadPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.BushSet;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.CropSet;
import necesse.level.maps.presets.set.FenceSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.PresetSet;
import necesse.level.maps.presets.set.WallSet;

public class MystrilHomesteadGenerationPreset extends SimpleGenerationPreset {
   public final WallSet[] innerWalls;
   public final WallSet[] outerWalls;
   public final FloorSet[] bedRoomsFloors;
   public final FloorSet[] bathroomsFloors;
   public final FloorSet[] mainFloors;
   public final FloorSet[] entranceFloors;
   public final CarpetSet[] entranceCarpets;
   public final CarpetSet[] doubleBedCarpets;
   public final CarpetSet[] bedTopCarpets;
   public final CarpetSet[] bedRightCarpets;
   public final CarpetSet[] bathroomMats;
   public final CarpetSet[] kitchenCarpets;
   public final FurnitureSet[] doubleBedFurniture;
   public final FurnitureSet[] topBedFurniture;
   public final FurnitureSet[] rightBedFurniture;
   public final FurnitureSet[] bathroomFurniture;
   public final FurnitureSet[] mainLights;
   public final FurnitureSet[] kitchenTables;
   public final BushSet[] bushes;
   public final CropSet[] rightFarms;
   public final CropSet[] leftFarms;
   public final FenceSet[] fences;

   public MystrilHomesteadGenerationPreset(LevelIdentifier identifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.innerWalls = WallSet.getReducedSetForBiome(new WallSet[]{WallSet.pine, WallSet.palm, WallSet.wood}, biome, identifier);
      this.outerWalls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.pine, WallSet.granite, WallSet.swampStone, WallSet.snowStone, WallSet.wood, WallSet.brick, WallSet.sandstone, WallSet.stone},
         biome,
         identifier
      );
      this.bedRoomsFloors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{FloorSet.pine, FloorSet.palm, FloorSet.dryad, FloorSet.deadWood, FloorSet.wood}, biome, identifier
      );
      this.bathroomsFloors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.deepSnowStoneBrick,
            FloorSet.sandstoneBrick,
            FloorSet.deepStoneBrick,
            FloorSet.graniteBrick,
            FloorSet.granite,
            FloorSet.deepSwampStone,
            FloorSet.snowStone,
            FloorSet.snowStoneBrick,
            FloorSet.stoneTiled,
            FloorSet.dungeon,
            FloorSet.swampStoneBrick
         },
         biome,
         identifier
      );
      this.mainFloors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.sandstoneBrick,
            FloorSet.deepStoneBrick,
            FloorSet.pine,
            FloorSet.palm,
            FloorSet.granite,
            FloorSet.deepStoneTiled,
            FloorSet.deepSnowStone,
            FloorSet.dryad,
            FloorSet.deepSwampStone,
            FloorSet.snowStone,
            FloorSet.deadWood,
            FloorSet.dungeon
         },
         biome,
         identifier
      );
      this.entranceFloors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.deepStoneBrick,
            FloorSet.graniteBrick,
            FloorSet.deepStone,
            FloorSet.deepSwampStoneBrick,
            FloorSet.stoneTiled,
            FloorSet.sandstone,
            FloorSet.stone,
            FloorSet.swampStoneBrick
         },
         biome,
         identifier
      );
      this.doubleBedFurniture = this.kitchenTables = this.topBedFurniture = this.rightBedFurniture = this.bathroomFurniture = PresetSet.getReducedSetForBiome(
         new FurnitureSet[]{
            FurnitureSet.pine, FurnitureSet.birch, FurnitureSet.palm, FurnitureSet.maple, FurnitureSet.oak, FurnitureSet.dungeon, FurnitureSet.spruce
         },
         biome,
         identifier
      );
      this.bushes = PresetSet.getReducedSetForBiome(new BushSet[]{BushSet.raspberry, BushSet.blueberry, BushSet.blackberry}, biome, identifier);
      this.rightFarms = this.leftFarms = PresetSet.getReducedSetForBiome(
         new CropSet[]{
            CropSet.cabbage,
            CropSet.corn,
            CropSet.potato,
            CropSet.onion,
            CropSet.strawberry,
            CropSet.wheat,
            CropSet.tomato,
            CropSet.chilipepper,
            CropSet.beet,
            CropSet.pumpkin,
            CropSet.eggplant,
            CropSet.carrot
         },
         biome,
         identifier
      );
      this.mainLights = new FurnitureSet[]{
         FurnitureSet.willow,
         FurnitureSet.pine,
         FurnitureSet.birch,
         FurnitureSet.palm,
         FurnitureSet.oak,
         FurnitureSet.maple,
         FurnitureSet.deadwood,
         FurnitureSet.spruce
      };
      this.entranceCarpets = new CarpetSet[]{CarpetSet.blue, CarpetSet.purple};
      this.doubleBedCarpets = new CarpetSet[]{
         CarpetSet.velour,
         CarpetSet.wool,
         CarpetSet.brownbear,
         CarpetSet.green,
         CarpetSet.blue,
         CarpetSet.purple,
         CarpetSet.redyarn,
         CarpetSet.steelgrey,
         CarpetSet.heart,
         CarpetSet.goldgrid
      };
      this.bedRightCarpets = this.bedTopCarpets = new CarpetSet[]{
         CarpetSet.velour,
         CarpetSet.wool,
         CarpetSet.brownbear,
         CarpetSet.green,
         CarpetSet.blue,
         CarpetSet.purple,
         CarpetSet.steelgrey,
         CarpetSet.heart,
         CarpetSet.goldgrid
      };
      this.kitchenCarpets = new CarpetSet[]{CarpetSet.velour, CarpetSet.brownbear, CarpetSet.green, CarpetSet.purple, CarpetSet.steelgrey, CarpetSet.leather};
      this.bathroomMats = new CarpetSet[]{CarpetSet.velour, CarpetSet.brownbear, CarpetSet.green, CarpetSet.purple, CarpetSet.steelgrey, CarpetSet.leather};
      this.fences = new FenceSet[]{FenceSet.iron, FenceSet.wood, FenceSet.stone};
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(new WorldApplyAreaPredicate(-5, -5, tester.width + 5, tester.height + 5, 0, new WorldApplyAreaPredicate.WorldApplyGridTest(5) {
         @Override
         public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            if (presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
               return !generatorStack.isSurfaceOceanOrRiverOrBeach(tileX, tileY);
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
      return new MystrilHomesteadPreset(
         random,
         random.getOneOf(this.innerWalls),
         random.getOneOf(this.outerWalls),
         random.getOneOf(this.bedRoomsFloors),
         random.getOneOf(this.bathroomsFloors),
         random.getOneOf(this.mainFloors),
         random.getOneOf(this.entranceFloors),
         random.getOneOf(this.entranceCarpets),
         random.getOneOf(this.doubleBedCarpets),
         random.getOneOf(this.bedTopCarpets),
         random.getOneOf(this.bedRightCarpets),
         random.getOneOf(this.bathroomMats),
         random.getOneOf(this.kitchenCarpets),
         random.getOneOf(this.doubleBedFurniture),
         random.getOneOf(this.topBedFurniture),
         random.getOneOf(this.rightBedFurniture),
         random.getOneOf(this.bathroomFurniture),
         random.getOneOf(this.mainLights),
         random.getOneOf(this.kitchenTables),
         random.getOneOf(this.bushes),
         random.getOneOf(this.rightFarms),
         random.getOneOf(this.leftFarms),
         random.getOneOf(this.fences)
      );
   }
}
