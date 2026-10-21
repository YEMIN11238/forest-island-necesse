package necesse.engine.journal;

import necesse.engine.journal.listeners.ItemPickedUpJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.entity.pickup.ItemPickupEntity;

public class PickupItemJournalChallenge extends SimpleJournalChallenge implements ItemPickedUpJournalChallengeListener {
   protected String[] itemStringIDs;
   protected boolean onlyByNonPlayerDropped;

   public PickupItemJournalChallenge(boolean onlyByNonPlayerDropped, String... itemStringIDs) {
      this.onlyByNonPlayerDropped = onlyByNonPlayerDropped;
      this.itemStringIDs = itemStringIDs;
   }

   @Override
   public void onItemPickedUp(ServerClient serverClient, ItemPickupEntity entity, int amount, boolean addedToNonPlayerInventory) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         if (!this.onlyByNonPlayerDropped || !entity.droppedByPlayer) {
            for (String itemStringID : this.itemStringIDs) {
               if (entity.item.item.getStringID().equals(itemStringID)) {
                  this.markCompleted(serverClient);
                  serverClient.forceCombineNewStats();
                  return;
               }
            }
         }
      }
   }
}
