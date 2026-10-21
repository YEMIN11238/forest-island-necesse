package necesse.engine.util;

import java.util.Iterator;

public class ForIterator implements Iterator<Integer> {
   public final int startIndex;
   public final int endIndex;
   public final int delta;
   private int current;

   public ForIterator(int startIndex, int endIndex, int delta) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.current = startIndex;
      this.delta = getDelta(startIndex, endIndex, delta);
   }

   public ForIterator(int startIndex, int endIndex) {
      this(startIndex, endIndex, 1);
   }

   private static int getDelta(int startIndex, int endIndex, int delta) {
      if (delta == 0) {
         throw new IllegalArgumentException("Delta cannot be 0");
      } else {
         return endIndex < startIndex ? -Math.abs(delta) : Math.abs(delta);
      }
   }

   @Override
   public boolean hasNext() {
      return this.delta < 0 ? this.current >= this.endIndex : this.current <= this.endIndex;
   }

   public Integer next() {
      int next = this.current;
      this.current = this.current + this.delta;
      return next;
   }

   public Iterable<Integer> iterable() {
      return () -> this;
   }
}
