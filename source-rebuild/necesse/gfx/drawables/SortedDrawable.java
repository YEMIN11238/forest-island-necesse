package necesse.gfx.drawables;

public abstract class SortedDrawable implements Drawable, Comparable<SortedDrawable> {
   private int priority = this.getPriority();

   public abstract int getPriority();

   public int compareTo(SortedDrawable o) {
      return Integer.compare(this.priority, o.priority);
   }
}
