package necesse.engine.network.client.loading;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketPlayerStats;
import necesse.engine.network.packet.PacketRequestPacket;

public class ClientLoadingPlayerStats extends ClientLoadingAutoPhase {
   public ClientLoadingPlayerStats(ClientLoading loading) {
      super(loading, false);
   }

   public void submitStatsPacket(PacketPlayerStats packet) {
      if (this.client.characterStats != null && this.client.storyManager != null) {
         this.markDone();
      }
   }

   @Override
   public GameMessage getLoadingMessage() {
      return new LocalMessage("loading", "connectstats");
   }

   @Override
   public void tick() {
      if (!this.isWaiting()) {
         this.client.network.sendPacket(new PacketRequestPacket(PacketRequestPacket.RequestType.PLAYER_STATS));
         this.setWait(200);
      }
   }

   @Override
   public void end() {
   }
}
