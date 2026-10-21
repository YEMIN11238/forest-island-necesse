package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.LinesGeneration;

public class KatanaRuinsWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(9, 9);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.PLAINS.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.PLAINS, 0.004F);

      for (int i = 0; i < total; i++) {
         final Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.PLAINS,
            50,
            this.size,
            new String[]{"loot", "villages"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return KatanaRuinsWorldPreset.this.runCornerCheck(
                     tileX,
                     tileY,
                     KatanaRuinsWorldPreset.this.size.width,
                     KatanaRuinsWorldPreset.this.size.height,
                     new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return !generatorStack.isSurfaceOceanOrBeach(tileX, tileY)
                              && generatorStack.getLazyBiomeID(tileX, tileY) == BiomeRegistry.PLAINS.getID();
                        }
                     }
                  );
               }
            }
         );
         if (tile != null) {
            presetsRegion.addPreset(this, tile.x, tile.y, this.size, new String[]{"loot", "villages"}, new LevelPresetsRegion.WorldPresetPlaceFunction() {
               @Override
               public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                  WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, KatanaRuinsWorldPreset.this.size.width, KatanaRuinsWorldPreset.this.size.height);
                  int tileX = tile.x + KatanaRuinsWorldPreset.this.size.width / 2;
                  int tileY = tile.y + KatanaRuinsWorldPreset.this.size.height / 2;
                  level.setObject(tileX, tileY, ObjectRegistry.getObjectID("katanastone"));
                  GameTile gravelTile = TileRegistry.getTile(TileRegistry.gravelID);
                  LinesGeneration lg = new LinesGeneration(tileX, tileY).addRandomArms(random, 6, 6.0F, 9.0F, 2.0F, 3.0F);
                  CellAutomaton cellAutomaton = lg.doCellularAutomaton(random);
                  cellAutomaton.forEachTile(level, (newLevel, cellX, cellY) -> {
                     GameObject object = newLevel.getObject(cellX, cellY);
                     if ((object.getID() == 0 || object.isGrass) && !newLevel.getTile(cellX, cellY).isLiquid && random.getChance(0.3F)) {
                        gravelTile.placeTile(newLevel, cellX, cellY, false);
                     }
                  });
                  ArrayList<Point> validStoneTiles = new ArrayList<>();

                  for (int x = -5; x <= 5; x++) {
                     for (int y = -5; y <= 5; y++) {
                        GameObject object = level.getObject(tileX + x, tileY + y);
                        if ((object.getID() == 0 || object.isGrass) && !level.getTile(tileX + x, tileY + y).isFloor) {
                           validStoneTiles.add(new Point(tileX + x, tileY + y));
                        }
                     }
                  }

                  GameObject stoneObject = ObjectRegistry.getObject("surfacerocksmall");

                  for (int j = 0; j < 10 && !validStoneTiles.isEmpty(); j++) {
                     Point stoneTile = validStoneTiles.remove(random.nextInt(validStoneTiles.size()));
                     stoneObject.placeObject(level, stoneTile.x, stoneTile.y, 0, false);
                  }
               }
            }).setRemoveIfWithinSpawnRegionRange(1);
         }
      }
   }
}
