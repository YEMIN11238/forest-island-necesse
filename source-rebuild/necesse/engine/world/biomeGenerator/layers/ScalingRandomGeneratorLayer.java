package necesse.engine.world.biomeGenerator.layers;

import necesse.engine.util.GameRandom;

public class ScalingRandomGeneratorLayer extends GeneratorLayer {
   public ScalingRandomGeneratorLayer(int layerSeed) {
      super(layerSeed, 1);
   }

   @Override
   protected int sample(int x, int y) {
      int parent = this.getParent(x >> 1, y >> 1);
      boolean xEven = (x & 1) == 0;
      boolean yEven = (y & 1) == 0;
      if (xEven && yEven) {
         return parent;
      }

      GameRandom random = this.getRandom(x, y);
      int east = this.getParent(x >> 1, y + 1 >> 1);
      if (xEven) {
         return random.nextInt(2) == 0 ? parent : east;
      }

      int south = this.getParent(x + 1 >> 1, y >> 1);
      if (yEven) {
         return random.nextInt(2) == 0 ? parent : south;
      }

      int southEast = this.getParent(x + 1 >> 1, y + 1 >> 1);
      return random.getOneOf(parent, east, south, southEast);
   }
}
