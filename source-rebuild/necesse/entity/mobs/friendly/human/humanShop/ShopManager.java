package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.GameRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.IntRange;
import necesse.engine.world.WorldEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.events.ShopWealthUpdateEvent;
import necesse.inventory.container.mob.NetworkBuyingShopItem;
import necesse.inventory.container.mob.NetworkSellingShopItem;
import necesse.level.maps.Level;

public class ShopManager {
   public int uniqueID = -1;
   public ShopManager.SellingShopRegistry sellingShop = new ShopManager.SellingShopRegistry();
   public ShopManager.BuyingShopRegistry buyingShop = new ShopManager.BuyingShopRegistry();
   public int lastDayStockUpdate = -1;
   public int shopWealth;
   public IntRange maxShopWealthRange;
   public IntRange shopWealthChangePerDay;
   protected boolean forceSaveWealth;

   public ShopManager() {
      this.setMaxShopWealth(1000, 100);
   }

   public void addSaveData(SaveData save) {
      if (this.sellingShop.getSize() > 0 || this.buyingShop.getSize() > 0 || this.forceSaveWealth) {
         if (this.lastDayStockUpdate >= 0) {
            save.addInt("lastDayStockUpdate", this.lastDayStockUpdate);
         }

         if (this.maxShopWealthRange != null) {
            save.addInt("shopWealth", this.shopWealth);
         }
      }

      SaveData itemsSave = new SaveData("ITEMS");

      for (SellingShopItem item : this.sellingShop.getItems()) {
         if (item.shouldSave()) {
            SaveData itemSave = new SaveData("ITEM");
            itemSave.addSafeString("stringID", item.getStringID());
            item.addSaveData(itemSave);
            itemsSave.addSaveData(itemSave);
         }
      }

      save.addSaveData(itemsSave);
   }

   public void applyLoadData(LoadData save) {
      this.lastDayStockUpdate = save.getInt("lastDayStockUpdate", this.lastDayStockUpdate, false);
      this.shopWealth = save.getInt("shopWealth", this.shopWealth, false);
      LoadData itemsSave = save.getFirstLoadDataByName("ITEMS");
      if (itemsSave != null) {
         for (LoadData itemSave : itemsSave.getLoadDataByName("ITEM")) {
            String itemStringID = itemSave.getSafeString("stringID", null);
            if (itemStringID != null) {
               SellingShopItem item = this.sellingShop.getItem(itemStringID);
               if (item != null) {
                  item.applyLoadData(itemSave);
               }
            }
         }
      }
   }

   public void init(Level level, int uniqueSeed) {
      if (this.uniqueID == -1) {
         this.uniqueID = new GameRandom(uniqueSeed).nextSeeded().nextInt();
      }

      this.sellingShop.closeRegistry();
      this.buyingShop.closeRegistry();

      for (SellingShopItem item : this.sellingShop.getItems()) {
         item.init(level, uniqueSeed);
      }

      for (BuyingShopItem item : this.buyingShop.getItems()) {
         item.init(level, uniqueSeed);
      }
   }

   public void serverTick(WorldEntity worldEntity, Server server) {
      if (worldEntity != null) {
         int currentDay = worldEntity.getDay();
         if (this.lastDayStockUpdate < 0) {
            this.lastDayStockUpdate = currentDay;
         } else if (this.lastDayStockUpdate < currentDay) {
            int daysPassed = currentDay - this.lastDayStockUpdate;
            this.lastDayStockUpdate = currentDay;

            for (SellingShopItem item : this.sellingShop.getItems()) {
               item.restock(server, daysPassed);
            }

            int lastShopWealth = this.shopWealth;
            if (this.maxShopWealthRange != null) {
               int thisDayMaxWealth = this.maxShopWealthRange.getRandomValueInRange(GameRandom.globalRandom);
               int wealthChange = this.shopWealthChangePerDay.getRandomValueInRange(GameRandom.globalRandom);
               if (this.shopWealth < thisDayMaxWealth) {
                  this.shopWealth = Math.min(this.shopWealth + wealthChange * daysPassed, thisDayMaxWealth);
               } else {
                  double multiplier = (double)this.shopWealth / thisDayMaxWealth;
                  this.shopWealth = Math.max(this.shopWealth - (int)(wealthChange * daysPassed * multiplier), thisDayMaxWealth);
               }
            } else {
               this.shopWealth = -1;
            }

            if (lastShopWealth != this.shopWealth && server != null) {
               new ShopWealthUpdateEvent(this).applyAndSendToAllClients(server);
            }
         }
      }
   }

