package necesse.engine.network.client.loading;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketRequestPlayerData;

public class ClientLoadingClient extends ClientLoadingAutoPhase {
   private int oldSelectedInventory;

   public ClientLoadingClient(ClientLoading loading) {
      super(loading, true);
   }

   @Override
   public GameMessage getLoadingMessage() {
      return new LocalMessage("loading", "connectclient");
   }

   @Override
   public void tick() {
      ClientClient me = this.client.getClient();
      if (me != null && me.playerMob != null && me.loadedPlayer) {
         this.markDone();
      } else {
         if (this.isWaiting()) {
            return;
         }

         this.client.network.sendPacket(new PacketRequestPlayerData(this.client.getSlot()));
         this.setWait(200);
      }
   }

   @Override
   public void end() {
      this.client.getPlayer().setSelectedSlot(this.oldSelectedInventory);
      this.client.initInventoryContainer();
   }

   @Override
   public void reset() {
      super.reset();
      if (this.client.getPlayer() != null) {
         this.oldSelectedInventory = this.client.getPlayer().getSelectedSlot();
      } else {
         this.oldSelectedInventory = 0;
      }
   }
}
