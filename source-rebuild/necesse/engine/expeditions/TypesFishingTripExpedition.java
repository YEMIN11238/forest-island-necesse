package necesse.engine.expeditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class TypesFishingTripExpedition extends FishingTripExpedition {
   public String majorStoryObjectiveStringID;
   public int baseCost;
   public int valueGottenMin;
   public int valueGottenMax;
   public ArrayList<TypesFishingTripExpedition.FishConfig> fish;

   public TypesFishingTripExpedition(
      String majorStoryObjectiveStringID, int baseCost, int valueGottenMin, int valueGottenMax, List<TypesFishingTripExpedition.FishConfig> fish
   ) {
      this.majorStoryObjectiveStringID = majorStoryObjectiveStringID;
      this.baseCost = baseCost;
      this.valueGottenMin = valueGottenMin;
      this.valueGottenMax = valueGottenMax;
      this.fish = new ArrayList<>(fish);
   }

   public TypesFishingTripExpedition(
      String majorStoryObjectiveStringID, int baseCost, int valueGottenMin, int valueGottenMax, TypesFishingTripExpedition.FishConfig... fish
   ) {
      this(majorStoryObjectiveStringID, baseCost, valueGottenMin, valueGottenMax, Arrays.asList(fish));
   }

   public TypesFishingTripExpedition(
      String majorStoryObjectiveStringID, int baseCost, int valueGottenMin, int valueGottenMax, String[] primaryFishStringIDs, String... secondaryFishStringIDs
   ) {
      this(
         majorStoryObjectiveStringID,
         baseCost,
         valueGottenMin,
         valueGottenMax,
         concat(stringIDsToConfigs(400, primaryFishStringIDs), stringIDsToConfigs(100, secondaryFishStringIDs))
      );
   }

   public TypesFishingTripExpedition(
      String majorStoryObjectiveStringID, int baseCost, int valueGottenMin, int valueGottenMax, String primaryFishStringID, String... secondaryFishStringIDs
   ) {
      this(
         majorStoryObjectiveStringID,
         baseCost,
         valueGottenMin,
         valueGottenMax,
         concat(stringIDsToConfigs(400, primaryFishStringID), stringIDsToConfigs(100, secondaryFishStringIDs))
      );
   }

   private static TypesFishingTripExpedition.FishConfig[] stringIDsToConfigs(int tickets, String... stringIDs) {
      TypesFishingTripExpedition.FishConfig[] out = new TypesFishingTripExpedition.FishConfig[stringIDs.length];

      for (int i = 0; i < stringIDs.length; i++) {
         out[i] = new TypesFishingTripExpedition.FishConfig(stringIDs[i], tickets);
      }

      return out;
   }

   private static TypesFishingTripExpedition.FishConfig[] concat(TypesFishingTripExpedition.FishConfig[]... configArrays) {
      int totalSize = 0;

      for (TypesFishingTripExpedition.FishConfig[] configs : configArrays) {
         totalSize += configs.length;
      }

      TypesFishingTripExpedition.FishConfig[] out = new TypesFishingTripExpedition.FishConfig[totalSize];
      int currentIndex = 0;

      for (TypesFishingTripExpedition.FishConfig[] configArray : configArrays) {
         for (TypesFishingTripExpedition.FishConfig fishConfig : configArray) {
            out[currentIndex++] = fishConfig;
         }
      }

      return out;
   }

   @Override
   public void initDisplayName() {
      if (!this.fish.isEmpty()) {
         this.displayName = ItemRegistry.getItem(this.fish.get(0).itemStringID).getNewLocalization();
      } else {
         super.initDisplayName();
      }
   }

   @Override
   public GameMessage getUnavailableMessage() {
      return new LocalMessage("expedition", "completestory");
   }

   @Override
   public int getBaseCost(ServerSettlementData settlement) {
      return this.baseCost;
   }

   @Override
   public List<InventoryItem> getRewardItems(ServerSettlementData settlement, HumanMob mob) {
      TicketSystemList<InventoryItem> items = new TicketSystemList<>();

      for (TypesFishingTripExpedition.FishConfig fish : this.fish) {
         items.addObject(fish.tickets, new InventoryItem(fish.itemStringID, fish.maxAmount));
      }

      int value = GameRandom.globalRandom.getIntBetween(this.valueGottenMin, this.valueGottenMax);
      ArrayList<InventoryItem> out = GameLootUtils.getItemsValuedAt(GameRandom.globalRandom, value, 0.8F, items);
      out.sort(Comparator.comparing(InventoryItem::getBrokerValue).reversed());
      rareHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
      return out;
   }

   @Override
   public float getSuccessChance(ServerSettlementData settlement) {
      return this.majorStoryObjectiveStringID == null ? 1.0F : storyProgressSuccessChance(settlement, this.majorStoryObjectiveStringID, 2);
   }

   @Override
   public List<InventoryItem> getItemIcons() {
      ArrayList<InventoryItem> items = new ArrayList<>();
      Iterator var2 = this.fish.iterator();
      if (var2.hasNext()) {
         TypesFishingTripExpedition.FishConfig fishConfig = (TypesFishingTripExpedition.FishConfig)var2.next();
         items.add(new InventoryItem(fishConfig.itemStringID));
      }

      return items;
   }

   public static class FishConfig {
      public String itemStringID;
      public float chance;
      public int tickets;
      public int maxAmount;

      public FishConfig(String itemStringID, float chance, int tickets, int maxAmount) {
         this.itemStringID = itemStringID;
         this.chance = chance;
         this.tickets = tickets;
         this.maxAmount = maxAmount;
      }

      public FishConfig(String itemStringID, float chance, int tickets) {
         this(itemStringID, chance, tickets, Integer.MAX_VALUE);
      }

      public FishConfig(String itemStringID, int tickets) {
         this(itemStringID, 1.0F, tickets);
      }
   }
}
