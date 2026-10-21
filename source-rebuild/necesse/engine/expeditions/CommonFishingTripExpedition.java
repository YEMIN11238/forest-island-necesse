package necesse.engine.expeditions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class CommonFishingTripExpedition extends FishingTripExpedition {
   public static List<String> commonFishStringIDs = new ArrayList<>();

   public CommonFishingTripExpedition() {
      commonFishStringIDs.add("carp");
      commonFishStringIDs.add("cod");
      commonFishStringIDs.add("herring");
      commonFishStringIDs.add("mackerel");
      commonFishStringIDs.add("salmon");
      commonFishStringIDs.add("trout");
      commonFishStringIDs.add("tuna");
   }

   @Override
   public GameMessage getUnavailableMessage() {
      return new LocalMessage("expedition", "completestory");
   }

   @Override
   public int getBaseCost(ServerSettlementData settlement) {
      return 300;
   }

   @Override
   public List<InventoryItem> getRewardItems(ServerSettlementData settlement, HumanMob mob) {
      TicketSystemList<InventoryItem> items = new TicketSystemList<>();

      for (String fishStringID : commonFishStringIDs) {
         items.addObject(100, new InventoryItem(fishStringID, Integer.MAX_VALUE));
      }

      int value = GameRandom.globalRandom.getIntBetween(400, 500);
      ArrayList<InventoryItem> out = GameLootUtils.getItemsValuedAt(GameRandom.globalRandom, value, 0.8F, items);
      out.sort(Comparator.comparing(InventoryItem::getBrokerValue).reversed());
      rareHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
      return out;
   }

   @Override
   public List<InventoryItem> getItemIcons() {
      ArrayList<InventoryItem> items = new ArrayList<>();

      for (String fishStringID : commonFishStringIDs) {
         items.add(new InventoryItem(fishStringID));
      }

      return items;
   }
}
