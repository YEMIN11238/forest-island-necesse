package necesse.level.maps.levelData.settlementData.settler.romancePersonalities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.playerStats.stats.MobKillsStat;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.TypeParsers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerEnterRelationshipDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerGiveItemDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerReceiveGiftDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerStartDatingDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CraftAndGiveItemRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CraftAndPlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.KillMobRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeAndLocationRomanceActivity;

public class AspiringHeroRomancePersonality extends SettlerRomancePersonality {
   public static HashSet<String> GIVE_ITEM_ACTIVITY_1_SWORDS = new HashSet<>();
   public static HashMap<String, GameMessage> KILL_MOB_ACTIVITY_2_MOBS_AND_LOCATIONS = new HashMap<>();
   public static HashSet<String> PLACE_OBJECT_ACTIVITY_3_OBJECTS = new HashSet<>();
   public static HashMap<String, GameMessage> KILL_MOB_ACTIVITY_4_BOSSES_AND_LOCATIONS = new HashMap<>();
   public static HashSet<String> VISIT_BIOME_ACTIVITY_5_BIOMES = new HashSet<>();
   public static HashSet<String> SWORDS_PERSONALITY_WILL_GIFT = new HashSet<>();

   public AspiringHeroRomancePersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public ArrayList<GameMessage> getInRelationShipChatMessages(ServerClient client) {
      ArrayList<GameMessage> chatMessages = new ArrayList<>();
      chatMessages.add(messageBuilder("romance", this.getStringID() + "1_relationshipchat", 2, "name", client.getName()));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "2_relationshipchat", 2, "name", client.getName()));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "3_relationshipchat", 4, "name", client.getName()));
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
      if (mobKills.getKills("chieftain") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance",
               this.getStringID() + "_chieftain",
               4,
               "name",
               playerName,
               "mob1",
               TypeParsers.getMobAndNameParseString("chieftain", "**"),
               "mob2",
               TypeParsers.getMobAndNameParseString("thecursedcrone", "**")
            )
         );
      }

      if (mobKills.getKills("piratecaptain") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_piratecaptain", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("piratecaptain", "**")
            )
         );
      }

      if (mobKills.getKills("thecursedcrone") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_thecursedcrone", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("thecursedcrone", "**")
            )
         );
      }

      if (mobKills.getKills("sunlightchampion") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance",
               this.getStringID() + "_sunlightchampion",
               3,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString("sunlightchampion", "**")
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
      } else if (mobKills.getKills("voidwizard") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_voidwizard", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("voidwizard", "**")
            )
         );
      }

      return statsMessages;
   }

   @Override
   public RomanceSettlerDialogue receiveGiftDialogue(ServerClient client) {
      int currentDay = this.mob.getWorldEntity().getDay();
      GameRandom random = new GameRandom(client.authentication).nextSeeded(this.mob.getUniqueID() + currentDay);
      if (!random.getChance(0.75F)) {
         return super.receiveGiftDialogue(client);
      }

      TicketSystemList<String> giftsToChooseFrom = new TicketSystemList<>();
      String settlerName = this.mob.getSettlerName();

      for (String swords : SWORDS_PERSONALITY_WILL_GIFT) {
         giftsToChooseFrom.addObject(100, swords);
      }

      String randomGiftStringID = giftsToChooseFrom.getRandomObject(random);
      return new RomanceSettlerReceiveGiftDialogue(this.mob, randomGiftStringID, 1)
         .addMainTextSections(
            "romance",
            this.getStringID() + "_giftsword",
            3,
            "name",
            client.getName(),
            "settlername",
            settlerName,
            "item",
            TypeParsers.getItemAndNameParseString(randomGiftStringID, "**")
         )
         .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_giftswordaccept", "settlername", settlerName));
   }

   @Override
   public ArrayList<RomanceActivity> generateRomanceActivities(PlayerRomanceManager manager) {
      ArrayList<RomanceActivity> romanceActivities = new ArrayList<>();
      GameRandom seededRandom = new GameRandom(manager.clientAuth).nextSeeded(this.mob.getUniqueID());
      TicketSystemList<RomanceActivity> level1Activities = new TicketSystemList<>();
      GIVE_ITEM_ACTIVITY_1_SWORDS.forEach(object -> {
         Item item = ItemRegistry.getItem(object);
         String craftLocationID = Recipes.getRecipesFromResult(item.getID()).get(0).tech.itemStringID;
         level1Activities.addObject(100, new CraftAndGiveItemRomanceActivity(manager, object, 1, craftLocationID));
      });
      romanceActivities.add(level1Activities.getRandomObject(seededRandom));
      TicketSystemList<KillMobRomanceActivity> level2Activities = new TicketSystemList<>();
      KILL_MOB_ACTIVITY_2_MOBS_AND_LOCATIONS.forEach((boss, location) -> level2Activities.addObject(100, new KillMobRomanceActivity(manager, boss, location)));
      romanceActivities.add(level2Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level3Activities = new TicketSystemList<>();
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.forEach(object -> {
         Item item = ItemRegistry.getItem(object);
         String workstationItemStringID = Recipes.getRecipesFromResult(item.getID()).get(0).tech.itemStringID;
         level3Activities.addObject(100, new CraftAndPlaceObjectRomanceActivity(manager, object, workstationItemStringID));
      });
      romanceActivities.add(level3Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level4Activities = new TicketSystemList<>();
      KILL_MOB_ACTIVITY_4_BOSSES_AND_LOCATIONS.forEach((boss, location) -> level4Activities.addObject(100, new KillMobRomanceActivity(manager, boss, location)));
      romanceActivities.add(level4Activities.getRandomObject(seededRandom));
      TicketSystemList<RomanceActivity> level5Activities = new TicketSystemList<>();
      VISIT_BIOME_ACTIVITY_5_BIOMES.forEach(
         biome -> level5Activities.addObject(
            100, new VisitBiomeAndLocationRomanceActivity(manager, biome, VisitBiomeAndLocationRomanceActivity.SpecificLocation.NEAR_WATER)
         )
      );
      romanceActivities.add(level5Activities.getRandomObject(seededRandom));
      return romanceActivities;
   }

   @Override
   public RomanceSettlerDialogue startDatingDialogue(ServerClient client) {
      InventoryItem flower = new InventoryItem("prettyflower");
      String playerName = client.getName();
      return new RomanceSettlerStartDatingDialogue(this.mob, client)
         .addMainTextSections(
            "romance", this.getStringID() + "_suggestdating", 3, "name", playerName, "item", TypeParsers.getItemAndNameParseString(flower, "**")
         )
         .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinedatingbutton"))
         .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_starteddatingchat", 2, "name", playerName)
         .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_declineddatingchat", 1, "name", playerName);
   }

   @Override
   public RomanceSettlerDialogue craftAndGiveItemRomanceActivityDialogue(
      ServerClient client, String turnInItemStringID, int amount, String whereToCraftWorkStationStringID
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false)
            .addMainTextSections(
               "romance", this.getStringID() + "_itemgiven", 5, "name", playerName, "item", TypeParsers.getItemAndNameParseString(turnInItemStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_giveitemcompletedbutton"))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_giveitemcompletedchat", 2, "name", playerName)
         : new RomanceSettlerGiveItemDialogue(this.mob, turnInItemStringID, 1, true, true, 6)
            .addMainTextSections(
               "romance",
               this.getStringID() + "_giveitem",
               4,
               "name",
               playerName,
               "item",
               TypeParsers.getItemAndNameParseString(turnInItemStringID, "**"),
               "settlerclass",
               MobRegistry.getLocalization(this.mob.getID())
            )
            .setAcceptActivityButtonText(
               new LocalMessage(
                  "romance",
                  this.getStringID() + "_acceptgiveitembutton",
                  "settlername",
                  settlerName,
                  "item",
                  TypeParsers.getItemAndNameParseString(turnInItemStringID, "**")
               )
            )
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinegiveitembutton"))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_itemgivenchat", 1, "name", playerName)
            .addActivitiesMessageTip(
               "romance",
               this.getStringID() + "_giveitemtip",
               2,
               "name",
               playerName,
               "item",
               TypeParsers.getItemAndNameParseString(turnInItemStringID, "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**")
            );
   }

   @Override
   public RomanceSettlerDialogue killMobRomanceActivityDialogue(ServerClient client, String mobToKillStringID, GameMessage mobToKillLocationMessage) {
      String settlerName = this.mob.getSettlerName();
      String playerName = client.getName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      int romanceLevel = manager.romanceLevel;
      if (romanceLevel == 2) {
         String translationID = this.getStringID() + "1";
         return activityCompleted
            ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
               .addSettlerChatOnCompletingActivity(
                  new LocalMessage(
                     "romance",
                     translationID + "_mobkilledchat",
                     "mob",
                     TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                     "settlername",
                     settlerName
                  )
               )
               .addMainTextSections(
                  "romance", translationID + "_mobkilled", 3, "mob", TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"), "name", playerName
               )
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_completekilledmobbutton", "settlername", settlerName))
               .addSettlerChatsAfterAccepting("romance", translationID + "_completekilledmobchat", 2, "name", playerName)
            : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
               .addMainTextSections(
                  "romance",
                  translationID + "_killmob",
                  4,
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                  "name",
                  playerName,
                  "settlername",
                  settlerName,
                  "location",
                  highlightedMessage(mobToKillLocationMessage)
               )
               .setAcceptActivityButtonText(
                  new LocalMessage(
                     "romance",
                     translationID + "_acceptkillmobbutton",
                     "mob",
                     TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                     "settlername",
                     settlerName
                  )
               )
               .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declinekillmobbutton"))
               .addSettlerChatsAfterAccepting(
                  "romance",
                  translationID + "_killmobacceptedchat",
                  2,
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                  "location",
                  highlightedMessage(mobToKillLocationMessage)
               )
               .addActivitiesMessageTip(
                  "romance",
                  translationID + "_killmobtip",
                  3,
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                  "location",
                  highlightedMessage(mobToKillLocationMessage),
                  "name",
                  playerName
               );
      } else if (romanceLevel == 4) {
         String translationID = this.getStringID() + "2";
         return activityCompleted
            ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
               .addSettlerChatOnCompletingActivity(new LocalMessage("romance", translationID + "_mobkilledchat", "name", playerName))
               .addMainTextSections(
                  "romance",
                  translationID + "_mobkilled",
                  4,
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                  "name",
                  playerName,
                  "settlername",
                  settlerName
               )
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_completekilledmobbutton"))
               .addSettlerChatsAfterAccepting("romance", translationID + "_completekilledmobchat", 3, "name", playerName)
               .setCooldownInDaysBeforeNextActivity(1)
            : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
               .addMainTextSections(
                  "romance", translationID + "_killmob", 4, "mob", TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"), "name", playerName
               )
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_acceptkillmobbutton", "settlername", settlerName))
               .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declinekillmobbutton"))
               .addSettlerChatsAfterAccepting(
                  "romance",
                  translationID + "_killmobacceptedchat",
                  2,
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                  "location",
                  highlightedMessage(mobToKillLocationMessage),
                  "name",
                  playerName
               )
               .addActivitiesMessageTip(
                  "romance",
                  translationID + "_killmobtip",
                  2,
                  "mob",
                  TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"),
                  "location",
                  highlightedMessage(mobToKillLocationMessage),
                  "name",
                  playerName
               )
               .addSettlerChatAtCustomMoment(
                  new LocalMessage("romance", translationID + "_mobengagedchat", "mob", TypeParsers.getMobAndNameParseString(mobToKillStringID, "**"))
               );
      } else {
         System.out.println("No KillMobActivitiesDialogue was found for romance level: " + romanceLevel + " for personality: " + this.getStringID());
         return null;
      }
   }

   @Override
   public RomanceSettlerDialogue craftAndPlaceObjectRomanceActivityDialogue(
      ServerClient client, String objectToPlaceStringID, String whereToCraftWorkStationStringID
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      String previousMobKilledID = ((KillMobRomanceActivity)manager.romanceActivities.get(1)).mobToKillStringID;
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false, true)
            .addSettlerChatsOnCompletingActivity(
               "romance",
               this.getStringID() + "_objectplacedchat",
               3,
               "object",
               TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
               "settlername",
               settlerName
            )
            .addMainTextSections(
               "romance", this.getStringID() + "_objectplaced", 4, "object", TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_completeobjectplacedbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_completeobjectplacedchat", 3, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections(
               "romance",
               this.getStringID() + "_placeobject",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString(previousMobKilledID, "**"),
               "settlername",
               settlerName
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptplaceobjectbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declineplaceobjectbutton"))
            .addSettlerChatsAfterAccepting(
               "romance",
               this.getStringID() + "_placeobjectacceptedchat",
               3,
               "object",
               TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
               "name",
               playerName
            )
            .addSettlerChatsAtCustomMoment(
               "romance", this.getStringID() + "_objectcraftedchat", 2, "object", TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
            )
            .addActivitiesMessageTip(
               "romance",
               this.getStringID() + "_craftandplaceobjecttip",
               5,
               "object",
               TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**"),
               "name",
               playerName
            )
            .addSettlerChatOnDoingActivityWrong(
               new LocalMessage(
                  "romance",
                  this.getStringID() + "_objectplacedoutsidesettlementchat",
                  "name",
                  playerName,
                  "object",
                  TypeParsers.getItemAndNameParseString(objectToPlaceStringID, "**")
               )
            );
   }

   public void onObjectPlacedCustomEvent() {
      final String settlerName = this.mob.getSettlerName();
      final String romancePersonalityStringID = this.getStringID();
      int checkForMobsRange = 384;
      List<HumanMob> settlersFound = this.mob
         .getLevel()
         .entityManager
         .mobs
         .streamInRegionsInRange(this.mob.getX(), this.mob.getY(), checkForMobsRange)
         .filter(m -> m.isHuman && m.shouldSendSpawnPacket() && m != this.mob)
         .map(m -> (HumanMob)m)
         .filter(hm -> hm.isFriendlyHuman(this.mob))
         .filter(m -> m.getDistance(this.mob) <= checkForMobsRange)
         .collect(Collectors.toList());
      int currentCount = 1;

      for (final Mob settler : settlersFound) {
         if (currentCount < 4) {
            float timeBetweenTextResponsesInSeconds = 2.0F;
            final int countForMessage = currentCount;
            settler.getLevel()
               .entityManager
               .events
               .addHidden(
                  new WaitForSecondsEvent(timeBetweenTextResponsesInSeconds * currentCount) {
                     @Override
                     public void onWaitOver() {
                        GameMessage message = new LocalMessage(
                           "romance", romancePersonalityStringID + "_objectplacedchatresponses" + countForMessage, "settlername", settlerName
                        );
                        AspiringHeroRomancePersonality.this.mob
                           .getServer()
                           .network
                           .sendToClientsWithEntity(new PacketMobChat(settler.getUniqueID(), message), settler);
                     }
                  }
               );
            currentCount++;
         }
      }
   }

   @Override
   public RomanceSettlerDialogue visitBiomeAndLocationRomanceActivityDialogue(
      ServerClient client, String biomeStringID, VisitBiomeAndLocationRomanceActivity.SpecificLocation specificLocation
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      GameMessage location = BiomeRegistry.getBiome(biomeStringID).getLocalization();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      String previousMobKilledStringID = ((KillMobRomanceActivity)manager.romanceActivities.get(1)).mobToKillStringID;
      String previousBossKilledStringID = ((KillMobRomanceActivity)manager.romanceActivities.get(3)).mobToKillStringID;
      return activityCompleted
         ? new RomanceSettlerEnterRelationshipDialogue(this.mob, true)
            .addSettlerChatsOnCompletingActivity("romance", this.getStringID() + "_visitbiomesittingdownchat", 3, "name", playerName)
            .addMainTextSections(
               "romance",
               this.getStringID() + "_visitbiomecompleted",
               4,
               "name",
               playerName,
               "mob",
               TypeParsers.getMobAndNameParseString(previousMobKilledStringID, "**"),
               "bossmob",
               TypeParsers.getMobAndNameParseString(previousBossKilledStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompletedacceptbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompleteddeclinebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_visitbiomecompletedacceptedchat", 3, "name", playerName)
            .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_visitbiomecompleteddeclinedchat", 2, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections("romance", this.getStringID() + "_visitbiome", 4, "name", playerName, "location", highlightedMessage(location))
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptvisitbiomebutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinevisitbiomebutton"))
            .addSettlerChatsAfterAccepting(
               "romance", this.getStringID() + "_visitbiomeacceptedchat", 3, "location", highlightedMessage(location), "name", playerName
            )
            .addActivitiesMessageTip("romance", this.getStringID() + "_visitbiometip", 2, "location", highlightedMessage(location), "name", playerName)
            .addSettlerChatsAtCustomMoment("romance", this.getStringID() + "_visitbiomelocationfound", 2, "name", playerName);
   }

   static {
      GIVE_ITEM_ACTIVITY_1_SWORDS.add("goldsword");
      GIVE_ITEM_ACTIVITY_1_SWORDS.add("amethystsword");
      GIVE_ITEM_ACTIVITY_1_SWORDS.add("frostgreatsword");
      GIVE_ITEM_ACTIVITY_1_SWORDS.add("ivygreatsword");
      KILL_MOB_ACTIVITY_2_MOBS_AND_LOCATIONS.put("grizzlybear", new LocalMessage("journal", "forestcave"));
      KILL_MOB_ACTIVITY_2_MOBS_AND_LOCATIONS.put("trenchcoatgoblinstacked", new LocalMessage("journal", "forestcave"));
      KILL_MOB_ACTIVITY_2_MOBS_AND_LOCATIONS.put("blackcavespider", new LocalMessage("journal", "snowcave"));
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("hoodedknightstatue");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("mossymonkstatue");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("dinoplush");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("largeglobe");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("dogplush");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("brazier");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("bonetoilet");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("mapletoilet");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("largefrostbanner");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("largeeggcellentbanner");
      PLACE_OBJECT_ACTIVITY_3_OBJECTS.add("largepiratebanner");
      KILL_MOB_ACTIVITY_4_BOSSES_AND_LOCATIONS.put("pestwarden", new LocalMessage("journal", "swampdeepcave"));
      KILL_MOB_ACTIVITY_4_BOSSES_AND_LOCATIONS.put("reaper", new LocalMessage("journal", "forestdeepcave"));
      KILL_MOB_ACTIVITY_4_BOSSES_AND_LOCATIONS.put("cryoqueen", new LocalMessage("journal", "snowdeepcave"));
      KILL_MOB_ACTIVITY_4_BOSSES_AND_LOCATIONS.put("fallenwizard", new LocalMessage("journal", "temple"));
      VISIT_BIOME_ACTIVITY_5_BIOMES.add("plains");
      VISIT_BIOME_ACTIVITY_5_BIOMES.add("swamp");
      SWORDS_PERSONALITY_WILL_GIFT.add("cutlass");
      SWORDS_PERSONALITY_WILL_GIFT.add("barkblade");
      SWORDS_PERSONALITY_WILL_GIFT.add("agedchampionsword");
      SWORDS_PERSONALITY_WILL_GIFT.add("venomslasher");
   }
}
