package necesse.engine.storyObjectives.objectives;

import java.util.HashSet;
import java.util.stream.Collectors;
import necesse.engine.GlobalData;
import necesse.engine.gameTool.GameTool;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketOpenHelpForm;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.AddToSettlementStorageObjectiveListener;
import necesse.engine.storyObjectives.listeners.AssignBedObjectiveListener;
import necesse.engine.storyObjectives.utils.MobMarkerObjectiveManager;
import necesse.engine.storyObjectives.utils.TileMarkerObjectiveManager;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormLocalTextButtonToggle;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.gfx.forms.presets.containerComponent.object.OEInventoryContainerForm;
import necesse.gfx.forms.presets.containerComponent.settlement.AssignSettlerBedTool;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementContainerForm;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.Container;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementInventory;

public class RecruitSettlerStoryObjective extends StoryObjective implements AssignBedObjectiveListener, AddToSettlementStorageObjectiveListener {
   public static HashSet<String> FOOD_ITEMS = new HashSet<>();
   public static HashSet<String> CHEST_ITEMS = new HashSet<>();
   public static LootTable reward = new LootTable(new LootItem("prettyflower"));
   private final TileMarkerObjectiveManager nearbyChestFinder = new TileMarkerObjectiveManager(1, 20) {
      @Override
      public boolean isValidTile(Level level, int tileX, int tileY) {
         return RecruitSettlerStoryObjective.this.isChest(level, tileX, tileY);
      }
   };
   private final MobMarkerObjectiveManager settlerFinder = new MobMarkerObjectiveManager(1, 50) {
      @Override
      public boolean isValidMob(Mob mob) {
         if (!(mob instanceof HumanMob)) {
            return false;
         } else {
            HumanMob humanMob = (HumanMob)mob;
            if (!humanMob.isVisitor(RecruitSettlerStoryObjective.this.currentClientSettlementUniqueID)) {
               return false;
            } else if (humanMob.settlerStringID == null) {
               return false;
            } else {
               return humanMob.settlerStringID.equals("exoticmerchant") ? false : !humanMob.settlerStringID.equals("pawnbroker");
            }
         }
      }
   };
   public int currentClientSettlementUniqueID;
   public boolean hasRecruitedSettler;
   public boolean hasAssignedBed;
   public boolean hasClickedAddChestToSettlementStorage;

   public RecruitSettlerStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void onBedAssigned() {
      if (!this.isCompleted() && !this.hasAssignedBed && this.hasRecruitedSettler) {
         this.hasAssignedBed = true;
         this.markDirty();
      }
   }

   @Override
   public void onClickedAddToSettlementStorage(SettlementInventory settlementInventory) {
      if (!this.isCompleted()
         && !this.hasClickedAddChestToSettlementStorage
         && this.hasRecruitedSettler
         && this.hasAssignedBed
         && settlementInventory.oeInventory != null
         && this.isValidInventory(settlementInventory.oeInventory.getInventory())) {
         this.hasClickedAddChestToSettlementStorage = true;
         this.markDirty();
      }
   }

   public boolean isChest(Level level, int tileX, int tileY) {
      ObjectEntity oe = level.entityManager.getObjectEntity(tileX, tileY);
      if (oe instanceof OEInventory) {
         Inventory inventory = ((OEInventory)oe).getInventory();
         return this.isValidInventory(inventory);
      } else {
         return false;
      }
   }

   public boolean isValidInventory(Inventory inventory) {
      return inventory != null && inventory.getSize() > 20;
   }

