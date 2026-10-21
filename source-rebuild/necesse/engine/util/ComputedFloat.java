package necesse.engine.util;

public class ComputedFloat {
   private ComputedFloat.FloatSupplier supplier;
   private float value;

   public ComputedFloat(ComputedFloat.FloatSupplier supplier) {
      this.supplier = supplier;
   }

   public float get() {
      if (this.supplier == null) {
         return this.value;
      }

      this.value = this.supplier.get();
      this.supplier = null;
      return this.value;
   }

   public interface FloatSupplier {
      float get();
   }
}
