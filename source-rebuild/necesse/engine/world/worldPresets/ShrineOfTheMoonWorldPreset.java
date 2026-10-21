package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.ShrineOfTheMoonPreset;

public class ShrineOfTheMoonWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(30, 24);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.PLAINS.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.PLAINS, 0.002F);

      for (int i = 0; i < total; i++) {
         final Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.PLAINS,
            100,
            this.size,
            new String[]{"loot", "minibiomes"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return ShrineOfTheMoonWorldPreset.this.runGridCheck(
                     tileX,
                     tileY,
                     ShrineOfTheMoonWorldPreset.this.size.width,
                     ShrineOfTheMoonWorldPreset.this.size.height,
                     4,
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
                        level, tile.x, tile.y, ShrineOfTheMoonWorldPreset.this.size.width, ShrineOfTheMoonWorldPreset.this.size.height
                     );
                     Preset preset = new ShrineOfTheMoonPreset();
                     preset = PresetUtils.randomizeXMirror(preset, random);
                     preset.applyToLevel(level, tile.x, tile.y);
                  }
               }
            );
         }
      }
   }
}
