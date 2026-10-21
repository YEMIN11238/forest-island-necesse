package necesse.engine.storyObjectives.objectives;

import java.util.HashSet;
import java.util.stream.Collectors;
import necesse.engine.GlobalData;
import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.GlobalIngredientRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.utils.MobMarkerObjectiveManager;
import necesse.engine.storyObjectives.utils.TileMarkerObjectiveManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

public class GettingStartedStoryObjective extends StoryObjective {
   public static HashSet<String> TREE_ITEMS = new HashSet<>();
   public static LootTable reward = new LootTable(new LootItem("leatherdashers"));
   private final TileMarkerObjectiveManager treeFinder = new TileMarkerObjectiveManager(1, 20) {
      @Override
      public boolean isValidTile(Level level, int tileX, int tileY) {
         return level.getObject(tileX, tileY).isTree;
      }
   };
   private final MobMarkerObjectiveManager elderFinder = new MobMarkerObjectiveManager(1, 50) {
      @Override
      public boolean isValidMob(Mob mob) {
         return mob.getStringID().equals("elderhuman");
      }
   };
   public boolean hasObtainedAnyLog;

   public GettingStartedStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.hasObtainedAnyLog) {
         save.addBoolean("hasObtainedAnyLog", this.hasObtainedAnyLog);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hasObtainedAnyLog = save.getBoolean("hasObtainedAnyLog", this.hasObtainedAnyLog, false);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextBoolean(this.hasObtainedAnyLog);
   }

   @Override
   public void readPacket(PacketReader reader) {
      super.readPacket(reader);
      this.hasObtainedAnyLog = reader.getNextBoolean();
   }

   @Override
   public void serverTickCurrentObjective() {
      super.serverTickCurrentObjective();
      if (!this.hasObtainedAnyLog) {
         int anyLogID = GlobalIngredientRegistry.getGlobalIngredientID("anylog");
         boolean hasAnyLog = this.getPlayer().getInv().hasAnyItem(false, false, false, false, "tutorial", item -> item.item.isGlobalIngredient(anyLogID));
         if (hasAnyLog) {
            this.hasObtainedAnyLog = true;
            this.markDirty();
         }
      } else {
         int torchID = ItemRegistry.getItemID("torch");
         boolean hasAnyTorch = this.getPlayer().getInv().hasAnyItem(false, false, false, false, "tutorial", item -> item.item.getID() == torchID);
         if (hasAnyTorch) {
            this.markCompleted();
         }
      }
   }

   @Override
   public void clientTickCurrentObjective() {
      super.clientTickCurrentObjective();
      if (!this.isCompleted() && !this.hasObtainedAnyLog) {
         this.treeFinder.clientTick(this.getPlayer());
      }

      if (this.isCompleted()) {
         this.elderFinder.clientTick(this.getPlayer());
      }
   }

   @Override
   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (!this.isCompleted()) {
         if (!this.hasObtainedAnyLog) {
            String woodAxeIcon = TypeParsers.getItemParseString(new InventoryItem("woodaxe"));
            String treeIcons = TypeParsers.getItemsParseString(TREE_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
            LocalMessage hitTreeMessage = new LocalMessage(
               "objectives", "hittree", "key", TypeParsers.getInputParseString(Control.MOUSE1), "axeicon", woodAxeIcon, "treeicon", treeIcons
            );
            return this.treeFinder
               .getArrowHudDrawOptions(hitTreeMessage, new LocalMessage("objectives", "findtree", "treeicon", treeIcons), camera, perspective);
         } else if (!perspective.isInventoryExtended()) {
            LocalMessage openInvMessage = new LocalMessage("objectives", "openinv", "key", TypeParsers.getInputParseString(Control.INVENTORY));
            return StoryObjective.getLevelTextDrawOptions(openInvMessage, perspective.getDrawX(), perspective.getDrawY() - 48, camera, perspective, false);
         } else {
            return super.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
         }
      } else {
         LocalMessage interactElderTutorial = new LocalMessage(
            "objectives", "elderwalk", "elder", TypeParsers.getMobAndNameParseString("elderhuman"), "key", TypeParsers.getInputParseString(Control.MOUSE2)
         );
         return this.elderFinder.getArrowHudDrawOptions(interactElderTutorial, null, camera, perspective);
      }
   }

   @Override
   public void drawCurrentObjectiveOverForms(MainGame mainGame, PlayerMob perspective) {
      super.drawCurrentObjectiveOverForms(mainGame, perspective);
      if (!this.isCompleted() && this.hasObtainedAnyLog && perspective.isInventoryExtended() && !perspective.getWorldSettings().creativeMode) {
         InventoryItem torchItem = new InventoryItem("torch");
         String torchIcon = TypeParsers.getItemParseString(torchItem);
         String torch = torchIcon + torchItem.getItemDisplayName();
         Form form = ((MainGame)GlobalData.getCurrentState()).formManager.crafting;
         FairTypeDrawOptions text = getTextDrawOptions(getObjectiveText(new LocalMessage("objectives", "crafttorch", "torch", torch).translate()));
         getTextDrawOptions(text, null, form, form.getWidth() / 2 - 40, 0, true).draw();
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      String treeIcons = TypeParsers.getItemsParseString(TREE_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      form.addTitle(flow, new LocalMessage("objectives", "gettingstartedtitle"));
      form.addObjective(
         flow,
         1,
         new LocalMessage("objectives", "choptree", "woodaxe", TypeParsers.getItemAndNameParseString("woodaxe"), "treeicon", treeIcons),
         this.isCompleted() || this.hasObtainedAnyLog
      );
      if (this.isCompleted() || this.hasObtainedAnyLog) {
         form.addObjective(
            flow, 2, new LocalMessage("objectives", "invcrafttorch", "torch", TypeParsers.getItemAndNameParseString("torch")), this.isCompleted()
         );
      }

      if (this.isCompleted()) {
         form.addObjective(flow, 3, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false);
      }

      form.addRewardsText(flow);
   }

   static {
      TREE_ITEMS.add("sprucetree");
      TREE_ITEMS.add("oaktree");
   }
}
