package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.SmallOvergrownShipwreckPreset;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;

public class SmallOvergrownShipwreckGenerationPreset extends SimpleGenerationPreset {
   public final Biome biome;
   public final LevelIdentifier levelIdentifier;
   public final FloorSet[] floor;
   public final FurnitureSet[] bed;
   public final FurnitureSet[] chest;

   public SmallOvergrownShipwreckGenerationPreset(LevelIdentifier levelIdentifier, Biome biome) {
      super(20, true, true, true, false, biome);
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.floor = FloorSet.getReducedSetForBiome(new FloorSet[]{FloorSet.bamboo, FloorSet.wood}, biome, levelIdentifier);
      this.bed = FurnitureSet.getReducedSetForBiome(new FurnitureSet[]{FurnitureSet.oak, FurnitureSet.dryad, FurnitureSet.deadwood}, biome, levelIdentifier);
      this.chest = FurnitureSet.getReducedSetForBiome(new FurnitureSet[]{FurnitureSet.bamboo, FurnitureSet.willow}, biome, levelIdentifier);
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(new WorldApplyAreaPredicate(0, 0, tester.width - 1, tester.height - 1, 0, new WorldApplyAreaPredicate.WorldApplyCornerTest() {
         @Override
         public boolean isValidTile(WorldPreset preset, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int tileX, int tileY) {
            return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) ? generatorStack.isSurfaceOcean(tileX, tileY) : false;
         }
      }));
   }

   @Override
   public Preset getPreset(GameRandom random) {
      return new SmallOvergrownShipwreckPreset(
         this.biome, this.levelIdentifier, random, random.getOneOf(this.floor), random.getOneOf(this.bed), random.getOneOf(this.chest)
      );
   }
}
