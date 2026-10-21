package necesse.engine.network.packet;

import necesse.engine.commands.PermissionLevel;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PacketCreativeWorldSettings extends PacketCreativeCheck {
   public PacketCreativeWorldSettings(byte[] data) {
      super(data);
   }

   public PacketCreativeWorldSettings(Client client) {
      client.worldSettings.setupCreativePacket(new PacketWriter(this));
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (checkCreativeAndSendUpdate(server, client)) {
         if (client.getPermissionLevel().getLevel() < PermissionLevel.CREATIVESETTINGS.getLevel()) {
            client.sendChatMessage(new LocalMessage("ui", "creativeyoudonothavepermission"));
            client.sendPacket(new PacketSettings(server.world.settings));
            client.sendPacket(new PacketPermissionUpdate(client.getPermissionLevel()));
         } else {
            server.world.settings.applyCreativePacket(new PacketReader(this), client);
            server.world.settings.saveSettings();
            server.world.settings.sendSettingsPacket();
         }
      }
   }
}
