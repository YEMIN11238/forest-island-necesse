package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.engine.util.GameRandom;

public class RandomPlaceGeneratorLayer extends GeneratorLayer {
   protected float chance;
   protected int value;

   public RandomPlaceGeneratorLayer(int layerSeed, float chance, int placeValue) {
      super(layerSeed, 0);
      this.chance = chance;
      this.value = placeValue;
   }

   @Override
   protected Color getDebugColor(int value) {
      return value == this.value ? new Color(255, 0, 255) : null;
   }

   @Override
   protected int sample(int x, int y) {
      GameRandom random = this.getRandom(x, y);
      return random.getChance(this.chance) ? this.value : this.getParent(x, y);
   }
}
