package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.presets.MimeGardenPreset;
import necesse.level.maps.presets.PresetUtils;

public class MimeGardenWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(9, 9);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.FOREST.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.FOREST, 0.001F);

      for (int i = 0; i < total; i++) {
         final Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.FOREST,
            50,
            this.size,
            new String[]{"loot", "villages"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return MimeGardenWorldPreset.this.runCornerCheck(
                     tileX,
                     tileY,
                     MimeGardenWorldPreset.this.size.width,
                     MimeGardenWorldPreset.this.size.height,
                     new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return !generatorStack.isSurfaceOceanOrBeach(tileX, tileY)
                              && generatorStack.getLazyBiomeID(tileX, tileY) == BiomeRegistry.FOREST.getID();
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
                  WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, MimeGardenWorldPreset.this.size.width, MimeGardenWorldPreset.this.size.height);
                  MimeGardenPreset preset = new MimeGardenPreset(random);
                  PresetUtils.clearMobsInPreset(preset, level, tile.x, tile.y);
                  preset.applyToLevel(level, tile.x, tile.y);
               }
            }).setRemoveIfWithinSpawnRegionRange(4);
         }
      }
   }
}
