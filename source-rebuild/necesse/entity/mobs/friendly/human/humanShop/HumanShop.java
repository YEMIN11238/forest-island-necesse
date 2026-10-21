package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.Packet;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanAngerTargetAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ContainerQuest;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.hudManager.floatText.MoodFloatText;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

public abstract class HumanShop extends HumanMob {
   public final ShopManager shop = new ShopManager();
   public static int QUALITY_OF_LIFE_PRICE_CAP = 4;

   public HumanShop(int nonSettlerHealth, int settlerHealth, String settlerStringID) {
      super(nonSettlerHealth, settlerHealth, settlerStringID);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      SaveData shopSave = new SaveData("SHOP");
      this.shop.addSaveData(shopSave);
      if (!shopSave.isEmpty()) {
         save.addSaveData(shopSave);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      LoadData shopSave = save.getFirstLoadDataByName("SHOP");
      if (shopSave != null) {
         this.shop.applyLoadData(shopSave);
      }
   }

   @Override
   public void init() {
      super.init();
      this.shop.init(this.getLevel(), this.getSettlerSeed());
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.shop.serverTick(this.getWorldEntity(), this.getServer());
   }

   public GameMessage getWorkInvNotificationMessage() {
      return null;
   }

   public List<InventoryItem> getFreeItems() {
      return this.workInventory;
   }

   public ShopManager getShop() {
      return this.isVisitor() && !this.isVisitorShop() ? null : this.shop;
   }

   public int getShopQualityOfLife() {
      return this.isSettler() ? super.getSettlerQualityOfLife() : QUALITY_OF_LIFE_PRICE_CAP / 2;
   }

   public int getShopHappiness() {
      return this.isSettler()
         ? super.getSettlerHappiness()
         : GameMath.lerp(0.5F, HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD, HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD);
   }

   public static float getHappinessPriceDiscountPercent(int qualityOfLifeLevel, int happinessLevel) {
      float happinessWithinQualityOfLife = GameMath.clamp(happinessLevel, HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD, HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD);
      happinessWithinQualityOfLife = GameMath.limit(happinessWithinQualityOfLife, 0.0F, 1.0F);
      happinessWithinQualityOfLife = GameMath.lerp(happinessWithinQualityOfLife, -0.25F, 0.25F);
      float qualityOfLifeFloat = qualityOfLifeLevel + happinessWithinQualityOfLife;
      float qualityOfLifePercent = GameMath.limit(qualityOfLifeFloat / QUALITY_OF_LIFE_PRICE_CAP, 0.0F, 1.0F);
      return GameMath.lerp(qualityOfLifePercent, 0.0F, 1.0F);
   }

   public static int getRandomHappinessPrice(GameRandom random, int qualityOfLifeLevel, int happinessLevel, int best, int worst, int range) {
      float discountPercent = getHappinessPriceDiscountPercent(qualityOfLifeLevel, happinessLevel);
      int totalRange = worst - best;
      range = GameMath.limit(range, 1, Math.abs(totalRange));
      if (totalRange < 0) {
         range = -range;
      }

      int added = (int)((1.0F - discountPercent) * (totalRange - range));
      int randomInt = range < 0 ? -random.nextInt(-range + 1) : random.nextInt(range + 1);
      return best + added + randomInt;
   }

   public static int getRandomHappinessMiddlePrice(
      GameRandom random, int qualityOfLifeLevel, int happinessLevel, int middlePrice, int rangeDivisor, int happinessDivisor
   ) {
      int fullRange = middlePrice / rangeDivisor;
      int range = fullRange / happinessDivisor;
      return getRandomHappinessPrice(random, qualityOfLifeLevel, happinessLevel, middlePrice - fullRange / 2, middlePrice + fullRange / 2, range);
   }

   public static void conditionSection(GameRandom random, boolean active, Consumer<GameRandom> ifActive, Consumer<GameRandom> ifInactive) {
      GameRandom nextRandom = random.nextSeeded();
      if (active) {
         if (ifActive != null) {
            ifActive.accept(nextRandom);
         }
      } else if (ifInactive != null) {
         ifInactive.accept(nextRandom);
      }
   }

   public static void conditionSection(GameRandom random, boolean active, Consumer<GameRandom> ifActive) {
      conditionSection(random, active, ifActive, null);
   }

   public ArrayList<SettlerDialogue> getDialogues(ServerClient client, ServerSettlementData settlementData, boolean clientHasAccess) {
      ArrayList<SettlerDialogue> list = new ArrayList<>();

      for (SettlerPersonality personality : this.personalities) {
         Collection<SettlerDialogue> personalityDialogues = personality.getDialogues(client, settlementData, clientHasAccess);
         if (personalityDialogues != null) {
            list.addAll(personalityDialogues);
         }
      }

      if (this.romancePersonality != null) {
         Collection<SettlerDialogue> romancePersonalityDialogues = this.romancePersonality.getDialogues(client, settlementData, clientHasAccess);
         if (romancePersonalityDialogues != null) {
            list.addAll(romancePersonalityDialogues);
         }
      }

      return list;
   }

   @Override
   public void interact(PlayerMob player) {
      super.interact(player);
      if (this.isServer() && player.isServerClient()) {
         this.updateHappiness();
         ServerClient client = player.getServerClient();
         GameMessage error = this.getInteractError(player);
         if (error == null) {
            this.romancePersonality.updateRelationship(client.getServer(), false);
            PacketOpenContainer openShopPacket = this.getOpenShopPacket(client.getServer(), client);
            if (openShopPacket != null) {
               ContainerRegistry.openAndSendContainer(client, openShopPacket);
            }
         } else {
            client.sendChatMessage(error);
         }
      }
   }

   public PacketOpenContainer getOpenShopPacket(Server server, ServerClient client) {
      return this.getShopContainerData(client).getPacket(ContainerRegistry.SHOP_CONTAINER, this);
   }

   public GameMessage getDialogueIntroMessage(ServerClient client) {
      ArrayList<MoodFloatText.Moods> moods = this.getMoods();
      return moods != null && !moods.isEmpty() ? GameRandom.globalRandom.getOneOf(moods).moodMessage : this.getRandomMessage(GameRandom.globalRandom, client);
   }

   public final GameMessage getBaseRecruitError(ServerClient client) {
      int settlementUniqueID = this.getRecruitedToSettlementUniqueID(client);
      if (settlementUniqueID != 0) {
         CachedSettlementData settlement = SettlementsWorldData.getSettlementsData(this).getCachedData(settlementUniqueID);
         if (settlement == null) {
            return new LocalMessage("ui", "settlementnotfound");
         }

         if (!settlement.hasAccess(client)) {
            return new LocalMessage("ui", this.isDowned() ? "settlerrevivenoperm" : "settlerrecruitnoperm");
         }
      }

      return null;
   }

   public GameMessage getRecruitError(ServerClient client) {
      return null;
   }

   public List<InventoryItem> getRecruitItems(ServerClient client) {
      return null;
   }

   public boolean startInRecruitForm(ServerClient client) {
      return this.isDowned();
   }

   public boolean isVisitorShop() {
      return false;
   }

   public ArrayList<ContainerQuest> getQuests(ServerClient client) {
      return null;
   }

   public boolean completeQuest(ServerClient client, int questUniqueID) {
      return false;
   }

   public boolean skipQuest(ServerClient client, int questUniqueID) {
      return false;
   }

   public ShopContainerData getShopContainerData(ServerClient client) {
      boolean isVisitorShop = this.isVisitorShop();
      boolean isVisitor = this.isVisitor();
      boolean isSettler = this.isSettler();
      boolean isDowned = this.downedState != null;
      ServerSettlementData settlementData = isSettler ? this.getSettlerSettlementServerData() : null;
      boolean hasAccess = settlementData != null && settlementData.networkData.doesClientHaveAccess(client);
      ArrayList<ContainerQuest> quests = !isDowned && !isVisitor ? this.getQuests(client) : null;
      GameMessage introMessage = this.isTrapped() ? this.getTrappedMessage(client) : this.getDialogueIntroMessage(client);
      GameMessage recruitError;
      if (isSettler) {
         recruitError = null;
      } else {
         recruitError = this.getBaseRecruitError(client);
         if (!this.isDowned() && (!isVisitor || isVisitorShop) && recruitError == null) {
            recruitError = this.getRecruitError(client);
         }
      }

      List<InventoryItem> recruitItems = null;
      if (recruitError == null) {
         if (this.isDowned()) {
            recruitItems = new ArrayList<>();
            recruitItems.add(new InventoryItem("revivalpotion", 1));
         } else if (!isSettler) {
            recruitItems = this.getRecruitItems(client);
         }
      }

      ShopManager shopManager = !isDowned && (!isVisitor || isVisitorShop) ? this.getShop() : null;
      ArrayList<SettlerDialogue> dialogues = this.getDialogues(client, settlementData, hasAccess);
      Packet content = ShopContainer.getContainerContent(
         this,
         client,
         settlementData,
         hasAccess,
         this.getWorkInvNotificationMessage(),
         this.getMissionFailedMessage(),
         introMessage,
         quests,
         shopManager,
         dialogues,
         recruitError,
         recruitItems,
         this.startInRecruitForm(client),
         this.getPossibleExpeditions(),
         this.workSettings
      );
      return new ShopContainerData(content, shopManager, dialogues);
   }

   public GameMessage getMissionFailedMessage() {
      return this.missionFailedMessage;
   }

   public GameMessage getInteractError(Mob mob) {
      if (this.buffManager.hasBuff(BuffRegistry.HUMAN_ANGRY)) {
         HumanAngerTargetAINode<?> humanAngerHandler = this.ai.blackboard.getObject(HumanAngerTargetAINode.class, "humanAngerHandler");
         if (humanAngerHandler != null && humanAngerHandler.enemies.contains(mob)) {
            return new LocalMessage("mobmsg", "angryshop");
         }
      }

      return this.getCurrentMission() != null ? new LocalMessage("mobmsg", "busyhuman", "name", this.getSettlerName()) : null;
   }
}
