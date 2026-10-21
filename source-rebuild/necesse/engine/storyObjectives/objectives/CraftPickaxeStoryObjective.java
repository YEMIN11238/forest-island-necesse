package necesse.engine.storyObjectives.objectives;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import necesse.engine.input.Control;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.GlobalIngredientRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.ObjectInteractedObjectiveListener;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.container.CraftingStationObject;
import necesse.level.maps.Level;
import necesse.level.maps.multiTile.MultiTile;

public class CraftPickaxeStoryObjective extends StoryObjective implements ObjectInteractedObjectiveListener {
   public static HashSet<String> LOG_ITEMS = new HashSet<>();
   public static LootTable reward = new LootTable(new LootItem("healthpotion", 5));
   public boolean hasObtained18Logs;
   public boolean hasPlacedOrInteractedWithWorkstation;

   public CraftPickaxeStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.hasObtained18Logs) {
         save.addBoolean("hasObtained18Logs", this.hasObtained18Logs);
      }

      if (this.hasPlacedOrInteractedWithWorkstation) {
         save.addBoolean("hasPlacedOrInteractedWithWorkstation", this.hasPlacedOrInteractedWithWorkstation);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hasObtained18Logs = save.getBoolean("hasObtained18Logs", this.hasObtained18Logs, false);
      this.hasPlacedOrInteractedWithWorkstation = save.getBoolean("hasPlacedOrInteractedWithWorkstation", this.hasPlacedOrInteractedWithWorkstation, false);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextBoolean(this.hasObtained18Logs);
      writer.putNextBoolean(this.hasPlacedOrInteractedWithWorkstation);
   }

   @Override
   public void readPacket(PacketReader reader) {
      super.readPacket(reader);
      this.hasObtained18Logs = reader.getNextBoolean();
      this.hasPlacedOrInteractedWithWorkstation = reader.getNextBoolean();
   }

   @Override
   public void serverTickCurrentObjective() {
      super.serverTickCurrentObjective();
      if (!this.hasObtained18Logs) {
         int anyLogID = GlobalIngredientRegistry.getGlobalIngredientID("anylog");
         boolean has18Logs = this.getPlayer()
            .getInv()
            .hasCountOfItem(false, false, false, false, "tutorial", 18, item -> item.item.isGlobalIngredient(anyLogID));
         if (has18Logs) {
            this.hasObtained18Logs = true;
            this.markDirty();
         }
      } else if (this.hasPlacedOrInteractedWithWorkstation) {
         boolean hasAnyPickaxe = this.getPlayer().getInv().hasAnyItem(false, false, false, false, "tutorial", item -> {
            if (item.item instanceof ToolDamageItem) {
               ToolDamageItem toolDamageItem = (ToolDamageItem)item.item;
               return toolDamageItem.getToolType() == ToolType.PICKAXE;
            } else {
               return false;
            }
         });
         if (hasAnyPickaxe) {
            this.markCompleted();
         }
      }
   }

   @Override
   public void onObjectInteracted(GameObject object, Level level, int tileX, int tileY) {
      if (this.hasObtained18Logs && !this.hasPlacedOrInteractedWithWorkstation && this.isValidWorkstationObject(object, level, tileX, tileY)) {
         this.hasPlacedOrInteractedWithWorkstation = true;
         this.markDirty();
      }
   }

   public boolean isValidWorkstationObject(GameObject object, Level level, int tileX, int tileY) {
      MultiTile multiTile = object.getMultiTile(level, 0, tileX, tileY);
      GameObject masterObject = multiTile.getMasterObject();
      if (this.hasObtained18Logs && !this.hasPlacedOrInteractedWithWorkstation && masterObject instanceof CraftingStationObject) {
         CraftingStationObject craftingStationObject = (CraftingStationObject)masterObject;
         return Arrays.stream(craftingStationObject.getCraftingTechs()).anyMatch(tech -> tech.getStringID().equals("workstation"));
      } else {
         return false;
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      String logsIcon = TypeParsers.getItemsParseString(LOG_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      form.addTitle(flow, new LocalMessage("objectives", "craftingpicktitle", "pickaxe", TypeParsers.getItemAndNameParseString("woodpickaxe")));
      form.addObjective(flow, 1, new LocalMessage("objectives", "obtainlogs", "count", "18 " + logsIcon), this.isCompleted() || this.hasObtained18Logs);
      if (this.isCompleted() || this.hasObtained18Logs) {
         GameMessage interactWorkstationMessage = new LocalMessage(
               "objectives", "interactworkstation", "workstation", TypeParsers.getItemAndNameParseString("workstationduo")
            )
            .addReplacement("key1", TypeParsers.getInputParseString(Control.MOUSE1))
            .addReplacement("key2", TypeParsers.getInputParseString(Control.MOUSE2));
         form.addObjective(flow, 2, interactWorkstationMessage, this.isCompleted() || this.hasPlacedOrInteractedWithWorkstation);
         if (this.isCompleted() || this.hasPlacedOrInteractedWithWorkstation) {
            GameMessage craftPickMessage = new LocalMessage(
               "objectives",
               "craftpickaxe",
               "logs",
               "8 " + logsIcon,
               "pickaxe",
               TypeParsers.getItemAndNameParseString("woodpickaxe"),
               "workstation",
               TypeParsers.getItemAndNameParseString("workstationduo")
            );
            form.addObjective(flow, 3, craftPickMessage, this.isCompleted());
         }
      }

      if (this.isCompleted()) {
         form.addObjective(flow, 4, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false);
      }

      form.addRewardsText(flow);
   }

   static {
      LOG_ITEMS.add("oaklog");
      LOG_ITEMS.add("sprucelog");
      LOG_ITEMS.add("pinelog");
   }
}
