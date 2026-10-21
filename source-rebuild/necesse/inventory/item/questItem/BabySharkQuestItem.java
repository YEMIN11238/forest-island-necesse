package necesse.inventory.item.questItem;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;

public class BabySharkQuestItem extends QuestFishItem {
   public BabySharkQuestItem() {
      super(new LocalMessage("itemtooltip", "babysharkobtain"));
   }

   @Override
   public FishingLootTable getExtraFishingLoot(ServerClient client, FishingSpot spot) {
      return spot.tile.getHeight() <= -10 && client.playerMob.getInv().getAmount(this, false, false, true, true, "questdrop") <= 0
         ? new FishingLootTable().addWater(400, this.getStringID())
         : super.getExtraFishingLoot(client, spot);
   }
}
