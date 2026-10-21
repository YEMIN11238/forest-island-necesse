package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.engine.GameRandomNoise;
import necesse.engine.GameTileRange;
import necesse.engine.util.GameRandom;
import necesse.engine.world.biomeGenerator.GeneratorStack;

public class ChanceVeinRandomSizeGeneratorLayer extends GeneratorLayer {
   protected int minVeinSize;
   protected int maxVeinSize;
   protected float placeChance;
   protected int placeValue;
   protected GameRandomNoise noise;
   protected GameTileRange[] tileRanges;

   public ChanceVeinRandomSizeGeneratorLayer(int layerSeed, int minVeinSize, int maxVeinSize, float placeChance, int placeValue, boolean circular) {
      super(layerSeed, 0);
      if (minVeinSize >= 0 && maxVeinSize >= 0 && minVeinSize <= maxVeinSize) {
         this.minVeinSize = minVeinSize;
         this.maxVeinSize = maxVeinSize;
         this.placeChance = placeChance;
         this.placeValue = placeValue;
         if (circular) {
            int deltaSize = maxVeinSize - minVeinSize + 1;
            this.tileRanges = new GameTileRange[deltaSize];

            for (int i = 0; i < deltaSize; i++) {
               this.tileRanges[i] = new GameTileRange(Math.max((minVeinSize + i) / 2, 2));
            }
         }
      } else {
         throw new IllegalArgumentException("Invalid vein size parameters");
      }
   }

   @Override
   public void init(GeneratorStack stack, GeneratorLayer parent, boolean isParentDifferentBranch, int worldSeed) {
      super.init(stack, parent, isParentDifferentBranch, worldSeed);
      this.noise = new GameRandomNoise(this.getRandom(0, 0).nextInt());
   }

   @Override
   protected int sample(int x, int y) {
      int scaledTileX = x / this.maxVeinSize;
      int scaledTileY = y / this.maxVeinSize;
      GameRandom random = this.getRandom(scaledTileX, scaledTileY);
      if (random.getChance(this.placeChance)) {
         int modTileX = Math.abs(x % this.maxVeinSize);
         int modTileY = Math.abs(y % this.maxVeinSize);
         if (this.tileRanges != null) {
            int veinSize = this.minVeinSize + random.nextInt(this.maxVeinSize - this.minVeinSize + 1);
            int padding = veinSize - this.minVeinSize;
            int offsetX = padding < 1 ? 0 : random.nextInt(padding);
            int offsetY = padding < 1 ? 0 : random.nextInt(padding);
            int offsetTileX = modTileX - offsetX;
            int offsetTileY = modTileY - offsetY;
            return this.tileRanges[veinSize - this.minVeinSize].isWithinRange(veinSize / 2 - offsetX, veinSize / 2 - offsetY, offsetTileX, offsetTileY)
               ? this.placeValue
               : -2;
         } else {
            int veinSizeX = this.minVeinSize + random.nextInt(this.maxVeinSize - this.minVeinSize + 1);
            int veinSizeY = this.minVeinSize + random.nextInt(this.maxVeinSize - this.minVeinSize + 1);
            int paddingX = veinSizeX - this.minVeinSize;
            int paddingY = veinSizeY - this.minVeinSize;
            int offsetX = paddingX < 1 ? 0 : random.nextInt(paddingX);
            int offsetY = paddingY < 1 ? 0 : random.nextInt(paddingY);
            int offsetTileX = modTileX - offsetX;
            int offsetTileY = modTileY - offsetY;
            return offsetTileX >= 0 && offsetTileY >= 0 && offsetTileX < veinSizeX && offsetTileY < veinSizeY ? this.placeValue : -2;
         }
      } else {
         return this.getParent(x, y);
      }
   }

   @Override
   protected Color getDebugColor(int value) {
      return value == this.placeValue ? new Color(255, 0, 255) : super.getDebugColor(value);
   }
}
