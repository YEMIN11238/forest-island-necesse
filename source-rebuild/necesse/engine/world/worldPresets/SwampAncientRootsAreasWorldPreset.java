package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
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

public class SwampAncientRootsAreasWorldPreset extends WorldPreset {
   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.SWAMP.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.SWAMP, 0.04F);

      for (int i = 0; i < total; i++) {
         final Dimension size = new Dimension(15, 15);
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
                  return SwampAncientRootsAreasWorldPreset.this.runCornerCheck(
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
               .addRandomArms(random, random.getIntBetween(2, 4), 8.0F, 18.0F, 2.0F, 2.0F);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               presetsRegion.addPreset(this, lg.getOccupiedTileRectangle(), "minibiomes", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     GameTile ancientRootsTile = TileRegistry.getTile(TileRegistry.ancientRootsID);
                     GameObject wildAncientRoots = ObjectRegistry.getObject("wildancientroots");
                     GameObject mossySkull = ObjectRegistry.getObject("mosscoveredskull");
                     CellAutomaton ca = lg.doCellularAutomaton(random);
                     ca.forEachTile(level, (level1, tileX, tileY) -> {
                        level.setTile(tileX, tileY, ancientRootsTile.getID());
                        level.setObject(tileX, tileY, 0);
                        if (level.getObjectID(tileX, tileY) == 0) {
                           if (random.getChance(0.4F)) {
                              wildAncientRoots.placeObject(level, tileX, tileY, 0, false);
                           } else if (random.getChance(0.02F)) {
                              mossySkull.placeObject(level, tileX, tileY, 0, false);
                           }
                        }
                     });
                  }
               });
            }
         }
      }
   }
}
