package necesse.engine.world.biomeGenerator.layers;

public abstract class RandomReplaceGeneratorLayer extends GeneratorLayer {
   protected float replaceChance;
   protected boolean returnParentIfNotReplaced = false;

   public RandomReplaceGeneratorLayer(int layerSeed, float replaceChance, boolean reducedCacheSize) {
      super(layerSeed, 0, reducedCacheSize);
      this.replaceChance = replaceChance;
   }

   @Override
   protected int sample(int x, int y) {
      if (this.getRandom(x, y).getChance(this.replaceChance) && this.shouldReplaceValue(x, y)) {
         return this.getPlaceValue(x, y);
      } else {
         return this.returnParentIfNotReplaced ? this.getParent(x, y) : 0;
      }
   }

   protected abstract boolean shouldReplaceValue(int var1, int var2);

   protected abstract int getPlaceValue(int var1, int var2);
}
