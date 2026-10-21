package necesse.engine.storyObjectives.objectives;

import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketOpenHelpForm;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.ContainerOpenedObjectiveListener;
import necesse.engine.storyObjectives.utils.TileMarkerObjectiveManager;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.Container;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;

public class StartSettlementStoryObjective extends StoryObjective implements ContainerOpenedObjectiveListener {
   public static LootTable reward = new LootTable(new LootItem("ironanvil"));
   private final TileMarkerObjectiveManager flagChestFinder = new TileMarkerObjectiveManager(1, 20) {
      @Override
      public boolean isValidTile(Level level, int tileX, int tileY) {
         return StartSettlementStoryObjective.this.containsSettlementFlag(level, tileX, tileY);
      }
   };
   private final TileMarkerObjectiveManager flagObjectFinder = new TileMarkerObjectiveManager(1, 20) {
      @Override
      public boolean isValidTile(Level level, int tileX, int tileY) {
         return level.getObject(tileX, tileY).getStringID().equals("settlementflag");
      }
   };
   public boolean clientIsInSettlement;

   public StartSettlementStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void onContainerOpened(Container container) {
      if (!this.isCompleted() && container instanceof SettlementContainer) {
         this.markCompleted();
         this.manager.getServerClient().sendPacket(new PacketOpenHelpForm("settlementintro"));
      }
   }

   public boolean containsSettlementFlag(Level level, int tileX, int tileY) {
      ObjectEntity oe = level.entityManager.getObjectEntity(tileX, tileY);
      if (oe instanceof OEInventory) {
         Inventory inventory = ((OEInventory)oe).getInventory();
         if (inventory != null) {
            for (int i = 0; i < inventory.getSize(); i++) {
               InventoryItem item = inventory.getItem(i);
               if (item != null && item.item.getStringID().equals("settlementflag")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @Override
   public void clientTickCurrentObjective() {
      super.clientTickCurrentObjective();
      Client client = this.manager.getClient();
      PlayerMob player = client.getPlayer();
      NetworkSettlementData networkData = SettlementsWorldData.getSettlementsData(client)
         .getNetworkDataAtTile(player.getLevel().getIdentifier(), player.getTileX(), player.getTileY());
      boolean lastIsInSettlement = this.clientIsInSettlement;
      this.clientIsInSettlement = networkData != null && networkData.hasFlag();
      if (lastIsInSettlement != this.clientIsInSettlement) {
         this.manager.updateFormComponents();
      }

      if (this.clientIsInSettlement) {
         this.flagObjectFinder.clientTick(player);
      } else {
         this.flagChestFinder.clientTick(player);
      }
   }

   @Override
   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (!this.isCompleted()) {
         InventoryItem settlementFlagItem = new InventoryItem("settlementflag");
         String settlementFlagIcon = TypeParsers.getItemParseString(settlementFlagItem);
         String settlementFlag = settlementFlagIcon + " " + settlementFlagItem.getItemDisplayName();
         if (this.clientIsInSettlement) {
            LocalMessage useFlagMessage = new LocalMessage(
               "objectives", "useflag", "settlementflag", settlementFlag, "key", TypeParsers.getInputParseString(Control.MOUSE2)
            );
            return this.flagObjectFinder.getArrowHudDrawOptions(useFlagMessage, null, camera, perspective);
         } else {
            InventoryItem storageBoxItem = new InventoryItem("storagebox");
            String storageBoxIcon = TypeParsers.getItemParseString(storageBoxItem);
            String storageBox = storageBoxIcon + " " + storageBoxItem.getItemDisplayName();
            LocalMessage takeFlagMessage = new LocalMessage("objectives", "takeflag", "settlementflag", settlementFlag, "storagebox", storageBox);
            return this.flagChestFinder.getArrowHudDrawOptions(takeFlagMessage, null, camera, perspective);
         }
      } else {
         return super.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      String elderIcon = TypeParsers.getMobParseString("elderhuman");
      form.addTitle(flow, new LocalMessage("objectives", "startsettlementttile"));
      form.addObjective(
         flow,
         1,
         new LocalMessage(
            "objectives",
            "findflag",
            "settlementflag",
            TypeParsers.getItemAndNameParseString("settlementflag"),
            "storagebox",
            TypeParsers.getItemAndNameParseString("storagebox"),
            "eldericon",
            elderIcon,
            "workstation",
            TypeParsers.getItemAndNameParseString("workstationduo")
         ),
         this.isCompleted() || this.clientIsInSettlement
      );
      if (this.isCompleted() || this.clientIsInSettlement) {
         LocalMessage openSettlementMessage = new LocalMessage(
            "objectives",
            "opensettlement",
            "settlementflag",
            TypeParsers.getItemAndNameParseString("settlementflag"),
            "key",
            TypeParsers.getInputParseString(Control.OPEN_SETTLEMENT)
         );
         form.addObjective(flow, 2, openSettlementMessage, this.isCompleted());
      }

      if (this.isCompleted()) {
         form.addObjective(flow, 3, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false);
      }

      form.addRewardsText(flow);
   }
}
