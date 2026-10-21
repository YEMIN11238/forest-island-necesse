package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.engine.GameRandomNoise;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorStack;

public class OceanGeneratorLayer extends GeneratorLayer {
   protected float sizeModifier;
   protected float maxValue;
   protected float beachSize;
   protected GameRandomNoise noise;

   public OceanGeneratorLayer(int layerSeed, float sizeModifier, float oceanPercent, float beachSize) {
      super(layerSeed, 0);
      this.sizeModifier = sizeModifier;
      this.maxValue = oceanPercent * 2.0F - 1.0F;
      this.beachSize = beachSize;
   }

   @Override
   public void init(GeneratorStack stack, GeneratorLayer parent, boolean isParentDifferentBranch, int worldSeed) {
      super.init(stack, parent, isParentDifferentBranch, worldSeed);
      this.noise = new GameRandomNoise(this.getRandom(0, 0).nextInt());
   }

   protected double getValue(int tileX, int tileY) {
      int scale = this.getStackScale();
      return this.noise.perlin2Fractal((double)tileX / scale * this.sizeModifier, (double)tileY / scale * this.sizeModifier, 4, 0.5);
   }

   @Override
   protected int sample(int x, int y) {
      double value = this.getValue(x, y);
      if (value < this.maxValue) {
         return -1;
      } else {
         return value < this.maxValue + this.beachSize * this.sizeModifier ? -2 : this.getParent(x, y);
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
