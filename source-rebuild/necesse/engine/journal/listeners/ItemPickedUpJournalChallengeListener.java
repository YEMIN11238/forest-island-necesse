package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.entity.pickup.ItemPickupEntity;

public interface ItemPickedUpJournalChallengeListener {
   void onItemPickedUp(ServerClient var1, ItemPickupEntity var2, int var3, boolean var4);
}
