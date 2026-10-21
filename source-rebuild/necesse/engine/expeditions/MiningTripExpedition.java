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

public class MiningTripExpedition extends SettlerExpedition {
   public String majorStoryObjectiveStringID;
   public boolean isDeepCave;
   public int baseCost;
   public int valueGottenMin;
   public int valueGottenMax;
   public String[] stoneStringIDs;
   public ArrayList<MiningTripExpedition.OreConfig> ores;

   public MiningTripExpedition(
      String majorStoryObjectiveStringID,
      boolean isDeepCave,
      int baseCost,
      int valueGottenMin,
      int valueGottenMax,
      String[] stoneStringIDs,
      List<MiningTripExpedition.OreConfig> ores
   ) {
      this.majorStoryObjectiveStringID = majorStoryObjectiveStringID;
      this.isDeepCave = isDeepCave;
      this.baseCost = baseCost;
      this.valueGottenMin = valueGottenMin;
      this.valueGottenMax = valueGottenMax;
      this.stoneStringIDs = stoneStringIDs;
      this.ores = new ArrayList<>(ores);
   }

   public MiningTripExpedition(
      String majorStoryObjectiveStringID,
      boolean isDeepCave,
      int baseCost,
      int valueGottenMin,
      int valueGottenMax,
      String[] stoneStringIDs,
      MiningTripExpedition.OreConfig... ores
   ) {
      this(majorStoryObjectiveStringID, isDeepCave, baseCost, valueGottenMin, valueGottenMax, stoneStringIDs, Arrays.asList(ores));
   }

   public MiningTripExpedition(
      String majorStoryObjectiveStringID,
      boolean isDeepCave,
      int baseCost,
      int valueGottenMin,
      int valueGottenMax,
      String stoneStringID,
      MiningTripExpedition.OreConfig... ores
   ) {
      this(
         majorStoryObjectiveStringID,
         isDeepCave,
         baseCost,
         valueGottenMin,
         valueGottenMax,
         stoneStringID == null ? null : new String[]{stoneStringID},
         Arrays.asList(ores)
      );
   }

   public MiningTripExpedition(
      String majorStoryObjectiveStringID,
      boolean isDeepCave,
      int baseCost,
      int valueGottenMin,
      int valueGottenMax,
      String stoneStringID,
      String primaryOreStringID,
      String... secondaryOreStringIDs
   ) {
      this(
         majorStoryObjectiveStringID,
         isDeepCave,
         baseCost,
         valueGottenMin,
         valueGottenMax,
         stoneStringID == null ? null : new String[]{stoneStringID},
         concat(stringIDsToConfigs(400, primaryOreStringID), stringIDsToConfigs(100, secondaryOreStringIDs))
      );
   }

   private static MiningTripExpedition.OreConfig[] stringIDsToConfigs(int tickets, String... stringIDs) {
      MiningTripExpedition.OreConfig[] out = new MiningTripExpedition.OreConfig[stringIDs.length];

      for (int i = 0; i < stringIDs.length; i++) {
         out[i] = new MiningTripExpedition.OreConfig(stringIDs[i], tickets);
      }

      return out;
   }

   private static MiningTripExpedition.OreConfig[] concat(MiningTripExpedition.OreConfig[]... configArrays) {
      int totalSize = 0;

      for (MiningTripExpedition.OreConfig[] configs : configArrays) {
         totalSize += configs.length;
      }

      MiningTripExpedition.OreConfig[] out = new MiningTripExpedition.OreConfig[totalSize];
      int currentIndex = 0;

      for (MiningTripExpedition.OreConfig[] configArray : configArrays) {
         for (MiningTripExpedition.OreConfig oreConfig : configArray) {
            out[currentIndex++] = oreConfig;
         }
      }

      return out;
   }

   @Override
   public void initDisplayName() {
      if (!this.ores.isEmpty()) {
         this.displayName = ItemRegistry.getItem(this.ores.get(0).itemStringID).getNewLocalization();
      } else {
         super.initDisplayName();
      }
   }

   @Override
   public float getSuccessChance(ServerSettlementData settlement) {
      if (this.majorStoryObjectiveStringID == null) {
         return 1.0F;
      } else {
         return storyProgressSuccessChance(settlement, this.majorStoryObjectiveStringID) > 0.5 ? 1.0F : 0.0F;
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
      TicketSystemList<InventoryItem> oreItems = new TicketSystemList<>();

      for (MiningTripExpedition.OreConfig ore : this.ores) {
         if (GameRandom.globalRandom.getChance(ore.chance)) {
            oreItems.addObject(ore.tickets, new InventoryItem(ore.itemStringID, ore.maxAmount));
         }
      }

      int value = GameRandom.globalRandom.getIntBetween(this.valueGottenMin, this.valueGottenMax);
      ArrayList<InventoryItem> out = GameLootUtils.getItemsValuedAt(GameRandom.globalRandom, value, 0.8F, oreItems);
      out.sort(Comparator.comparing(InventoryItem::getBrokerValue).reversed());
      if (this.stoneStringIDs != null) {
         String stoneStringID = GameRandom.globalRandom.getOneOf(this.stoneStringIDs);
         if (stoneStringID != null) {
            int totalOres = out.stream().mapToInt(InventoryItem::getAmount).sum();
            int stoneAmount = (int)(totalOres * GameRandom.globalRandom.getFloatBetween(2.0F, 3.5F));
            out.add(new InventoryItem(stoneStringID, stoneAmount));
         }
      }

      if (this.isDeepCave) {
         epicHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
      } else {
         rareHappinessObjects.addItems(out, GameRandom.globalRandom, 1.0F);
      }

      return out;
   }

   @Override
   public List<InventoryItem> getItemIcons() {
      ArrayList<InventoryItem> items = new ArrayList<>();
      if (this.ores.isEmpty()) {
         if (this.stoneStringIDs != null) {
            for (String stoneStringID : this.stoneStringIDs) {
               items.add(new InventoryItem(stoneStringID));
            }
         }
      } else {
         Iterator var6 = this.ores.iterator();
         if (var6.hasNext()) {
            MiningTripExpedition.OreConfig ore = (MiningTripExpedition.OreConfig)var6.next();
            items.add(new InventoryItem(ore.itemStringID));
         }
      }

      return items;
   }

   public static class OreConfig {
      public String itemStringID;
      public float chance;
      public int tickets;
      public int maxAmount;

      public OreConfig(String itemStringID, float chance, int tickets, int maxAmount) {
         this.itemStringID = itemStringID;
         this.chance = chance;
         this.tickets = tickets;
         this.maxAmount = maxAmount;
      }

      public OreConfig(String itemStringID, float chance, int tickets) {
         this(itemStringID, chance, tickets, Integer.MAX_VALUE);
      }

      public OreConfig(String itemStringID, int tickets) {
         this(itemStringID, 1.0F, tickets);
      }
   }
}
