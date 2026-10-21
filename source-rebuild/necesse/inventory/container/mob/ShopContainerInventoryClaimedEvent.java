package necesse.inventory.container.mob;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.container.events.ContainerEvent;

public class ShopContainerInventoryClaimedEvent extends ContainerEvent {
   public int mobUniqueID;

   public ShopContainerInventoryClaimedEvent(HumanMob mob) {
      this.mobUniqueID = mob.getUniqueID();
   }

   public ShopContainerInventoryClaimedEvent(PacketReader reader) {
      super(reader);
   }

   @Override
   public void write(PacketWriter writer) {
   }
}
