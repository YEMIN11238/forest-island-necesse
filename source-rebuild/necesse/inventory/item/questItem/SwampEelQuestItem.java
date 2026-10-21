package necesse.inventory.item.questItem;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;
import necesse.level.maps.biomes.swamp.SwampBiome;

public class SwampEelQuestItem extends QuestFishItem {
   public SwampEelQuestItem() {
      super(new LocalMessage("itemtooltip", "swampeelobtain"));
   }

   @Override
   public FishingLootTable getExtraFishingLoot(ServerClient client, FishingSpot spot) {
      return spot.getBiome() instanceof SwampBiome
            && spot.tile.level.isCave
            && client.playerMob.getInv().getAmount(this, false, false, true, true, "questdrop") <= 0
         ? new FishingLootTable().addWater(400, this.getStringID())
         : super.getExtraFishingLoot(client, spot);
   }
}
