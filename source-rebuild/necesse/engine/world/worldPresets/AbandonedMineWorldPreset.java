package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.ModularGeneration;
import necesse.level.maps.generationModules.PlacedPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineBedroomPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineBedroomRPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineBlacksmithPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineBlacksmithRPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineCrateRoomPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineDiningPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineDiningRPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineHallwayPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineLibraryPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineLibraryRPreset;

public class AbandonedMineWorldPreset extends WorldPreset {
   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER);
   }

   @Override
   public void addToRegion(GameRandom random, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.FOREST, 0.005F);

      for (int i = 0; i < total; i++) {
         final int cellsRes = 6;
         final int cellsWidth = random.getIntBetween(6, 10);
         final int cellsHeight = random.getIntBetween(6, 10);
         Dimension dimension = new Dimension(cellsWidth * cellsRes + 1, cellsHeight * cellsRes + 1);
         final Point tile = findRandomBiomePresetTile(
            random, presetsRegion, generatorStack, BiomeRegistry.FOREST, 50, dimension, new String[]{"minibiomes", "loot"}, null
         );
         if (tile != null) {
            presetsRegion.addPreset(
               this,
               tile.x,
               tile.y,
               dimension,
               new String[]{"minibiomes", "loot"},
               new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     ModularGeneration mg = new ModularGeneration(level, cellsWidth, cellsHeight, cellsRes, 5, 1) {
                        @Override
                        public Point getStartCell() {
                           return new Point((this.cellsWidth - this.startPreset.sectionWidth) / 2, (this.cellsHeight - this.startPreset.sectionHeight) / 2);
                        }
                     };
                     int xOffset = tile.x;
                     int yOffset = tile.y;
                     AbandonedMineHallwayPreset path = new AbandonedMineHallwayPreset(mg.random, true, true, true, true);
                     mg.setStartPreset(path);
                     mg.initGeneration(xOffset, yOffset);
                     Point startCell = mg.getStartCell();
                     float hallwayChance = 0.1F;
                     ArrayList<AbandonedMineWorldPreset.HallwayCell> openHallwayCells = new ArrayList<>();
                     ArrayList<AbandonedMineWorldPreset.HallwayCell> closedHallwayCells = new ArrayList<>();

                     for (int ix = 0; ix < 4; ix++) {
                        openHallwayCells.add(new AbandonedMineWorldPreset.HallwayCell(new Point(startCell), ix));
                     }

                     int hallwayCounter = 0;

                     while (!openHallwayCells.isEmpty()) {
                        AbandonedMineWorldPreset.HallwayCell current = openHallwayCells.remove(mg.random.nextInt(openHallwayCells.size()));
                        Point prevCell = current.cell;

                        while (true) {
                           Point nextCell = mg.getNextCell(prevCell, current.dir);
                           Point oneOverCell = mg.getNextCell(nextCell, current.dir);
                           if (closedHallwayCells.stream().anyMatch(c -> c.cell.equals(oneOverCell))
                              || closedHallwayCells.stream().anyMatch(c -> c.cell.equals(nextCell))
                              || nextCell.x < 0
                              || nextCell.x >= mg.cellsWidth
                              || nextCell.y < 0
                              || nextCell.y >= mg.cellsHeight) {
                              closedHallwayCells.add(current);
                              break;
                           }

                           if (current.dir != 0 && current.dir != 2) {
                              int cellOffset = Math.abs(nextCell.x - startCell.x);
                              if (cellOffset % 2 == 0) {
                                 if (mg.random.getChance(hallwayChance)) {
                                    openHallwayCells.add(new AbandonedMineWorldPreset.HallwayCell(nextCell, 0));
                                 }

                                 if (mg.random.getChance(hallwayChance)) {
                                    openHallwayCells.add(new AbandonedMineWorldPreset.HallwayCell(nextCell, 2));
                                 }
                              }
                           } else {
                              int cellOffset = Math.abs(nextCell.y - startCell.y);
                              if (cellOffset % 2 == 0) {
                                 if (mg.random.getChance(hallwayChance)) {
                                    openHallwayCells.add(new AbandonedMineWorldPreset.HallwayCell(nextCell, 1));
                                 }

                                 if (mg.random.getChance(hallwayChance)) {
                                    openHallwayCells.add(new AbandonedMineWorldPreset.HallwayCell(nextCell, 3));
                                 }
                              }
                           }

                           mg.applyPreset(path, nextCell, false, false, xOffset, yOffset, prevCell);
                           hallwayCounter++;
                           prevCell = nextCell;
                        }
                     }

                     for (int floorID : new int[]{TileRegistry.deepStoneBrickFloorID}) {
                        mg.addPreset(new AbandonedMineBedroomPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineBedroomRPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineBlacksmithPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineBlacksmithRPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineCrateRoomPreset(mg.random, floorID), 150);
                        mg.addPreset(new AbandonedMineDiningPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineDiningRPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineLibraryPreset(mg.random, floorID), 100);
                        mg.addPreset(new AbandonedMineLibraryRPreset(mg.random, floorID), 100);
                     }

                     mg.tickGeneration(xOffset, yOffset, (int)(hallwayCounter * 1.5F));

                     for (PlacedPreset placedPreset : mg.getPlacedPresets()) {
                        if (placedPreset.preset instanceof AbandonedMineHallwayPreset) {
                           AbandonedMineWorldPreset.fixHallway(level, mg, xOffset, yOffset, placedPreset);
                        }
                     }

                     mg.endGeneration();
                  }
               }
            );
         }
      }
   }

   private static void fixHallway(Level level, ModularGeneration mg, int xOffset, int yOffset, PlacedPreset preset) {
      for (int i = 0; i < 4; i++) {
         Point nextCell = mg.getNextCell(preset.cell, i);
         PlacedPreset placedPreset = mg.getPlacedPreset(nextCell);
         if (placedPreset == null) {
            preset.preset.closeLevel(level, 0, 0, mg.getCellRealX(preset.cell.x) + xOffset, mg.getCellRealY(preset.cell.y) + yOffset, i, mg.cellRes);
         }
      }
   }

   private static class HallwayCell {
      public final Point cell;
      public final int dir;

      public HallwayCell(Point cell, int dir) {
         this.cell = cell;
         this.dir = dir;
      }
   }
}
