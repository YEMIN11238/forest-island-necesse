package necesse.engine.util;

import java.util.ArrayList;
import java.util.TreeSet;

public class PriorityMap<T> {
   private HashMapArrayList<Integer, T> map = new HashMapArrayList<>();
   private TreeSet<Integer> priorities = new TreeSet<>();

   public boolean isEmpty() {
      return this.priorities.isEmpty();
   }

   public void add(int priority, T object) {
      this.priorities.add(priority);
      this.map.add(priority, object);
   }

   public boolean hasBetter(int priority) {
      return this.priorities.isEmpty() ? false : this.priorities.last() > priority;
   }

   public boolean addIfHasNoBetter(int priority, T object) {
      if (this.hasBetter(priority)) {
         return false;
      }

      this.add(priority, object);
      return true;
   }

   public ArrayList<T> getBestObjectsList() {
      for (int preference : this.priorities.descendingSet()) {
         ArrayList<T> list = this.map.get(preference);
         if (!list.isEmpty()) {
            return new ArrayList<>(list);
         }
      }

      return new ArrayList<>();
   }

   public ArrayList<T> getBestObjects(int minListSize) {
      ArrayList<T> objects = new ArrayList<>(minListSize);

      for (int preference : this.priorities.descendingSet()) {
         ArrayList<T> list = this.map.get(preference);
         int needed = minListSize - objects.size();
         if (list.size() >= needed) {
            if (objects.isEmpty()) {
               return new ArrayList<>(list);
            }

            objects.addAll(list);
            break;
         }

         objects.addAll(list);
      }

      return objects;
   }

   public T getRandomBestObject(GameRandom random, int minListSize) {
      return random.getOneOf(this.getBestObjects(minListSize));
   }

   public void printDebug() {
      for (int preference : this.priorities.descendingSet()) {
         ArrayList<T> list = this.map.get(preference);
         System.out.println(list.size() + " with priority " + preference);
      }
   }
}
