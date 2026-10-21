package necesse.entity.events;

import necesse.engine.network.Packet;
import necesse.engine.network.packet.PacketObjectEntityEvent;
import necesse.engine.network.server.Server;
import necesse.entity.objectEntity.ObjectEntity;

public class ObjectEntityEventRegistry extends EntityEventRegistry<ObjectEntity> {
   public ObjectEntityEventRegistry(ObjectEntity objectEntity) {
      super(objectEntity);
   }

   @Override
   public String getDebugIdentifierString() {
      return null;
   }

   @Override
   public void sendEventPacket(Server server, EntityEvent event, Packet content) {
      server.network
         .sendToClientsWithTile(new PacketObjectEntityEvent(this.entity, event.getID(), content), this.entity.getLevel(), this.entity.tileX, this.entity.tileY);
   }
}
