package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.presets.CoreKeeperCorePreset;
import necesse.level.maps.presets.Preset;

public class CoreKeeperCoreWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(30, 30);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.SWAMP.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.SWAMP, 0.002F);

      for (int i = 0; i < total; i++) {
         final Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.SWAMP,
            100,
            this.size,
            new String[]{"loot", "minibiomes"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return CoreKeeperCoreWorldPreset.this.runCornerCheck(
                     tileX,
                     tileY,
                     CoreKeeperCoreWorldPreset.this.size.width,
                     CoreKeeperCoreWorldPreset.this.size.height,
                     new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return !generatorStack.isDeepCaveRiverOrLava(tileX, tileY);
                        }
                     }
                  );
               }
            }
         );
         if (tile != null) {
            presetsRegion.addPreset(
               this,
               tile.x,
               tile.y,
               this.size,
               "loot",
               new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     WorldPreset.ensureRegionsAreGenerated(
                        level, tile.x, tile.y, CoreKeeperCoreWorldPreset.this.size.width, CoreKeeperCoreWorldPreset.this.size.height
                     );
                     Preset preset = new CoreKeeperCorePreset(random);
                     preset.applyToLevel(level, tile.x, tile.y);
                  }
               }
            );
         }
      }
   }
}
