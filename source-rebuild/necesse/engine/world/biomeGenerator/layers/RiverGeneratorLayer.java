package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.engine.GameRandomNoise;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorStack;

public class RiverGeneratorLayer extends GeneratorLayer {
   protected float spreadModifier;
   protected float size;
   protected float beachSize;
   protected GameRandomNoise noise;

   public RiverGeneratorLayer(int layerSeed, float spreadModifier, float size, float beachSize) {
      super(layerSeed, 0);
      this.spreadModifier = spreadModifier;
      this.size = size;
      this.beachSize = beachSize;
   }

   @Override
   public void init(GeneratorStack stack, GeneratorLayer parent, boolean isParentDifferentBranch, int worldSeed) {
      super.init(stack, parent, isParentDifferentBranch, worldSeed);
      this.noise = new GameRandomNoise(this.getRandom(0, 0).nextInt());
   }

   @Override
   protected int sample(int x, int y) {
      int parent = this.getParent(x, y);
      if (parent == -1) {
         return parent;
      } else {
         int scale = this.getStackScale();
         double value = this.noise.perlin2Fractal((double)x / scale * this.spreadModifier, (double)y / scale * this.spreadModifier, 4, 0.5);
         double abs = Math.abs(value);
         float maxValue = this.size * this.spreadModifier;
         if (abs < maxValue) {
            return -1;
         } else {
            return abs < maxValue + this.beachSize * this.spreadModifier ? -2 : parent;
         }
      }
   }

   @Override
   protected Color getDebugColor(int value) {
      return value == -1 ? BiomeGeneratorStack.waterDebugColor : super.getDebugColor(value);
   }
}
