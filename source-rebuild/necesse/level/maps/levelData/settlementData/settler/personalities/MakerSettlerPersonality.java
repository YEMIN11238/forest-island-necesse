package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleScalingHappinessSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class MakerSettlerPersonality extends SettlerPersonality {
   public int uniqueCraftingStationCount;

   public MakerSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_none", new SimpleSettlerThought("personalities", "nomakerstations", 0).setPersonalitySubmitter(this)
      );
      LocalMessage someStationsMessage = new LocalMessage("personalities", "somemakerstations");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_some", new SimpleScalingHappinessSettlerThought(someStationsMessage, 8, -2, 2).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      super.onRoomStatsCalculated(bed, settlementRoom);
      this.uniqueCraftingStationCount = 0;
      if (settlementRoom != null) {
         this.uniqueCraftingStationCount = (int)settlementRoom.streamObjectsInRoom(true, true).mapToInt(o -> {
            ObjectItem item = o.getObjectItem();
            if (ItemRegistry.isObtainable(item.getID())) {
               for (ItemCategory category = ItemCategory.getItemsCategory(item); category != null; category = category.parent) {
                  if (category.stringID.equals("craftingstations")) {
                     return item.getID();
                  }
               }
            }

            return -1;
         }).filter(id -> id > 0).distinct().count();
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.uniqueCraftingStationCount == 0) {
         list.addThought(this.getStringID() + "_none");
      } else {
         list.addThought(this.getStringID() + "_some", this.uniqueCraftingStationCount);
      }

      return list;
   }
}
