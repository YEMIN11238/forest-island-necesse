package necesse.inventory.container.mob;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketShopContainerUpdate;
import necesse.engine.network.server.AdventureParty;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.quest.Quest;
import necesse.engine.quest.QuestManager;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldData.SettlersWorldData;
import necesse.entity.mobs.friendly.human.ExpeditionList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.friendly.human.humanShop.SellingShopItem;
import necesse.entity.mobs.friendly.human.humanShop.ShopContainerData;
import necesse.entity.mobs.friendly.human.humanShop.ShopManager;
import necesse.entity.mobs.friendly.human.humanShop.explorerMission.ExpeditionMission;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanWorkContainerHandler;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanWorkSetting;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanWorkSettingRegistry;
import necesse.gfx.GameResources;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.container.customAction.BooleanCustomAction;
import necesse.inventory.container.customAction.ContainerCustomAction;
import necesse.inventory.container.customAction.ContentCustomAction;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.customAction.EnumCustomAction;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.inventory.container.customAction.PointCustomAction;
import necesse.inventory.container.events.FullShopStockUpdateEvent;
import necesse.inventory.container.events.ShopContainerQuestUpdateEvent;
import necesse.inventory.container.events.ShopThoughtsUpdateEvent;
import necesse.inventory.container.events.ShopWealthUpdateEvent;
import necesse.inventory.container.events.SingleShopStockUpdateEvent;
import necesse.inventory.container.slots.PartyInventoryContainerSlot;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class ShopContainer extends MobContainer {
   public HumanShop humanShop;
   public boolean hasSettlerAccess;
   public boolean isSettlerOutsideSettlement;
   public boolean canJoinAdventureParties;
   public boolean isInAdventureParty;
   public boolean isInYourAdventureParty;
   public boolean isInPartyConfig;
   public final BooleanCustomAction setIsInPartyConfig;
   public int PARTY_SLOTS_START = -1;
   public int PARTY_SLOTS_END = -1;
   public int romanceLevel = -1;
   public final long priceSeed;
   public GameMessage workInvNotificationMessage;
   public GameMessage missionFailedMessage;
   public GameMessage introMessage;
   public GameMessage strikeMessage;
   public ArrayList<ContainerQuest> quests = new ArrayList<>();
   public ArrayList<SettlerDialogue> dialogues;
   public int settlerQualityOfLife;
   public int settlerHappiness;
   public ArrayList<NetworkSettlerThought> thoughts;
   public final EmptyCustomAction requestThoughts;
   public ShopManager serverShopManager;
   public int shopWealth;
   public LinkedHashMap<Integer, NetworkSellingShopItem> sellingItems;
   public LinkedHashMap<Integer, NetworkBuyingShopItem> buyingItems;
   public ArrayList<ShopContainer.ContainerWorkSetting> workSettings;
   public GameMessage recruitError;
   public ArrayList<InventoryItem> recruitItems;
   public final boolean startInRecruitment;
   public ArrayList<ExpeditionList> expeditionLists;
   public final EmptyCustomAction continueFailedMissionAction;
   public final EnumCustomAction<ShopContainer.WorkItemsAction> workItemsAction;
   public final IntCustomAction questTakeButton;
   public final IntCustomAction questCompleteButton;
   public final IntCustomAction questSkipButton;
   public final EmptyCustomAction acceptRecruitAction;
   public final EmptyCustomAction joinAdventurePartyAction;
   public final EmptyCustomAction leaveAdventurePartyAction;
   public final EmptyCustomAction returnToSettlementAction;
   public final ShopContainer.BuyItemAction buyItemAction;
   public final ShopContainer.SellItemAction sellItemAction;
   public final PointCustomAction buyExpeditionButton;
   private final ContentCustomAction workSettingAction;

   public ShopContainer(final NetworkClient client, int uniqueSeed, final HumanShop mob, Packet contentPacket, ShopContainerData serverData) {
      super(client, uniqueSeed, mob);
      this.humanShop = mob;
      client.playerMob.getInv().party.compressItems();
      PacketReader reader = new PacketReader(contentPacket);
      this.hasSettlerAccess = reader.getNextBoolean();
      this.isSettlerOutsideSettlement = reader.getNextBoolean();
      if (this.hasSettlerAccess) {
         new ShopContainerPartyUpdateEvent(reader).applyUpdate(this);
         AdventureParty party = null;
         if (client.isServer()) {
            party = client.getServerClient().adventureParty;
         } else if (client.isClient() && client.getClientClient().isLocalClient()) {
            party = client.getClientClient().getClient().adventureParty;
         }

         for (int i = 0; i < PlayerInventoryManager.MAX_PARTY_INVENTORY_SIZE; i++) {
            int index = this.addSlot(new PartyInventoryContainerSlot(client.playerMob.getInv().party, i, party));
            if (this.PARTY_SLOTS_START == -1) {
               this.PARTY_SLOTS_START = index;
            }

            if (this.PARTY_SLOTS_END == -1) {
               this.PARTY_SLOTS_END = index;
            }

            this.PARTY_SLOTS_START = Math.min(this.PARTY_SLOTS_START, index);
            this.PARTY_SLOTS_END = Math.max(this.PARTY_SLOTS_END, index);
         }

         this.addInventoryQuickTransfer(s -> this.isInPartyConfig, this.PARTY_SLOTS_START, this.PARTY_SLOTS_END);
      }

      if (reader.getNextBoolean()) {
         this.romanceLevel = reader.getNextShortUnsigned();
      }

      this.priceSeed = reader.getNextLong();
      if (reader.getNextBoolean()) {
         this.workInvNotificationMessage = GameMessage.fromPacket(reader);
      }

      if (reader.getNextBoolean()) {
         this.missionFailedMessage = GameMessage.fromPacket(reader);
      }

      this.introMessage = GameMessage.fromPacket(reader);
      if (serverData != null) {
         this.dialogues = serverData.dialogues;
         SettlerDialogue.skipReadDialoguesList(reader);
      } else {
         this.dialogues = SettlerDialogue.readDialoguesList(reader, mob);
      }

      if (this.hasSettlerAccess) {
         if (reader.getNextBoolean()) {
            this.strikeMessage = GameMessage.fromPacket(reader);
         }

         ShopThoughtsUpdateEvent thoughtsEvent = new ShopThoughtsUpdateEvent(reader);
         this.thoughts = thoughtsEvent.thoughts;
         this.settlerQualityOfLife = thoughtsEvent.qualityOfLifeLevel;
         this.settlerHappiness = GameMath.limit(thoughtsEvent.getTotalHappiness(mob), 0, 100);
         this.subscribeEvent(ShopThoughtsUpdateEvent.class, e -> true, () -> true);
         this.onEvent(ShopThoughtsUpdateEvent.class, e -> {
            this.thoughts = e.thoughts;
            this.settlerQualityOfLife = thoughtsEvent.qualityOfLifeLevel;
            this.settlerHappiness = GameMath.limit(e.getTotalHappiness(mob), 0, 100);
         });
      } else {
         this.settlerQualityOfLife = mob.getShopQualityOfLife();
         this.settlerHappiness = mob.getShopHappiness();
      }

      this.requestThoughts = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (mob.isServer() && ShopContainer.this.hasSettlerAccess) {
               new ShopThoughtsUpdateEvent(mob).applyAndSendToClient(client.getServerClient());
            }
         }
      });
      this.quests = this.replaceContainerQuestsWithManager((new ShopContainerQuestUpdateEvent(reader)).quests);
      this.serverShopManager = serverData == null ? null : serverData.shopManager;
      this.subscribeEvent(ShopWealthUpdateEvent.class, e -> this.serverShopManager != null && e.managerUniqueID == this.serverShopManager.uniqueID, () -> true);
      this.onEvent(ShopWealthUpdateEvent.class, e -> this.shopWealth = e.shopWealth);
      this.shopWealth = reader.getNextInt();
      if (reader.getNextBoolean()) {
         if (client.isServer() && this.serverShopManager == null) {
            throw new IllegalArgumentException("Servers shop registry cannot be null");
         }

         this.subscribeEvent(FullShopStockUpdateEvent.class, e -> e.managerUniqueID == this.serverShopManager.uniqueID, () -> true);
         this.onEvent(FullShopStockUpdateEvent.class, e -> {
            this.shopWealth = e.shopWealth;

            for (Entry<Integer, Integer> entry : e.shopItemIDsStock.entrySet()) {
               NetworkSellingShopItem item = this.sellingItems.get(entry.getKey());
               if (item != null) {
                  item.currentStock = entry.getValue();
               }
            }
         });
         this.subscribeEvent(
            SingleShopStockUpdateEvent.class,
            e -> e.managerUniqueID == this.serverShopManager.uniqueID && this.sellingItems.containsKey(e.shopItemID),
            () -> true
         );
         this.onEvent(SingleShopStockUpdateEvent.class, e -> {
            NetworkSellingShopItem item = this.sellingItems.get(e.shopItemID);
            if (item != null) {
               item.currentStock = e.currentStock;
            }

            if (e.shopWealth >= 0) {
               this.shopWealth = e.shopWealth;
            }
         });
         int totalItems = reader.getNextShortUnsigned();
         this.sellingItems = new LinkedHashMap<>(totalItems);

         for (int i = 0; i < totalItems; i++) {
            NetworkSellingShopItem item = new NetworkSellingShopItem(reader);
            this.sellingItems.put(item.shopItemID, item);
         }
      } else {
         this.sellingItems = null;
      }

      if (reader.getNextBoolean()) {
         if (client.isServer() && this.serverShopManager == null) {
            throw new IllegalArgumentException("Servers shop registry cannot be null");
         }

         int totalItems = reader.getNextShortUnsigned();
         this.buyingItems = new LinkedHashMap<>(totalItems);

         for (int i = 0; i < totalItems; i++) {
            NetworkBuyingShopItem item = new NetworkBuyingShopItem(reader);
            if (item.item != null) {
               this.buyingItems.put(item.shopItemID, item);
            }
         }
      } else {
         this.buyingItems = null;
      }

      if (reader.getNextBoolean()) {
         this.recruitError = GameMessage.fromContentPacket(reader.getNextContentPacket());
      } else if (reader.getNextBoolean()) {
         int totalRecruitItems = reader.getNextShortUnsigned();
         this.recruitItems = new ArrayList<>(totalRecruitItems);

         for (int i = 0; i < totalRecruitItems; i++) {
            this.recruitItems.add(InventoryItem.fromContentPacket(reader));
         }
      }

      this.startInRecruitment = reader.getNextBoolean();
      int expeditionsListLength = reader.getNextShortUnsigned();
      this.expeditionLists = new ArrayList<>(expeditionsListLength);

      for (int i = 0; i < expeditionsListLength; i++) {
         this.expeditionLists.add(new ExpeditionList(reader));
      }

      this.workSettings = new ArrayList<>();

      for (HumanWorkSetting<?> setting : mob.workSettings.getSettings()) {
         this.workSettings.add(new ShopContainer.ContainerWorkSetting<>(setting, setting.readContainer(this, reader)));
      }

      this.continueFailedMissionAction = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (client.isServer()) {
               ShopContainer.this.humanShop.clearMissionResult();
            }
         }
      });
      this.workItemsAction = this.registerAction(new EnumCustomAction<ShopContainer.WorkItemsAction>(ShopContainer.WorkItemsAction.class) {
         protected void run(ShopContainer.WorkItemsAction value) {
            ShopContainer.this.handleWorkItemsAction(value);
         }
      });
      this.subscribeEvent(ShopContainerInventoryClaimedEvent.class, e -> this.humanShop.getUniqueID() == e.mobUniqueID, () -> true);
      this.subscribeEvent(ShopContainerQuestUpdateEvent.class, e -> true, () -> true);
      this.onEvent(ShopContainerQuestUpdateEvent.class, e -> this.quests = this.replaceContainerQuestsWithManager(e.quests));
      this.questTakeButton = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            if (client.isServer()) {
               ContainerQuest cq = ShopContainer.this.quests.stream().filter(q -> q.quest != null && q.quest.getUniqueID() == value).findFirst().orElse(null);
               if (cq != null && cq.quest != null) {
                  ServerClient serverClient = client.getServerClient();
                  cq.quest.makeActiveFor(serverClient.getServer(), serverClient);
               }
            }
         }
      });
      this.questCompleteButton = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            if (client.isServer()) {
               ContainerQuest cq = ShopContainer.this.quests.stream().filter(q -> q.quest != null && q.quest.getUniqueID() == value).findFirst().orElse(null);
               if (cq != null && cq.quest != null) {
                  ServerClient serverClient = client.getServerClient();
                  if (cq.quest.canComplete(client)) {
                     ShopContainer.this.humanShop.completeQuest(serverClient, value);
                     new ShopContainerQuestUpdateEvent(ShopContainer.this.humanShop.getQuests(serverClient)).applyAndSendToClient(serverClient);
                  }
               }
            }
         }
      });
      this.questSkipButton = this.registerAction(new IntCustomAction() {
         @Override
         protected void run(int value) {
            if (client.isServer()) {
               ContainerQuest cq = ShopContainer.this.quests.stream().filter(q -> q.quest != null && q.quest.getUniqueID() == value).findFirst().orElse(null);
               if (cq != null && cq.quest != null && cq.canSkip && cq.skipError == null) {
                  ServerClient serverClient = client.getServerClient();
                  if (!ShopContainer.this.humanShop.skipQuest(serverClient, value)) {
                     new ShopContainerQuestUpdateEvent(ShopContainer.this.humanShop.getQuests(serverClient)).applyAndSendToClient(serverClient);
                  }
               }
            }
         }
      });
      this.subscribeEvent(ShopContainerPartyUpdateEvent.class, update -> this.hasSettlerAccess && update.mobUniqueID == mob.getUniqueID(), () -> true);
      this.onEvent(ShopContainerPartyUpdateEvent.class, event -> event.applyUpdate(this));
      this.subscribeEvent(ShopContainerPartyResponseEvent.class, update -> true, () -> true);
      this.joinAdventurePartyAction = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (ShopContainer.this.hasSettlerAccess && ShopContainer.this.canJoinAdventureParties && client.isServer()) {
               GameMessage message = ShopContainer.this.humanShop.willJoinAdventureParty(client.getServerClient());
               new ShopContainerPartyResponseEvent(message).applyAndSendToClient(client.getServerClient());
               if (message == null) {
                  mob.commandFollow(client.getServerClient(), client.playerMob);
                  mob.adventureParty.set(client.getServerClient());
               }
            }
         }
      });
      this.leaveAdventurePartyAction = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (ShopContainer.this.hasSettlerAccess && client.isServer() && mob.adventureParty.getServerClient() == client) {
               mob.adventureParty.clear(true);
               if (mob.isSettlerWithinSettlement()) {
                  mob.clearCommandsOrders(client.getServerClient());
               } else if (mob.getHealthPercent() <= 0.5F) {
                  SettlersWorldData settlersData = SettlersWorldData.getSettlersData(mob.getLevel().getServer());
                  settlersData.returnToSettlement(mob, false);
               }

               ShopContainer.this.close();
            }
         }
      });
      this.returnToSettlementAction = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (ShopContainer.this.hasSettlerAccess && client.isServer() && ShopContainer.this.isSettlerOutsideSettlement) {
               if (mob.adventureParty.isInAdventureParty()) {
                  mob.adventureParty.clear(false);
               }

               SettlersWorldData settlersData = SettlersWorldData.getSettlersData(mob.getLevel().getServer());
               settlersData.returnToSettlement(mob, false);
               ShopContainer.this.close();
            }
         }
      });
      this.acceptRecruitAction = this.registerAction(
         new EmptyCustomAction() {
            @Override
            protected void run() {
               if (client.isServer()) {
                  ServerClient serverClient = client.getServerClient();
                  int recruitedToSettlementUniqueID = ShopContainer.this.humanShop.getRecruitedToSettlementUniqueID(serverClient);
                  Server server = serverClient.getServer();
                  if (recruitedToSettlementUniqueID != 0) {
                     PacketShopContainerUpdate.recruitSettler(
                        server,
                        serverClient,
                        ShopContainer.this,
                        () -> SettlementsWorldData.getSettlementsData(server).getOrLoadServerData(recruitedToSettlementUniqueID)
                     );
                  } else if (ShopContainer.this.canPayForRecruit()) {
                     SettlementsWorldData worldData = SettlementsWorldData.getSettlementsData(server);
                     List<CachedSettlementData> settlements = worldData.collectCachedSettlements(
                        data -> data.hasAccess(serverClient) && mob.isValidRecruitment(data, serverClient) && data.getName() != null
                     );
                     if (settlements.size() == 1) {
                        CachedSettlementData settlement = settlements.get(0);
                        PacketShopContainerUpdate.recruitSettler(
                           server, serverClient, ShopContainer.this, () -> worldData.getOrLoadServerData(settlement.uniqueID)
                        );
                     } else {
                        serverClient.sendPacket(PacketShopContainerUpdate.settlementsList(server, serverClient, ShopContainer.this.humanShop));
                     }
                  }
               }
            }
         }
      );
      this.buyItemAction = this.registerAction(new ShopContainer.BuyItemAction());
      this.sellItemAction = this.registerAction(new ShopContainer.SellItemAction());
      this.buyExpeditionButton = this.registerAction(
         new PointCustomAction() {
            @Override
            protected void run(int listIndex, int expeditionIndex) {
               if (listIndex >= 0 && listIndex < ShopContainer.this.expeditionLists.size()) {
                  ExpeditionList list = ShopContainer.this.expeditionLists.get(listIndex);
                  if (expeditionIndex >= 0 && expeditionIndex < list.expeditions.size()) {
                     ContainerExpedition expedition = list.expeditions.get(expeditionIndex);
                     int price = expedition.price;
                     if (price == 0 || price > 0 && client.playerMob.getInv().getAmount(ItemRegistry.getItem("coin"), true, true, false, false, "buy") >= price
                        )
                      {
                        if (price > 0) {
                           client.playerMob.getInv().removeItems(ItemRegistry.getItem("coin"), price, true, true, false, false, "buy");
                        }

                        if (client.isClient()) {
                           SoundManager.playSound(GameResources.coins, SoundEffect.globalEffect());
                        } else if (client.isServer()) {
                           System.out
                              .println(
                                 client.getName() + " bought " + expedition.expedition.getDisplayName().translate() + " expedition for " + price + " coins"
                              );
                           client.getServerClient().closeContainer(true);
                           ShopContainer.this.humanShop.startMission(new ExpeditionMission(expedition.expedition, price, expedition.successChance, false));
                        }
                     }
                  }
               }
            }
         }
      );
      this.workSettingAction = this.registerAction(new ContentCustomAction() {
         @Override
         protected void run(Packet content) {
            if (client.isServer()) {
               mob.workSettingAction.runAndSend(content);
            }
         }
      });
      this.setIsInPartyConfig = this.registerAction(new BooleanCustomAction() {
         @Override
         protected void run(boolean value) {
            ShopContainer.this.isInPartyConfig = value;
         }
      });

      for (SettlerDialogue dialogue : this.dialogues) {
         dialogue.init(this);
      }
   }

   public void handleWorkItemsAction(ShopContainer.WorkItemsAction action) {
      if (action == ShopContainer.WorkItemsAction.RECEIVE) {
         if (this.client.isServer() && this.humanShop.workInventory.isEmpty()) {
            this.client.playerMob.getInv().markFullDirty();
         }

         for (InventoryItem item : this.humanShop.workInventory) {
            item.setNew(true);
            this.client.playerMob.getInv().addItemsDropRemaining(item, "transfer", this.humanShop, !this.client.isServer(), false);
         }

         this.humanShop.workInventory.clear();
         if (this.client.isServer()) {
            new ShopContainerInventoryClaimedEvent(this.humanShop).applyAndSendToClientsAt(this.client.getServerClient());
         }
      }

      if (this.client.isServer()) {
         this.humanShop.clearMissionResult();
      }
   }

   public void setupWorkInvDialogue(DialogueForm dialogueForm) {
      dialogueForm.reset(this.humanShop, true, this.romanceLevel, (contentBox, flow) -> {
         if (this.workInvNotificationMessage != null) {
            Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
            DialogueForm.addText(contentBox, flow, this.workInvNotificationMessage, true);
            GameMessageBuilder items = new GameMessageBuilder();
            Iterator<InventoryItem> iterator = this.humanShop.workInventory.iterator();

            while (iterator.hasNext()) {
               InventoryItem next = iterator.next();
               items.append(TypeParsers.getItemParseString(next) + "x" + next.getAmount() + " ").append(next.getItemLocalization());
               if (iterator.hasNext()) {
                  items.append("\n");
               }
            }

            flow.next(3);
            DialogueForm.addText(contentBox, flow, items, true);
            chatBubble.run();
         }
      });
   }

   public void setupRecruitDialogue(DialogueForm dialogueForm) {
      dialogueForm.reset(this.humanShop, true, this.romanceLevel, (contentBox, flow) -> {
         Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);
         if (this.recruitItems != null) {
            if (this.recruitItems.isEmpty()) {
               GameMessage intro = new LocalMessage("ui", this.humanShop.isDowned() ? "settlerrevivefree" : "settlerrecruitfree");
               DialogueForm.addText(contentBox, flow, intro, true);
            } else {
               GameMessage intro = new LocalMessage("ui", this.humanShop.isDowned() ? "settlerrevivecostintro" : "settlerrecruitcostintro");
               DialogueForm.addText(contentBox, flow, intro, true);
               GameMessageBuilder items = new GameMessageBuilder();
               Iterator<InventoryItem> iterator = this.recruitItems.iterator();

               while (iterator.hasNext()) {
                  InventoryItem next = iterator.next();
                  items.append(TypeParsers.getItemParseString(next) + "x" + next.getAmount() + " ").append(next.getItemLocalization());
                  if (iterator.hasNext()) {
                     items.append("\n");
                  }
               }

               flow.next(3);
               DialogueForm.addText(contentBox, flow, items, true);
            }
         } else if (this.recruitError != null) {
            GameMessageBuilder intro = new GameMessageBuilder().append("\"").append(this.recruitError).append("\"");
            DialogueForm.addText(contentBox, flow, intro, true);
         }

         chatBubble.run();
      });
   }

   public boolean canPayForRecruit() {
      if (this.recruitItems == null) {
         return false;
      }

      for (InventoryItem recruitItem : this.recruitItems) {
         int items = this.client.playerMob.getInv().main.getAmount(this.client.playerMob.getLevel(), this.client.playerMob, recruitItem.item, "buy");
         if (items < recruitItem.getAmount()) {
            return false;
         }
      }

      return true;
   }

   public void payForRecruit() {
      for (InventoryItem recruitItem : this.recruitItems) {
         this.client
            .playerMob
            .getInv()
            .main
            .removeItems(this.client.playerMob.getLevel(), this.client.playerMob, recruitItem.item, recruitItem.getAmount(), "recruit");
      }
   }

   public ArrayList<ContainerQuest> replaceContainerQuestsWithManager(ArrayList<ContainerQuest> quests) {
      if (quests == null) {
         return quests;
      }

      for (ContainerQuest cq : quests) {
         cq.quest = this.replaceManagerQuest(cq.quest);
      }

      return quests;
   }

   public Quest replaceManagerQuest(Quest quest) {
      if (quest == null) {
         return quest;
      }

      QuestManager questManager = null;
      if (this.client.isServer()) {
         questManager = this.client.getServerClient().getServer().world.getQuests();
      } else if (this.client.isClient()) {
         questManager = this.client.getClientClient().getClient().quests;
      }

      if (questManager != null) {
         Quest existingQuest = questManager.getQuest(quest.getUniqueID());
         if (existingQuest != null) {
            quest = existingQuest;
         }
      }

      return quest;
   }

   public static Packet getContainerContent(
      HumanShop mob,
      ServerClient client,
      ServerSettlementData settlementData,
      boolean clientHasAccess,
      GameMessage workInvNotificationMessage,
      GameMessage missionFailedMessage,
      GameMessage introMessage,
      ArrayList<ContainerQuest> quests,
      ShopManager shopManager,
      ArrayList<SettlerDialogue> dialogues,
      GameMessage recruitError,
      List<InventoryItem> recruitItems,
      boolean startInRecruit,
      List<ExpeditionList> possibleExpeditions,
      HumanWorkSettingRegistry workSettings
   ) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextBoolean(clientHasAccess);
      writer.putNextBoolean(mob.isSettler() && !mob.isSettlerWithinSettlement());
      if (clientHasAccess) {
         new ShopContainerPartyUpdateEvent(mob, client).write(writer);
      }

      PlayerRomanceManager romanceManager = mob.getRomanceManager(client, false);
      if (romanceManager != null && client.relationshipData.isInAnyRelationship() && !mob.getRomancePersonality().isInRelationshipWith(client)) {
         romanceManager = null;
      }

      if (romanceManager != null) {
         writer.putNextBoolean(true);
         writer.putNextShortUnsigned(romanceManager.romanceLevel);
      } else {
         writer.putNextBoolean(false);
      }

      writer.putNextLong(mob.getShopSeed());
      if (workInvNotificationMessage != null) {
         writer.putNextBoolean(true);
         workInvNotificationMessage.writePacket(writer);
      } else {
         writer.putNextBoolean(false);
      }

      if (missionFailedMessage != null) {
         writer.putNextBoolean(true);
         missionFailedMessage.writePacket(writer);
      } else {
         writer.putNextBoolean(false);
      }

      introMessage.writePacket(writer);
      SettlerDialogue.writeDialoguesList(writer, dialogues);
      if (clientHasAccess) {
         HumanMob.StrikeChance strikeChance = mob.wantsToGoOnStrikeChance();
         if (strikeChance != null && strikeChance.reason != null) {
            writer.putNextBoolean(true);
            strikeChance.reason.writePacket(writer);
         } else {
            writer.putNextBoolean(false);
         }

         new ShopThoughtsUpdateEvent(mob).write(writer);
      }

      new ShopContainerQuestUpdateEvent(quests).write(writer);
      ArrayList<NetworkBuyingShopItem> buyingList;
      ArrayList<NetworkSellingShopItem> sellingList;
      if (shopManager == null) {
         sellingList = null;
         buyingList = null;
      } else {
         sellingList = shopManager.getSellingItemsList(client, mob, mob.getShopSeed());
         buyingList = shopManager.getBuyingItemsList(client, mob, mob.getShopSeed());
      }

      writer.putNextInt(shopManager == null ? -1 : shopManager.shopWealth);
      writer.putNextBoolean(sellingList != null);
      if (sellingList != null) {
         writer.putNextShortUnsigned(sellingList.size());

         for (NetworkSellingShopItem item : sellingList) {
            item.writePacket(writer);
         }
      }

      writer.putNextBoolean(buyingList != null);
      if (buyingList != null) {
         writer.putNextShortUnsigned(buyingList.size());

         for (NetworkBuyingShopItem item : buyingList) {
            item.writePacket(writer);
         }
      }

      writer.putNextBoolean(recruitError != null);
      if (recruitError != null) {
         writer.putNextContentPacket(recruitError.getContentPacket());
      } else {
         writer.putNextBoolean(recruitItems != null);
         if (recruitItems != null) {
            writer.putNextShortUnsigned(recruitItems.size());

            for (InventoryItem recruitItem : recruitItems) {
               InventoryItem.addPacketContent(recruitItem, writer);
            }
         }
      }

      writer.putNextBoolean(startInRecruit);
      writer.putNextShortUnsigned(possibleExpeditions.size());

      for (ExpeditionList expeditionList : possibleExpeditions) {
         expeditionList.writePacket(writer);
      }

      for (HumanWorkSetting<?> setting : workSettings.getSettings()) {
         setting.writeContainerPacket(client, writer);
      }

      return content;
   }

   public class BuyItemAction extends ContainerCustomAction {
      public void runAndSend(NetworkSellingShopItem item, int amount) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextShortUnsigned(item.shopItemID);
         writer.putNextInt(amount);
         this.runAndSendAction(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int shopID = reader.getNextShortUnsigned();
         int amount = reader.getNextInt();
         NetworkSellingShopItem item = ShopContainer.this.sellingItems.get(shopID);
         if (item != null) {
            SellingShopItem serverItem = ShopContainer.this.serverShopManager == null
               ? null
               : ShopContainer.this.serverShopManager.sellingShop.getItem(item.shopItemID);
            if (serverItem != null) {
               item.currentStock = serverItem.currentStock;
               ShopContainer.this.shopWealth = ShopContainer.this.serverShopManager.shopWealth;
            }

            int timesCompleted = 0;

            for (int i = 0; i < amount; i++) {
               if (!item.canCompleteTrade(ShopContainer.this, ShopContainer.this.getCraftInventories())) {
                  if (i == 0 && ShopContainer.this.client.isServer() && ShopContainer.this.serverShopManager != null) {
                     new SingleShopStockUpdateEvent(ShopContainer.this.serverShopManager, item)
                        .applyAndSendToClient(ShopContainer.this.client.getServerClient());
                     ShopContainer.this.client.getServerClient().playerMob.getInv().markFullDirty();
                  }
                  break;
               }

               int price = item.completeTrade(ShopContainer.this, ShopContainer.this.getCraftInventories());
               if (ShopContainer.this.shopWealth >= 0) {
                  ShopContainer.this.shopWealth += price;
                  if (ShopContainer.this.serverShopManager != null) {
                     ShopContainer.this.serverShopManager.shopWealth += price;
                  }
               }

               timesCompleted++;
            }

            if (timesCompleted > 0 && serverItem != null) {
               serverItem.currentStock = Math.max(serverItem.currentStock - timesCompleted, 0);
               new SingleShopStockUpdateEvent(ShopContainer.this.serverShopManager, item)
                  .applyAndSendToAllClients(ShopContainer.this.client.getServerClient().getServer());
            }
         } else if (ShopContainer.this.client.isServer()) {
            new FullShopStockUpdateEvent(ShopContainer.this.serverShopManager, ShopContainer.this.sellingItems.values())
               .applyAndSendToClient(ShopContainer.this.client.getServerClient());
         }
      }
   }

   public class ContainerWorkSetting<T> {
      public final HumanWorkSetting<T> setting;
      public final T data;
      public final HumanWorkContainerHandler handler;

      public ContainerWorkSetting(HumanWorkSetting<T> setting, Object data) {
         this.setting = setting;
         this.data = (T)data;
         this.handler = setting.setupHandler(ShopContainer.this, this);
      }

      public void sendAction(Packet content) {
         Packet settingsContent = new Packet();
         PacketWriter writer = new PacketWriter(settingsContent);
         writer.putNextShortUnsigned(this.setting.getID());
         writer.putNextContentPacket(content);
         ShopContainer.this.workSettingAction.runAndSend(settingsContent);
      }

      public void sendServerOnlyAction(Packet content) {
         Packet settingsContent = new Packet();
         PacketWriter writer = new PacketWriter(settingsContent);
         writer.putNextShortUnsigned(this.setting.getID());
         writer.putNextContentPacket(content);
         ShopContainer.this.workSettingAction.runAndSend(settingsContent);
      }
   }

   public class SellItemAction extends ContainerCustomAction {
      public void runAndSend(NetworkBuyingShopItem item, int amount, boolean forcedBeyondShopWealth) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextShortUnsigned(item.shopItemID);
         writer.putNextInt(amount);
         writer.putNextBoolean(forcedBeyondShopWealth);
         this.runAndSendAction(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int itemID = reader.getNextShortUnsigned();
         int amount = reader.getNextInt();
         boolean forceBeyondShopWealth = reader.getNextBoolean();
         NetworkBuyingShopItem item = ShopContainer.this.buyingItems.get(itemID);
         if (item != null) {
            if (ShopContainer.this.serverShopManager != null) {
               ShopContainer.this.shopWealth = ShopContainer.this.serverShopManager.shopWealth;
            }

            int timesCompleted = 0;

            for (int i = 0; i < amount; i++) {
               if (!item.canCompleteTrade(
                  ShopContainer.this, ShopContainer.this.getCraftInventories(), forceBeyondShopWealth ? -1 : ShopContainer.this.shopWealth
               )) {
                  if (i == 0 && ShopContainer.this.client.isServer() && ShopContainer.this.serverShopManager != null) {
                     new ShopWealthUpdateEvent(ShopContainer.this.serverShopManager).applyAndSendToClient(ShopContainer.this.client.getServerClient());
                     ShopContainer.this.client.getServerClient().playerMob.getInv().markFullDirty();
                  }
                  break;
               }

               int price = item.completeTrade(ShopContainer.this, ShopContainer.this.getCraftInventories());
               if (ShopContainer.this.shopWealth >= 0) {
                  ShopContainer.this.shopWealth = Math.max(ShopContainer.this.shopWealth - price, 0);
                  if (ShopContainer.this.serverShopManager != null) {
                     ShopContainer.this.serverShopManager.shopWealth = ShopContainer.this.shopWealth;
                  }
               }

               timesCompleted++;
            }

            if (timesCompleted > 0 && ShopContainer.this.serverShopManager != null) {
               new ShopWealthUpdateEvent(ShopContainer.this.serverShopManager)
                  .applyAndSendToAllClients(ShopContainer.this.client.getServerClient().getServer());
            }
         } else if (ShopContainer.this.client.isServer()) {
            ShopContainer.this.close();
         }
      }
   }

   public enum WorkItemsAction {
      RECEIVE,
      CONTINUE;
   }
}
