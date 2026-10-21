package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.engine.GameRandomNoise;
import necesse.engine.world.biomeGenerator.GeneratorStack;

public class SimplexVeinGeneratorLayer extends GeneratorLayer {
   protected float sizeModifier;
   protected float minValue;
   protected float placeChance;
   protected int placeValue;
   protected GameRandomNoise noise;

   public SimplexVeinGeneratorLayer(int layerSeed, float sizeModifier, float minValue, float placeChance, int placeValue) {
      super(layerSeed, 0, true);
      this.sizeModifier = sizeModifier;
      this.minValue = minValue;
      this.placeChance = placeChance;
      this.placeValue = placeValue;
   }

   @Override
   public void init(GeneratorStack stack, GeneratorLayer parent, boolean isParentDifferentBranch, int worldSeed) {
      super.init(stack, parent, isParentDifferentBranch, worldSeed);
      this.noise = new GameRandomNoise(this.getRandom(0, 0).nextInt());
   }

   @Override
   protected int sample(int x, int y) {
      int scale = this.getStackScale();
      double value = this.noise.simplex2Fractal((double)x / scale * this.sizeModifier, (double)y / scale * this.sizeModifier, 4, 0.5);
      return !(Math.abs(value) > this.minValue) || !(this.placeChance >= 1.0F) && !this.getRandom(x, y).getChance(this.placeChance)
         ? this.getParent(x, y)
         : this.placeValue;
   }

   @Override
   protected Color getDebugColor(int value) {
      return value == this.placeValue ? new Color(255, 0, 255) : super.getDebugColor(value);
   }
}
