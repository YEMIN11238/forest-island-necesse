package necesse.engine.world.biomeGenerator.layers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import necesse.engine.registries.BiomeRegistry;

public class BiomeRulesGeneratorLayer extends AdjacentGeneratorLayer {
   public BiomeRulesGeneratorLayer(int layerSeed) {
      super(layerSeed);
   }

   protected boolean hasAnyBiomeID(int[] array, int biomeID) {
      for (int id : array) {
         if (id == biomeID) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected int sample(int x, int y, int[] adjacent) {
      int parent = this.getParent(x, y);
      if (parent == BiomeRegistry.DESERT.getID() && this.hasAnyBiomeID(adjacent, BiomeRegistry.SNOW.getID())) {
         return this.getMostFrequent(x, y, adjacent, BiomeRegistry.DESERT.getID(), BiomeRegistry.SWAMP.getID(), BiomeRegistry.SNOW.getID());
      } else {
         return parent == BiomeRegistry.SWAMP.getID() && this.hasAnyBiomeID(adjacent, BiomeRegistry.SNOW.getID())
            ? this.getMostFrequent(x, y, adjacent, BiomeRegistry.SWAMP.getID(), BiomeRegistry.DESERT.getID(), BiomeRegistry.SNOW.getID())
            : parent;
      }
   }

   protected int getMostFrequent(int x, int y, int[] adjacent, int... excludeBiomeIDs) {
      HashMap<Integer, Integer> frequency = new HashMap<>();

      for (int i : adjacent) {
         if (!this.hasAnyBiomeID(excludeBiomeIDs, i)) {
            frequency.compute(i, (k, v) -> v == null ? 1 : v + 1);
         }
      }

      if (frequency.isEmpty()) {
         return adjacent[4];
      }

      Iterator<Entry<Integer, Integer>> iterator = frequency.entrySet().stream().sorted(Comparator.comparingInt(e -> -e.getValue())).iterator();
      ArrayList<Integer> biomeIDs = new ArrayList<>(8);
      Entry<Integer, Integer> first = iterator.next();
      int bestFrequency = first.getValue();
      biomeIDs.add(first.getKey());

      while (iterator.hasNext()) {
         Entry<Integer, Integer> next = iterator.next();
         if (next.getValue() != bestFrequency) {
            break;
         }

         biomeIDs.add(next.getKey());
      }

      return this.getRandom(x, y).getOneOf(biomeIDs);
   }
}
