package necesse.engine.world.biomeGenerator.layers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class ClearSinglesGeneratorLayer extends AdjacentGeneratorLayer {
   public int maxFrequency;

   public ClearSinglesGeneratorLayer(int maxFrequency) {
      super(0);
      this.maxFrequency = maxFrequency;
   }

   @Override
   protected int sample(int x, int y, int[] adjacent) {
      HashMap<Integer, Integer> frequency = new HashMap<>();

      for (int i : adjacent) {
         frequency.compute(i, (k, v) -> v == null ? 1 : v + 1);
      }

      int center = adjacent[4];
      if (frequency.getOrDefault(center, 0) > this.maxFrequency) {
         return center;
      }

      Iterator<Entry<Integer, Integer>> iterator = frequency.entrySet()
         .stream()
         .filter(e -> e.getKey() != center)
         .sorted(Comparator.comparingInt(e -> -e.getValue()))
         .iterator();
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
