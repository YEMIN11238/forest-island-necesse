package necesse.engine.world.worldPresets;

import necesse.engine.util.GameRandom;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.MageShopPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.set.FurnitureSet;

public class MageShopGenerationPreset extends SimpleGenerationPreset {
   public FurnitureSet[] furnitureSets;

   public MageShopGenerationPreset(FurnitureSet[] furnitureSets, Biome... biomes) {
      super(20, true, true, false, false, biomes);
      this.furnitureSets = furnitureSets;
   }

   @Override
   public void setupTester(WorldPresetTester tester) {
      tester.addApplyPredicate(
         new WorldApplyAreaPredicate(
            -1,
            -1,
            tester.width + 1,
            tester.height + 1,
            0,
            (WorldApplyAreaPredicate.WorldApplyCornerTest)(preset, presetsRegion, generatorStack, tileX, tileY) -> !generatorStack.isSurfaceOceanOrRiverOrBeach(
               tileX, tileY
            )
         )
      );
   }

   @Override
   public Preset getPreset(GameRandom random) {
      FurnitureSet furnitureSet = random.getOneOf(this.furnitureSets);
      return new MageShopPreset(random, furnitureSet);
   }
}
