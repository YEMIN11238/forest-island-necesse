package necesse.level.maps.generationModules;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageDogStatuePreset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageFarm1Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageFarm2Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageFarm3Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageFlatgrassPaintingPreset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse1Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse2Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse3Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse4Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse5Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse6Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse7Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse8Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillageHouse9Preset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillagePathPreset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillagePreset;
import necesse.level.maps.presets.set.VillageSet;

public class VillageGeneration {
   private final Level level;
   private final int maxSize;
   public GameRandom random;
   private final VillageSet defaultSet;
   private final TicketSystemList<VillageSet> villageSets;
   private VillageSet pathSet;
   private final ArrayList<VillageGeneration.AddedPreset> basicPresets;
   private final ArrayList<VillageGeneration.AddedPreset> edgePresets;

   public VillageGeneration(Level level, int maxSize, TicketSystemList<VillageSet> villageSets, GameRandom random) {
      this.defaultSet = VillageSet.defaultSet;
      this.basicPresets = new ArrayList<>();
      this.edgePresets = new ArrayList<>();
      this.level = level;
      this.maxSize = maxSize;
      this.villageSets = villageSets;
      this.random = random;
   }

   private static TicketSystemList<VillageSet> toTicketSystem(VillageSet villageSet) {
      TicketSystemList<VillageSet> ticketSystem = new TicketSystemList<>();
      ticketSystem.addObject(100, villageSet);
      return ticketSystem;
   }

   public VillageGeneration addStandardPresets() {
      this.addBasicPreset(new VillageHouse1Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse2Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse5Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse6Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse7Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse8Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse9Preset(this.random), 8, 1);
      this.addBasicPreset(new VillageHouse1Preset(this.random), 3, 2);
      this.addBasicPreset(new VillageHouse2Preset(this.random), 3, 2);
      this.addBasicPreset(new VillageHouse5Preset(this.random), 3, 2);
      this.addBasicPreset(new VillageHouse6Preset(this.random), 3, 2);
      this.addBasicPreset(new VillageHouse7Preset(this.random), 3, 2);
      this.addBasicPreset(new VillageHouse8Preset(this.random), 3, 2);
      this.addBasicPreset(new VillageHouse9Preset(this.random), 3, 2);
      this.addEdgePreset(new VillageFarm1Preset(this.random), 8, 2);
      this.addEdgePreset(new VillageFarm2Preset(this.random), 25, 1);
      this.addEdgePreset(new VillageFarm3Preset(this.random), 25, 1);
      this.addEdgePreset(new VillageFarm2Preset(this.random), 5, 1);
      this.addEdgePreset(new VillageFarm3Preset(this.random), 5, 1);
      this.addEdgePreset(new VillageHouse3Preset(this.random), 50, 1);
      if (this.random.getChance(0.1F)) {
         this.addEdgePreset(new VillageFlatgrassPaintingPreset(this.random), 50, 1);
      } else {
         this.addEdgePreset(new VillageHouse4Preset(this.random), 50, 1);
      }

      if (this.random.getChance(0.2F)) {
         this.addBasicPreset(new VillageDogStatuePreset(this.random), 50, 1);
      }

      return this;
   }

   public VillageGeneration addBasicPreset(VillagePreset preset, int tickets, int maxUsage) {
      this.basicPresets.add(new VillageGeneration.AddedPreset(preset, tickets, maxUsage));
      return this;
   }

   public VillageGeneration addEdgePreset(VillagePreset preset, int tickets, int maxUsage) {
      this.edgePresets.add(new VillageGeneration.AddedPreset(preset, tickets, maxUsage));
      return this;
   }

