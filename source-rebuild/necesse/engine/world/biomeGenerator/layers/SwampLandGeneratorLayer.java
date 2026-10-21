package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import java.awt.Point;
import java.util.function.BiFunction;
import necesse.engine.GameRandomNoise;
import necesse.engine.GameTileRange;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorStack;

public class SwampLandGeneratorLayer extends GeneratorLayer {
   protected float sizeModifier;
   protected float maxValue;
   protected float beachSize;
   protected GameTileRange beachFixRange;
   protected BiFunction<Integer, Integer, Integer> biomeGetter;
   protected GameRandomNoise noise;

   public SwampLandGeneratorLayer(
      int layerSeed, float sizeModifier, float oceanPercent, float beachSize, int beachFixRange, BiFunction<Integer, Integer, Integer> biomeGetter
   ) {
      super(layerSeed, 0);
      this.sizeModifier = sizeModifier;
      this.maxValue = oceanPercent * 2.0F - 1.0F;
      this.beachSize = beachSize;
      this.biomeGetter = biomeGetter;
      this.beachFixRange = new GameTileRange(beachFixRange);
   }

   @Override
   public void init(GeneratorStack stack, GeneratorLayer parent, boolean isParentDifferentBranch, int worldSeed) {
      super.init(stack, parent, isParentDifferentBranch, worldSeed);
      this.noise = new GameRandomNoise(this.getRandom(0, 0).nextInt());
   }

   @Override
   protected int sample(int x, int y) {
      int biomeID = this.biomeGetter.apply(x, y);
      int scale = this.getStackScale();
      int swampBiomeID = BiomeRegistry.SWAMP.getID();
      if (biomeID == swampBiomeID) {
         double value = this.noise.perlin2Fractal((double)x / scale * this.sizeModifier, (double)y / scale * this.sizeModifier, 4, 0.5);
         if (value < this.maxValue) {
            return -1;
         }

         for (Point tile : this.beachFixRange.getValidTiles(x, y)) {
            if (this.biomeGetter.apply(tile.x, tile.y) != swampBiomeID && this.getParent(tile.x, tile.y) == -1) {
               return -1;
            }
         }

         return value < this.maxValue + this.beachSize * this.sizeModifier ? -2 : biomeID;
      } else {
         return this.getParent(x, y);
      }
   }

   @Override
   protected Color getDebugColor(int value) {
      if (value == -1) {
         return BiomeGeneratorStack.waterDebugColor;
      } else {
         return value == -2 ? new Color(255, 246, 75) : super.getDebugColor(value);
      }
   }
}
