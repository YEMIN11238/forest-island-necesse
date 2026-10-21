package necesse.gfx.forms.presets.debug.tools;

import java.util.ArrayList;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class LootTableTestGameTool extends MouseDebugGameTool {
   public static ArrayList<LootTableTestGameTool.LabeledLootTable> lootTables = new ArrayList<>();
   private final LootTableTestGameTool.LabeledLootTable lootTable;

   public static SelectionFloatMenu getSelectionMenu(DebugForm parent) {
      SelectionFloatMenu menu = new SelectionFloatMenu(parent);

      for (LootTableTestGameTool.LabeledLootTable lootTable : lootTables) {
         menu.add(lootTable.name, () -> {
            LootTableTestGameTool tool = new LootTableTestGameTool(parent, lootTable);
            GameToolManager.clearGameTools(parent);
            GameToolManager.setGameTool(tool, parent);
            menu.remove();
         });
      }

      return menu;
   }

   public LootTableTestGameTool(DebugForm parent, LootTableTestGameTool.LabeledLootTable lootTable) {
      super(parent, "Test " + lootTable.name);
      this.lootTable = lootTable;
   }

   @Override
   public void init() {
      this.onLeftClick(e -> {
         GameObject chestObject = ObjectRegistry.getObject(ObjectRegistry.getObjectID("storagebox"));
         GameRandom random = new GameRandom();
         Level cLevel = this.getLevel();
         int tileX = this.getMouseTileX();
         int tileY = this.getMouseTileY();
         if (cLevel != null) {
            if (cLevel.getObjectID(tileX, tileY) != chestObject.getID()) {
               chestObject.placeObject(cLevel, tileX, tileY, 2, false);
            }

            this.lootTable.table.applyToLevel(random, cLevel.buffManager.getModifier(LevelModifiers.LOOT), cLevel, tileX, tileY);
         }

         Level sLevel = this.getServerLevel();
         if (sLevel != null) {
            if (sLevel.getObjectID(tileX, tileY) != chestObject.getID()) {
               chestObject.placeObject(sLevel, tileX, tileY, 2, false);
            }

            this.lootTable.table.applyToLevel(random, sLevel.buffManager.getModifier(LevelModifiers.LOOT), sLevel, tileX, tileY);
         }

         return true;
      }, "Place chest");
      this.onRightClick(e -> {
         GameObject airObject = ObjectRegistry.getObject(ObjectRegistry.getObjectID("air"));
         Level cLevel = this.getLevel();
         if (cLevel != null) {
            airObject.placeObject(cLevel, this.getMouseTileX(), this.getMouseTileY(), 0, false);
         }

         Level sLevel = this.getServerLevel();
         if (sLevel != null) {
            airObject.placeObject(sLevel, this.getMouseTileX(), this.getMouseTileY(), 0, false);
         }

         return true;
      }, "Remove object");
   }

   static {
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Start chest", LootTablePresets.startChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Ruins chest", LootTablePresets.surfaceRuinsChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Crate", LootTablePresets.basicCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Swamp Crate", LootTablePresets.swampCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Desert Crate", LootTablePresets.desertCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Plains Crate", LootTablePresets.plainsCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Deep crate", LootTablePresets.basicDeepCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Snow Deep crate", LootTablePresets.snowDeepCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Desert Deep crate", LootTablePresets.desertDeepCrate));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Basic cave chest", LootTablePresets.basicCaveChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Snow cave chest", LootTablePresets.snowCaveChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Swamp cave chest", LootTablePresets.swampCaveChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Desert cave chest", LootTablePresets.desertCaveChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Plains cave chest", LootTablePresets.plainsCaveChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Basic cave ruins chest", LootTablePresets.basicCaveRuinsChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Snow cave ruins chest", LootTablePresets.snowCaveRuinsChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Swamp cave ruins chest", LootTablePresets.swampCaveRuinsChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Desert cave ruins chest", LootTablePresets.desertCaveRuinsChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Dungeon chest", LootTablePresets.dungeonChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Pirate chest", LootTablePresets.pirateChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Pirate display stand", LootTablePresets.pirateDisplayStand));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Deep cave chest", LootTablePresets.deepCaveChest));
      lootTables.add(new LootTableTestGameTool.LabeledLootTable("Abandoned mine chest", LootTablePresets.abandonedMineChest));
   }

   public static class LabeledLootTable {
      public final String name;
      public final LootTable table;

      public LabeledLootTable(String name, LootTable table) {
         this.name = name;
         this.table = table;
      }
   }
}
