package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.VampireChurchPreset;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class VampireChurchGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final WallSet[] walls;
   public final FurnitureSet[] furniture;
   public final CarpetSet[] carpetRing;
   public final CarpetSet[] prayerCarpet;

   public VampireChurchGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, false, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.walls = WallSet.getReducedSetForBiome(
         new WallSet[]{WallSet.basalt, WallSet.stone, WallSet.deepStone, WallSet.swampStone, WallSet.deepSnowStone, WallSet.deepSwampStone, WallSet.dungeon},
         biome,
         levelIdentifier
      );
      this.furniture = FurnitureSet.getReducedSetForBiome(
         new FurnitureSet[]{
            FurnitureSet.willow, FurnitureSet.oak, FurnitureSet.deadwood, FurnitureSet.dryad, FurnitureSet.maple, FurnitureSet.bone, FurnitureSet.dungeon
         },
         biome,
         levelIdentifier
      );
      this.carpetRing = CarpetSet.getReducedSetForBiome(new CarpetSet[]{CarpetSet.green, CarpetSet.brownbear, CarpetSet.blue}, biome, levelIdentifier);
      this.prayerCarpet = CarpetSet.getReducedSetForBiome(
         new CarpetSet[]{CarpetSet.green, CarpetSet.blue, CarpetSet.steelgrey, CarpetSet.goldgrid}, biome, levelIdentifier
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
      return new VampireChurchPreset(
         this.biome,
         this.levelIdentifier,
         random,
         random.getOneOf(this.walls),
         random.getOneOf(this.furniture),
         random.getOneOf(this.carpetRing),
         random.getOneOf(this.prayerCarpet)
      );
   }
}
