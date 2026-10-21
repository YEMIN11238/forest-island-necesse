package necesse.engine.platforms.steam.network.packet;

import com.codedisaster.steamworks.SteamID;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.platforms.steam.network.server.SteamServerSettings;
import necesse.engine.platforms.steam.network.server.network.SteamServerOpenNetwork;

public class PacketAddSteamInvite extends Packet {
   public final SteamID steamID;

   public PacketAddSteamInvite(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.steamID = SteamID.createFromNativeHandle(reader.getNextLong());
   }

   public PacketAddSteamInvite(SteamID steamID) {
      this.steamID = steamID;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextLong(SteamID.getNativeHandle(steamID));
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      SteamServerSettings serverSettings = (SteamServerSettings)server.getSettings();
      if (this.steamID.isValid()
         && serverSettings.steamLobbyType == SteamServerSettings.SteamLobbyType.InviteOnly
         && server.network instanceof SteamServerOpenNetwork) {
         ((SteamServerOpenNetwork)server.network).addInvitedUser(this.steamID);
      }
   }
}
