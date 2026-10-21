package necesse.engine.world.biomeGenerator.layers;

public class ScalingNearestGeneratorLayer extends GeneratorLayer {
   public ScalingNearestGeneratorLayer(int layerSeed) {
      super(layerSeed, 1);
   }

   @Override
   protected int sample(int x, int y) {
      return this.getParent(x >> 1, y >> 1);
   }
}
