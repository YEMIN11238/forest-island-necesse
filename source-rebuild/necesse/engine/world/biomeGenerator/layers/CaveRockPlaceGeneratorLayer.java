package necesse.engine.world.biomeGenerator.layers;

import java.awt.Color;
import necesse.level.maps.biomes.Biome;

public abstract class CaveRockPlaceGeneratorLayer extends GeneratorLayer {
   public CaveRockPlaceGeneratorLayer(int layerSeed) {
      super(layerSeed, 0);
   }

   @Override
   protected int sample(int x, int y) {
      Biome biome = this.getBiome(x, y);
      return this.getRandom(x, y).getChance(this.getRockPlaceChance(biome, x, y)) ? -1 : this.getParent(x, y);
   }

   protected abstract Biome getBiome(int var1, int var2);

   protected abstract float getRockPlaceChance(Biome var1, int var2, int var3);

   @Override
   protected Color getDebugColor(int value) {
      return value == -1 ? new Color(255, 0, 255) : super.getDebugColor(value);
   }
}
