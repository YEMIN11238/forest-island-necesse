package necesse.inventory.container.events;

public abstract class ContainerEventSubscription<T extends ContainerEvent> {
   public final Class<T> eventClass;

   public ContainerEventSubscription(Class<T> eventClass) {
      this.eventClass = eventClass;
   }

   public abstract boolean shouldReceiveEvent(T var1);

   public final boolean testUntypedEvent(ContainerEvent event) {
      return this.eventClass == event.getClass() && this.shouldReceiveEvent((T)event);
   }

   public abstract boolean isActive();
}
