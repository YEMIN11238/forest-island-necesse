package necesse.engine.world.worldPresets;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;

public class DeepSwampSpiderNestsWorldPreset extends SpiderNestsWorldPreset {
   public DeepSwampSpiderNestsWorldPreset() {
      super(BiomeRegistry.SWAMP, LevelIdentifier.DEEP_CAVE_IDENTIFIER, 0.025F, "smallswampcavespider");
      this.armsMinRange = 15;
      this.armsMaxRange = 25;
   }

   @Override
   public boolean isValidPosition(int tileX, int tileY, int width, int height, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack) {
      return this.runCornerCheck(tileX, tileY, width, height, new WorldPreset.ValidTilePredicate() {
         @Override
         public boolean isValidPosition(int tileX, int tileYx) {
            return generatorStack.getLazyBiomeID(tileX, tileYx) == BiomeRegistry.SWAMP.getID();
         }
      });
   }
}
