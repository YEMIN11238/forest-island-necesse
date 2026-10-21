package necesse.entity.pickup;

import java.util.LinkedList;
import java.util.function.Consumer;
import necesse.engine.util.GameLinkedList;
import necesse.engine.world.WorldEntity;
import necesse.entity.Entity;
import necesse.inventory.InventoryItem;

public class ItemPickupReservedAmount {
   public final ItemPickupEntity entity;
   private GameLinkedList<ItemPickupReservedAmount>.Element element;
   public final int pickupAmount;
   protected int prevAmountReserved;
   protected long reserveTick;
   protected LinkedList<Consumer<ItemPickupReservedCombinedEvent>> pickedUpListeners = new LinkedList<>();

   protected ItemPickupReservedAmount(ItemPickupEntity entity, int pickupAmount, int prevAmountReserved) {
      this.entity = entity;
      this.pickupAmount = pickupAmount;
      this.prevAmountReserved = prevAmountReserved;
   }

   protected void init(GameLinkedList<ItemPickupReservedAmount>.Element element, WorldEntity worldEntity) {
      if (this.element != null) {
         throw new IllegalStateException("Storage slot already initialized");
      }

      this.element = element;
      this.reserve(worldEntity);
   }

   public void submitCombinedEvent(ItemPickupReservedCombinedEvent event) {
      for (Consumer<ItemPickupReservedCombinedEvent> listener : this.pickedUpListeners) {
         listener.accept(event);
      }
   }

   public void onItemCombined(Consumer<ItemPickupReservedCombinedEvent> listener) {
      this.pickedUpListeners.add(listener);
   }

   public void reserve(WorldEntity worldEntity) {
      this.reserveTick = worldEntity.getGameTicks();
   }

   public void reserve(Entity entity) {
      this.reserve(entity.getWorldEntity());
   }

   public boolean isReserved(WorldEntity worldEntity) {
      return this.reserveTick >= worldEntity.getGameTicks() - 2L;
   }

   public void remove() {
      for (GameLinkedList<ItemPickupReservedAmount>.Element next = this.element;
         next.hasPrev();
         ((ItemPickupReservedAmount)next.object).prevAmountReserved = ((ItemPickupReservedAmount)next.object).prevAmountReserved - this.pickupAmount
      ) {
         next = next.prev();
      }

      this.element.remove();
   }

   public boolean isValid() {
      if (this.element.isRemoved()) {
         return false;
      } else if (this.entity.removed()) {
         return false;
      } else if (this.entity.item.getAmount() < this.pickupAmount) {
         this.remove();
         return false;
      } else {
         return true;
      }
   }

   public InventoryItem pickupItem() {
      if (this.element.isRemoved()) {
         return null;
      }

      if (this.entity.removed()) {
         this.remove();
         return null;
      }

      InventoryItem pickedUp = this.entity.item.copy(Math.min(this.entity.item.getAmount(), this.pickupAmount));
      this.entity.item.setAmount(this.entity.item.getAmount() - pickedUp.getAmount());
      if (this.entity.item.getAmount() <= 0) {
         this.entity.remove();
      } else {
         this.entity.onItemUpdated();
         this.entity.markDirty();
      }

      this.remove();
      return pickedUp;
   }

   public boolean isRemoved() {
      return this.element.isRemoved();
   }
}
