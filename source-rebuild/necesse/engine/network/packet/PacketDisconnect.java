package necesse.engine.network.packet;

import java.util.function.Function;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PacketDisconnect extends Packet {
   public final int slot;
   public final PacketDisconnect.Code code;
   public final Packet codeContent;

   public PacketDisconnect(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      int slot = reader.getNextByteUnsigned();
      if (slot > 250) {
         slot = (byte)slot;
      }

      this.slot = slot;
      this.code = PacketDisconnect.Code.getCode(reader.getNextByteUnsigned());
      this.codeContent = reader.getNextContentPacket();
   }

   public PacketDisconnect(PacketDisconnect disconnectPacket, int slot) {
      this(slot, disconnectPacket.code, disconnectPacket.codeContent);
   }

   public PacketDisconnect(int slot, PacketDisconnect.Code code) {
      this(slot, code, new Packet());
   }

   public PacketDisconnect(int slot, GameMessage custom) {
      this(slot, PacketDisconnect.Code.CUSTOM, custom.getContentPacket());
   }

   private PacketDisconnect(int slot, PacketDisconnect.Code code, Packet codeContent) {
      this.slot = slot;
      this.code = code;
      this.codeContent = codeContent;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextByteUnsigned(slot);
      writer.putNextByteUnsigned(code.getID());
      writer.putNextContentPacket(codeContent);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (this.slot != client.slot) {
         System.out
            .println(
               "Player "
                  + client.authentication
                  + " (\""
                  + client.getName()
                  + "\", slot "
                  + client.slot
                  + ") tried to disconnect wrong client slot: "
                  + this.slot
            );
      } else {
         if (this.code != PacketDisconnect.Code.CLIENT_DISCONNECT) {
            System.out
               .println(
                  "Player " + client.authentication + " (\"" + client.getName() + "\", slot " + client.slot + ") tried to disconnect wrong code: " + this.code
               );
         } else {
            System.out
               .println(
                  "Player "
                     + client.authentication
                     + " (\""
                     + client.getName()
                     + "\") disconnected with message: "
                     + this.code.getErrorMessage(this.codeContent)
               );
            server.disconnectClient(client, this);
         }
      }
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (this.slot != client.getSlot() && this.slot != -2 && this.slot != -1) {
         if (client.getClient(this.slot) != null) {
            client.chat
               .addMessage(
                  Localization.translate(
                     "disconnect", "chatmsg", "name", client.getClient(this.slot).getName(), "msg", this.code.getErrorMessage(this.codeContent)
                  )
               );
            if (client.getPlayer(this.slot) != null && client.getPlayer(this.slot).isRiding()) {
               client.getPlayer(this.slot).dismount();
            }
         }

         client.clearClient(this.slot);
         client.loading.playersPhase.submitLoadedPlayer(this.slot);
      } else {
         if (this.code == PacketDisconnect.Code.WRONG_PASSWORD) {
            long worldUniqueID = new PacketReader(this.codeContent).getNextLong();
            client.loading.connectingPhase.submitWrongPassword(worldUniqueID);
         }

         client.error(this.code.getErrorMessage(this.codeContent), false);
      }
   }

   public static PacketDisconnect wrongPassword(Server server) {
      Packet content = new Packet();
      new PacketWriter(content).putNextLong(server.world.getUniqueID());
      return new PacketDisconnect(-1, PacketDisconnect.Code.WRONG_PASSWORD, content);
   }

   public static PacketDisconnect kickPacket(int slot, String message) {
      Packet content = new Packet();
      new PacketWriter(content).putNextString(message);
      return new PacketDisconnect(slot, PacketDisconnect.Code.KICK, content);
   }

   public static PacketDisconnect networkError(int slot, String message) {
      Packet content = new Packet();
      new PacketWriter(content).putNextString(message);
      return new PacketDisconnect(slot, PacketDisconnect.Code.NETWORK_ERROR, content);
   }

   public static PacketDisconnect clientDisconnect(int slot, String message) {
      Packet content = new Packet();
      new PacketWriter(content).putNextString(message);
      return new PacketDisconnect(slot, PacketDisconnect.Code.CLIENT_DISCONNECT, content);
   }

   public enum Code {
      NULL(p -> Localization.translate("disconnect", "unknown")),
      INTERNAL_ERROR(p -> Localization.translate("disconnect", "internal")),
      KICK(p -> {
         String reason = new PacketReader(p).getNextString();
         if (reason.length() == 0) {
            reason = Localization.translate("disconnect", "noreason");
         }

         return Localization.translate("disconnect", "kicked", "reason", reason);
      }),
      CLIENT_NOT_RESPONDING(p -> Localization.translate("disconnect", "clientresponding")),
      SERVER_STOPPED(p -> Localization.translate("disconnect", "serverstopped")),
      SERVER_ERROR(p -> Localization.translate("disconnect", "servererror")),
      CLIENT_ERROR(p -> Localization.translate("disconnect", "clienterror")),
      WRONG_PASSWORD(p -> Localization.translate("disconnect", "wrongpassword")),
      MISSING_CLIENT(p -> Localization.translate("disconnect", "missingclient")),
      BANNED_CLIENT(p -> Localization.translate("disconnect", "banned")),
      WRONG_VERSION(p -> Localization.translate("disconnect", "wrongversion")),
      ALREADY_PLAYING(p -> Localization.translate("disconnect", "alreadyplaying")),
      SERVER_FULL(p -> Localization.translate("disconnect", "serverfull")),
      CLIENT_DISCONNECT(p -> {
         String msg = new PacketReader(p).getNextString();
         if (msg.length() == 0) {
            msg = Localization.translate("disconnect", "nomessage");
         }

         return msg;
      }),
      MISSING_APPEARANCE(p -> Localization.translate("disconnect", "missingappearance")),
      NETWORK_ERROR(p -> {
         String m = new PacketReader(p).getNextString();
         String o = "";
         if (m.length() == 0) {
            o = Localization.translate("disconnect", "networkerror");
         } else {
            o = Localization.translate("disconnect", "networkerrormsg", "msg", m);
         }

         return o;
      }),
      STATE_DESYNC(p -> Localization.translate("disconnect", "statedesync")),
      INVITE_ONLY(p -> Localization.translate("disconnect", "inviteonly")),
      FRIENDS_ONLY(p -> Localization.translate("disconnect", "friendsonly")),
      CUSTOM(p -> GameMessage.fromContentPacket(p).translate());

      public Function<Packet, String> errorMessage;

      Code(Function<Packet, String> errorMessage) {
         this.errorMessage = errorMessage;
      }

      public String getErrorMessage(Packet packet) {
         return this.errorMessage.apply(packet);
      }

      public int getID() {
         return this.ordinal();
      }

      public static PacketDisconnect.Code getCode(int id) {
         PacketDisconnect.Code[] codes = values();
         return id >= 0 && id < codes.length ? codes[id] : NULL;
      }
   }
}
