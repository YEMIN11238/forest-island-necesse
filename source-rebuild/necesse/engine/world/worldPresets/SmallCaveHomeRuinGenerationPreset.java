package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.SmallCaveHomeRuinPreset;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class SmallCaveHomeRuinGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FloorSet[] floors;
   public final FurnitureSet[] furniture;
   public final CarpetSet[] carpet;
   public final WallSet[] walls;

   public SmallCaveHomeRuinGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.floors = FloorSet.getReducedSetForBiome(
         new FloorSet[]{
            FloorSet.palm,
            FloorSet.wood,
            FloorSet.stone,
            FloorSet.deadWood,
            FloorSet.sandstone,
            FloorSet.snowStone,
            FloorSet.sandstoneBrick,
            FloorSet.swampStone,
            FloorSet.swampStoneBrick,
            FloorSet.snowStoneBrick,
            FloorSet.dungeon
         },
         biome,
         levelIdentifier
      );
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.pine, FurnitureSet.oak, FurnitureSet.maple, FurnitureSet.spruce}, biome, levelIdentifier
      );
      this.carpet = CarpetSet.getReducedSetForBiome(
         new CarpetSet[]{CarpetSet.velour, CarpetSet.brownbear, CarpetSet.green, CarpetSet.leather}, biome, levelIdentifier
      );
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.stone, WallSet.granite, WallSet.basalt, WallSet.swampStone, WallSet.snowStone, WallSet.sandstone}, biome, levelIdentifier
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
      return new SmallCaveHomeRuinPreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.floors),
         random.getOneOf(this.floors),
         random.getOneOf(this.floors),
         random.getOneOf(this.furniture),
         random.getOneOf(this.carpet),
         random.getOneOf(this.walls)
      );
   }
}
