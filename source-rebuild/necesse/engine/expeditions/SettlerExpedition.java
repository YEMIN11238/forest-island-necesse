package necesse.engine.expeditions;

import java.util.List;
import necesse.engine.GameLog;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.ExpeditionMissionRegistry;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.StoryObjectiveRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.IntRange;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class SettlerExpedition implements IDDataContainer {
   public static int minCompleteTicks = 6000;
   public static int maxCompleteTicks = 12000;
   public final IDData idData = new IDData();
   protected GameMessage displayName;
   protected String categoryStringID;
   public static OneOfLootItems rareHappinessObjects = new OneOfLootItems();
   public static OneOfLootItems epicHappinessObjects = new OneOfLootItems();
   public static OneOfLootItems legendaryHappinessObjects = new OneOfLootItems();
   public static float[] storyProgressSuccessChances = new float[]{0.6F, 0.8F, 1.0F};

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public void onExpeditionMissionRegistryClosed() {
   }

   public void initDisplayName() {
      this.displayName = new LocalMessage("expedition", this.getStringID());
   }

   public GameMessage getDisplayName() {
      return this.displayName;
   }

   public GameMessage getFullDisplayName() {
      if (this.categoryStringID == null) {
         return this.getDisplayName();
      }

      GameMessage category = ExpeditionMissionRegistry.categoryDisplayNames.get(this.categoryStringID);
      if (category == null) {
         category = new StaticMessage("NULL");
      }

      return new LocalMessage("ui", "missionnameformat", "name", this.getDisplayName(), "type", category);
   }

   public String getCategoryStringID() {
      return this.categoryStringID;
   }

   public SettlerExpedition setCategory(String categoryStringID) {
      this.categoryStringID = categoryStringID;
      return this;
   }

   public boolean isAvailable(ServerSettlementData settlement) {
      return this.getSuccessChance(settlement) > 0.0F;
   }

   public abstract GameMessage getUnavailableMessage();

   public abstract float getSuccessChance(ServerSettlementData var1);

   public abstract int getBaseCost(ServerSettlementData var1);

   public IntRange getCostRange(int baseCost) {
      if (baseCost == 0) {
         new IntRange(0, 0);
      }

      int minCost = Math.max((int)(0.85F * baseCost), 0);
      int maxCost = Math.max((int)(1.15F * baseCost), 0);
      return new IntRange(minCost, maxCost);
   }

   public final IntRange getCostRange(ServerSettlementData settlement) {
      return this.getCostRange(this.getBaseCost(settlement));
   }

   public int getCurrentCost(ServerSettlementData settlement, long shopSeed) {
      return this.getCostRange(settlement).getRandomValueInRange(new GameRandom(shopSeed).nextSeeded(this.getID()));
   }

   public List<InventoryItem> getItemIcons() {
      return null;
   }

   public abstract List<InventoryItem> getRewardItems(ServerSettlementData var1, HumanMob var2);

   public int getTicksToComplete() {
      return GameRandom.globalRandom.getIntBetween(minCompleteTicks, maxCompleteTicks);
   }

   public static float storyProgressSuccessChance(ServerSettlementData settlement, String storyMajorObjectiveStringID) {
      return storyProgressSuccessChance(settlement, storyMajorObjectiveStringID, 0);
   }

   public static float storyProgressSuccessChance(ServerSettlementData settlement, String storyMajorObjectiveStringID, int offset) {
      int index = StoryObjectiveRegistry.getMajorObjectiveIndex(storyMajorObjectiveStringID);
      if (index == -1) {
         GameLog.warn.println("Could not find major objective with stringID: " + storyMajorObjectiveStringID);
      }

      int highest = settlement.storyObjectives.getHighestMajorObjectiveIndexCompleted() + 1;
      int deltaTier = highest - index - offset;
      return deltaTier < 0 ? 0.0F : storyProgressSuccessChances[Math.min(deltaTier, storyProgressSuccessChances.length - 1)];
   }

   static {
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "luckyrabbitsfoot"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "dryadplant"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "wizardbookshelf"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "sheepchair"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "championbelt"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "woodenduck"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "goblinfriendspainting"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "slimebench"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "fishianbathtub"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "bearrug"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "piratecarpet"));
      rareHappinessObjects.add(new ChanceLootItem(0.3F, "mossycarpet"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "friendlybush"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "parrotperch"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "fangoftheprotector"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "sarcophagus"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "tropicalislandpainting"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "voidflame"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "swampguardianpainting"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "runebounddrum"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "queeneggnest"));
      epicHappinessObjects.add(new ChanceLootItem(0.3F, "vulturedisplay"));
      legendaryHappinessObjects.add(new ChanceLootItem(0.1F, "spiritbasin"));
      legendaryHappinessObjects.add(new ChanceLootItem(0.1F, "pestwardenlamp"));
      legendaryHappinessObjects.add(new ChanceLootItem(0.1F, "unmeltableice"));
      legendaryHappinessObjects.add(new ChanceLootItem(0.1F, "sagepedestal"));
      legendaryHappinessObjects.add(new ChanceLootItem(0.1F, "gritpedestal"));
      legendaryHappinessObjects.add(new ChanceLootItem(0.1F, "reapersscythestand"));
   }
}
