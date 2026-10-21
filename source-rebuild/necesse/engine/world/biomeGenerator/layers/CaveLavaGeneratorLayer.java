package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.engine.GameRandomNoise;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorStack;

public class CaveLavaGeneratorLayer extends GeneratorLayer {
   protected float sizeModifier;
   protected float maxValue;
   protected GameRandomNoise noise;

   public CaveLavaGeneratorLayer(int layerSeed, float sizeModifier, float oceanPercent) {
      super(layerSeed, 0);
      this.sizeModifier = sizeModifier;
      this.maxValue = oceanPercent * 2.0F - 1.0F;
   }

   @Override
   public void init(GeneratorStack stack, GeneratorLayer parent, boolean isParentDifferentBranch, int worldSeed) {
      super.init(stack, parent, isParentDifferentBranch, worldSeed);
      this.noise = new GameRandomNoise(this.getRandom(0, 0).nextInt());
   }

   @Override
   protected int sample(int x, int y) {
      int scale = this.getStackScale();
      double value = this.noise.perlin2Fractal((double)x / scale * this.sizeModifier, (double)y / scale * this.sizeModifier, 4, 0.5);
      return value < this.maxValue ? -1 : this.getParent(x, y);
   }

   @Override
   protected Color getDebugColor(int value) {
      return value == -1 ? BiomeGeneratorStack.lavaDebugColor : super.getDebugColor(value);
   }
}
