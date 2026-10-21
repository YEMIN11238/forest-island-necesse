package necesse.level.maps.levelData.settlementData.settler.romancePersonalities;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketClientRelationshipData;
import necesse.engine.network.packet.PacketStartExpression;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.friendly.critters.flyingbugs.ButterflyMob;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceBreakUpSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerKissDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerReceiveGiftDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeAndLocationRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitLocationRomanceActivity;

public abstract class SettlerRomancePersonality implements IDDataContainer {
   public static int MIN_QOL_LEVEL_TO_DATE = 2;
   public static HashSet<String> POTIONS_PERSONALITY_CAN_GIFT = new HashSet<>();
   public static HashSet<String> GREATER_POTIONS_PERSONALITY_CAN_GIFT = new HashSet<>();
   public final IDData idData = new IDData();
   public final HumanMob mob;
   protected long isInRelationshipWithAuth = -1L;
   protected String relationshipClientName = null;
   protected HashMap<Long, PlayerRomanceManager> playerManagers = new HashMap<>();

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public SettlerRomancePersonality(HumanMob mob) {
      this.mob = mob;
   }

   public void onRegistered() {
   }

   public void addSaveData(SaveData save) {
      save.addLong("isInRelationshipWithAuth", this.isInRelationshipWithAuth);
      SaveData managersSave = new SaveData("managers");

      for (Entry<Long, PlayerRomanceManager> entry : this.playerManagers.entrySet()) {
         SaveData managerSave = new SaveData("");
         managerSave.addLong("clientAuth", entry.getKey());
         entry.getValue().addSaveData(managerSave);
         managersSave.addSaveData(managerSave);
      }

      if (!managersSave.isEmpty()) {
         save.addSaveData(managersSave);
      }
   }

   public void applyLoadData(LoadData save) {
      this.isInRelationshipWithAuth = save.getLong("isInRelationshipWithAuth", this.isInRelationshipWithAuth, false);
      LoadData managersSave = save.getFirstLoadDataByName("managers");
      if (managersSave != null) {
         for (LoadData managerSave : managersSave.getLoadData()) {
            long clientAuth = managerSave.getLong("clientAuth", -1L, true);
            if (clientAuth == -1L) {
               throw new LoadDataException("Player auth not found in romance manager save data.");
            }

            PlayerRomanceManager playerRomanceManager = new PlayerRomanceManager(this, clientAuth, managerSave);
            this.playerManagers.put(clientAuth, playerRomanceManager);
         }
      }
   }

   public void setupSpawnPacket(PacketWriter writer) {
      if (this.isInAnyRelationship()) {
         writer.putNextBoolean(true);
         writer.putNextLong(this.isInRelationshipWithAuth);
         if (this.relationshipClientName != null) {
            writer.putNextBoolean(true);
            writer.putNextString(this.relationshipClientName);
         } else {
            writer.putNextBoolean(false);
         }
      } else {
         writer.putNextBoolean(false);
      }
   }

   public void applySpawnPacket(PacketReader reader) {
      if (reader.getNextBoolean()) {
         this.isInRelationshipWithAuth = reader.getNextLong();
         if (reader.getNextBoolean()) {
            this.relationshipClientName = reader.getNextString();
         }
      } else {
         this.isInRelationshipWithAuth = -1L;
      }
   }

   public void init() {
      if (this.mob.isServer()) {
         this.updateRelationship(this.mob.getServer(), true);
      }
   }

   public String getInRelationshipName() {
      if (this.isInAnyRelationship()) {
         if (this.mob.isClient()) {
            ClientClient clientClient = this.mob.getClient().getClientByAuth(this.isInRelationshipWithAuth);
            if (clientClient != null) {
               return clientClient.getName();
            }
         } else if (this.mob.isServer()) {
            ServerClient serverClient = this.mob.getServer().getClientByAuth(this.isInRelationshipWithAuth);
            if (serverClient != null) {
               this.relationshipClientName = serverClient.getName();
            }
         }

         return this.relationshipClientName;
      } else {
         return null;
      }
   }

