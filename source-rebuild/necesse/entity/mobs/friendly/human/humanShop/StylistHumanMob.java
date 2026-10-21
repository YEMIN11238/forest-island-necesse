package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.StylistContainer;
import necesse.inventory.item.armorItem.cosmetics.misc.ShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.WigArmorItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.CountOfTicketLootItems;
import necesse.inventory.lootTable.lootItem.LootItem;

public class StylistHumanMob extends HumanShop {
   public boolean wasTrappedByPirates;

   public StylistHumanMob() {
      super(500, 200, "stylist");
      this.attackCooldown = 500;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.equipmentInventory.setItem(6, new InventoryItem("ironsword"));
      this.shop.addSellingItem("prettyflower", new SellingShopItem()).setStaticPriceBasedOnHappiness(600, 950, 50);
      this.shop
         .addSellingItem("wig", new SellingShopItem())
         .setItem((random, client, mob, blackboard) -> WigArmorItem.addWigData(new InventoryItem("wig"), client.playerMob.look))
         .setStaticPrice(200, 200);
      this.shop
         .addSellingItem("shirt", new SellingShopItem())
         .setItem((random, client, mob, blackboard) -> ShirtArmorItem.addColorData(new InventoryItem("shirt"), client.playerMob.look.getShirtColor()))
         .setStaticPrice(200, 200);
      this.shop
         .addSellingItem("shoes", new SellingShopItem())
         .setItem((random, client, mob, blackboard) -> ShirtArmorItem.addColorData(new InventoryItem("shoes"), client.playerMob.look.getShoesColor()))
         .setStaticPrice(200, 200);
      this.shop.addSellingItem("surgicalmask", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("elderhat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("eldershirt", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("eldershoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("minerhat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("minershirt", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("minerboots", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("merchantshirt", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("merchantboots", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("stylishflower", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("stylistshirt", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("stylistshoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("safarihat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("safarishirt", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("safarishoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("tophat", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("blazer", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("dressshoes", new SellingShopItem()).setStaticPriceBasedOnHappiness(75, 150, 20);
      this.shop.addSellingItem("plaguemask", new SellingShopItem()).setStaticPriceBasedOnHappiness(200, 400, 40).addKilledMobRequirement("evilsprotector");
      this.shop.addSellingItem("plaguerobe", new SellingShopItem()).setStaticPriceBasedOnHappiness(200, 400, 40).addKilledMobRequirement("evilsprotector");
      this.shop.addSellingItem("plagueboots", new SellingShopItem()).setStaticPriceBasedOnHappiness(200, 400, 40).addKilledMobRequirement("evilsprotector");
      this.shop.addBuyingItem("wool", new BuyingShopItem()).setPriceBasedOnHappiness(13, 7, 2);
      this.shop.addBuyingItem("leather", new BuyingShopItem()).setPriceBasedOnHappiness(16, 8, 2);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.wasTrappedByPirates = reader.getNextBoolean();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.wasTrappedByPirates);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.wasTrappedByPirates) {
         save.addBoolean("wasTrappedByPirates", this.wasTrappedByPirates);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.wasTrappedByPirates = save.getBoolean("wasTrappedByPirates", false, false);
   }

   @Override
   public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
      this.gender = HumanGender.FEMALE;
      super.randomizeLook(look, this.gender, random);
      this.settlerName = this.getRandomName(new GameRandom(this.settlerSeed));
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
         return client.characterStats().mob_kills.getKills("piratecaptain") > 0 ? null : new LocalMessage("ui", "settlernorecruit");
      }
   }

   @Override
   public List<InventoryItem> getRecruitItems(ServerClient client) {
      if (this.isTrapped()) {
         return Collections.emptyList();
      }

      GameRandom random = new GameRandom(this.getSettlerSeed() * 263L);
      if (this.isVisitor()) {
         return Collections.singletonList(new InventoryItem("coin", random.getIntBetween(500, 800)));
      }

      LootTable items = new LootTable(
         new CountOfTicketLootItems(1, 100, new LootItem("wool", Integer.MAX_VALUE), 100, new LootItem("leather", Integer.MAX_VALUE))
      );
      ArrayList<InventoryItem> out = GameLootUtils.getItemsValuedAt(random, random.getIntBetween(800, 1000), 0.2F, new LootItem("coin", Integer.MAX_VALUE));
      out.addAll(GameLootUtils.getItemsValuedAt(random, random.getIntBetween(100, 200), 0.2F, items));
      out.sort(Comparator.comparing(InventoryItem::getBrokerValue).reversed());
      return out;
   }

   @Override
   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      ArrayList<GameMessage> out = this.getLocalMessages("stylisttalk", 7);
      HumanMob m = this.getRandomHuman("angler");
      if (m != null) {
         out.add(new LocalMessage("mobmsg", "stylistspecial", "angler", m.getSettlerName()));
      }

      return out;
   }

   @Override
   public PacketOpenContainer getOpenShopPacket(Server server, ServerClient client) {
      return StylistContainer.getStylistContainerContent(this, client).getPacket(ContainerRegistry.STYLIST_CONTAINER, this);
   }
}
