package necesse.engine.world.biomeGenerator.layers;

public class RandomReplaceValueGeneratorLayer extends RandomReplaceGeneratorLayer {
   protected int valueToReplace;
   protected int valueToPlace;

   public RandomReplaceValueGeneratorLayer(int layerSeed, int valueToReplace, float replaceChance, int valueToPlace) {
      super(layerSeed, replaceChance, true);
      this.valueToReplace = valueToReplace;
      this.valueToPlace = valueToPlace;
      this.returnParentIfNotReplaced = true;
   }

   @Override
   protected boolean shouldReplaceValue(int x, int y) {
      return this.getParent(x, y) == this.valueToReplace;
   }

   @Override
   protected int getPlaceValue(int x, int y) {
      return this.valueToPlace;
   }
}
