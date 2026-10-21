package necesse.inventory.container;

public class ContainerTransferResult {
   public int amount;
   public String error;

   public ContainerTransferResult(int amount, String error) {
      this.amount = amount;
      this.error = error;
   }
}