   public void updateRelationship(Server server, boolean fromInit) {
      if (this.isInAnyRelationship()) {
         ServerClient client = server.getClientByAuth(this.isInRelationshipWithAuth);
         if (client != null) {
            if (client.relationshipData.hasNeverBeenInRelationship()) {
               client.relationshipData.setIsInRelationshipWith(this.mob);
               client.getServer().network.sendToAllClients(new PacketClientRelationshipData(client));
               System.out
                  .println(
                     "Updated "
                        + client.getName()
                        + " relationship with "
                        + this.mob.getDisplayName()
                        + " ("
                        + this.mob.getUniqueID()
                        + ") because their client data was not set"
                  );
            } else if (!client.relationshipData.isInRelationshipWithSettler(this.mob.getUniqueID())) {
               System.out
                  .println(
                     "Cleared "
                        + this.mob.getDisplayName()
                        + " ("
                        + this.mob.getUniqueID()
                        + ") relationship status with "
                        + client.getName()
                        + " because they have broken up"
                  );
               this.playerManagers.remove(this.isInRelationshipWithAuth);
               this.isInRelationshipWithAuth = -1L;
            }
         }
      }

      if (this.isInAnyRelationship()) {
         this.relationshipClientName = server.getNameByAuth(this.isInRelationshipWithAuth, this.relationshipClientName);
      } else {
         this.relationshipClientName = null;
      }
   }

   public boolean isInAnyRelationship() {
      return this.isInRelationshipWithAuth != -1L;
   }

   public boolean isInRelationshipWith(long clientAuth) {
      return this.isInRelationshipWithAuth == clientAuth;
   }

   public boolean isInRelationshipWith(ServerClient client) {
      return this.isInRelationshipWith(client.authentication);
   }

   public PlayerRomanceManager getPlayerManager(ServerClient client, boolean createIfDoesNotExist) {
      return createIfDoesNotExist ? this.playerManagers.compute(client.authentication, (key, last) -> {
         if (last == null) {
            last = new PlayerRomanceManager(this, key);
         }

         return (PlayerRomanceManager)last;
      }) : this.playerManagers.get(client.authentication);
   }

   public void spawnSettlerHeartParticlesAndHappyExpression() {
      this.mob.startExpressionAbility.runAndSend(FormExpressionWheel.Expression.HAPPY, 8000);
      this.mob.spawnHeartParticlesAbility.runAndSend(this.mob, GameRandom.globalRandom.getIntBetween(2500, 3500));
   }

   public void setIsInRelationshipWith(ServerClient client) {
      if (!client.relationshipData.isInRelationshipWithSettler(this.mob.getUniqueID())) {
         this.isInRelationshipWithAuth = client.authentication;
         client.relationshipData.setIsInRelationshipWith(this.mob);
         client.getServer().network.sendToAllClients(new PacketClientRelationshipData(client));
         this.relationshipClientName = client.getName();
         this.spawnSettlerHeartParticlesAndHappyExpression();
         this.mob.updateRomancePersonalityAbility.runAndSend();
         if (client.achievementsLoaded()) {
            client.achievements().TRUE_LOVE.markCompleted(client);
         }

         PlayerRomanceManager manager = this.getPlayerManager(client, true);
         manager.startGiftCooldown(GameRandom.globalRandom.getIntBetween(1, 3));
      }
   }

