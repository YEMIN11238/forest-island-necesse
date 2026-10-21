package necesse.engine.util;

import java.util.Comparator;

public class CompareSequence<T> {
   private CompareSequence<T> before;
   private CompareSequence<T> then;
   private final Comparator<T> comparator;

   public CompareSequence(Comparator<T> comparator) {
      this.comparator = comparator;
   }

   public CompareSequence<T> beforeBy(Comparator<T> comparator) {
      CompareSequence<T> prev = new CompareSequence<>(comparator);
      prev.then = this;
      if (this.before != null) {
         prev.before = this.before;
         this.before.then = prev;
      }

      this.before = prev;
      return prev;
   }

   public CompareSequence<T> thenBy(Comparator<T> comparator) {
      CompareSequence<T> next = new CompareSequence<>(comparator);
      next.before = this;
      if (this.then != null) {
         next.then = this.then;
         this.then.before = next;
      }

      this.then = next;
      return next;
   }

   public CompareSequence<T> firstBy(Comparator<T> comparator) {
      return this.getFirst().beforeBy(comparator);
   }

   public CompareSequence<T> lastBy(Comparator<T> comparator) {
      return this.getLast().thenBy(comparator);
   }

   private CompareSequence<T> getFirst() {
      return this.before != null ? this.before.getFirst() : this;
   }

   private CompareSequence<T> getLast() {
      return this.then != null ? this.then.getLast() : this;
   }

   public Comparator<T> getComparator() {
      CompareSequence<T> current = this.getFirst();
      Comparator<T> comparing = current.comparator;
      if (current.then != null) {
         current = current.then;
         comparing = comparing.thenComparing(current.comparator);
      }

      return comparing;
   }
}
