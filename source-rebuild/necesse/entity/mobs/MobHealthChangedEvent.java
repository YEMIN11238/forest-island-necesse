package necesse.entity.mobs;

public class MobHealthChangedEvent extends MobGenericEvent {
   public final int lastHealth;
   public final int currentHealth;
   public final boolean fromUpdatePacket;

   public MobHealthChangedEvent(int lastHealth, int currentHealth, boolean fromUpdatePacket) {
      this.lastHealth = lastHealth;
      this.currentHealth = currentHealth;
      this.fromUpdatePacket = fromUpdatePacket;
   }
}
