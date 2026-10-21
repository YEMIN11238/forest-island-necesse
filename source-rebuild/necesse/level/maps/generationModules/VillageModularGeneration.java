package necesse.level.maps.generationModules;

import java.awt.Point;
import necesse.level.maps.Level;
import necesse.level.maps.presets.modularPresets.ModularPreset;
import necesse.level.maps.presets.modularPresets.vilagePresets.VillagePreset;

public class VillageModularGeneration extends ModularGeneration {
   public VillageModularGeneration(Level level, int cellsWidth, int cellsHeight, int cellRes, int openingSize, int openingDepth) {
      super(level, cellsWidth, cellsHeight, cellRes, openingSize, openingDepth);
   }

   @Override
   public boolean canApplyPreset(ModularPreset preset, Point cell, boolean xMirror, boolean yMirror, Point fromCell) {
      if (!super.canApplyPreset(preset, cell, xMirror, yMirror, fromCell)) {
         return false;
      }

      if (fromCell == null) {
         return true;
      }

      if (preset instanceof VillagePreset && ((VillagePreset)preset).isPath()) {
         for (int i = 0; i < 4; i++) {
            Point nextCell = this.getNextCell(cell, i);
            if (!nextCell.equals(fromCell)) {
               PlacedPreset pPreset = this.getPlacedPreset(nextCell);
               ModularPreset nextPreset = pPreset == null ? null : pPreset.preset;
               if (nextPreset instanceof VillagePreset && ((VillagePreset)preset).isPath()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public Point getNextCellPlus(Point cell, int dir) {
      dir %= 8;
      if (dir == 0) {
         return new Point(cell.x - 1, cell.y - 1);
      } else if (dir == 1) {
         return new Point(cell.x, cell.y - 1);
      } else if (dir == 2) {
         return new Point(cell.x + 1, cell.y - 1);
      } else if (dir == 3) {
         return new Point(cell.x - 1, cell.y);
      } else if (dir == 4) {
         return new Point(cell.x + 1, cell.y);
      } else if (dir == 5) {
         return new Point(cell.x - 1, cell.y + 1);
      } else if (dir == 6) {
         return new Point(cell.x, cell.y + 1);
      } else {
         return dir == 7 ? new Point(cell.x + 1, cell.y + 1) : null;
      }
   }
}
