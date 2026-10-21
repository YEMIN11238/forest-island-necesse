package necesse.level.maps.levelData.settlementData.settler.romancePersonalities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.playerStats.stats.MobKillsStat;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.friendly.critters.flyingbugs.ButterflyMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.TypeParsers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerEnterRelationshipDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerReceiveGiftDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerStartDatingDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureAnimalRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureButterflyWithNetRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CraftAndPlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.PlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitLocationRomanceActivity;

public class HonestFarmerRomancePersonality extends SettlerRomancePersonality {
   public static HashSet<String> PLACE_OBJECT_ACTIVITY_1_SEEDS = new HashSet<>();
   public static HashSet<String> PLACE_OBJECT_ACTIVITY_2_OBJECTS = new HashSet<>();
   public static HashMap<String, GameMessage> CAPTURE_ACTIVITY_3_ANIMALS_AND_LOCATIONS = new HashMap<>();
   public static HashSet<String> DECORATIONS_PERSONALITY_WILL_GIFT = new HashSet<>();

   public HonestFarmerRomancePersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public ArrayList<GameMessage> getInRelationShipChatMessages(ServerClient client) {
      ArrayList<GameMessage> chatMessages = new ArrayList<>();
      String playerName = client.getName();
      chatMessages.add(messageBuilder("romance", this.getStringID() + "1_relationshipchat", 2));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "2_relationshipchat", 2, "name", playerName));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "3_relationshipchat", 4, "name", playerName));
      return chatMessages;
   }

   @Override
   public ArrayList<GameMessage> getInRelationShipNightChatMessages(ServerClient client) {
      ArrayList<GameMessage> chatMessages = new ArrayList<>();
      chatMessages.add(messageBuilder("romance", this.getStringID() + "_relationshipnight", 3, "name", client.getName()));
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
               "romance", this.getStringID() + "_evilsprotector", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("evilsprotector", "**")
            )
         );
      }

      if (mobKills.getKills("queenspider") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance",
               this.getStringID() + "_queenspider",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString("queenspider", "**"),
               "cow",
               TypeParsers.getMobAndNameParseString("cow", "**")
            )
         );
      }

      if (mobKills.getKills("ancientvulture") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_ancientvulture", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("ancientvulture", "**")
            )
         );
      }

      if (mobKills.getKills("pestwarden") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_pestwarden", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("pestwarden", "**")
            )
         );
      }

      if (mobKills.getKills("ascendedwizard") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_ascendedwizard", 4, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("ascendedwizard", "**")
            )
         );
      } else if (mobKills.getKills("fallenwizard") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance",
               this.getStringID() + "_fallenwizard",
               4,
               "name",
               playerName,
               "mob1",
               TypeParsers.getMobAndNameParseString("voidwizard", "**"),
               "mob2",
               TypeParsers.getMobAndNameParseString("fallenwizard", "**")
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

      for (String decorations : DECORATIONS_PERSONALITY_WILL_GIFT) {
         giftsToChooseFrom.addObject(100, decorations);
      }

      String randomGiftStringID = giftsToChooseFrom.getRandomObject(random);
      return new RomanceSettlerReceiveGiftDialogue(this.mob, randomGiftStringID, 1)
         .addMainTextSections(
            "romance",
            this.getStringID() + "_giftdecoration",
            3,
            "name",
            client.getName(),
            "settlername",
            settlerName,
            "item",
            TypeParsers.getItemAndNameParseString(randomGiftStringID, "**")
         )
         .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_giftdecorationaccept", "settlername", settlerName));
   }

   @Override
   public ArrayList<RomanceActivity> generateRomanceActivities(PlayerRomanceManager manager) {
      ArrayList<RomanceActivity> romanceActivities = new ArrayList<>();
      GameRandom seededRandom = new GameRandom(manager.clientAuth).nextSeeded(this.mob.getUniqueID());
      TicketSystemList<RomanceActivity> level1Activities = new TicketSystemList<>();
      PLACE_OBJECT_ACTIVITY_1_SEEDS.forEach(seed -> level1Activities.addObject(100, new PlaceObjectRomanceActivity(manager, seed)));
      romanceActivities.add(level1Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level2Activities = new TicketSystemList<>();
      PLACE_OBJECT_ACTIVITY_2_OBJECTS.forEach(object -> {
         Item item = ItemRegistry.getItem(object);
         String workstationItemStringID = Recipes.getRecipesFromResult(item.getID()).get(0).tech.itemStringID;
         level2Activities.addObject(100, new CraftAndPlaceObjectRomanceActivity(manager, object, workstationItemStringID));
      });
      romanceActivities.add(level2Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level3Activities = new TicketSystemList<>();
      CAPTURE_ACTIVITY_3_ANIMALS_AND_LOCATIONS.forEach(
         (mobID, location) -> level3Activities.addObject(100, new CaptureAnimalRomanceActivity(manager, mobID, location))
      );
      romanceActivities.add(level3Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level4Activities = new TicketSystemList<>();
      level4Activities.addObject(
         100, new CaptureButterflyWithNetRomanceActivity(manager, "butterfly", new LocalMessage("journal", "snowcave"), ButterflyMob.SmallButterflyType.WHITE)
      );
      level4Activities.addObject(
         100,
         new CaptureButterflyWithNetRomanceActivity(manager, "butterfly", new LocalMessage("journal", "plainsdeepcave"), ButterflyMob.SmallButterflyType.PURPLE)
      );
      romanceActivities.add(level4Activities.getRandomObject(seededRandom));
      romanceActivities.add(new VisitLocationRomanceActivity(manager, VisitLocationRomanceActivity.SpecificLocation.VILLAGE));
      return romanceActivities;
   }

   @Override
   public RomanceSettlerDialogue startDatingDialogue(ServerClient client) {
      InventoryItem flower = new InventoryItem("prettyflower");
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      return new RomanceSettlerStartDatingDialogue(this.mob, client)
         .addMainTextSections(
            "romance",
            this.getStringID() + "_suggestdating",
            3,
            "name",
            playerName,
            "item",
            TypeParsers.getItemAndNameParseString(flower, "**"),
            "settlername",
            settlerName
         )
         .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinedatingbutton"))
         .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_starteddatingchat", 1, "name", playerName)
         .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_declineddatingchat", 1);
   }

   @Override
   public RomanceSettlerDialogue placeObjectRomanceActivityDialogue(ServerClient client, String objectToPlaceStringID) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      String translationID = this.getStringID() + "1";
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addSettlerChatsOnCompletingActivity("romance", translationID + "_objectplacedchat", 2, "name", playerName)
            .addMainTextSections("romance", translationID + "_objectplaced", 4, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_completeobjectplacedbutton"))
            .addSettlerChatsAfterAccepting("romance", translationID + "_completeobjectplacedchat", 2)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections(
               "romance", translationID + "_placeobject", 4, "name", playerName, "object", TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
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
               TypeParsers.getItemAndNameParseString("workstationduo", "**"),
               "farmland",
               TypeParsers.getItemAndNameParseString("farmland", "**"),
               "farmer",
               TypeParsers.getMobAndNameParseString("farmerhuman", "**"),
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
               TypeParsers.getItemAndNameParseString("workstationduo", "**"),
               "farmland",
               TypeParsers.getItemAndNameParseString("farmland", "**"),
               "name",
               playerName
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
   }

   @Override
   public RomanceSettlerDialogue craftAndPlaceObjectRomanceActivityDialogue(
      ServerClient client, String objectToPlaceStringID, String whereToCraftWorkStationStringID
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      String translationID = this.getStringID() + "2";
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addSettlerChatsOnCompletingActivity("romance", translationID + "_objectplacedchat", 2, "name", playerName)
            .addMainTextSections("romance", translationID + "_objectplaced", 3, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_completeobjectplacedbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", translationID + "_completeobjectplacedchat", 2, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections(
               "romance", translationID + "_placeobject", 4, "object", TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"), "name", playerName
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_acceptplaceobjectbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declineplaceobjectbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting(
               "romance",
               translationID + "_placeobjectacceptedchat",
               3,
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
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
                  "name",
                  playerName
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
   }

   @Override
   public RomanceSettlerDialogue captureAnimalRomanceActivityDialogue(ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocationMessage) {
      String settlerName = this.mob.getSettlerName();
      String playerName = client.getName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addSettlerChatOnCompletingActivity(new LocalMessage("romance", this.getStringID() + "_releasedanimalinsettlementchat", "name", playerName))
            .addMainTextSections(
               "romance",
               this.getStringID() + "_captureanimalcomplete",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString(mobToCaptureStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_captureanimalcompletebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_captureanimalcompletechat", 2, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections(
               "romance", this.getStringID() + "_captureanimal", 4, "mob", TypeParsers.getMobAndNameParseString(mobToCaptureStringID, "**"), "name", playerName
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptcaptureanimalbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinecaptureanimalbutton"))
            .addSettlerChatsAfterAccepting(
               "romance",
               this.getStringID() + "_captureanimalacceptedchat",
               3,
               "mob",
               TypeParsers.getMobAndNameParseString(mobToCaptureStringID, "**"),
               "location",
               highlightedMessage(mobToCaptureLocationMessage),
               "rope",
               TypeParsers.getItemAndNameParseString("rope", "**")
            )
            .addActivitiesMessageTip(
               "romance",
               this.getStringID() + "_captureanimaltip",
               3,
               "mob",
               TypeParsers.getMobAndNameParseString(mobToCaptureStringID, "**"),
               "location",
               highlightedMessage(mobToCaptureLocationMessage),
               "name",
               playerName,
               "rope",
               TypeParsers.getItemAndNameParseString("rope", "**")
            )
            .addSettlerChatsAtCustomMoment("romance", this.getStringID() + "_capturedcorrectanimalchat", 2, "name", playerName);
   }

   @Override
   public RomanceSettlerDialogue captureButterflyWithNetRomanceActivityDialogue(
      ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocation, ButterflyMob.SmallButterflyType butterflyType
   ) {
      String settlerName = this.mob.getSettlerName();
      String playerName = client.getName();
      String butterflyItemStringID = butterflyType.droppedItemStringID;
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addSettlerChatOnCompletingActivity(new LocalMessage("romance", this.getStringID() + "_capturedcorrectcritterchat", "name", playerName))
            .addMainTextSections(
               "romance",
               this.getStringID() + "_showcritter",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getItemAndNameParseString(butterflyItemStringID, "**"),
               "location",
               mobToCaptureLocation
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_completeshowcritter", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_showcrittercompletedchat", 3, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections(
               "romance",
               this.getStringID() + "_capturecritter",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getItemAndNameParseString(butterflyItemStringID, "**")
            )
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
               TypeParsers.getItemAndNameParseString(butterflyItemStringID, "**"),
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
               TypeParsers.getItemAndNameParseString(butterflyItemStringID, "**"),
               "net",
               TypeParsers.getItemAndNameParseString("net", "**"),
               "location",
               highlightedMessage(mobToCaptureLocation),
               "name",
               playerName
            )
            .addSettlerChatsAtCustomMoment(
               "romance",
               this.getStringID() + "_capturedwrongcritterchat",
               2,
               "name",
               playerName,
               "mob",
               TypeParsers.getItemAndNameParseString(butterflyItemStringID, "**"),
               "location",
               highlightedMessage(mobToCaptureLocation)
            );
   }

   @Override
   public RomanceSettlerDialogue visitLocationRomanceActivityDialogue(ServerClient client, VisitLocationRomanceActivity.SpecificLocation specificLocation) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerEnterRelationshipDialogue(this.mob, true)
            .addSettlerChatsOnCompletingActivity("romance", this.getStringID() + "_visitbiomesittingdownchat", 3, "name", playerName)
            .addMainTextSections("romance", this.getStringID() + "_visitbiomecompleted", 5, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompletedacceptbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompleteddeclinebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_visitbiomecompletedacceptedchat", 3, "name", playerName)
            .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_visitbiomecompleteddeclinedchat", 2, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections("romance", this.getStringID() + "_visitbiome", 4, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptvisitbiomebutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinevisitbiomebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting(
               "romance",
               this.getStringID() + "_visitbiomeacceptedchat",
               3,
               "name",
               playerName,
               "map",
               TypeParsers.getItemAndNameParseString("villagemap", "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString("demonicworkstationduo", "**")
            )
            .addActivitiesMessageTip(
               "romance",
               this.getStringID() + "_visitbiometip",
               4,
               "name",
               playerName,
               "map",
               TypeParsers.getItemAndNameParseString("villagemap", "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString("demonicworkstationduo", "**")
            )
            .addSettlerChatsAtCustomMoment("romance", this.getStringID() + "_visitbiomelocationfound", 3, "name", playerName);
   }

   static {
      PLACE_OBJECT_ACTIVITY_1_SEEDS.add("wheatseed");
      PLACE_OBJECT_ACTIVITY_1_SEEDS.add("cornseed");
      PLACE_OBJECT_ACTIVITY_1_SEEDS.add("tomatoseed");
      PLACE_OBJECT_ACTIVITY_1_SEEDS.add("cabbageseed");
      PLACE_OBJECT_ACTIVITY_1_SEEDS.add("chilipepperseed");
      PLACE_OBJECT_ACTIVITY_1_SEEDS.add("sugarbeetseed");
      PLACE_OBJECT_ACTIVITY_2_OBJECTS.add("cheesepress");
      PLACE_OBJECT_ACTIVITY_2_OBJECTS.add("grainmill");
      CAPTURE_ACTIVITY_3_ANIMALS_AND_LOCATIONS.put("bull", new LocalMessage("journal", "forestsurface"));
      CAPTURE_ACTIVITY_3_ANIMALS_AND_LOCATIONS.put("ram", new LocalMessage("journal", "forestsurface"));
      DECORATIONS_PERSONALITY_WILL_GIFT.add("tableclock");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("dogplush");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("dryadspiritcage");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("observantmask");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("unamusedmask");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("farseersorb");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("fishonastick");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("voidcube");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("relicofthedepths");
      DECORATIONS_PERSONALITY_WILL_GIFT.add("largeglobe");
   }
}
