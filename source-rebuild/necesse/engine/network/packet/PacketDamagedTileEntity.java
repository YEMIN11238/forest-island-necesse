package necesse.engine.network.packet;

import necesse.engine.GameLog;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.DamagedObjectEntity;
import necesse.level.maps.Level;

public class PacketDamagedTileEntity extends Packet {
   public final int levelIdentifierHashCode;
   public final int tileX;
   public final int tileY;
   public final Packet content;

   public PacketDamagedTileEntity(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.levelIdentifierHashCode = reader.getNextInt();
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.content = reader.getNextContentPacket();
   }

   public PacketDamagedTileEntity(DamagedObjectEntity entity) {
      this.levelIdentifierHashCode = entity.getLevel().getIdentifierHashCode();
      this.tileX = entity.tileX;
      this.tileY = entity.tileY;
      this.content = new Packet();
      entity.setupSpawnPacket(new PacketWriter(this.content));
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.levelIdentifierHashCode);
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextContentPacket(this.content);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (client.getPermissionLevel().getLevel() >= PermissionLevel.ADMIN.getLevel()) {
         if (server.world.settings.cheatsAllowedOrHidden()) {
            Level level = server.world.getLevel(client);
            if (level.getIdentifierHashCode() == this.levelIdentifierHashCode) {
               DamagedObjectEntity ent = level.entityManager.getOrCreateDamagedObjectEntity(this.tileX, this.tileY);
               ent.applySpawnPacket(new PacketReader(this.content));
               server.network.sendToClientsWithEntityExcept(this, ent, client);
            } else {
               GameLog.warn.println(client.getName() + " tried to update damaged tile on wrong level");
            }
         } else {
            GameLog.warn.println(client.getName() + " tried to update damaged tile, but cheats aren't allowed");
         }
      } else {
         GameLog.warn.println(client.getName() + " tried to update damaged tile, but isn't admin");
      }
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.levelManager.checkIfLoadedRegionAtTile(this.levelIdentifierHashCode, this.tileX, this.tileY, true)) {
         DamagedObjectEntity damagedTile = client.getLevel().entityManager.getOrCreateDamagedObjectEntity(this.tileX, this.tileY);
         damagedTile.applySpawnPacket(new PacketReader(this.content));
      }
   }
}
