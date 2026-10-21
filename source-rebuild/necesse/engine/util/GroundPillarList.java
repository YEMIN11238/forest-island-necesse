package necesse.engine.util;

import java.util.Iterator;
import java.util.LinkedList;

public class GroundPillarList<T extends GroundPillar> implements Iterable<T> {
   private final LinkedList<T> pillars = new LinkedList<>();

   public void add(T pillar) {
      this.pillars.addFirst(pillar);
   }

   public void clean(long localTime, double currentDistance) {
      while (!this.pillars.isEmpty() && this.pillars.getLast().shouldRemove(localTime, currentDistance)) {
         this.pillars.removeLast();
      }
   }

   public void cleanThorough(long currentTime, double currentDistance) {
      this.pillars.removeIf(p -> p.shouldRemove(currentTime, currentDistance));
   }

   @Override
   public Iterator<T> iterator() {
      return this.pillars.iterator();
   }

   public int size() {
      return this.pillars.size();
   }

   public boolean isEmpty() {
      return this.pillars.isEmpty();
   }

   public LinkedList<T> copyPillars() {
      return new LinkedList<>(this.pillars);
   }
}