   public void setMaxShopWealth(IntRange maxWealthRange, IntRange changePerDay) {
      if (this.sellingShop.isClosed()) {
         throw new IllegalStateException("Cannot set max shop wealth on closed registry");
      }

      this.maxShopWealthRange = maxWealthRange;
      if (maxWealthRange != null) {
         this.shopWealth = GameRandom.globalRandom.getIntBetween(maxWealthRange.min, maxWealthRange.max);
      } else {
         this.shopWealth = -1;
      }

      this.shopWealthChangePerDay = changePerDay;
   }

   public void setMaxShopWealth(int maxWealth, int changePerDay) {
      int maxWealthRange = maxWealth / 5;
      int changePerDayRange = changePerDay / 5;
      this.setMaxShopWealth(
         new IntRange(maxWealth - maxWealthRange, maxWealth + maxWealthRange), new IntRange(changePerDay - changePerDayRange, changePerDay + changePerDayRange)
      );
   }

   public SellingShopItem addSellingItem(String stringID, SellingShopItem item) {
      return this.sellingShop.addItem(stringID, item);
   }

   public BuyingShopItem addBuyingItem(String stringID, BuyingShopItem item) {
      return this.buyingShop.addItem(stringID, item);
   }

   public void forceSaveWealth() {
      this.forceSaveWealth = true;
   }

   public ArrayList<NetworkSellingShopItem> getSellingItemsList(ServerClient client, HumanShop mob, long shopSeed) {
      ArrayList<NetworkSellingShopItem> items = new ArrayList<>();
      GameRandom random = new GameRandom(shopSeed + 5L);
      GameBlackboard blackboard = new GameBlackboard();

      for (SellingShopItem shopItem : this.sellingShop.getItems()) {
         GameRandom itemRandom = random.nextSeeded();
         if (shopItem.isAvailable(itemRandom, client, mob, blackboard)) {
            InventoryItem item = shopItem.generateItem(itemRandom, client, mob, blackboard);
            if (item != null) {
               IntRange price = shopItem.generatePrice(itemRandom, client, mob, blackboard, item);
               if (price != null) {
                  items.add(new NetworkSellingShopItem(shopItem.getID(), item, price.min, price.max, shopItem.currentStock, shopItem.maxStock));
               }
            }
         }
      }

      return items;
   }

   public ArrayList<NetworkBuyingShopItem> getBuyingItemsList(ServerClient client, HumanShop mob, long shopSeed) {
      ArrayList<NetworkBuyingShopItem> items = new ArrayList<>();
      GameRandom random = new GameRandom(shopSeed + 5L);
      GameBlackboard blackboard = new GameBlackboard();

      for (BuyingShopItem shopItem : this.buyingShop.getItems()) {
         GameRandom itemRandom = random.nextSeeded();
         if (shopItem.isAvailable(itemRandom, client, mob, blackboard)) {
            int price = shopItem.generatePrice(itemRandom, client, mob, blackboard);
            items.add(new NetworkBuyingShopItem(shopItem.item, price));
         }
      }

      return items;
   }

   public class BuyingShopRegistry extends GameRegistry<BuyingShopItem> {
      public BuyingShopRegistry() {
         super("BuyingShopItem", 32767);
      }

      @Override
      public void registerCore() {
      }

      protected void onRegister(BuyingShopItem object, int id, String stringID, boolean isReplace) {
         object.onRegistered(ShopManager.this);
      }

      @Override
      protected void onRegistryClose() {
         for (BuyingShopItem element : this.getElements()) {
            element.onRegistryClosed();
         }
      }

      public BuyingShopItem addItem(String stringID, BuyingShopItem item) {
         this.register(stringID, item);
         return item;
      }

      public int getSize() {
         return this.getTotalElements();
      }

      public Iterable<BuyingShopItem> getItems() {
         return this.getElements();
      }

      public BuyingShopItem getItem(String stringID) {
         return this.getElement(stringID);
      }

      public BuyingShopItem getItem(int id) {
         return this.getElement(id);
      }
   }

   public class SellingShopRegistry extends GameRegistry<SellingShopItem> {
      public SellingShopRegistry() {
         super("SellingShopItem", 32767);
      }

      @Override
      public void registerCore() {
      }

      protected void onRegister(SellingShopItem object, int id, String stringID, boolean isReplace) {
         object.onRegistered(ShopManager.this);
      }

      @Override
      protected void onRegistryClose() {
         for (SellingShopItem element : this.getElements()) {
            element.onRegistryClosed();
         }
      }

      public SellingShopItem addItem(String stringID, SellingShopItem item) {
         this.register(stringID, item);
         return item;
      }

      public int getSize() {
         return this.getTotalElements();
      }

      public Iterable<SellingShopItem> getItems() {
         return this.getElements();
      }

      public SellingShopItem getItem(String stringID) {
         return this.getElement(stringID);
      }

      public SellingShopItem getItem(int id) {
         return this.getElement(id);
      }
   }
}
