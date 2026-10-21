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
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.FindAndGiveItemRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.GiveItemRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeAndLocationRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeRomanceActivity;

public class WealthIsAllRomancePersonality extends SettlerRomancePersonality {
   public static HashMap<String, GameMessage> GIVE_ITEM_ACTIVITY_1_FLOWERS_AND_LOCATIONS = new HashMap<>();
   public static HashMap<String, GameMessage> GIVE_ITEM_ACTIVITY_2_GEMS_AND_LOCATIONS = new HashMap<>();
   public static HashSet<String> GIVE_ITEM_ACTIVITY_3_HELMETS = new HashSet<>();
   public static HashSet<String> VISIT_BIOME_ACTIVITY_5_BIOMES = new HashSet<>();

   public WealthIsAllRomancePersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public ArrayList<GameMessage> getInRelationShipChatMessages(ServerClient client) {
      ArrayList<GameMessage> chatMessages = new ArrayList<>();
      chatMessages.add(messageBuilder("romance", this.getStringID() + "1_relationshipchat", 3, "name", client.getName()));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "2_relationshipchat", 2, "name", client.getName()));
      chatMessages.add(messageBuilder("romance", this.getStringID() + "3_relationshipchat", 2, "name", client.getName()));
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
            messageBuilder("romance", this.getStringID() + "_chieftain", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("chieftain", "**"))
         );
      }

      if (mobKills.getKills("reaper") > 0) {
         statsMessages.add(
            messageBuilder("romance", this.getStringID() + "_reaper", 4, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("reaper", "**"))
         );
      }

      if (mobKills.getKills("sageandgrit") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_sageandgrit", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("sageandgrit", "**")
            )
         );
      }

      if (mobKills.getKills("motherslime") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_motherslime", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("motherslime", "**")
            )
         );
      }

      if (mobKills.getKills("ascendedwizard") > 0) {
         statsMessages.add(
            messageBuilder(
               "romance", this.getStringID() + "_ascendedwizard", 3, "name", playerName, "mob", TypeParsers.getMobAndNameParseString("ascendedwizard", "**")
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
      if (random.getChance(0.7F)) {
         String settlerName = this.mob.getSettlerName();
         int amount = random.getIntBetween(250, 750);
         InventoryItem coin = new InventoryItem("coin", amount);
         return new RomanceSettlerReceiveGiftDialogue(this.mob, coin.item.getStringID(), coin.getAmount())
            .addMainTextSections(
               "romance",
               this.getStringID() + "_giftcoins",
               3,
               "name",
               client.getName(),
               "settlername",
               settlerName,
               "item",
               TypeParsers.getItemAndNameParseString(coin, "**", true)
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_giftcoinsaccept", "settlername", settlerName));
      } else {
         return super.receiveGiftDialogue(client);
      }
   }

   @Override
   public ArrayList<RomanceActivity> generateRomanceActivities(PlayerRomanceManager manager) {
      ArrayList<RomanceActivity> romanceActivities = new ArrayList<>();
      GameRandom seededRandom = new GameRandom(manager.clientAuth).nextSeeded(this.mob.getUniqueID());
      TicketSystemList<GiveItemRomanceActivity> level1Activities = new TicketSystemList<>();
      GIVE_ITEM_ACTIVITY_1_FLOWERS_AND_LOCATIONS.forEach((flower, location) -> {
         int amountx = seededRandom.getIntBetween(19, 32);
         level1Activities.addObject(100, new FindAndGiveItemRomanceActivity(manager, flower, amountx, location));
      });
      romanceActivities.add(level1Activities.getRandomObject(seededRandom));
      TicketSystemList<GiveItemRomanceActivity> level2Activities = new TicketSystemList<>();
      GIVE_ITEM_ACTIVITY_2_GEMS_AND_LOCATIONS.forEach((gem, location) -> {
         int amountx = seededRandom.getIntBetween(15, 26);
         level2Activities.addObject(100, new FindAndGiveItemRomanceActivity(manager, gem, amountx, location));
      });
      romanceActivities.add(level2Activities.getRandomObject(seededRandom));
      TicketSystemList<GiveItemRomanceActivity> level3Activities = new TicketSystemList<>();

      for (String helmetStringID : GIVE_ITEM_ACTIVITY_3_HELMETS) {
         Item helmet = ItemRegistry.getItem(helmetStringID);
         String techStringID = Recipes.getRecipesFromResult(helmet.getID()).get(0).tech.itemStringID;
         level3Activities.addObject(100, new CraftAndGiveItemRomanceActivity(manager, helmetStringID, 1, techStringID));
      }

      romanceActivities.add(level3Activities.getRandomObject(seededRandom));
      TicketSystemList<GiveItemRomanceActivity> level4Activities = new TicketSystemList<>();
      int amount = 5000;
      level4Activities.addObject(100, new GiveItemRomanceActivity(manager, "coin", amount));
      romanceActivities.add(level4Activities.getRandomObject(seededRandom));
      TicketSystemList<VisitBiomeRomanceActivity> level5Activities = new TicketSystemList<>();
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
         .addMainTextSections("romance", this.getStringID() + "_suggestdating", 4, "item", TypeParsers.getItemAndNameParseString(flower, "**"))
         .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinedatingbutton"))
         .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_starteddatingchat", 1, "name", playerName)
         .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_declineddatingchat", 1);
   }

   @Override
   public RomanceSettlerDialogue findAndGiveItemRomanceActivityDialogue(
      ServerClient client, String turnInItemStringID, int amount, GameMessage whereToFindObjectLocation
   ) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      int romanceLevel = manager.romanceLevel;
      InventoryItem item = new InventoryItem(turnInItemStringID, amount);
      if (romanceLevel == 1) {
         String translationID = this.getStringID() + "1";
         return activityCompleted
            ? new RomanceSettlerDialogue(this.mob, activityInitialized, false)
               .addMainTextSections("romance", translationID + "_itemgiven", 4, "name", playerName)
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_giveitemcompletedbutton", "settlername", settlerName))
               .addSettlerChatsAfterAccepting("romance", translationID + "_giveitemcompletedchat", 2, "name", playerName)
            : new RomanceSettlerGiveItemDialogue(this.mob, turnInItemStringID, amount, true, true)
               .addMainTextSections(
                  "romance",
                  translationID + "_giveitem",
                  5,
                  "name",
                  playerName,
                  "item",
                  TypeParsers.getItemAndNameParseString(item, "**"),
                  "location",
                  highlightedMessage(whereToFindObjectLocation)
               )
               .setAcceptActivityButtonText(
                  new LocalMessage(
                     "romance", translationID + "_acceptgiveitembutton", "settlername", settlerName, "item", TypeParsers.getItemAndNameParseString(item, "**")
                  )
               )
               .setDeclineActivityButtonText(
                  new LocalMessage("romance", translationID + "_declinegiveitembutton", "item", TypeParsers.getItemAndNameParseString(item, "**"))
               )
               .addActivitiesMessageTip(
                  "romance",
                  translationID + "_giveitemtip",
                  2,
                  "name",
                  playerName,
                  "item",
                  TypeParsers.getItemAndNameParseString(item, "**"),
                  "location",
                  highlightedMessage(whereToFindObjectLocation)
               )
               .addSettlerChatsAfterAccepting(
                  "romance", translationID + "_itemgivenchat", 1, "name", playerName, "number1", amount - 2, "number2", amount - 1, "amount", amount
               );
      } else if (romanceLevel == 2) {
         String translationID = this.getStringID() + "2";
         return activityCompleted
            ? new RomanceSettlerDialogue(this.mob, activityInitialized, false)
               .addMainTextSections("romance", translationID + "_itemgiven", 4, "name", playerName, "item", TypeParsers.getItemAndNameParseString(item, "**"))
               .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_giveitemcompletedbutton", "settlername", settlerName))
               .addSettlerChatsAfterAccepting("romance", translationID + "_giveitemcompletedchat", 1, "name", playerName)
            : new RomanceSettlerGiveItemDialogue(this.mob, turnInItemStringID, amount, true, true)
               .addMainTextSections(
                  "romance",
                  translationID + "_giveitem",
                  5,
                  "name",
                  playerName,
                  "item",
                  TypeParsers.getItemAndNameParseString(item, "**"),
                  "location",
                  highlightedMessage(whereToFindObjectLocation)
               )
               .setAcceptActivityButtonText(
                  new LocalMessage(
                     "romance", translationID + "_acceptgiveitembutton", "settlername", settlerName, "item", TypeParsers.getItemAndNameParseString(item, "**")
                  )
               )
               .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declinegiveitembutton"))
               .addActivitiesMessageTip(
                  "romance",
                  translationID + "_giveitemtip",
                  3,
                  "name",
                  playerName,
                  "item",
                  TypeParsers.getItemAndNameParseString(item, "**"),
                  "location",
                  highlightedMessage(whereToFindObjectLocation)
               )
               .addSettlerChatsAfterAccepting("romance", translationID + "_itemgivenchat", 1);
      } else {
         System.out.println("No giveItemRomanceActivityDialogue was found at romance level: " + romanceLevel + " for personality: " + this.getStringID());
         return null;
      }
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
      String translationID = this.getStringID() + "3";
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false)
            .addMainTextSections(
               "romance", translationID + "_itemgiven", 4, "name", playerName, "item", TypeParsers.getItemAndNameParseString(turnInItemStringID, "**")
            )
            .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_giveitemcompletedbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", translationID + "_giveitemcompletedchat", 2, "name", playerName)
         : new RomanceSettlerGiveItemDialogue(this.mob, turnInItemStringID, amount, true, true, 0)
            .addMainTextSections(
               "romance",
               translationID + "_giveitem",
               5,
               "name",
               playerName,
               "item",
               TypeParsers.getItemAndNameParseString(turnInItemStringID, "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**")
            )
            .setAcceptActivityButtonText(
               new LocalMessage(
                  "romance",
                  translationID + "_acceptgiveitembutton",
                  "settlername",
                  settlerName,
                  "item",
                  TypeParsers.getItemAndNameParseString(turnInItemStringID, "**")
               )
            )
            .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declinegiveitembutton"))
            .addActivitiesMessageTip(
               "romance",
               translationID + "_giveitemtip",
               3,
               "name",
               playerName,
               "item",
               TypeParsers.getItemAndNameParseString(turnInItemStringID, "**"),
               "workstation",
               TypeParsers.getItemAndNameParseString(whereToCraftWorkStationStringID, "**")
            )
            .addSettlerChatsAfterAccepting("romance", translationID + "_itemgivenchat", 1);
   }

   @Override
   public RomanceSettlerDialogue giveItemRomanceActivityDialogue(ServerClient client, String turnInItemStringID, int amount) {
      String playerName = client.getName();
      String settlerName = this.mob.getSettlerName();
      InventoryItem item = new InventoryItem(turnInItemStringID, amount);
      PlayerRomanceManager manager = this.getPlayerManager(client, true);
      boolean activityInitialized = manager.getCurrentRomanceActivity().hasInitializedActivity;
      boolean activityCompleted = manager.getCurrentRomanceActivity().activityCompleted;
      String translationID = this.getStringID() + "4";
      return activityCompleted
         ? new RomanceSettlerDialogue(this.mob, activityInitialized, false)
            .addMainTextSections("romance", translationID + "_itemgiven", 4, "name", playerName, "item", TypeParsers.getItemAndNameParseString(item, "**"))
            .setAcceptActivityButtonText(new LocalMessage("romance", translationID + "_giveitemcompletedbutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", translationID + "_giveitemcompletedchat", 3, "name", playerName)
         : new RomanceSettlerGiveItemDialogue(this.mob, turnInItemStringID, amount, true, true)
            .addMainTextSections(
               "romance", translationID + "_giveitem", 4, "amount", amount, "name", playerName, "item", TypeParsers.getItemAndNameParseString(item, "**")
            )
            .setAcceptActivityButtonText(
               new LocalMessage(
                  "romance", translationID + "_acceptgiveitembutton", "settlername", settlerName, "item", TypeParsers.getItemAndNameParseString(item, "**")
               )
            )
            .setDeclineActivityButtonText(new LocalMessage("romance", translationID + "_declinegiveitembutton"))
            .addActivitiesMessageTip(
               "romance", translationID + "_giveitemtip", 2, "name", playerName, "item", TypeParsers.getItemAndNameParseString(turnInItemStringID, "**")
            )
            .addSettlerChatsAfterAccepting("romance", translationID + "_itemgivenchat", 1, "item", TypeParsers.getItemAndNameParseString(item, "**"));
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
      return activityCompleted
         ? new RomanceSettlerEnterRelationshipDialogue(this.mob, true)
            .addSettlerChatsOnCompletingActivity("romance", this.getStringID() + "_visitbiomesittingdownchat", 4, "name", playerName)
            .addMainTextSections("romance", this.getStringID() + "_visitbiomecompleted", 4, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompletedacceptbutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_visitbiomecompleteddeclinebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting("romance", this.getStringID() + "_visitbiomecompletedacceptedchat", 3, "name", playerName)
            .addSettlerChatsAfterDeclining("romance", this.getStringID() + "_visitbiomecompleteddeclinedchat", 3, "name", playerName)
         : new RomanceSettlerDialogue(this.mob, activityInitialized, true)
            .addMainTextSections("romance", this.getStringID() + "_visitbiome", 5, "name", playerName)
            .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + "_acceptvisitbiomebutton", "settlername", settlerName))
            .setDeclineActivityButtonText(new LocalMessage("romance", this.getStringID() + "_declinevisitbiomebutton", "settlername", settlerName))
            .addSettlerChatsAfterAccepting(
               "romance", this.getStringID() + "_visitbiomeacceptedchat", 3, "location", highlightedMessage(location), "name", playerName
            )
            .addActivitiesMessageTip("romance", this.getStringID() + "_visitbiometip", 3, "location", highlightedMessage(location), "name", playerName)
            .addSettlerChatsAtCustomMoment("romance", this.getStringID() + "_visitbiomelocationfound", 2, "name", playerName);
   }

   static {
      GIVE_ITEM_ACTIVITY_1_FLOWERS_AND_LOCATIONS.put("firemone", new LocalMessage("journal", "forestsurface"));
      GIVE_ITEM_ACTIVITY_1_FLOWERS_AND_LOCATIONS.put("sunflower", new LocalMessage("journal", "plainssurface"));
      GIVE_ITEM_ACTIVITY_1_FLOWERS_AND_LOCATIONS.put("iceblossom", new LocalMessage("journal", "snowsurface"));
      GIVE_ITEM_ACTIVITY_2_GEMS_AND_LOCATIONS.put("sapphire", new LocalMessage("journal", "forestcave"));
      GIVE_ITEM_ACTIVITY_2_GEMS_AND_LOCATIONS.put("amethyst", new LocalMessage("journal", "desertcave"));
      GIVE_ITEM_ACTIVITY_3_HELMETS.add("goldcrown");
      GIVE_ITEM_ACTIVITY_3_HELMETS.add("quartzcrown");
      GIVE_ITEM_ACTIVITY_3_HELMETS.add("runiccrown");
      GIVE_ITEM_ACTIVITY_3_HELMETS.add("runichat");
      GIVE_ITEM_ACTIVITY_3_HELMETS.add("soulseedcrown");
      GIVE_ITEM_ACTIVITY_3_HELMETS.add("ivycirclet");
      VISIT_BIOME_ACTIVITY_5_BIOMES.add("plains");
      VISIT_BIOME_ACTIVITY_5_BIOMES.add("desert");
   }
}
