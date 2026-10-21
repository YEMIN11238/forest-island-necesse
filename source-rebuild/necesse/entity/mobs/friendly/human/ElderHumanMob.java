package necesse.entity.mobs.friendly.human;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import necesse.engine.dlc.DLC;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.packet.PacketQuestGiverRequest;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.JobTypeRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.seasons.GameSeasons;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.QuestMarkerOptions;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.gfx.GameHair;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ContainerQuest;
import necesse.inventory.container.mob.ElderContainer;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ConditionLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.ConsumeFoodLevelJob;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.light.GameLight;

public class ElderHumanMob extends HumanShop {
   private final int elderHairWeight = 140;
   public static LootTable lootTable = new LootTable(
      new ConditionLootItem("petrock", (random, objects) -> GameSeasons.isAprilFools()),
      new OneOfLootItems(new LootItem("elderhat"), new LootItem("eldershirt"), new LootItem("eldershoes"))
   );

   public ElderHumanMob() {
      super(500, 500, "elder");
      this.setSpeed(30.0F);
      this.attackCooldown = 500;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.canJoinAdventureParties = false;
      this.jobTypeHandler.getJobHandler(ConsumeFoodLevelJob.class).disabledBySettler = true;
      this.jobTypeHandler
         .getTypePriorities()
         .stream()
         .filter(p -> p.type.getID() != JobTypeRegistry.needsID && p.type.getID() != JobTypeRegistry.recreationID)
         .forEach(p -> p.disabledBySettler = true);
      this.equipmentInventory.setItem(6, new InventoryItem("coppersword"));
      this.shop
         .addSellingItem("divorcepapers", new SellingShopItem())
         .setRandomPrice(1000, 1500)
         .addRequirement((random, client, mob, blackboard) -> client.relationshipData.isInAnyRelationship());
      this.shop.addSellingItem("revivalpotion", new SellingShopItem()).setRandomPrice(300, 400);
      this.shop.addSellingItem("supporterhelmet", new SellingShopItem()).setStaticPrice(250, 250).addDLCRequirement(DLC.SUPPORTER_PACK);
      this.shop.addSellingItem("supporterchestplate", new SellingShopItem()).setStaticPrice(250, 250).addDLCRequirement(DLC.SUPPORTER_PACK);
      this.shop.addSellingItem("supporterboots", new SellingShopItem()).setStaticPrice(250, 250).addDLCRequirement(DLC.SUPPORTER_PACK);
      this.shop.addSellingItem("potionpouch", new SellingShopItem()).setRandomPrice(1000, 1200).addHasCompletedStoryObjectiveRequirement("exploringnecesse");
      this.shop.addSellingItem("lunchbox", new SellingShopItem()).setRandomPrice(400, 500).addHasCompletedStoryObjectiveRequirement("defeatevilsprotector");
      this.shop.addSellingItem("ammopouch", new SellingShopItem()).setRandomPrice(500, 600).addHasCompletedStoryObjectiveRequirement("defeatqueenspider");
      this.shop.addSellingItem("voidpouch", new SellingShopItem()).setRandomPrice(600, 800).addHasCompletedStoryObjectiveRequirement("defeatvoidwizard");
      this.shop
         .addSellingItem("recallflask", new SellingShopItem())
         .setRandomPrice(1000, 1200)
         .addHasCompletedStoryObjectiveRequirement("defeatancientvulture");
      this.shop.addSellingItem("shippingchest", new SellingShopItem()).setRandomPrice(1100, 1400).addHasCompletedStoryObjectiveRequirement("defeatchieftain");
      this.shop.addSellingItem("coinpouch", new SellingShopItem()).setRandomPrice(1200, 1500).addHasCompletedStoryObjectiveRequirement("defeatpiratecaptain");
      this.shop.addSellingItem("hoverboard", new SellingShopItem()).setRandomPrice(1500, 1800).addHasCompletedStoryObjectiveRequirement("defeatreaper");
      this.shop.addSellingItem("bannerstand", new SellingShopItem()).setRandomPrice(250, 350).addHasCompletedStoryObjectiveRequirement("defeatcryoqueen");
      this.shop
         .addSellingItem("missionboard", new SellingShopItem())
         .setRandomPrice(1500, 2000)
         .addHasCompletedStoryObjectiveRequirement("defeatthecursedcrone");
      this.shop.addSellingItem("portalflask", new SellingShopItem()).setRandomPrice(1600, 2400).addHasCompletedStoryObjectiveRequirement("defeatpestwarden");
      this.shop.addSellingItem("blinkscepter", new SellingShopItem()).setRandomPrice(1700, 2400).addHasCompletedStoryObjectiveRequirement("defeatsageandgrit");
      this.shop.addSellingItem("voidbag", new SellingShopItem()).setRandomPrice(1900, 2600).addHasCompletedStoryObjectiveRequirement("defeatfallenwizard");
      this.shop
         .addSellingItem("tabletbox", new SellingShopItem())
         .setRandomPrice(2200, 2800)
         .addHasCompletedStoryObjectiveRequirement("completetieroneincursion");
      this.shop
         .addSellingItem("wormholelocket", new SellingShopItem())
         .setRandomPrice(2400, 3000)
         .addRequirement(
            (random, client, mob, blackboard) -> client != null
               && client.playerMob.getInv().equipment.getTrinketSlotsSize() < 8
               && client.playerMob.getInv().getAmount(ItemRegistry.getItem("wormholelocket"), false, false, true, true, "have") == 0
         )
         .addHasCompletedStoryObjectiveRequirement("completetierfiveincursion");
      this.shop
         .addSellingItem("ringofascension", new SellingShopItem())
         .setRandomPrice(4000, 5000)
         .addHasCompletedStoryObjectiveRequirement("defeatascendedwizard");
      this.shop
         .addSellingItem("frogmask", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.FOREST_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("frogcostumeshirt", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.FOREST_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("frogcostumeboots", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.FOREST_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("constructionhammer", new SellingShopItem())
         .setRandomPrice(400, 600)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.FOREST_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("seedgun", new SellingShopItem())
         .setRandomPrice(1000, 1200)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.FOREST_DEEP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("seedpouch", new SellingShopItem())
         .setRandomPrice(800, 1000)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.FOREST_DEEP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("telescopicladder", new SellingShopItem())
         .setRandomPrice(400, 600)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.PLAINS_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("hoverboots", new SellingShopItem())
         .setRandomPrice(300, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.PLAINS_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("challengersbanner", new SellingShopItem())
         .setRandomPrice(2000, 2500)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.PLAINS_DEEP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("bannerofpeace", new SellingShopItem())
         .setRandomPrice(400, 600)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SNOW_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("horsemask", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SNOW_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("horsecostumeshirt", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SNOW_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("horsecostumeboots", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SNOW_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("minersprosthetic", new SellingShopItem())
         .setRandomPrice(800, 1200)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SNOW_DEEP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("chickenmask", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DUNGEON_CHALLENGES_ID);
      this.shop
         .addSellingItem("chickencostumeshirt", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DUNGEON_CHALLENGES_ID);
      this.shop
         .addSellingItem("chickencostumeboots", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DUNGEON_CHALLENGES_ID);
      this.shop
         .addSellingItem("itemattractor", new SellingShopItem())
         .setRandomPrice(400, 600)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SWAMP_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("infiniterope", new SellingShopItem())
         .setRandomPrice(800, 1200)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SWAMP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("infinitewaterbucket", new SellingShopItem())
         .setRandomPrice(2000, 2500)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.SWAMP_DEEP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("toolextender", new SellingShopItem())
         .setRandomPrice(400, 600)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DESERT_SURFACE_CHALLENGES_ID);
      this.shop
         .addSellingItem("alienmask", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DESERT_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("aliencostumeshirt", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DESERT_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("aliencostumeboots", new SellingShopItem())
         .setRandomPrice(200, 400)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DESERT_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("callofthesea", new SellingShopItem())
         .setRandomPrice(1600, 2000)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.DESERT_DEEP_CAVES_CHALLENGES_ID);
      this.shop
         .addSellingItem("bannerofwar", new SellingShopItem())
         .setRandomPrice(2500, 3000)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.TEMPLE_CHALLENGES_ID);
      this.shop
         .addSellingItem("teleportationstone", new SellingShopItem())
         .setRandomPrice(2000, 2500)
         .addJournalChallengeCompleteRequirement(JournalChallengeRegistry.PIRATE_VILLAGE_CHALLENGES_ID);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(480, true, true, 25000), new AIMover(humanPathIterations));
      if (this.isClient()) {
         this.getLevel().getClient().network.sendPacket(new PacketQuestGiverRequest(this.getUniqueID()));
      }
   }

   @Override
   public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
      this.gender = HumanGender.MALE;
      super.randomizeLook(look, this.gender, random);
      look.setFacialFeature(random.getOneOf(1, 3, 4));
      look.setHairColor(GameHair.getRandomHairColorAtSpecificWeight(random, 140));
      this.getRandomName(random);
   }

   @Override
   public LootTable getLootTable() {
      return new LootTable(lootTable, super.getLootTable());
   }

   @Override
   protected void setupPersonalities() {
      this.personalities = new ArrayList<>(Collections.singletonList(SettlerPersonalityRegistry.getNewSettlerPersonality("elder", this)));
   }

   @Override
   public void serverTickSettlerQualityOfLife() {
      this.settlerQualityOfLife = 0;
      this.settlerQualityOfLifeChangeBuffer = 0.0F;
   }

   @Override
   public HumanMob.StrikeChance wantsToGoOnStrikeChance() {
      return null;
   }

   @Override
   public int getSettlerQualityOfLife() {
      return 0;
   }

   @Override
   public int getSettlerHappiness() {
      return 100;
   }

   @Override
   public boolean shouldIgnoreQualityOfLifeAndHappiness() {
      return true;
   }

   @Override
   public boolean canSubmitNoBedNotification() {
      return false;
   }

   @Override
   public float getRegenFlat() {
      return this.adventureParty.isInAdventureParty() && !this.isSettlerWithinSettlement() ? super.getRegenFlat() : super.getRegenFlat() * 2.5F;
   }

   @Override
   public void tickHunger() {
      this.hungerLevel = 1.0F;
   }

   @Override
   public void addCommandFormHeader(ComponentListContainer<? super FormComponent> container, FormFlow flow, int maxWidth) {
      DialogueForm.addSettlerHeader(this, true, -1, maxWidth, container, flow);
   }

   @Override
   public SettlerThoughtsList getStaticThoughts() {
      return new SettlerThoughtsList();
   }

   @Override
   public void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (GameSeasons.isAprilFools() && !this.isAttacking) {
         if (this.objectUser != null && !this.objectUser.drawsUser()) {
            return;
         }

         if (!this.isVisible()) {
            return;
         }

         int drawX = camera.getDrawX(x) - 32;
         int drawY = camera.getDrawY(y) - 48;
         GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
         int dir = this.getDir();
         Point sprite = this.getAnimSprite(x, y, dir);
         final DrawOptions options = MobRegistry.Textures.cavelingElder
            .initDraw()
            .sprite(sprite.x, sprite.y, 64)
            .startGlowOptions(this, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX, drawY);
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      } else {
         super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      }
   }

   @Override
   public QuestMarkerOptions getMarkerOptions(PlayerMob perspective) {
      QuestMarkerOptions markerOptions = super.getMarkerOptions(perspective);
      StoryObjectiveManager storyManager = this.getStoryManager(perspective);
      if (storyManager != null && storyManager.hasUnclaimedObjectives()) {
         markerOptions = QuestMarkerOptions.combine(new QuestMarkerOptions('?', QuestMarkerOptions.orangeColor), markerOptions);
      }

      return markerOptions;
   }

   public StoryObjectiveManager getStoryManager(PlayerMob perspective) {
      if (perspective != null) {
         if (perspective.isClient()) {
            return perspective.getClient().storyManager;
         }

         if (perspective.isServer()) {
            ServerClient serverClient = perspective.getServerClient();
            if (serverClient != null) {
               return serverClient.storyManager;
            }
         }
      }

      return null;
   }

   @Override
   protected String getRandomName(GameRandom random) {
      return random.getChance(0.1F) ? getRandomName(random, elderNames) : super.getRandomName(random);
   }

   @Override
   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      return GameSeasons.isAprilFools() ? this.getLocalMessages("elderapriltalk", 5) : this.getLocalMessages("eldertalk", 6);
   }

   @Override
   public ArrayList<ContainerQuest> getQuests(ServerClient client) {
      return null;
   }

   @Override
   public boolean completeQuest(ServerClient client, int questUniqueID) {
      return false;
   }

   @Override
   public boolean skipQuest(ServerClient client, int questUniqueID) {
      return false;
   }

   @Override
   public PacketOpenContainer getOpenShopPacket(Server server, ServerClient client) {
      return ElderContainer.getElderContainerData(this, client).getPacket(ContainerRegistry.ELDER_CONTAINER, this);
   }
}
