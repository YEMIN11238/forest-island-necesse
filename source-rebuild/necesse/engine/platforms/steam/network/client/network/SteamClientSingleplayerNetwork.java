package necesse.engine.platforms.steam.network.client.network;

import com.codedisaster.steamworks.SteamID;
import java.util.function.BiConsumer;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.client.network.ClientNetwork;
import necesse.engine.network.server.Server;
import necesse.engine.platforms.steam.SteamData;
import necesse.engine.platforms.steam.network.client.SteamClient;

public class SteamClientSingleplayerNetwork extends ClientNetwork {
   private final SteamClient client;
   private boolean isOpen;

   public SteamClientSingleplayerNetwork(SteamClient client) {
      this.client = client;
   }

   @Override
   public boolean openConnection() {
      this.isOpen = true;
      return true;
   }

   @Override
   public String getOpenError() {
      return null;
   }

   @Override
   public boolean isOpen() {
      return this.isOpen;
   }

   @Override
   public void sendPacket(Packet packet) {
      NetworkPacket networkPacket = new NetworkPacket(packet, null);
      this.client.packetManager.submitOutPacket(networkPacket);
      this.client.submitSinglePlayerPacket(this.client.getLocalServer().packetManager, networkPacket);
   }

   @Override
   public void close() {
      this.isOpen = false;
   }

   @Override
   public String getDebugString() {
      return "LOCAL";
   }

   @Override
   public LocalMessage getPlayingMessage() {
      if (!this.client.isSingleplayer()) {
         Server server = this.client.getLocalServer();
         if (server != null) {
            return new LocalMessage("richpresence", "hosting");
         }
      }

      return new LocalMessage("richpresence", "playingsingleplayer");
   }

   @Override
   public String getRichPresenceGroup() {
      Server server = this.client.getLocalServer();
      if (!this.client.isSingleplayer() && server != null) {
         SteamID steamID = SteamData.getSteamID();
         return steamID == null ? null : steamID.toString();
      } else {
         return super.getRichPresenceGroup();
      }
   }

   @Override
   public void writeLobbyConnectInfo(BiConsumer<String, String> writer) {
      if (!this.client.isSingleplayer() && this.client.getSteamLobbyType() != null) {
         SteamID steamID = SteamData.getSteamID();
         if (steamID != null) {
            writer.accept("serverHostSteamID", String.valueOf(SteamID.getNativeHandle(steamID)));
         }
      }
   }
}
