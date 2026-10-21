package necesse.entity.mobs.friendly.human.humanShop;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.particle.Particle;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class FriendlyWitchHumanMob extends HumanShop {
   protected boolean isLost;

   public FriendlyWitchHumanMob() {
      super(2000, 200, null);
      this.attackCooldown = 600;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.equipmentInventory.setItem(6, new InventoryItem("boulderstaff"));
      this.shop
         .addSellingItem("necroticgreatsword", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(700, 1100, 200)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("necroticflask", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(800, 1200, 200)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("necroticbow", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(750, 1200, 200)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("necroticsoulskull", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(800, 1200, 200)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("witchhat", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(200, 400, 100)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("witchrobe", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(200, 400, 100)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("witchshoes", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(200, 400, 100)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      this.shop
         .addSellingItem("witchbroom", new SellingShopItem())
         .setStaticPriceBasedOnHappiness(3000, 4000, 500)
         .addKilledEitherMobsRequirement("evilwitchflask", "evilwitchbow", "evilwitchgreatsword", "evilwitch");
      ArrayList<String> potionSelection = new ArrayList<>(
         Arrays.asList(
            "manaregenpotion",
            "speedpotion",
            "healthregenpotion",
            "resistancepotion",
            "battlepotion",
            "attackspeedpotion",
            "accuracypotion",
            "rapidpotion",
            "knockbackpotion",
            "thornspotion",
            "fishingpotion",
            "miningpotion",
            "spelunkerpotion",
            "treasurepotion",
            "trackerpotion",
            "passivepotion",
            "buildingpotion"
         )
      );
      int averagePotionsInShop = 5;
      float chancePerPotion = (float)averagePotionsInShop / potionSelection.size();

      for (String itemStringID : potionSelection) {
         this.shop
            .addSellingItem(itemStringID, new SellingShopItem(25, 5))
            .setStaticBrokerPriceBasedOnHappiness(3.0F, 6.0F, 2.0F)
            .addRandomAvailableRequirement(chancePerPotion);
      }

      ArrayList<String> deepCavePotionSelection = new ArrayList<>(Arrays.asList("strengthpotion", "rangerpotion", "wisdompotion", "minionpotion", "webpotion"));
      int averageDeepCavePotionsInShop = 2;
      float chancePerDeepCavePotion = (float)averageDeepCavePotionsInShop / potionSelection.size();

      for (String itemStringID : deepCavePotionSelection) {
         this.shop
            .addSellingItem(itemStringID, new SellingShopItem(25, 5))
            .setStaticBrokerPriceBasedOnHappiness(3.0F, 6.0F, 2.0F)
            .addRandomAvailableRequirement(chancePerDeepCavePotion)
            .addKilledEitherMobsRequirement("reaper", "cryoqueen", "thecursedcrone", "pestwarden", "sageandgrit");
      }

      this.shop.addSellingItem("witchcauldron", new SellingShopItem(1, 1).setStaticBrokerPriceBasedOnHappiness(10.0F, 20.0F, 2.0F));
      this.shop.addBuyingItem("glassbottle", new BuyingShopItem()).setRandomPrice(1, 5);
      this.shop.addBuyingItem("firemone", new BuyingShopItem()).setRandomPrice(3, 13);
      this.shop.addBuyingItem("iceblossom", new BuyingShopItem()).setRandomPrice(3, 13);
      this.shop.addBuyingItem("sunflower", new BuyingShopItem()).setRandomPrice(3, 13);
      this.shop.addBuyingItem("mushroom", new BuyingShopItem()).setRandomPrice(3, 13);
      this.shop.addBuyingItem("thorns", new BuyingShopItem()).setRandomPrice(3, 13);
      this.shop.addBuyingItem("frogleg", new BuyingShopItem()).setRandomPrice(4, 15);
      this.shop.addBuyingItem("batwing", new BuyingShopItem()).setRandomPrice(4, 15);
      this.shop.addBuyingItem("cavespidergland", new BuyingShopItem()).setRandomPrice(4, 15);
      this.shop.addBuyingItem("spidervenom", new BuyingShopItem()).setRandomPrice(4, 15);
      this.shop.addBuyingItem("bone", new BuyingShopItem()).setRandomPrice(4, 15);
      this.shop.addBuyingItem("rockfish", new BuyingShopItem()).setRandomPrice(16, 44);
      this.shop.addBuyingItem("terrorfish", new BuyingShopItem()).setRandomPrice(30, 54);
      this.shop.addBuyingItem("swampfish", new BuyingShopItem()).setRandomPrice(16, 44);
      this.shop.addBuyingItem("furfish", new BuyingShopItem()).setRandomPrice(16, 44);
      this.shop.addBuyingItem("gobfish", new BuyingShopItem()).setRandomPrice(16, 44);
      this.shop.addBuyingItem("icefish", new BuyingShopItem()).setRandomPrice(16, 44);
      this.shop.addBuyingItem("halffish", new BuyingShopItem()).setRandomPrice(16, 44);
      this.shop.addBuyingItem("wormbait", new BuyingShopItem()).setRandomPrice(2, 11);
      this.shop.addBuyingItem("caveglow", new BuyingShopItem()).setRandomPrice(2, 12);
      this.shop.addBuyingItem("voidshard", new BuyingShopItem()).setRandomPrice(11, 21);
      this.shop.addBuyingItem("clay", new BuyingShopItem()).setRandomPrice(3, 12);
      this.shop.addBuyingItem("obsidian", new BuyingShopItem()).setRandomPrice(4, 12);
      this.shop.addBuyingItem("ectoplasm", new BuyingShopItem()).setRandomPrice(12, 22);
      this.shop.addBuyingItem("silk", new BuyingShopItem()).setRandomPrice(12, 22);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isLost);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isLost = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      this.updateAI();
   }

   @Override
   protected void setupPersonalities() {
      this.personalities = new ArrayList<>(Collections.singletonList(SettlerPersonalityRegistry.getNewSettlerPersonality("friendlywitch", this)));
   }

   public void updateAI() {
      this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(192, false, false, this.isLost ? 5000 : 25000), new AIMover(HumanMob.humanPathIterations));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 100; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, Particle.GType.CRITICAL)
            .movesConstant(GameRandom.globalRandom.getIntBetween(2, 36) * dir.x, GameRandom.globalRandom.getIntBetween(2, 36) * dir.y)
            .color(this.getParticleColor(GameRandom.globalRandom));
      }
   }

   protected Color getParticleColor(GameRandom random) {
      return new Color(random.getIntBetween(22, 77), random.getIntBetween(22, 77), random.getIntBetween(22, 77));
   }

   public void setLost(boolean isLost) {
      if (this.isLost != isLost) {
         this.isLost = isLost;
         this.updateTeam();
         this.updateAI();
      }
   }

   @Override
   public void updateTeam() {
      if (this.getLevel() != null && !this.isClient()) {
         if (this.isLost) {
            this.team.set(-1);
            this.owner.set(-1L);
         } else {
            super.updateTeam();
         }
      }
   }

   @Override
   public void makeSettler(ServerSettlementData data, LevelSettler settler) {
      if (this.isLost) {
         this.setLost(false);
         this.updateAI();
      }

      super.makeSettler(data, settler);
   }

   @Override
   public Predicate<Mob> filterHumanTargets() {
      Predicate<Mob> superPredicate = super.filterHumanTargets();
      return m -> {
         if (m.getStringID().equals("enchantedcrawlingzombie")) {
            int targetRegionID = m.getLevel().regionManager.getRegionIDByTile(m.getTileX(), m.getTileY());
            int myRegionID = this.getLevel().regionManager.getRegionIDByTile(this.getTileX(), this.getTileY());
            if (targetRegionID != myRegionID) {
               return false;
            }
         }

         return superPredicate.test(m);
      };
   }

   @Override
   public boolean shouldOnlyUseHumanLikeLook() {
      return false;
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions) {
      drawOptions.helmet(new InventoryItem("witchhat"));
      drawOptions.chestplate(new InventoryItem("witchrobe"));
      drawOptions.boots(new InventoryItem("witchshoes"));
   }

   @Override
   public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
      this.gender = HumanGender.FEMALE;
      super.randomizeLook(look, this.gender, random);
      look.setFacialFeature(0);
      look.setSkin(random.getOneOf(0, 1, 2, 3, 18));
      look.setHair(random.getOneOf(21, 28, 31, 33));
      look.setHairColor(random.getOneOf(2, 7, 9));
      look.setEyeColor(random.getOneOf(3, 9, 10));
      this.getRandomName(random);
   }

   @Override
   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      return this.getLocalMessages("friendlywitchtalk", 4);
   }

   @Override
   public int getRecruitedToSettlementUniqueID(ServerClient client) {
      return this.isLost ? 0 : super.getRecruitedToSettlementUniqueID(client);
   }

   @Override
   public GameMessage getRecruitError(ServerClient client) {
      return null;
   }

   @Override
   public List<InventoryItem> getRecruitItems(ServerClient client) {
      return null;
   }
}
