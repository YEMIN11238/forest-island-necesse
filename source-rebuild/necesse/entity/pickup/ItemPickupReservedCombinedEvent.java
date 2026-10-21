package necesse.entity.pickup;

public class ItemPickupReservedCombinedEvent {
   public final ItemPickupReservedAmount reserved;
   public final ItemPickupEntity next;
   public final int combinedAmount;

   public ItemPickupReservedCombinedEvent(ItemPickupReservedAmount reserved, ItemPickupEntity next, int combinedAmount) {
      this.reserved = reserved;
      this.next = next;
      this.combinedAmount = combinedAmount;
   }
}
