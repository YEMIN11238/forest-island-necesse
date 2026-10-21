package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.MageContainer;
import necesse.inventory.item.placeableItem.objectItem.WaystoneObjectItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.CountOfTicketLootItems;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class MageHumanMob extends HumanShop {
   public MageHumanMob() {
      super(500, 200, "mage");
      this.attackCooldown = 600;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.updateLook();
      this.equipmentInventory.setItem(6, new InventoryItem("voidmissile"));
      this.shop.addSellingItem("manapotion", new SellingShopItem(50, 10)).setStaticPriceBasedOnHappiness(5, 25, 5);
      this.shop.addSellingItem("book", new SellingShopItem()).setStaticPriceBasedOnHappiness(100, 200, 25);
      this.shop.addSellingItem("magicmanual", new SellingShopItem()).setStaticPriceBasedOnHappiness(500, 1000, 100);
      this.shop.addSellingItem("homestone", new SellingShopItem()).setItem((random, client, mob, blackboard) -> {
         ServerSettlementData settlement = mob.getSettlerSettlementServerData();
         return settlement != null ? new InventoryItem("homestone") : null;
      }).setStaticPriceBasedOnHappiness(1000, 1600, 100);
      this.shop.addSellingItem("waystone", new SellingShopItem()).setItem((random, client, mob, blackboard) -> {
         ServerSettlementData settlement = mob.getSettlerSettlementServerData();
         return settlement != null ? WaystoneObjectItem.setupWaystoneItem(new InventoryItem("waystone"), settlement.uniqueID) : null;
      }).setStaticPriceBasedOnHappiness(250, 550, 100);
      this.shop.addSellingItem("voidstaff", new SellingShopItem()).setStaticPriceBasedOnHappiness(400, 600, 100).addKilledMobRequirement("voidwizard");
      this.shop.addSellingItem("voidmissile", new SellingShopItem()).setStaticPriceBasedOnHappiness(500, 700, 100).addKilledMobRequirement("voidwizard");
      this.shop.addSellingItem("recallscroll", new SellingShopItem(25, 5)).setStaticPriceBasedOnHappiness(40, 80, 10);
      this.shop
         .addSellingItem("teleportationscroll", new SellingShopItem(25, 2))
         .setStaticPriceBasedOnHappiness(150, 200, 10)
         .addKilledMobRequirement("voidwizard");
      this.shop.addSellingItem("magicstilts", new SellingShopItem()).setStaticPriceBasedOnHappiness(600, 1200, 200).addKilledMobRequirement("voidwizard");
      this.shop.addSellingItem("genielamp", new SellingShopItem()).setStaticPriceBasedOnHappiness(800, 1200, 100).addKilledMobRequirement("piratecaptain");
      this.shop.addBuyingItem("batwing", new BuyingShopItem()).setPriceBasedOnHappiness(22, 10, 4);
      this.shop.addBuyingItem("ectoplasm", new BuyingShopItem()).setPriceBasedOnHappiness(30, 12, 10);
      this.shop.addSellingItem("magehat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("magerobe", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("mageshoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("crimsonhat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("crimsonrobe", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("crimsonshoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
   }

   @Override
   public void init() {
      super.init();
      this.updateLook();
   }

   public void updateLook() {
   }

   @Override
   public LootTable getLootTable() {
      return super.getLootTable();
   }

   @Override
   public GameMessage getRecruitError(ServerClient client) {
      GameMessage superError = super.getRecruitError(client);
      if (superError != null) {
         return superError;
      } else if (this.isTrapped()) {
         return null;
      } else {
         return client.characterStats().mob_kills.getKills("voidwizard") > 0 ? null : new LocalMessage("ui", "settlernorecruit");
      }
   }

   @Override
   public List<InventoryItem> getRecruitItems(ServerClient client) {
      if (this.isTrapped()) {
         return Collections.emptyList();
      }

      GameRandom random = new GameRandom(this.getSettlerSeed() * 557L);
      if (this.isVisitor()) {
         return Collections.singletonList(new InventoryItem("coin", random.getIntBetween(600, 800)));
      }

      LootTable items = new LootTable(
         new CountOfTicketLootItems(1, 100, new LootItem("teleportationscroll", Integer.MAX_VALUE), 100, new LootItem("recallscroll", Integer.MAX_VALUE))
      );
      ArrayList<InventoryItem> out = GameLootUtils.getItemsValuedAt(random, random.getIntBetween(600, 800), 0.2F, new LootItem("coin", Integer.MAX_VALUE));
      out.addAll(GameLootUtils.getItemsValuedAt(random, random.getIntBetween(100, 200), 0.2F, items));
      out.sort(Comparator.comparing(InventoryItem::getBrokerValue).reversed());
      return out;
   }

   @Override
   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      return this.getLocalMessages("magetalk", 7);
   }

   @Override
   public PacketOpenContainer getOpenShopPacket(Server server, ServerClient client) {
      return MageContainer.getMageContainerContent(this, client).getPacket(ContainerRegistry.MAGE_CONTAINER, this);
   }
}
