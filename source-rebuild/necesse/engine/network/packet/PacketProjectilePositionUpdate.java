package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.projectile.Projectile;
import necesse.level.maps.Level;

public class PacketProjectilePositionUpdate extends Packet {
   public final int projectileUniqueID;
   public final Packet positionContent;

   public PacketProjectilePositionUpdate(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.projectileUniqueID = reader.getNextInt();
      this.positionContent = reader.getNextContentPacket();
   }

   public PacketProjectilePositionUpdate(Projectile projectile) {
      if (!projectile.isNetworkCapable()) {
         throw new IllegalArgumentException("Projectile is not network capable and cannot be sent over network");
      }

      this.projectileUniqueID = projectile.getUniqueID();
      this.positionContent = new Packet();
      projectile.setupPositionPacket(new PacketWriter(this.positionContent));
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.projectileUniqueID);
      writer.putNextContentPacket(this.positionContent);
   }

   public Projectile getProjectile(Level level) {
      return level == null ? null : level.entityManager.projectiles.get(this.projectileUniqueID, false);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      Projectile p = this.getProjectile(server.world.getLevel(client));
      if (p != null && p.handlingClient == client) {
         p.applyPositionPacket(new PacketReader(this.positionContent));
         server.network.sendToClientsWithEntityExcept(this, p, client);
      }
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      Projectile p = this.getProjectile(client.getLevel());
      if (p != null) {
         p.applyPositionPacket(new PacketReader(this.positionContent));
      } else {
         client.network.sendPacket(new PacketRequestProjectile(this.projectileUniqueID));
      }
   }
}
