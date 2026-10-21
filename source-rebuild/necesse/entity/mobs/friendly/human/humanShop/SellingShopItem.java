package necesse.entity.mobs.friendly.human.humanShop;

import necesse.engine.dlc.DLC;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.IntRange;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.events.SingleShopStockUpdateEvent;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class SellingShopItem implements IDDataContainer {
   public final IDData idData = new IDData();
   protected ShopManager manager;
   protected SellingShopItem.ShopItemGenerator itemGenerator;
   protected SellingShopItem.ShopPriceGenerator priceGenerator;
   public int currentStock;
   public int maxStock;
   protected int restockPerDay;
   public SellingShopItem.ShopItemRequirement requirement;

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   @Override
   public final int getID() {
      return this.idData.getID();
   }

   @Override
   public String getStringID() {
      return this.idData.getStringID();
   }

   public SellingShopItem(int maxStock, int restockPerDay) {
      this.maxStock = maxStock;
      this.currentStock = -1;
      this.restockPerDay = restockPerDay;
   }

   public SellingShopItem() {
      this(-1, 0);
   }

   public void onRegistered(ShopManager manager) {
      if (this.manager != null) {
         throw new IllegalStateException("SellingShopItem is already registered to a registry");
      }

      this.manager = manager;
   }

   public void onRegistryClosed() {
      if (this.itemGenerator == null) {
         Item item = ItemRegistry.getItem(this.getStringID());
         if (item != null) {
            this.itemGenerator = (random, client, mob, blackboard) -> new InventoryItem(item);
         }
      }

      if (this.itemGenerator == null) {
         throw new NullPointerException("SellingShopItem item cannot be null");
      }

      if (this.priceGenerator == null) {
         throw new NullPointerException("SellingShopItem price cannot be null");
      }
   }

   public void init(Level level, int uniqueSeed) {
      if (this.currentStock == -1 && this.maxStock > 0) {
         this.currentStock = new GameRandom(uniqueSeed).nextSeeded(this.getID()).getIntBetween(this.maxStock / 2, this.maxStock);
      }
   }

   public SellingShopItem setItem(SellingShopItem.ShopItemGenerator itemGenerator) {
      this.itemGenerator = itemGenerator;
      return this;
   }

   public SellingShopItem setItem(InventoryItem item) {
      return this.setItem((random, client, mob, blackboard) -> item);
   }

   public SellingShopItem setItem(String itemStringID) {
      return this.setItem(new InventoryItem(itemStringID));
   }

   public SellingShopItem setPrice(SellingShopItem.ShopPriceGenerator priceGenerator) {
      this.priceGenerator = priceGenerator;
      return this;
   }

   public SellingShopItem setRandomPrice(int minPrice, int maxPrice) {
      return this.setPrice((random, client, mob, blackboard, item) -> {
         int price = random.getIntBetween(minPrice, maxPrice);
         return new IntRange(price, price);
      });
   }

   public SellingShopItem setStaticPrice(int noStockPrice, int fullStockPrice) {
      return this.setPrice((random, client, mob, blackboard, item) -> new IntRange(noStockPrice, fullStockPrice));
   }

   public SellingShopItem setStaticPriceBasedOnHappiness(int bestPrice, int worstPrice, int randomRange) {
      return this.setPrice((random, client, mob, blackboard, item) -> {
         float discountPercentage = HumanShop.getHappinessPriceDiscountPercent(mob.getShopQualityOfLife(), mob.getShopHappiness());
         int price = GameMath.lerp(discountPercentage, worstPrice, bestPrice);
         if (randomRange > 0) {
            price = GameMath.limit(random.getIntOffset(price, randomRange / 2), bestPrice, worstPrice);
         }

         return new IntRange(price, price);
      });
   }

   public SellingShopItem setStaticPriceBasedOnHappiness(int bestPrice, int worstPrice) {
      return this.setStaticPriceBasedOnHappiness(bestPrice, worstPrice, 0);
   }

   public SellingShopItem setStaticBrokerPriceBasedOnHappiness(float bestModifier, float worstModifier, float rangeModifier) {
      return this.setPrice((random, client, mob, blackboard, item) -> {
         float discountPercentage = HumanShop.getHappinessPriceDiscountPercent(mob.getShopQualityOfLife(), mob.getShopHappiness());
         float brokerValue = item.getBrokerValue();
         int bestPrice = (int)(brokerValue * bestModifier);
         int worstPrice = (int)(brokerValue * worstModifier);
         int range = (int)(Math.abs(worstPrice - bestPrice) / rangeModifier);
         int price = GameMath.lerp(discountPercentage, worstPrice, bestPrice);
         if (range > 0) {
            price = GameMath.limit(random.getIntOffset(price, range / 2), bestPrice, worstPrice);
         }

         return new IntRange(price, price);
      });
   }

   public SellingShopItem setStockPriceBasedOnHappiness(
      int bestFullStockPrice, int bestNoStockPrice, int worstFullStockPrice, int worstNoStockPrice, int randomRange
   ) {
      return this.setPrice((random, client, mob, blackboard, item) -> {
         float discountPercentage = HumanShop.getHappinessPriceDiscountPercent(mob.getShopQualityOfLife(), mob.getShopHappiness());
         int noStockPrice = GameMath.lerp(discountPercentage, worstNoStockPrice, bestNoStockPrice);
         int fullStockPrice = GameMath.lerp(discountPercentage, worstFullStockPrice, bestFullStockPrice);
         if (randomRange > 0) {
            noStockPrice = GameMath.limit(random.getIntOffset(noStockPrice, randomRange / 2), bestNoStockPrice, worstNoStockPrice);
            fullStockPrice = GameMath.limit(random.getIntOffset(fullStockPrice, randomRange / 2), bestFullStockPrice, worstFullStockPrice);
         }

         return new IntRange(noStockPrice, fullStockPrice);
      });
   }

   public SellingShopItem setStockPriceBasedOnHappiness(int bestFullStockPrice, int bestNoStockPrice, int worstFullStockPrice, int worstNoStockPrice) {
      return this.setStockPriceBasedOnHappiness(bestFullStockPrice, bestNoStockPrice, worstFullStockPrice, worstNoStockPrice, 0);
   }

   public SellingShopItem setRequirement(SellingShopItem.ShopItemRequirement isAvailable) {
      this.requirement = isAvailable;
      return this;
   }

   public SellingShopItem addRequirement(SellingShopItem.ShopItemRequirement isAvailable) {
      if (this.requirement == null) {
         this.requirement = isAvailable;
      } else {
         this.requirement = this.requirement.and(isAvailable);
      }

      return this;
   }

   public SellingShopItem addRandomAvailableRequirement(float chance) {
      return this.addRequirement((random, client, mob, blackboard) -> random.getChance(chance));
   }

   public SellingShopItem addKilledMobRequirement(String mobStringID) {
      return this.addRequirement((random, client, mob, blackboard) -> client.characterStats().mob_kills.getKills(mobStringID) > 0);
   }

   public SellingShopItem addKilledAllMobsRequirement(String... mobStringIDs) {
      return this.addRequirement((random, client, mob, blackboard) -> {
         for (String mobStringID : mobStringIDs) {
            if (client.characterStats().mob_kills.getKills(mobStringID) <= 0) {
               return false;
            }
         }

         return true;
      });
   }

   public SellingShopItem addKilledEitherMobsRequirement(String... mobStringIDs) {
      return this.addRequirement((random, client, mob, blackboard) -> {
         for (String mobStringID : mobStringIDs) {
            if (client.characterStats().mob_kills.getKills(mobStringID) > 0) {
               return true;
            }
         }

         return false;
      });
   }

   public SellingShopItem addHasCompletedStoryObjectiveRequirement(String storyObjectiveStringID) {
      return this.addRequirement((random, client, mob, blackboard) -> client.storyManager.hasCompletedObjective(storyObjectiveStringID));
   }

   public SellingShopItem addDLCRequirement(DLC dlc) {
      return this.addRequirement((random, client, mob, blackboard) -> client.hasDLC(dlc));
   }

   public SellingShopItem addJournalChallengeCompleteRequirement(int challengeID) {
      return this.addRequirement((random, client, mob, blackboard) -> JournalChallengeRegistry.getChallenge(challengeID).isCompleted(client));
   }

   public boolean shouldSave() {
      return this.maxStock >= 0;
   }

   public void addSaveData(SaveData save) {
      save.addInt("currentStock", this.currentStock);
   }

   public void applyLoadData(LoadData save) {
      if (this.maxStock >= 0) {
         this.currentStock = save.getInt("currentStock", this.currentStock);
         if (this.currentStock > this.maxStock) {
            this.currentStock = this.maxStock;
         }
      }
   }

   public void restock(Server server, int daysPassed) {
      if (this.maxStock > 0) {
         int lastStock = this.currentStock;
         this.currentStock = Math.min(this.maxStock, this.currentStock + this.restockPerDay * daysPassed);
         if (lastStock != this.currentStock && server != null) {
            new SingleShopStockUpdateEvent(this.manager, this).applyAndSendToAllClients(server);
         }
      }
   }

   public boolean isAvailable(GameRandom random, ServerClient client, HumanShop mob, GameBlackboard blackboard) {
      return this.requirement == null || this.requirement.test(random, client, mob, blackboard);
   }

   public InventoryItem generateItem(GameRandom random, ServerClient client, HumanShop mob, GameBlackboard blackboard) {
      return this.itemGenerator.generateItem(random, client, mob, blackboard);
   }

   public IntRange generatePrice(GameRandom random, ServerClient client, HumanShop mob, GameBlackboard blackboard, InventoryItem item) {
      return this.priceGenerator.generatePrice(random, client, mob, blackboard, item);
   }

   @FunctionalInterface
   public interface ShopItemGenerator {
      InventoryItem generateItem(GameRandom var1, ServerClient var2, HumanShop var3, GameBlackboard var4);
   }

   @FunctionalInterface
   public interface ShopItemRequirement {
      boolean test(GameRandom var1, ServerClient var2, HumanShop var3, GameBlackboard var4);

      default SellingShopItem.ShopItemRequirement and(SellingShopItem.ShopItemRequirement other) {
         return (random, client, mob, blackboard) -> this.test(random, client, mob, blackboard) && other.test(random, client, mob, blackboard);
      }
   }

   @FunctionalInterface
   public interface ShopPriceGenerator {
      IntRange generatePrice(GameRandom var1, ServerClient var2, HumanShop var3, GameBlackboard var4, InventoryItem var5);
   }
}
