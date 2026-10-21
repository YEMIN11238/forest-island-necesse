package necesse.inventory.container;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.packet.PacketAdventurePartyUpdate;
import necesse.engine.network.server.AdventureParty;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.container.events.AdventurePartyChangedEvent;
import necesse.inventory.container.mobCommands.MobCommandsHandler;
import necesse.inventory.container.slots.PartyInventoryContainerSlot;

public class AdventurePartyConfigContainer extends Container {
   public int PARTY_SLOTS_START = -1;
   public int PARTY_SLOTS_END = -1;
   public final MobCommandsHandler commandsHandler;

   public AdventurePartyConfigContainer(NetworkClient client, int uniqueSeed, Packet content) {
      super(client, uniqueSeed);
      AdventureParty party = null;
      if (client.isServer()) {
         party = client.getServerClient().adventureParty;
      } else if (client.isClient() && client.getClientClient().isLocalClient()) {
         party = client.getClientClient().getClient().adventureParty;
      }

      for (int i = 0; i < PlayerInventoryManager.MAX_PARTY_INVENTORY_SIZE; i++) {
         int index = this.addSlot(new PartyInventoryContainerSlot(client.playerMob.getInv().party, i, party));
         if (this.PARTY_SLOTS_START == -1) {
            this.PARTY_SLOTS_START = index;
         }

         if (this.PARTY_SLOTS_END == -1) {
            this.PARTY_SLOTS_END = index;
         }

         this.PARTY_SLOTS_START = Math.min(this.PARTY_SLOTS_START, index);
         this.PARTY_SLOTS_END = Math.max(this.PARTY_SLOTS_END, index);
      }

      this.addInventoryQuickTransfer(this.PARTY_SLOTS_START, this.PARTY_SLOTS_END);
      this.commandsHandler = new MobCommandsHandler(this) {
         @Override
         public void sendClientOutOfSyncPacket(ServerClient client) {
            client.sendPacket(new PacketAdventurePartyUpdate(client));
            new AdventurePartyChangedEvent().applyAndSendToClient(client);
         }
      };
   }
}