   public boolean chestContainsFood(InventoryRange inventoryRange) {
      if (inventoryRange != null) {
         for (int i = inventoryRange.startSlot; i <= inventoryRange.endSlot; i++) {
            InventoryItem item = inventoryRange.inventory.getItem(i);
            if (item != null && item.item instanceof FoodConsumableItem) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.hasRecruitedSettler) {
         save.addBoolean("hasRecruitedSettler", this.hasRecruitedSettler);
      }

      if (this.hasAssignedBed) {
         save.addBoolean("hasAssignedBed", this.hasAssignedBed);
      }

      if (this.hasClickedAddChestToSettlementStorage) {
         save.addBoolean("hasClickedAddChestToSettlementStorage", this.hasClickedAddChestToSettlementStorage);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hasRecruitedSettler = save.getBoolean("hasRecruitedSettler", this.hasRecruitedSettler, false);
      this.hasAssignedBed = save.getBoolean("hasAssignedBed", this.hasAssignedBed, false);
      this.hasClickedAddChestToSettlementStorage = save.getBoolean("hasClickedAddChestToSettlementStorage", this.hasClickedAddChestToSettlementStorage, false);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextBoolean(this.hasRecruitedSettler);
      writer.putNextBoolean(this.hasAssignedBed);
      writer.putNextBoolean(this.hasClickedAddChestToSettlementStorage);
   }

   @Override
   public void readPacket(PacketReader reader) {
      super.readPacket(reader);
      this.hasRecruitedSettler = reader.getNextBoolean();
      this.hasAssignedBed = reader.getNextBoolean();
      this.hasClickedAddChestToSettlementStorage = reader.getNextBoolean();
   }

   @Override
   public void clientTickCurrentObjective() {
      super.clientTickCurrentObjective();
      Client client = this.manager.getClient();
      PlayerMob player = client.getPlayer();
      NetworkSettlementData networkData = SettlementsWorldData.getSettlementsData(client)
         .getNetworkDataAtTile(player.getLevel().getIdentifier(), player.getTileX(), player.getTileY());
      if (networkData != null && networkData.hasFlag() && networkData.doIHaveAccess(client)) {
         this.currentClientSettlementUniqueID = networkData.uniqueID;
         if (!this.hasRecruitedSettler) {
            this.settlerFinder.clientTick(player);
         } else if (this.hasAssignedBed) {
            this.nearbyChestFinder.clientTick(player);
         }
      } else {
         this.currentClientSettlementUniqueID = 0;
         this.settlerFinder.clearFoundMobs();
         this.nearbyChestFinder.clearFoundTiles();
      }
   }

   @Override
   public void serverTickCurrentObjective() {
      super.serverTickCurrentObjective();
      if (!this.hasRecruitedSettler) {
         PlayerMob player = this.getPlayer();
         NetworkSettlementData networkData = SettlementsWorldData.getSettlementsData(player)
            .getNetworkDataAtTile(player.getLevel().getIdentifier(), player.getTileX(), player.getTileY());
         if (networkData != null) {
            int settlerCount = networkData.getServerData().settlers.values().size();
            if (settlerCount >= 2) {
               this.hasRecruitedSettler = true;
               this.markDirty();
            }
         }
      }

      if (!this.isCompleted() && this.hasClickedAddChestToSettlementStorage) {
         ServerClient client = this.manager.getServerClient();
         Container container = client.getContainer();
         if (container instanceof OEInventoryContainer) {
            OEInventoryContainer oeContainer = (OEInventoryContainer)container;
            ServerSettlementData serverData = oeContainer.getServerData();
            if (serverData != null && serverData.storageManager.hasInventoryAtTile(oeContainer.objectEntity.tileX, oeContainer.objectEntity.tileY)) {
               InventoryRange inventoryRange = oeContainer.getOEInventoryRange();
               Inventory inventory = oeContainer.getOEInventory().getInventory();
               if (inventoryRange != null && this.isValidInventory(inventory) && this.chestContainsFood(inventoryRange)) {
                  this.markCompleted();
               }
            }
         }
      }
   }

   @Override
   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (!this.isCompleted()) {
         FormComponent focusForm = ((MainGame)GlobalData.getCurrentState()).formManager.getFocusForm();
         String settlerIcon = TypeParsers.getMobParseString("human");
         if (!this.hasRecruitedSettler) {
            LocalMessage recruitSettlerMessage = new LocalMessage("objectives", "nearbysettler", "settlericon", settlerIcon);
            return this.settlerFinder.getArrowHudDrawOptions(recruitSettlerMessage, null, camera, perspective);
         }

         if (!this.hasAssignedBed) {
            if (!(focusForm instanceof SettlementContainerForm)) {
               LocalMessage openSettlementMenuMessage = new LocalMessage(
                  "objectives", "opensettlementmenu", "key", TypeParsers.getInputParseString(Control.OPEN_SETTLEMENT)
               );
               return StoryObjective.getLevelTextDrawOptions(
                  openSettlementMenuMessage, perspective.getDrawX(), perspective.getDrawY() - 48, camera, perspective, false
               );
            }
         } else if (!this.hasClickedAddChestToSettlementStorage && !(focusForm instanceof SettlementContainerForm)) {
            String chestIcons = TypeParsers.getItemsParseString(CHEST_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
            LocalMessage findChestMessage = new LocalMessage("objectives", "selectachest", "chesticons", chestIcons);
            if (!(focusForm instanceof OEInventoryContainerForm)) {
               return this.nearbyChestFinder.getArrowHudDrawOptions(findChestMessage, null, camera, perspective);
            }

            Inventory inventory = ((OEInventoryContainer)((OEInventoryContainerForm)focusForm).getContainer()).getOEInventory().getInventory();
            if (!this.isValidInventory(inventory)) {
               return this.nearbyChestFinder.getArrowHudDrawOptions(findChestMessage, null, camera, perspective);
            }
         }
      }

      return super.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
   }

   @Override
   public void drawCurrentObjectiveOverForms(MainGame mainGame, PlayerMob perspective) {
      if (this.hasRecruitedSettler) {
         FormComponent focusForm = ((MainGame)GlobalData.getCurrentState()).formManager.getFocusForm();
         if (!this.hasAssignedBed) {
            if (focusForm instanceof SettlementContainerForm) {
               SettlementContainerForm<?> settlementForm = (SettlementContainerForm<?>)focusForm;
               if (settlementForm.isCurrent(settlementForm.settlersForm)) {
                  GameTool firstGameTool = GameToolManager.getFirstGameTool();
                  if (!(firstGameTool instanceof AssignSettlerBedTool)) {
                     FormContentIconButton firstAssignBedButton = settlementForm.settlersForm.firstAssignBedButton;
                     if (firstAssignBedButton != null) {
                        LocalMessage assignSettlerToBedMessage = new LocalMessage("objectives", "assigntobed");
                        FairTypeDrawOptions text = getTextDrawOptions(getObjectiveText(assignSettlerToBedMessage.translate()));
                        getTextDrawOptions(text, null, firstAssignBedButton, firstAssignBedButton.getWidth() / 2, -6, true).draw();
                     }
                  }
               } else {
                  FormLocalTextButtonToggle settlerMenuButton = settlementForm.settlerMenuButton;
                  LocalMessage clickSettlersButtonMessage = new LocalMessage("objectives", "clickonsettlersbutton");
                  FairTypeDrawOptions text = getTextDrawOptions(getObjectiveText(clickSettlersButtonMessage.translate()));
                  getTextDrawOptions(text, null, settlerMenuButton, settlerMenuButton.getWidth() / 2, -6, true).draw();
               }
            }
         } else if (!this.hasClickedAddChestToSettlementStorage && focusForm instanceof OEInventoryContainerForm) {
            OEInventoryContainerForm<?> inventoryForm = (OEInventoryContainerForm<?>)focusForm;
            FormContentIconButton configureStorageButton = inventoryForm.settlementObjectFormManager.configureStorageButton;
            Inventory inventory = inventoryForm.getContainer().getOEInventory().getInventory();
            if (configureStorageButton != null && this.isValidInventory(inventory)) {
               String chestIcons = TypeParsers.getItemsParseString(CHEST_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
               LocalMessage addToSettlementStorageMessage = new LocalMessage("objectives", "addtosettlementstorage", "chesticons", chestIcons);
               FairTypeDrawOptions text = getTextDrawOptions(getObjectiveText(addToSettlementStorageMessage.translate()));
               getTextDrawOptions(text, null, configureStorageButton, configureStorageButton.getWidth() / 2, -6, true).draw();
            }
         }
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void claimRewards(ServerSettlementData serverSettlementData) {
      super.claimRewards(serverSettlementData);
      if (this.manager.isServer()) {
         this.manager.getServerClient().closeContainer(true);
         this.manager.getServerClient().sendPacket(new PacketOpenHelpForm("majorobjectiveintro"));
      }
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      String settlerIcon = TypeParsers.getMobParseString("human");
      String foodIcons = TypeParsers.getItemsParseString(FOOD_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      String chestIcons = TypeParsers.getItemsParseString(CHEST_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      String oakBedIcon = TypeParsers.getItemParseString(new InventoryItem("oakbed"));
      form.addTitle(flow, new LocalMessage("objectives", "recruitsettlertitle"));
      form.addObjective(flow, 1, new LocalMessage("objectives", "recruitsettler", "settlericon", settlerIcon), this.isCompleted() || this.hasRecruitedSettler);
      if (this.isCompleted() || this.hasRecruitedSettler) {
         form.addObjective(
            flow,
            2,
            new LocalMessage("objectives", "assignbedtosettler", "settlericon", settlerIcon, "bedicon", oakBedIcon),
            this.isCompleted() || this.hasAssignedBed
         );
      }

      if (this.isCompleted() || this.hasAssignedBed) {
         form.addObjective(
            flow,
            3,
            new LocalMessage("objectives", "addchesttosettlementstorage", "chesticons", chestIcons),
            this.isCompleted() || this.hasClickedAddChestToSettlementStorage
         );
      }

      if (this.isCompleted() || this.hasClickedAddChestToSettlementStorage) {
         form.addObjective(flow, 4, new LocalMessage("objectives", "putfoodinsettlementstorage", "foodicons", foodIcons), this.isCompleted());
      }

      if (this.isCompleted()) {
         form.addObjective(flow, 5, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false);
      }

      form.addRewardsText(flow);
   }

   static {
      FOOD_ITEMS.add("blueberry");
      FOOD_ITEMS.add("apple");
      FOOD_ITEMS.add("steak");
      CHEST_ITEMS.add("storagebox");
      CHEST_ITEMS.add("oakchest");
      CHEST_ITEMS.add("coolingbox");
   }
}
