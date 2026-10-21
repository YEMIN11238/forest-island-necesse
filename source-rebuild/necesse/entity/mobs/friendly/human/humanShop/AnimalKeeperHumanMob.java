package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.CountOfTicketLootItems;
import necesse.inventory.lootTable.lootItem.LootItem;

public class AnimalKeeperHumanMob extends HumanShop {
   public AnimalKeeperHumanMob() {
      super(500, 200, "animalkeeper");
      this.attackCooldown = 500;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.jobTypeHandler.getPriority("husbandry").disabledBySettler = false;
      this.equipmentInventory.setItem(6, new InventoryItem("coppersword"));
      this.shop.addSellingItem("inefficientfeather", new SellingShopItem()).setStaticPriceBasedOnHappiness(500, 1500, 300);
      this.shop.addSellingItem("weticicle", new SellingShopItem()).setStaticPriceBasedOnHappiness(300, 900, 200);
      this.shop.addSellingItem("exoticseeds", new SellingShopItem()).setStaticPriceBasedOnHappiness(400, 1000, 200);
      this.shop.addSellingItem("queenbee", new SellingShopItem()).setStaticPriceBasedOnHappiness(1000, 1600, 100).addKilledMobRequirement("piratecaptain");
      this.shop.addSellingItem("animalkeepershat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("animalkeepershirt", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("animalkeepershoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
   }

   @Override
   public LootTable getLootTable() {
      return super.getLootTable();
   }

   @Override
   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      return this.getLocalMessages("akeepertalk", 7);
   }

   @Override
   public GameMessage getRecruitError(ServerClient client) {
      GameMessage superError = super.getRecruitError(client);
      if (superError != null) {
         return superError;
      } else if (this.isTrapped()) {
         return null;
      } else if (this.isSettler() || this.isVisitor()) {
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

      GameRandom random = new GameRandom(this.getSettlerSeed() * 193L);
      if (this.isVisitor()) {
         return Collections.singletonList(new InventoryItem("coin", random.getIntBetween(500, 800)));
      }

      LootTable items = new LootTable(
         new CountOfTicketLootItems(1, 100, new LootItem("wool", Integer.MAX_VALUE), 100, new LootItem("leather", Integer.MAX_VALUE))
      );
      ArrayList<InventoryItem> out = GameLootUtils.getItemsValuedAt(random, random.getIntBetween(500, 800), 0.2F, new LootItem("coin", Integer.MAX_VALUE));
      out.addAll(GameLootUtils.getItemsValuedAt(random, random.getIntBetween(100, 200), 0.2F, items));
      out.sort(Comparator.comparing(InventoryItem::getBrokerValue).reversed());
      return out;
   }
}