   public void kissAndAddBuff(ServerClient client) {
      FormExpressionWheel.Expression expression = FormExpressionWheel.Expression.HAPPY;
      PlayerMob playerMob = client.playerMob;
      client.getServer().network.sendToClientsWithEntity(new PacketStartExpression(client.slot, expression), playerMob);
      this.mob.spawnHeartParticlesAbility.runAndSend(playerMob, GameRandom.globalRandom.getIntBetween(2500, 3500));
      playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.LOVED, playerMob, 1800.0F, null), true);
      this.spawnSettlerHeartParticlesAndHappyExpression();
   }

   public void endRelationship(ServerClient client) {
      if (this.isInRelationshipWithAuth == client.authentication) {
         if (client.playerMob.buffManager.hasBuff(BuffRegistry.LOVED)) {
            client.playerMob.buffManager.removeBuff(BuffRegistry.LOVED, true);
         }

         this.mob.startExpressionAbility.runAndSend(FormExpressionWheel.Expression.SAD, 5000);
         this.isInRelationshipWithAuth = -1L;
         client.relationshipData.clearRelationship();
         client.getServer().network.sendToAllClients(new PacketClientRelationshipData(client));
         this.relationshipClientName = null;
         this.mob.updateRomancePersonalityAbility.runAndSend();
         this.playerManagers.remove(client.authentication);
      }
   }

   public void debugClearProgress(ServerClient client) {
      if (this.isInRelationshipWith(client.authentication)) {
         this.isInRelationshipWithAuth = -1L;
         client.relationshipData.clearRelationship();
         client.getServer().network.sendToAllClients(new PacketClientRelationshipData(client));
         this.mob.updateRomancePersonalityAbility.runAndSend();
      }

      this.playerManagers.remove(client.authentication);
   }

   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      if (this.isInAnyRelationship()) {
         SettlerThoughtsList list = new SettlerThoughtsList();
         list.addThought("lifepartner");
         return list;
      } else {
         return null;
      }
   }

   public GameTooltips getDebugServerTooltips() {
      ListGameTooltips tooltips = new ListGameTooltips();
      if (!this.playerManagers.isEmpty()) {
         tooltips.add("romanceManagers:");

         for (Entry<Long, PlayerRomanceManager> entry : this.playerManagers.entrySet()) {
            String clientName = this.mob.getServer().getNameByAuth(entry.getKey(), "N/A");
            tooltips.add("    " + entry.getValue().getDebugServerTooltips(clientName));
         }
      }

      return tooltips;
   }

   public GameMessage getCurrentActivityMessage(ServerClient client, PlayerRomanceManager manager) {
      RomanceActivity currentActivity = manager.getCurrentRomanceActivity();
      if (currentActivity == null) {
         return null;
      }

      ArrayList<GameMessage> messages = currentActivity.getActivityMessage(client, manager);
      if (messages != null && !messages.isEmpty()) {
         GameMessageBuilder message = new GameMessageBuilder();

         for (int i = 0; i < messages.size(); i++) {
            message.append(messages.get(i));
            if (i != messages.size() - 1) {
               message.append("\n");
            }
         }

         return message;
      } else {
         return null;
      }
   }

   public ArrayList<GameMessage> getRomancePersonalityMessages(ServerClient client) {
      ArrayList<GameMessage> messages = new ArrayList<>();
      if (this.isInRelationshipWith(client)) {
         ArrayList<GameMessage> inRelationShipChatMessages = this.getInRelationShipChatMessages(client);
         if (inRelationShipChatMessages != null) {
            messages.addAll(inRelationShipChatMessages);
         }
      } else if (client.relationshipData.isInAnyRelationship()) {
         return messages;
      }

      PlayerRomanceManager manager = this.getPlayerManager(client, false);
      if (manager == null) {
         return messages;
      }

      if (manager.romanceLevel > 0) {
         ArrayList<GameMessage> statBasedMessages = this.getStatBasedMessages(client);
         if (statBasedMessages != null) {
            messages.addAll(statBasedMessages);
         }
      }

      return messages;
   }

   public ArrayList<GameMessage> getInRelationShipChatMessages(ServerClient client) {
      return new ArrayList<>();
   }

   public ArrayList<GameMessage> getInRelationShipNightChatMessages(ServerClient client) {
      return new ArrayList<>();
   }

   public ArrayList<GameMessage> getStatBasedMessages(ServerClient client) {
      return new ArrayList<>();
   }

   public static GameMessage messageBuilder(String category, String key, int count, Object... replacements) {
      GameMessageBuilder builder = new GameMessageBuilder();
      if (count <= 1) {
         return new LocalMessage(category, key, replacements);
      }

      for (int i = 0; i < count; i++) {
         builder.append(new LocalMessage(category, key + (i + 1), replacements));
         if (i != count - 1) {
            builder.append("\n");
         }
      }

      return builder;
   }

   public static GameMessage highlightedMessage(GameMessage message) {
      return new GameMessageBuilder().append("**").append(message).append("**");
   }

   public static GameMessage highlightedMessage(String message) {
      return new GameMessageBuilder().append("**").append(message).append("**");
   }

   public Collection<SettlerDialogue> getDialogues(ServerClient client, ServerSettlementData settlementData, boolean clientHasAccess) {
      ArrayList<SettlerDialogue> list = new ArrayList<>();
      if (!clientHasAccess) {
         return list;
      }

      PlayerRomanceManager manager = this.getPlayerManager(client, false);
      if (this.isInRelationshipWith(client)) {
         manager = manager == null ? this.getPlayerManager(client, true) : manager;
         list.add(new RomanceSettlerKissDialogue(this.mob));
         int divorcePapersID = ItemRegistry.getItemID("divorcepapers");
         boolean hasDivorcePapers = client.playerMob.getInv().hasAnyItem(false, false, false, false, "crafting", item -> item.item.getID() == divorcePapersID);
         if (hasDivorcePapers) {
            list.add(this.breakUpDialogue(client));
         }

         if (!manager.isGiftFromPartnerOnCooldown()) {
            list.add(this.receiveGiftDialogue(client));
         }
      } else if (!this.isInAnyRelationship()) {
         if (manager == null || client.relationshipData.isInAnyRelationship()) {
            if (this.mob.getSettlerQualityOfLife() >= MIN_QOL_LEVEL_TO_DATE) {
               int prettyFlowerID = ItemRegistry.getItemID("prettyflower");
               boolean hasPrettyFlower = client.playerMob
                  .getInv()
                  .hasAnyItem(false, false, false, false, "crafting", item -> item.item.getID() == prettyFlowerID);
               if (hasPrettyFlower) {
                  list.add(this.startDatingDialogue(client));
               }
            }
         } else if (!manager.isActivityOnCooldown() && manager.romanceLevel > 0) {
            SettlerDialogue activityDialogue = manager.getCurrentRomanceActivity().getDialogue(client);
            if (activityDialogue != null) {
               list.add(activityDialogue);
            }
         }
      }

      return list;
   }

   public ArrayList<RomanceActivity> generateRomanceActivities(PlayerRomanceManager manager) {
      return new ArrayList<>();
   }

   public GameMessage getDatingTooltipMessageBasedOnRomanceLevel(int romanceLevel) {
      return new LocalMessage("romance", "datingtooltip" + romanceLevel, "name", this.mob.getSettlerName());
   }

   public RomanceSettlerDialogue startDatingDialogue(ServerClient client) {
      return null;
   }

   public RomanceSettlerDialogue breakUpDialogue(ServerClient client) {
      return new RomanceBreakUpSettlerDialogue(this.mob, client);
   }

   public RomanceSettlerDialogue giveItemRomanceActivityDialogue(ServerClient client, String turnInItemStringID, int amount) {
      return null;
   }

   public RomanceSettlerDialogue craftAndGiveItemRomanceActivityDialogue(
      ServerClient client, String turnInItemStringID, int amount, String whereToCraftWorkStationStringID
   ) {
      return null;
   }

   public RomanceSettlerDialogue findAndGiveItemRomanceActivityDialogue(
      ServerClient client, String turnInItemStringID, int amount, GameMessage whereToFindObjectLocation
   ) {
      return null;
   }

   public RomanceSettlerDialogue killMobRomanceActivityDialogue(ServerClient client, String mobToKillStringID, GameMessage mobToKillLocationMessage) {
      return null;
   }

   public RomanceSettlerDialogue killMobAndVictimNameRomanceActivityDialogue(
      ServerClient client, String mobToKillStringID, GameMessage mobToKillLocationMessage, String victimDefeatedName
   ) {
      return null;
   }

   public RomanceSettlerDialogue captureAnimalRomanceActivityDialogue(ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocationMessage) {
      return null;
   }

   public RomanceSettlerDialogue captureCritterWithNetRomanceActivityDialogue(
      ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocation
   ) {
      return null;
   }

   public RomanceSettlerDialogue captureFireflyWithNetRomanceActivityDialogue(
      ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocation, FireflyMob.FireflyType fireflyType
   ) {
      return null;
   }

   public RomanceSettlerDialogue captureButterflyWithNetRomanceActivityDialogue(
      ServerClient client, String mobToCaptureStringID, GameMessage mobToCaptureLocation, ButterflyMob.SmallButterflyType butterflyType
   ) {
      return null;
   }

   public RomanceSettlerDialogue placeObjectRomanceActivityDialogue(ServerClient client, String objectToPlaceStringID) {
      return null;
   }

   public RomanceSettlerDialogue craftAndPlaceObjectRomanceActivityDialogue(
      ServerClient client, String objectToPlaceStringID, String whereToCraftWorkStationStringID
   ) {
      return null;
   }

   public RomanceSettlerDialogue findAndPlaceObjectRomanceActivityDialogue(
      ServerClient client, String objectToPlaceStringID, GameMessage whereToFindObjectLocation
   ) {
      return null;
   }

   public RomanceSettlerDialogue visitBiomeRomanceActivityDialogue(ServerClient client, String biomeStringID) {
      return null;
   }

   public RomanceSettlerDialogue visitBiomeAndLocationRomanceActivityDialogue(
      ServerClient client, String biomeStringID, VisitBiomeAndLocationRomanceActivity.SpecificLocation specificLocation
   ) {
      return null;
   }

   public RomanceSettlerDialogue visitLocationRomanceActivityDialogue(ServerClient client, VisitLocationRomanceActivity.SpecificLocation specificLocation) {
      return null;
   }

   public RomanceSettlerDialogue receiveGiftDialogue(ServerClient client) {
      TicketSystemList<String> giftsToChooseFrom = new TicketSystemList<>();
      int currentDay = this.mob.getWorldEntity().getDay();
      GameRandom random = new GameRandom(client.authentication).nextSeeded(this.mob.getUniqueID() + currentDay);
      String settlerName = this.mob.getSettlerName();
      int amount = random.getIntBetween(3, 12);
      String giftResponseStringID;
      if (random.getChance(0.5F)) {
         giftResponseStringID = "_giftpotion";
         if (client.characterStats().completed_incursions.getTotal() > 0) {
            for (String potions : GREATER_POTIONS_PERSONALITY_CAN_GIFT) {
               giftsToChooseFrom.addObject(100, potions);
            }
         } else {
            for (String potions : POTIONS_PERSONALITY_CAN_GIFT) {
               giftsToChooseFrom.addObject(100, potions);
            }
         }
      } else {
         giftResponseStringID = "_giftfood";
         if (client.characterStats().mob_kills.getKills("reaper") > 0) {
            for (Item gourmetFood : ItemCategory.foodQualityManager.getCategory("gourmet").getItems()) {
               giftsToChooseFrom.addObject(100, gourmetFood.getStringID());
            }
         } else {
            for (Item fineFood : ItemCategory.foodQualityManager.getCategory("fine").getItems()) {
               giftsToChooseFrom.addObject(100, fineFood.getStringID());
            }
         }
      }

      String randomGiftStringID = giftsToChooseFrom.getRandomObject(random);
      return new RomanceSettlerReceiveGiftDialogue(this.mob, randomGiftStringID, amount)
         .addMainTextSections(
            "romance",
            this.getStringID() + giftResponseStringID,
            3,
            "name",
            client.getName(),
            "settlername",
            settlerName,
            "item",
            TypeParsers.getItemAndNameParseString(randomGiftStringID, "**")
         )
         .setAcceptActivityButtonText(new LocalMessage("romance", this.getStringID() + giftResponseStringID + "accept", "settlername", settlerName));
   }

   public static void spawnHeartParticles(final Mob target, final int lifeTime) {
      int particleCount = 10;

      for (int i = 0; i < particleCount; i++) {
         target.getLevel()
            .entityManager
            .events
            .addHidden(
               new WaitForSecondsEvent(0.1F * i) {
                  @Override
                  public void onWaitOver() {
                     if (target.isSamePlace(this.level)) {
                        float posX = target.getX() + GameRandom.globalRandom.getIntBetween(-5, 5);
                        float posY = target.getY() + GameRandom.globalRandom.getIntBetween(-5, 5);
                        int startHeight = 8 + GameRandom.globalRandom.nextInt(24);
                        final int swing = GameRandom.globalRandom.getIntBetween(5, 10);
                        final float swingOffset = GameRandom.globalRandom.nextFloat();
                        float moveX = GameRandom.globalRandom.floatGaussian() * 2.0F;
                        float moveY = GameRandom.globalRandom.floatGaussian() * 1.5F;
                        final ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
                        this.getLevel()
                           .entityManager
                           .addParticle(posX, posY, Particle.GType.COSMETIC)
                           .sprite(GameResources.heartParticle)
                           .dontRotate()
                           .color(new Color(255, 255, 255))
                           .heightMoves(startHeight, startHeight + 40)
                           .moves(new ParticleOption.Mover() {
                              @Override
                              public void tick(Float pos, float delta, int lifeTimex, int timeAlive, float lifePercent) {
                                 frictionMover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
                                 float angle = (lifePercent + swingOffset) * 500.0F;
                                 pos.x = pos.x + GameMath.sin(angle) * swing * delta / 250.0F;
                              }
                           })
                           .lifeTime(lifeTime);
                     }
                  }
               }
            );
      }
   }

   static {
      POTIONS_PERSONALITY_CAN_GIFT.add("strengthpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("rangerpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("wisdompotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("minionpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("webpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("healthregenpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("manaregenpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("speedpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("attackspeedpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("accuracypotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("rapidpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("resistancepotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("knockbackpotion");
      POTIONS_PERSONALITY_CAN_GIFT.add("thornspotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterhealthpotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("superiorhealthpotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greatermanapotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("superiormanapotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterspeedpotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterhealthregenpotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterresistancepotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterbattlepotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterattackspeedpotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greatermanaregenpotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greateraccuracypotion");
      GREATER_POTIONS_PERSONALITY_CAN_GIFT.add("greaterrapidpotion");
   }
}
