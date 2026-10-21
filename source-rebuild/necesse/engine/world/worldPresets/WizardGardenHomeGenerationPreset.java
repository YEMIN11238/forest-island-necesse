package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.WizardGardenHomePreset;
import necesse.level.maps.presets.set.BushSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.HedgeSet;
import necesse.level.maps.presets.set.PathSet;
import necesse.level.maps.presets.set.SmallPaintingSet;
import necesse.level.maps.presets.set.TreeSet;
import necesse.level.maps.presets.set.WallSet;

public class WizardGardenHomeGenerationPreset extends SimpleGenerationPreset {
   public final HedgeSet[] hedges;
   public final WallSet[] walls;
   public final WallSet[] doorss;
   public final FurnitureSet[] furnitures;
   public final TreeSet[] treess;
   public final BushSet[] innerBerryBushs;
   public final SmallPaintingSet[] rightRoomPaintings;
   public final PathSet[] pathSets;

   public WizardGardenHomeGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.hedges = HedgeSet.getReducedSetForBiome(new HedgeSet[]{HedgeSet.forest, HedgeSet.snow, HedgeSet.plains, HedgeSet.swamp}, biome, levelIdentifier);
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.granite,
            WallSet.basalt,
            WallSet.crypt,
            WallSet.deepStone,
            WallSet.dryad,
            WallSet.swampStone,
            WallSet.snowStone,
            WallSet.deepSandstone,
            WallSet.ancientruin,
            WallSet.spidercastle,
            WallSet.dungeon,
            WallSet.stone
         },
         biome,
         levelIdentifier
      );
      this.doorss = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.pine,
            WallSet.crypt,
            WallSet.swampStone,
            WallSet.snowStone,
            WallSet.ice,
            WallSet.ancientruin,
            WallSet.spidercastle,
            WallSet.dungeon,
            WallSet.stone,
            WallSet.basalt,
            WallSet.granite,
            WallSet.deepStone,
            WallSet.dryad,
            WallSet.wood
         },
         biome,
         levelIdentifier
      );
      this.furnitures = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{
            FurnitureSet.willow,
            FurnitureSet.pine,
            FurnitureSet.oak,
            FurnitureSet.maple,
            FurnitureSet.deadwood,
            FurnitureSet.dryad,
            FurnitureSet.dungeon,
            FurnitureSet.spruce
         },
         biome,
         levelIdentifier
      );
      this.treess = TreeSet.getReducedSetForBiome(
         new TreeSet[]{TreeSet.willow, TreeSet.pine, TreeSet.birch, TreeSet.maple, TreeSet.oak, TreeSet.spruce}, biome, levelIdentifier
      );
      this.innerBerryBushs = new BushSet[]{BushSet.raspberry, BushSet.blueberry, BushSet.blackberry};
      this.rightRoomPaintings = SmallPaintingSet.getReducedSetForBiome(
         new SmallPaintingSet[]{
            SmallPaintingSet.rareSandstonecaveling, SmallPaintingSet.rareStonecaveling, SmallPaintingSet.rareSnowcaveling, SmallPaintingSet.rareSwampcaveling
         },
         biome,
         levelIdentifier
      );
      this.pathSets = PathSet.getReducedSetForBiome(
         new PathSet[]{PathSet.dryad, PathSet.stone, PathSet.sandStone, PathSet.swampStone, PathSet.snowStone, PathSet.granite, PathSet.crypt},
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
      return new WizardGardenHomePreset(
         random,
         random.getOneOf(this.hedges),
         random.getOneOf(this.walls),
         random.getOneOf(this.doorss),
         random.getOneOf(this.furnitures),
         random.getOneOf(this.treess),
         random.getOneOf(this.innerBerryBushs),
         random.getOneOf(this.rightRoomPaintings),
         random.getOneOf(this.pathSets)
      );
   }
}
