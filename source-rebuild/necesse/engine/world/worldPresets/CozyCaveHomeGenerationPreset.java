package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.CozyCaveHomePreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.ColumnSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class CozyCaveHomeGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final WallSet[] walls;
   public final FloorSet[] woodfloor;
   public final ColumnSet[] columns;
   public final FurnitureSet[] furniture;
   public final FloorSet[] stonefloor;

   public CozyCaveHomeGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{
            WallSet.palm,
            WallSet.swampStone,
            WallSet.deepSnowStone,
            WallSet.snowStone,
            WallSet.ice,
            WallSet.sandstone,
            WallSet.dungeon,
            WallSet.stone,
            WallSet.bamboo,
            WallSet.willow,
            WallSet.basalt,
            WallSet.granite,
            WallSet.wood,
            WallSet.brick
         },
         biome,
         levelIdentifier
      );
      this.woodfloor = FloorSet.getReducedSetForBiome(new FloorSet[]{FloorSet.pine, FloorSet.palm, FloorSet.dryad, FloorSet.wood}, biome, levelIdentifier);
      this.columns = ColumnSet.getReducedSetForBiome(
         new ColumnSet[]{ColumnSet.swampstone, ColumnSet.granite, ColumnSet.wood, ColumnSet.snowstone, ColumnSet.sandstone, ColumnSet.stone},
         biome,
         levelIdentifier
      );
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.birch, FurnitureSet.palm, FurnitureSet.spruce}, biome, levelIdentifier
      );
      this.stonefloor = FloorSet.getReducedSetForBiome(
         new FloorSet[]{FloorSet.snowStoneBrick, FloorSet.sandstone, FloorSet.swampStoneBrick, FloorSet.stone, FloorSet.graniteBrick}, biome, levelIdentifier
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
      return new CozyCaveHomePreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.walls),
         random.getOneOf(this.woodfloor),
         random.getOneOf(this.columns),
         random.getOneOf(this.furniture),
         random.getOneOf(this.stonefloor)
      );
   }
}
