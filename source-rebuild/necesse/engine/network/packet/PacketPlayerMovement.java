package necesse.engine.network.packet;

import necesse.engine.GameLog;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.PlayerMob;

public class PacketPlayerMovement extends Packet {
   public final int levelIdentifierHashCode;
   public final int slot;
   public final boolean hasSpawned;
   public final boolean isDirect;
   public final byte teleportBuffer;
   public final float x;
   public final float y;
   public final float dx;
   public final float dy;
   private final PacketReader reader;

   public PacketPlayerMovement(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.levelIdentifierHashCode = reader.getNextInt();
      this.slot = reader.getNextByteUnsigned();
      this.hasSpawned = reader.getNextBoolean();
      this.isDirect = reader.getNextBoolean();
      this.teleportBuffer = reader.getNextByte();
      this.x = reader.getNextFloat();
      this.y = reader.getNextFloat();
      this.dx = reader.getNextFloat();
      this.dy = reader.getNextFloat();
      this.reader = reader;
   }

   public PacketPlayerMovement(ServerClient client, boolean isDirect) {
      this(client.getLevelIdentifier(), client.slot, client.hasSpawned(), client.playerMob, isDirect);
   }

   public PacketPlayerMovement(Client client, ClientClient self, boolean isDirect) {
      this(client.getLevel().getIdentifier(), client.getSlot(), self.hasSpawned(), self.playerMob, isDirect);
   }

   private PacketPlayerMovement(LevelIdentifier levelIdentifier, int slot, boolean hasSpawned, PlayerMob player, boolean isDirect) {
      this.levelIdentifierHashCode = levelIdentifier.hashCode();
      this.slot = slot;
      this.hasSpawned = hasSpawned;
      this.isDirect = isDirect;
      this.teleportBuffer = player.getMovementTeleportBuffer();
      this.x = player.x;
      this.y = player.y;
      this.dx = player.dx;
      this.dy = player.dy;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.levelIdentifierHashCode);
      writer.putNextByteUnsigned(slot);
      writer.putNextBoolean(hasSpawned);
      writer.putNextBoolean(isDirect);
      writer.putNextByte(this.teleportBuffer);
      writer.putNextFloat(this.x);
      writer.putNextFloat(this.y);
      writer.putNextFloat(this.dx);
      writer.putNextFloat(this.dy);
      this.reader = new PacketReader(writer);
      player.setupPlayerMovementPacket(writer);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (this.slot == client.slot && client.checkHasRequestedSelf() && !client.isDead()) {
         client.checkSpawned();
         if (client.hasSpawned()) {
            if (client.playerMob.getMovementTeleportBuffer() != this.teleportBuffer) {
               GameLog.warn.println(client.getName() + " sent invalid movement packet after teleporting. Sending update...");
               client.sendPacket(new PacketPlayerGeneral(client));
            } else {
               double allowed = client.playerMob.allowServerMovement(server, client, this.x, this.y, this.dx, this.dy);
               if (allowed <= 0.0) {
                  client.playerMob.applyPlayerMovementPacket(this, new PacketReader(this.reader));
                  server.network.sendToClientsWithEntityExcept(new PacketPlayerMovement(client, this.isDirect), client.playerMob, client);
               } else {
                  GameLog.warn.println(client.getName() + " moved wrongly, snapping back " + allowed);
                  server.network.sendToClientsWithEntity(new PacketPlayerMovement(client, false), client.playerMob);
               }

               if (client.playerMob.moveX != 0.0F || client.playerMob.moveY != 0.0F) {
                  client.refreshAFKTimer();
               }
            }
         }
      }
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      ClientClient target = client.getClient(this.slot);
      if (target == null) {
         client.network.sendPacket(new PacketRequestPlayerData(this.slot));
      } else if (target.loadedPlayer) {
         if (this.hasSpawned && !target.hasSpawned()) {
            target.applySpawned(0);
         }

         target.playerMob.applyPlayerMovementPacket(this, new PacketReader(this.reader));
         if (this.slot == client.getSlot()) {
            client.resetPositionPointUpdate();
         }
      }
   }
}
