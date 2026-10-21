package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.LinesGeneration;

public class SwampCaveSporesWorldPreset extends WorldPreset {
   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.SWAMP.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.SWAMP, 0.02F);

      for (int i = 0; i < total; i++) {
         final Dimension size = new Dimension(24, 24);
         Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.SWAMP,
            20,
            size,
            "minibiomes",
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return SwampCaveSporesWorldPreset.this.runCornerCheck(
                     tileX - size.width / 2, tileY - size.height / 2, size.width, size.height, new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return !generatorStack.isCaveRiverOrLava(tileX, tileY) && generatorStack.getLazyBiomeID(tileX, tileY) == BiomeRegistry.SWAMP.getID();
                        }
                     }
                  );
               }
            }
         );
         if (tile != null) {
            final LinesGenerationWorldPreset lg = new LinesGenerationWorldPreset(tile.x + size.width / 2, tile.y + size.height / 2)
               .addRandomArms(random, 4, 8.0F, 12.0F, 8.0F, 12.0F);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               presetsRegion.addPreset(this, lg.getOccupiedTileRectangle(), "minibiomes", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     GameObject swampSpore = ObjectRegistry.getObject("swampspore");
                     GameObject swampGrass = ObjectRegistry.getObject("swampgrass");
                     GameObject swampCrate = ObjectRegistry.getObject("swampcrate");
                     CellAutomaton ca = lg.doCellularAutomaton(random);
                     ca.forEachTile(level, (level1, tileX, tileY) -> {
                        if (random.getChance(0.03F)) {
                           level.setObject(tileX, tileY, swampCrate.getID());
                        } else if (random.getChance(0.6F)) {
                           level.setObject(tileX, tileY, swampGrass.getID());
                        } else {
                           level.setObject(tileX, tileY, 0);
                        }
                     });

                     for (LinesGeneration line : lg.getLines()) {
                        swampSpore.placeObject(level, line.x2, line.y2, 0, false);
                     }
                  }
               });
            }
         }
      }
   }

   public void generateExtraContent(
      GameRandom random, Level level, PerformanceTimerManager timer, LinesGenerationWorldPreset linesGeneration, CellAutomaton cellAutomaton
   ) {
   }
}
