package necesse.entity.mobs.friendly.human.humanShop;

import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class BuyingShopItem implements IDDataContainer {
   public final IDData idData = new IDData();
   protected ShopManager manager;
   protected Item item;
   protected BuyingShopItem.BuyShopPriceGenerator priceGenerator;
   public BuyingShopItem.ShopItemRequirement requirement;

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

   public BuyingShopItem(String customItemStringID) {
      this.item = ItemRegistry.getItem(customItemStringID);
   }

   public BuyingShopItem() {
      this(null);
   }

   public void onRegistered(ShopManager manager) {
      if (this.manager != null) {
         throw new IllegalStateException("BuyingShopItem is already registered to a registry");
      }

      this.manager = manager;
   }

   public void onRegistryClosed() {
      if (this.item == null) {
         Item item = ItemRegistry.getItem(this.getStringID());
         if (item != null) {
            this.item = item;
         }
      }

      if (this.item == null) {
         throw new NullPointerException("BuyingShopItem item cannot be null");
      }

      if (this.priceGenerator == null) {
         throw new NullPointerException("BuyingShopItem price cannot be null");
      }
   }

   public void init(Level level, int uniqueSeed) {
   }

   public BuyingShopItem setPrice(BuyingShopItem.BuyShopPriceGenerator priceGenerator) {
      this.priceGenerator = priceGenerator;
      return this;
   }

   public BuyingShopItem setRandomPrice(int minPrice, int maxPrice) {
      return this.setPrice((random, client, mob, blackboard) -> random.getIntBetween(minPrice, maxPrice));
   }

   public BuyingShopItem setStaticPrice(int price) {
      return this.setPrice((random, client, mob, blackboard) -> price);
   }

   public BuyingShopItem setPriceBasedOnHappiness(int bestPrice, int worstPrice, int randomRange) {
      return this.setPrice((random, client, mob, blackboard) -> {
         float discountPercentage = HumanShop.getHappinessPriceDiscountPercent(mob.getShopQualityOfLife(), mob.getShopHappiness());
         int price = GameMath.lerp(discountPercentage, worstPrice, bestPrice);
         if (randomRange > 0) {
            price = GameMath.limit(random.getIntOffset(price, randomRange / 2), worstPrice, bestPrice);
         }

         return price;
      });
   }

   public BuyingShopItem setPriceBasedOnHappiness(int bestPrice, int worstPrice) {
      return this.setPriceBasedOnHappiness(bestPrice, worstPrice, 0);
   }

   public BuyingShopItem setRequirement(BuyingShopItem.ShopItemRequirement isAvailable) {
      this.requirement = isAvailable;
      return this;
   }

   public BuyingShopItem addRequirement(BuyingShopItem.ShopItemRequirement isAvailable) {
      if (this.requirement == null) {
         this.requirement = isAvailable;
      } else {
         this.requirement = this.requirement.and(isAvailable);
      }

      return this;
   }

   public BuyingShopItem addRandomAvailableRequirement(float chance) {
      return this.addRequirement((random, client, mob, blackboard) -> random.getChance(chance));
   }

   public BuyingShopItem addKilledMobRequirement(String mobStringID) {
      return this.addRequirement((random, client, mob, blackboard) -> client.characterStats().mob_kills.getKills(mobStringID) > 0);
   }

   public BuyingShopItem addKilledAllMobsRequirement(String... mobStringIDs) {
      return this.addRequirement((random, client, mob, blackboard) -> {
         for (String mobStringID : mobStringIDs) {
            if (client.characterStats().mob_kills.getKills(mobStringID) <= 0) {
               return false;
            }
         }

         return true;
      });
   }

   public BuyingShopItem addKilledEitherMobsRequirement(String... mobStringIDs) {
      return this.addRequirement((random, client, mob, blackboard) -> {
         for (String mobStringID : mobStringIDs) {
            if (client.characterStats().mob_kills.getKills(mobStringID) > 0) {
               return true;
            }
         }

         return false;
      });
   }

   public boolean isAvailable(GameRandom random, ServerClient client, HumanShop mob, GameBlackboard blackboard) {
      return this.requirement == null || this.requirement.test(random, client, mob, blackboard);
   }

   public int generatePrice(GameRandom random, ServerClient client, HumanShop mob, GameBlackboard blackboard) {
      return this.priceGenerator.generatePrice(random, client, mob, blackboard);
   }

   @FunctionalInterface
   public interface BuyShopPriceGenerator {
      int generatePrice(GameRandom var1, ServerClient var2, HumanShop var3, GameBlackboard var4);
   }

   @FunctionalInterface
   public interface ShopItemRequirement {
      boolean test(GameRandom var1, ServerClient var2, HumanShop var3, GameBlackboard var4);

      default BuyingShopItem.ShopItemRequirement and(BuyingShopItem.ShopItemRequirement other) {
         return (random, client, mob, blackboard) -> this.test(random, client, mob, blackboard) && other.test(random, client, mob, blackboard);
      }
   }
}
