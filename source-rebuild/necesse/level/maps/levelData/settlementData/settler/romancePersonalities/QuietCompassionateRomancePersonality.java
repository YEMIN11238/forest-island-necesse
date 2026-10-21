package necesse.level.maps.levelData.settlementData.settler.romancePersonalities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.playerStats.stats.MobKillsStat;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanGender;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerEnterRelationshipDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerReceiveGiftDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerStartDatingDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureFireflyWithNetRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CraftAndPlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.KillMobAndVictimNameRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.KillMobRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeAndLocationRomanceActivity;

public class QuietCompassionateRomancePersonality extends SettlerRomancePersonality {
   public static HashSet<String> PLACE_OBJECT_ACTIVITY_1_PLANTS = new HashSet<>();
   public static HashSet<String> PLACE_OBJECT_ACTIVITY_1_FLOWERS = new HashSet<>();
   public static HashMap<String, GameMessage> KILL_MOB_ACTIVITY_3_BOSSES_AND_LOCATIONS = new HashMap<>();
   public static HashSet<String> VISIT_BIOME_ACTIVITY_4_BIOMES = new HashSet<>();
   public static HashSet<String> PLACE_OBJECT_ACTIVITY_5_FIREFLY_JARS = new HashSet<>();
   public static HashSet<String> RANDOM_ITEMS_FOR_PERSONALITY_JOKE = new HashSet<>();

   public QuietCompassionateRomancePersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public ArrayList<GameMessage> getInRelationShipChatMessages(ServerClient client) {
      ArrayList<GameMessage> chatMessages = new ArrayList<>();
      String playerName = client.getName();
      chatMessages.add(messageBuilder("romance", this.getStringID() + "1_relationshipchat", 2, "name", playerName));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "2_relationshipchat", 2, "name", playerName));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "3_relationshipchat", 4));
      return chatMessages;
   }

   @Override
   public ArrayList<GameMessage> getInRelationShipNightChatMessages(ServerClient client) {
      ArrayList<GameMessage> chatMessages = new ArrayList<>();

      for (String itemStringID : RANDOM_ITEMS_FOR_PERSONALITY_JOKE) {
         chatMessages.add(
            messageBuilder(
               "romance",
               this.getStringID() + "_relationshipnight",
               3,
               "name",
               client.getName(),
               "item",
               ItemRegistry.getLocalization(itemStringID),
               "itemid",
               itemStringID
            )
         );
      }

      return chatMessages;
   }

   @Override
   public ArrayList<GameMessage> getStatBasedMessages(ServerClient client) {
      ArrayList<GameMessage> statsMessages = new ArrayList<>();
      PlayerStats characterStats = client.characterStats();
      MobKillsStat mobKills = characterStats.mob_kills;
      String playerName = client.getName();
      if (mobKills.getKills("evilsprotector") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_evilsprotector", 4, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("evilsprotector", "**")
            )
         );
      }

      if (mobKills.getKills("swampguardian") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_swampguardian", 4, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("swampguardian", "**")
            )
         );
      }

      if (mobKills.getKills("cryoqueen") > 0) {
         statsMessages.add(
            messageBuilder("romance", this.getStringID() + "_cryoqueen", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("cryoqueen", "**"))
         );
      }

      if (mobKills.getKills("crystaldragon") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_crystaldragon", 4, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("crystaldragon", "**")
            )
         );
      }

      if (mobKills.getKills("ascendedwizard") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance",
               this.getStringID() + "_ascendedwizard",
               4,
               "name",
               playerName,
               "mob1",
               TypeParsers.getMobAndNameParseString("fallenwizard", "**"),
               "mob2",
               TypeParsers.getMobAndNameParseString("ascendedwizard", "**")
            )
         );
      } else if (mobKills.getKills("fallenwizard") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_fallenwizard", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("fallenwizard", "**")
            )
         );
      }

      return statsMessages;
   }

   @Override
   public RomanceSettlerDialogue receiveGiftDialogue(ServerClient client) {
      int currentDay = this.mob.getWorldEntity().getDay();
      GameRandom random = new GameRandom(client.authentication).nextSeeded(this.mob.getUniqueID() + currentDay);
      if (!random.getChance(0.7F)) {
         return super.receiveGiftDialogue(client);
      }

      TicketSystemList<String> giftsToChooseFrom = new TicketSystemList<>();
      String settlerName = this.mob.getSettlerName();

      for (String flowers : PLACE_OBJECT_ACTIVITY_1_FLOWERS) {
         giftsToChooseFrom.addObject(100, flowers);
      }

      String randomGiftStringID = giftsToChooseFrom.getRandomObject(random);
      return new RomanceSettlerReceiveGiftDialogue(this.mob, randomGiftStringID, 1)
         .addMainTextSections(
            "romance",
            this.getStringID() + "_giftflower",
            3,
            "name",
            client.getName(),
            "settlername",
            settlerName,
            "item",
            TypeParsers.getItemAndNameParseString(randomGiftStringID, "**")
         )
         .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_giftfloweraccept", "settlername", settlerName));
   }

   @Override
   public ArrayList<RomanceActivity> generateRomanceActivities(PlayerRomanceManager manager) {
      ArrayList<RomanceActivity> romanceActivities = new ArrayList<>();
      GameRandom seededRandom = new GameRandom(manager.clientAuth).nextSeeded(this.mob.getUniqueID());
      TicketSystemList<RomanceActivity> level1Activities = new TicketSystemList<>();
      PLACE_OBJECT_ACTIVITY_1_PLANTS.forEach(object -> {
         Item item = ItemRegistry.getItem(object);
         String workstationItemStringID = Recipes.getRecipesFromResult(item.getID()).get(0).tech.itemStringID;
         level1Activities.addObject(100, new CraftAndPlaceObjectRomanceActivity(manager, object, workstationItemStringID));
      });
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.forEach(object -> {
         Item item = ItemRegistry.getItem(object);
         String workstationItemStringID = Recipes.getRecipesFromResult(item.getID()).get(0).tech.itemStringID;
         level1Activities.addObject(100, new CraftAndPlaceObjectRomanceActivity(manager, object, workstationItemStringID));
      });
      romanceActivities.add(level1Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level2Activities = new TicketSystemList<>();
      level2Activities.addObject(
         100, new CaptureFireflyWithNetRomanceActivity(manager, "firefly", new LocalMessage("level", "surface"), FireflyMob.FireflyType.YELLOW)
      );
      level2Activities.addObject(
         100, new CaptureFireflyWithNetRomanceActivity(manager, "firefly", new LocalMessage("level", "cave"), FireflyMob.FireflyType.GREEN)
      );
      level2Activities.addObject(
         100, new CaptureFireflyWithNetRomanceActivity(manager, "firefly", new LocalMessage("level", "deepcave"), FireflyMob.FireflyType.BLUE)
      );
      romanceActivities.add(level2Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level3Activities = new TicketSystemList<>();
      HumanGender genderOfFriendName = seededRandom.getOneOf(HumanGender.MALE, HumanGender.FEMALE);
      String nameOfFriend = HumanMob.getRandomName(seededRandom, genderOfFriendName);
      KILL_MOB_ACTIVITY_3_BOSSES_AND_LOCATIONS.forEach(
         (bossStringID, bossLocation) -> level3Activities.addObject(
            100, new KillMobAndVictimNameRomanceActivity(manager, bossStringID, bossLocation, nameOfFriend)
         )
      );
      romanceActivities.add(level3Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level4Activities = new TicketSystemList<>();
      VISIT_BIOME_ACTIVITY_4_BIOMES.forEach(
         biomeStringID -> level4Activities.addObject(
            100, new VisitBiomeAndLocationRomanceActivity(manager, biomeStringID, VisitBiomeAndLocationRomanceActivity.SpecificLocation.NEAR_WATER)
         )
      );
      romanceActivities.add(level4Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level5Activities = new TicketSystemList<>();
      PLACE_OBJECT_ACTIVITY_5_FIREFLY_JARS.forEach(objectStringID -> {
         Item item = ItemRegistry.getItem(objectStringID);
         String workstationItemStringID = Recipes.getRecipesFromResult(item.getID()).get(0).tech.itemStringID;
         level5Activities.addObject(100, new CraftAndPlaceObjectRomanceActivity(manager, objectStringID, workstationItemStringID));
      });
      romanceActivities.add(level5Activities.getRandomObject(seededRandom));
      return romanceActivities;
   }

   @Override
   public RomanceSettlerDialogue startDatingDialogue(ServerClient client) {
      String playerName = client.getName();
      return new RomanceSettlerStartDatingDialogue(this.mob, client)
         .addMainTextSections("romance", this.getStringID() + "_suggestdating", 2, "name", playerName)
         .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinedatingbutton"))
         .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_starteddatingchat", 1, "name", playerName)
         .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_declineddatingchat", 1)
         .setCooldownInDaysBeforeNextActivity(1);
   }

   @Override
   public RomanceSettlerDialogue craftAndPlaceObjectRomanceActivityDialogue(
      ServerClient client, String objectToPlaceStringID, String whereToCraftWorkStationStringID
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      int romanceLevel = manager.romanceLevel;
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      if (romanceLevel == 1) {
         String translationID = this.getStringID() + "1";
         return activityCompleted
            ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
               .addSettlerChatOnCompletingActivity(new LocalMessage("romance", translationID + "_objectplacedchat"))
               .addMainTextSections("romance", translationID + "_objectplaced", 4, "name", playerName)
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_completeobjectplacedbutton", "settlername", settlerName))
               .addSettlerChatsAfterAccepting("romance", translationID + "_completeobjectplacedchat", 2, "name", playerName)
            : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
               .addMainTextSections("romance", translationID + "_placeobject", 4, "name", playerName)
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_acceptplaceobjectbutton", "settlername", settlerName))
               .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declineplaceobjectbutton"))
               .addSettlerChatsAfterAccepting(
                  "romance",
                  translationID + "_placeobjectacceptedchat",
                  2,
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                  "workstation",
                  TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**")
               )
               .addActivitiesMessageTip(
                  "romance",
                  translationID + "_craftandplaceobjecttip",
                  3,
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                  "workstation",
                  TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**"),
                  "name",
                  playerName
               )
               .addSettlerChatAtCustomMoment(
                  new LocalMessage(
                     "romance",
                     translationID + "_objectcraftedchat",
                     "name",
                     playerName,
                     "object",
                     TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
                  )
               )
               .addSettlerChatOnDoingActivityWrong(
                  new LocalMessage(
                     "romance",
                     translationID + "_objectplacedoutsidesettlementchat",
                     "name",
                     playerName,
                     "object",
                     TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
                  )
               );
      } else if (romanceLevel == 5) {
         String friendName = ((KillMobAndVictimNameRomanceActivity)manager.romanceActivities.get(2)).getVictimDefeatedName();
         String mobWeKilledStringID = ((KillMobRomanceActivity)manager.romanceActivities.get(2)).mobToKillStringID;
         GameMessage fireFlyColor = this.getFireflyColor(objectToPlaceStringID);
         String translationID = this.getStringID() + "2";
         return activityCompleted
            ? new RomanceSettlerEnterRelationshipDialogue(this.mob, true)
               .addSettlerChatsOnCompletingActivity(
                  "romance", translationID + "_objectplacedchat", 4, "friendname", highlightedMessage(friendName), "name", playerName
               )
               .addMainTextSections(
                  "romance",
                  translationID + "_objectplaced",
                  4,
                  "name",
                  playerName,
                  "friendname",
                  highlightedMessage(friendName),
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobWeKilledStringID, "**")
               )
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_completeobjectplacedbutton", "settlername", settlerName))
               .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declineobjectplacedbutton", "settlername", settlerName))
               .addSettlerChatsAfterAccepting("romance", translationID + "_completeobjectplacedacceptedchat", 3, "name", playerName)
               .addSettlerChatsAfterDeclining("romance", translationID + "_completeobjectplaceddeclinedchat", 2, "name", playerName)
            : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
               .addMainTextSections(
                  "romance",
                  translationID + "_placeobject",
                  4,
                  "name",
                  playerName,
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                  "color",
                  fireFlyColor,
                  "friendname",
                  highlightedMessage(friendName)
               )
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_acceptplaceobjectbutton", "settlername", settlerName))
               .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declineplaceobjectbutton", "settlername", settlerName))
               .addSettlerChatsAfterAccepting(
                  "romance",
                  translationID + "_placeobjectacceptedchat",
                  4,
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                  "workstation",
                  TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**"),
                  "name",
                  playerName
               )
               .addActivitiesMessageTip(
                  "romance",
                  translationID + "_craftandplaceobjecttip",
                  3,
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                  "workstation",
                  TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**"),
                  "name",
                  playerName
               )
               .addSettlerChatAtCustomMoment(
                  new LocalMessage(
                     "romance",
                     translationID + "_objectcraftedchat",
                     "name",
                     playerName,
                     "object",
                     TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                     "friendname",
                     highlightedMessage(friendName)
                  )
               )
               .addSettlerChatOnDoingActivityWrong(
                  new LocalMessage(
                     "romance",
                     translationID + "_objectplacedoutsidesettlementchat",
                     "name",
                     playerName,
                     "object",
                     TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
                  )
               );
      } else {
         System.out
            .println("No CraftAndPlaceObjectActivitiesDialogue was found for romance level: " + romanceLevel + " for personality: " + this.getStringID());
         return null;
      }
   }

   private GameMessage getFireflyColor(String objectToCraftStringID) {
      GameMessage colorOfFirefly;
      if (objectToCraftStringID.contains("yellow")) {
         colorOfFirefly = new LocalMessage("misc", "coloryellow");
      } else if (objectToCraftStringID.contains("green")) {
         colorOfFirefly = new LocalMessage("misc", "colorgreen");
      } else if (objectToCraftStringID.contains("blue")) {
         colorOfFirefly = new LocalMessage("misc", "colorblue");
      } else {
         System.out
            .println(
               "Error in craftAndPlaceObjectRomanceActivityDialogue for "
                  + this.getStringID()
                  + ": object to craft string id does not contain a color. Defaulting to yellow for the dialogue."
            );
         colorOfFirefly = new LocalMessage("misc", "coloryellow");
      }

      return colorOfFirefly;
   }

   @Override
   public RomanceSettlerDialogue captureFireflyWithNetRomanceActivityDialogue(
      ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocation, FireflyMob.FireflyType fireflyType
   ) {
      String settlerName = this.mob.getSettlerName();
      String playerName = client.getName();
      String fireflyItemStringID = fireflyType.droppedItemStringID;
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addMainTextSections("romance", this.getStringID() + "_showcritter", 4, "name", playerName)
            .addSettlerChatsOnCompletingActivity(
               "romance", this.getStringID() + "_capturedcorrectcritterchat", 2, "mob", TypeParsers.getItemAndNameParseString(fireflyItemStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_completeshowcritter", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_showcrittercompletedchat", 2, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections("romance", this.getStringID() + "_capturecritter", 4, "name", playerName)
            .setAcceptActivityButtonText(
               new LocalMessage(
                  "romance",
                  this.getStringID() + "_acceptcapturecritterbutton",
                  "settlername",
                  settlerName,
                  "net",
                  TypeParsers.getItemAndNameParseString("net", "**")
               )
            )
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinecapturecritterbutton"))
            .addSettlerChatsAfterAccepting(
               "romance",
               this.getStringID() + "_capturecritteracceptedchat",
               2,
               "mob",
               TypeParsers.getItemAndNameParseString(fireflyItemStringID, "**"),
               "location",
               highlightedMessage(mobToCaptureLocation),
               "name",
               playerName
            )
            .addActivitiesMessageTip(
               "romance",
               this.getStringID() + "_capturecrittertip",
               3,
               "mob",
               TypeParsers.getItemAndNameParseString(fireflyItemStringID, "**"),
               "location",
               highlightedMessage(mobToCaptureLocation),
               "net",
               TypeParsers.getItemAndNameParseString("net", "**"),
               "name",
               playerName
            )
            .addSettlerChatAtCustomMoment(
               new LocalMessage(
                  "romance",
                  this.getStringID() + "_capturedwrongcritterchat",
                  "name",
                  playerName,
                  "mob",
                  TypeParsers.getItemAndNameParseString(fireflyItemStringID, "**"),
                  "location",
                  highlightedMessage(mobToCaptureLocation)
               )
            );
   }

   @Override
   public RomanceSettlerDialogue killMobAndVictimNameRomanceActivityDialogue(
      ServerClient client, String mobToKillStringID, GameMessage mobToKillLocationMessage, String victimDefeatedName
   ) {
      String settlerName = this.mob.getSettlerName();
      String playerName = client.getName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true, FormExpressionWheel.Expression.SAD)
            .addSettlerChatsOnCompletingActivity("romance", this.getStringID() + "_mobkilledchat", 1, "friendname", highlightedMessage(victimDefeatedName))
            .addMainTextSections("romance", this.getStringID() + "_mobkilled", 4, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_completekilledmobbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_completekilledmobchat", 2, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections("romance", this.getStringID() + "_killmob", 4, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptkillmobbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinekillmobbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting(
               "romance",
               this.getStringID() + "_killmobacceptedchat",
               2,
               "mob",
               TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
               "location",
               highlightedMessage(mobToKillLocationMessage)
            )
            .addActivitiesMessageTip(
               "romance",
               this.getStringID() + "_killmobtip",
               2,
               "mob",
               TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
               "location",
               highlightedMessage(mobToKillLocationMessage),
               "name",
               playerName
            );
   }

   @Override
   public RomanceSettlerDialogue visitBiomeAndLocationRomanceActivityDialogue(
      ServerClient client, String biomeStringID, VisitBiomeAndLocationRomanceActivity.SpecificLocation specificLocation
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      GameMessage location = BiomeRegistry.getBiome(biomeStringID).getLocalization();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      String previousBossKilledStringID = ((KillMobRomanceActivity)manager.romanceActivities.get(2)).mobToKillStringID;
      String friendName = ((KillMobAndVictimNameRomanceActivity)manager.romanceActivities.get(2)).getVictimDefeatedName();
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addSettlerChatsOnCompletingActivity("romance", this.getStringID() + "_visitbiomesittingdownchat", 3, "friendname", highlightedMessage(friendName))
            .addMainTextSections(
               "romance",
               this.getStringID() + "_visitbiomecompleted",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString(previousBossKilledStringID, "**"),
               "friendname",
               highlightedMessage(friendName)
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompletedacceptbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_visitbiomecompletedacceptedchat", 3, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections(
               "romance",
               this.getStringID() + "_visitbiome",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString(previousBossKilledStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptvisitbiomebutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinevisitbiomebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting(
               "romance", this.getStringID() + "_visitbiomeacceptedchat", 3, "location", highlightedMessage(location), "name", playerName
            )
            .addActivitiesMessageTip("romance", this.getStringID() + "_visitbiometip", 3, "location", highlightedMessage(location), "name", playerName)
            .addSettlerChatsAtCustomMoment("romance", this.getStringID() + "_visitbiomelocationfound", 2, "name", playerName);
   }

   static {
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant1");
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant2");
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant3");
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant4");
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant5");
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant6");
      PLACE_OBJECT_ACTIVITY_1_PLANTS.add("pottedplant7");
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.add("pottedflower1");
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.add("pottedflower2");
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.add("pottedflower3");
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.add("pottedflower4");
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.add("pottedflower5");
      PLACE_OBJECT_ACTIVITY_1_FLOWERS.add("pottedflower6");
      KILL_MOB_ACTIVITY_3_BOSSES_AND_LOCATIONS.put("piratecaptain", new LocalMessage("journal", "forestpiratevillage"));
      KILL_MOB_ACTIVITY_3_BOSSES_AND_LOCATIONS.put("reaper", new LocalMessage("journal", "forestdeepcave"));
      KILL_MOB_ACTIVITY_3_BOSSES_AND_LOCATIONS.put("voidwizard", new LocalMessage("journal", "dungeon"));
      VISIT_BIOME_ACTIVITY_4_BIOMES.add("snow");
      PLACE_OBJECT_ACTIVITY_5_FIREFLY_JARS.add("fireflyjaryellow");
      PLACE_OBJECT_ACTIVITY_5_FIREFLY_JARS.add("fireflyjargreen");
      PLACE_OBJECT_ACTIVITY_5_FIREFLY_JARS.add("fireflyjarblue");
      RANDOM_ITEMS_FOR_PERSONALITY_JOKE.add("dinoplush");
      RANDOM_ITEMS_FOR_PERSONALITY_JOKE.add("dogplush");
   }
}