   public void generate(OneWorldNPCVillageData villageData, int centerTileX, int centerTileY) {
      int cellsSize = this.maxSize / 3;
      VillageModularGeneration mg = new VillageModularGeneration(this.level, cellsSize, cellsSize, 3, 3, 1) {
         @Override
         public Point getStartCell() {
            return new Point(this.cellsWidth / 2, this.cellsHeight / 2);
         }
      };
      mg.random = this.random;
      int xOffset = centerTileX - mg.cellRes * mg.cellsWidth / 2;
      int yOffset = centerTileY - mg.cellRes * mg.cellsHeight / 2;
      Rectangle generationArea = new Rectangle(
         (mg.getCellRealX(0) + xOffset) * 32, (mg.getCellRealY(0) + yOffset) * 32, mg.cellsWidth * mg.cellRes * 32, mg.cellsHeight * mg.cellRes * 32
      );
      this.level.entityManager.mobs.streamInRegionsShape(generationArea, 0).filter(m -> generationArea.contains(m.getX(), m.getY())).forEach(Mob::remove);
      this.pathSet = this.villageSets.getOneOfMostCommon(this.random);
      VillagePreset path = this.defaultSet.replaceWith(this.pathSet, new VillagePathPreset());
      mg.setStartPreset(path);
      mg.initGeneration(xOffset, yOffset);
      mg.random = this.random;

      for (VillageGeneration.AddedPreset preset : this.basicPresets) {
         preset.fixPathTiles();
      }

      for (VillageGeneration.AddedPreset preset : this.edgePresets) {
         preset.fixPathTiles();
      }

      Point startCell = mg.getStartCell();
      int reach = Math.max(this.random.getIntBetween(cellsSize / 6, cellsSize / 3), 2);

      for (int i = 1; i <= reach; i++) {
         mg.applyPreset(path, new Point(startCell.x - i, startCell.y), false, false, xOffset, yOffset, new Point(startCell.x - (i - 1), startCell.y));
         mg.applyPreset(path, new Point(startCell.x + i, startCell.y), false, false, xOffset, yOffset, new Point(startCell.x + (i - 1), startCell.y));
         mg.applyPreset(path, new Point(startCell.x, startCell.y - i), false, false, xOffset, yOffset, new Point(startCell.x, startCell.y - (i - 1)));
         mg.applyPreset(path, new Point(startCell.x, startCell.y + i), false, false, xOffset, yOffset, new Point(startCell.x, startCell.y + (i - 1)));
      }

      VillagePathPreset path1;
      mg.addPreset(path1 = this.defaultSet.replaceWith(this.pathSet, new VillagePathPreset(true, true, true, true)), 6);
      VillagePathPreset path2;
      mg.addPreset(path2 = this.defaultSet.replaceWith(this.pathSet, new VillagePathPreset(true, false, true, false)), 3);
      VillagePathPreset path3;
      mg.addPreset(path3 = this.defaultSet.replaceWith(this.pathSet, new VillagePathPreset(false, true, false, true)), 3);
      mg.tickGeneration(xOffset, yOffset, 4);

      for (VillageGeneration.AddedPreset basicPreset : this.basicPresets) {
         mg.addPreset(basicPreset.preset, basicPreset.tickets, basicPreset.maxUsage);
      }

      mg.tickGeneration(xOffset, yOffset, this.maxSize / 2);

      for (VillageGeneration.AddedPreset edgePreset : this.edgePresets) {
         mg.addPreset(edgePreset.preset, edgePreset.tickets, edgePreset.maxUsage);
      }

      mg.tickGeneration(xOffset, yOffset, this.maxSize * 5);
      ArrayList<VillageGeneration.VillagePathLocation> closedCells = new ArrayList<>();
      ArrayList<VillageGeneration.VillagePathLocation> openCells = new ArrayList<>();
      VillageGeneration.VillagePathLocation startLocation = new VillageGeneration.VillagePathLocation(null, mg.getPlacedPreset(startCell));
      startLocation.markGoesSomewhere();
      openCells.add(startLocation);

      while (!openCells.isEmpty()) {
         VillageGeneration.VillagePathLocation current = openCells.remove(0);
         if (current.preset.isPresetOpenTop()) {
            this.tickPathRemove(mg, current, current.preset.cell.x, current.preset.cell.y - 1, closedCells, openCells);
         }

         if (current.preset.isPresetOpenBot()) {
            this.tickPathRemove(mg, current, current.preset.cell.x, current.preset.cell.y + 1, closedCells, openCells);
         }

         if (current.preset.isPresetOpenRight()) {
            this.tickPathRemove(mg, current, current.preset.cell.x + 1, current.preset.cell.y, closedCells, openCells);
         }

         if (current.preset.isPresetOpenLeft()) {
            this.tickPathRemove(mg, current, current.preset.cell.x - 1, current.preset.cell.y, closedCells, openCells);
         }

         closedCells.add(current);
      }

      for (VillageGeneration.VillagePathLocation closedCell : closedCells) {
         int realX = mg.getCellRealX(closedCell.preset.cell.x) + xOffset;
         int realY = mg.getCellRealY(closedCell.preset.cell.y) + yOffset;
         if (!closedCell.goesSomewhere()) {
            closedCell.preset.replacedLevel.applyToLevel(this.level, realX, realY);

            for (VillageGeneration.VillagePathLocation parent = closedCell.parent; parent != null && parent.goesSomewhere(); parent = parent.parent) {
               mg.openCell(closedCell.parent.preset.cell);
            }

            mg.removePlacedPreset(closedCell.preset);
         }
      }

      mg.removeAllPresets(path1);
      mg.removeAllPresets(path2);
      mg.removeAllPresets(path3);
      mg.tickGeneration(xOffset, yOffset, this.maxSize * 5);
      mg.endGeneration();
      if (villageData != null) {
         for (PlacedPreset placedPreset : mg.getPlacedPresets()) {
            int startTileX = mg.getCellRealX(placedPreset.cell.x)
               + xOffset
               - (placedPreset.xMirror ? placedPreset.preset.getMirroredX(0) - (mg.cellRes - 1) : 0);
            int startTileY = mg.getCellRealY(placedPreset.cell.y)
               + yOffset
               - (placedPreset.yMirror ? placedPreset.preset.getMirroredY(0) - (mg.cellRes - 1) : 0);
            if (placedPreset.x == 0 && placedPreset.y == 0) {
               for (int x = 0; x < placedPreset.preset.width; x++) {
                  int realTileX = startTileX + x;
                  int presetX = placedPreset.xMirror ? placedPreset.preset.getMirroredX(x) : x;

                  for (int y = 0; y < placedPreset.preset.height; y++) {
                     int realTileY = startTileY + y;
                     int presetY = placedPreset.yMirror ? placedPreset.preset.getMirroredY(y) : y;
                     int tileID = placedPreset.preset.getTile(presetX, presetY);
                     if (tileID != -1 && tileID != 0) {
                        for (Point offset : Level.adjacentGettersWithCenter) {
                           villageData.addVillageTile(realTileX + offset.x, realTileY + offset.y);
                        }
                     } else {
                        for (int layerID = 0; layerID < ObjectLayerRegistry.getTotalLayers(); layerID++) {
                           int objectID = placedPreset.preset.getObject(layerID, presetX, presetY);
                           if (objectID != -1 && objectID != 0) {
                              for (Point offset : Level.adjacentGettersWithCenter) {
                                 villageData.addVillageTile(realTileX + offset.x, realTileY + offset.y);
                              }
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void tickPathRemove(
      ModularGeneration mg,
      VillageGeneration.VillagePathLocation current,
      int nextX,
      int nextY,
      List<VillageGeneration.VillagePathLocation> closedCells,
      List<VillageGeneration.VillagePathLocation> openCells
   ) {
      PlacedPreset nextPreset = mg.getPlacedPreset(new Point(nextX, nextY));
      if (nextPreset != null) {
         if (nextPreset.preset instanceof VillagePathPreset) {
            if (closedCells.stream().noneMatch(c -> c.preset.cell.equals(new Point(nextX, nextY)))) {
               openCells.add(new VillageGeneration.VillagePathLocation(current, nextPreset));
            }
         } else {
            current.markGoesSomewhere();
         }
      }
   }

   private class AddedPreset {
      public final VillagePreset preset;
      public final VillageSet villageSet;
      public final int tickets;
      public final int maxUsage;

      public AddedPreset(VillagePreset preset, int tickets, int maxUsage) {
         preset.replaceObject("storagebox", "sprucechest");
         this.villageSet = VillageGeneration.this.villageSets.getRandomObject(VillageGeneration.this.random);
         preset = VillageGeneration.this.defaultSet.replaceWith(this.villageSet, preset);
         this.preset = preset;
         this.tickets = tickets;
         this.maxUsage = maxUsage;
      }

      public void fixPathTiles() {
         this.preset.replaceTile(this.villageSet.pathTile, VillageGeneration.this.pathSet.pathTile);
      }
   }

   private static class VillagePathLocation {
      public final VillageGeneration.VillagePathLocation parent;
      public final PlacedPreset preset;
      private boolean goesSomewhere = false;

      public VillagePathLocation(VillageGeneration.VillagePathLocation parent, PlacedPreset preset) {
         this.parent = parent;
         this.preset = preset;
      }

      public void markGoesSomewhere() {
         this.goesSomewhere = true;
         if (this.parent != null) {
            this.parent.markGoesSomewhere();
         }
      }

      public boolean goesSomewhere() {
         return this.goesSomewhere;
      }
   }
}
