package necesse.entity.events;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.entity.Entity;

public abstract class EntityEvent {
   private EntityEventRegistry<?> registry;
   private int id = -1;

   protected void onRegister(EntityEventRegistry<?> registry, int id) {
      if (this.registry != null) {
         throw new IllegalStateException("Cannot register same event twice");
      }

      this.registry = registry;
      this.id = id;
   }

   public int getID() {
      return this.id;
   }

   public Entity getEntity(Class<? extends Entity> entityClass) {
      return this.registry != null && entityClass.isInstance(this.registry.entity) ? entityClass.cast(this.registry.entity) : null;
   }

   public abstract void executePacket(PacketReader var1);

   protected void runAndSendAbility(Packet content) {
      if (this.registry != null) {
         this.registry.runAndSendAbility(this, content);
      } else {
         System.err.println("Cannot run event that hasn't been registered");
      }
   }
}
