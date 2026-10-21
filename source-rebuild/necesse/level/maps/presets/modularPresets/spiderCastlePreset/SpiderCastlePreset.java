package necesse.level.maps.presets.modularPresets.spiderCastlePreset;

import java.awt.Point;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.ModularGeneration;
import necesse.level.maps.generationModules.PlacedPreset;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionBiome;
import necesse.level.maps.presets.modularPresets.ModularPreset;
import necesse.level.maps.presets.modularPresets.abandonedMinePreset.AbandonedMineHallwayPreset;

public class SpiderCastlePreset extends ModularPreset {
   public final int wall;
   public final int floor;
   public GameRandom random;

   public SpiderCastlePreset(int sectionWidth, int sectionHeight, int openingSize, int openingDepth, GameRandom random) {
      super(sectionWidth, sectionHeight, 7, openingSize, openingDepth);
      this.random = random;
      this.overlap = true;
      this.wall = ObjectRegistry.getObjectID("deepstonewall");
      this.floor = TileRegistry.deepStoneFloorID;
   }

   public SpiderCastlePreset(int sectionWidth, int sectionHeight, GameRandom random) {
      this(sectionWidth, sectionHeight, 5, 1, random);
   }

   public SpiderCastlePreset(int sectionWidth, int sectionHeight) {
      this(sectionWidth, sectionHeight, null);
   }

   protected SpiderCastlePreset newModularObject(int sectionWidth, int sectionHeight, int sectionRes, int openingSize, int openingDepth) {
      return new SpiderCastlePreset(sectionWidth, sectionHeight, openingSize, openingDepth, this.random);
   }

   public static Point generateSpiderCasteOnLevel(Level level, GameRandom random, AltarData altarData) {
      boolean hasBiggerArenaPerk = altarData.hasPerk(IncursionPerksRegistry.BIGGER_ARENA);
      int cellWidth = random.getIntBetween(10, 12);
      int cellHeight = random.getIntBetween(8, 12);
      if (hasBiggerArenaPerk) {
         cellWidth += 2;
         cellHeight++;
      }

      ModularGeneration mg = new ModularGeneration(level, cellWidth, cellHeight, 7, 5, 1) {
         @Override
         public Point getStartCell() {
            return new Point((this.cellsWidth - this.startPreset.sectionWidth) / 2, (this.cellsHeight - this.startPreset.sectionHeight) / 2);
         }
      };
      int xOffset = level.tileWidth / 2 - mg.cellRes * mg.cellsWidth / 2;
      int yOffset = level.tileHeight / 2 - mg.cellRes * mg.cellsHeight / 2;
      SpiderCastlePreset throneRoom;
      if (hasBiggerArenaPerk) {
         throneRoom = new SpiderCastleThroneRoomClearedPreset(mg.random);
      } else {
         throneRoom = new SpiderCastleThroneRoomPreset(mg.random);
      }

      mg.setStartPreset(throneRoom);
      Point spawnCell = mg.initGeneration(xOffset, yOffset);
      int spawnLevelTileX = ModularGeneration.getCellRealPos(spawnCell.x, mg.cellRes) + mg.cellRes * throneRoom.sectionWidth / 2;
      int spawnLevelTileY = ModularGeneration.getCellRealPos(spawnCell.y, mg.cellRes) + mg.cellRes * throneRoom.sectionHeight / 2;
      mg.addPreset(new SpiderCastleHallwayCornerPreset(mg.random), 75);
      mg.addPreset(new SpiderCastleBigSpiderRoomPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleCrossRoomPreset(mg.random), 25);
      mg.addPreset(new SpiderCastleSmallCrossRoomPreset(mg.random), 35);
      mg.addPreset(new SpiderCastleLibraryPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleDoubleDoorHallwayPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleHallwayUTurnPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleLRoomPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleSecretTunnelRoomPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleSecretTunnelHallwayPreset(mg.random), 75);
      mg.addPreset(new SpiderCastleLongHallwayPreset(mg.random), 50);
      mg.addPreset(new SpiderCastleHallwayStraight1Preset(mg.random), 100);
      mg.addPreset(new SpiderCastleHallwayStraight2Preset(mg.random), 100);
      mg.addPreset(new SpiderCastleHallwayStraight3Preset(mg.random), 100);
      mg.addPreset(new SpiderCastleHallwayStraight4Preset(mg.random), 100);
      mg.addPreset(new SpiderCastleTRoom1Preset(mg.random), 75);
      mg.addPreset(new SpiderCastleTRoom2Preset(mg.random), 75);
      mg.addPreset(new SpiderCastleTRoom3Preset(mg.random), 75);
      mg.addPreset(new SpiderCastleDeadEnd1Preset(mg.random), 25);
      mg.addPreset(new SpiderCastleDeadEnd2Preset(mg.random), 25);
      mg.addPreset(new SpiderCastleDeadEnd3Preset(mg.random), 25);
      mg.addPreset(new SpiderCastleDeadEnd4Preset(mg.random), 25);
      mg.tickGeneration(xOffset, yOffset, Integer.MAX_VALUE);

      for (PlacedPreset placedPreset : mg.getPlacedPresets()) {
         if (placedPreset.preset instanceof AbandonedMineHallwayPreset) {
            fixHallway(level, mg, xOffset, yOffset, placedPreset);
         }
      }

      mg.endGeneration();
      IncursionBiome.addReturnPortal(level, (spawnLevelTileX + xOffset) * 32 + 0.5F, (spawnLevelTileY + yOffset) * 32);
      return new Point(spawnLevelTileX + xOffset, spawnLevelTileY + yOffset);
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
}
