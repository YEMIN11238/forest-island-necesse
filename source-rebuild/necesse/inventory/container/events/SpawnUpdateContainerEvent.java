package necesse.inventory.container.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.level.gameObject.RespawnObject;
import necesse.level.maps.LevelObject;

public class SpawnUpdateContainerEvent extends ContainerEvent {
   public final int tileX;
   public final int tileY;
   public final boolean isCurrentSpawn;

   public SpawnUpdateContainerEvent(int tileX, int tileY, boolean isCurrentSpawn) {
      this.tileX = tileX;
      this.tileY = tileY;
      this.isCurrentSpawn = isCurrentSpawn;
   }

   public SpawnUpdateContainerEvent(ServerClient client, LevelObject object) {
      this.tileX = object.tileX;
      this.tileY = object.tileY;
      if (object.level.isSamePlace(client.getLevel())) {
         if (object.object instanceof RespawnObject) {
            this.isCurrentSpawn = ((RespawnObject)object.object).isCurrentSpawn(object.level, this.tileX, this.tileY, client);
         } else {
            this.isCurrentSpawn = false;
         }
      } else {
         this.isCurrentSpawn = false;
      }
   }

   public SpawnUpdateContainerEvent(PacketReader reader) {
      super(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.isCurrentSpawn = reader.getNextBoolean();
   }

   @Override
   public void write(PacketWriter writer) {
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextBoolean(this.isCurrentSpawn);
   }
}
