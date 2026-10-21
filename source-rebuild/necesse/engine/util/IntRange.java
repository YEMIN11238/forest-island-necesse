package necesse.engine.util;

public class IntRange {
   public int min;
   public int max;

   public IntRange(int min, int max) {
      this.min = min;
      this.max = max;
   }

   public boolean isValueInRange(int value) {
      return value >= this.min && value <= this.max;
   }

   public int getRandomValueInRange(GameRandom random) {
      return this.min == this.max ? this.min : random.getIntBetween(this.min, this.max);
   }

   public int limitValueInRange(int value) {
      return GameMath.limit(value, this.min, this.max);
   }
}
