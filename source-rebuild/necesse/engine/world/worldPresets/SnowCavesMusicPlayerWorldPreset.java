package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.presets.MusicPlayerPreset1;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;

public class SnowCavesMusicPlayerWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(20, 18);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.SNOW.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.SNOW, 0.003F);

      for (int i = 0; i < total; i++) {
         final Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.SNOW,
            20,
            this.size,
            "loot",
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return SnowCavesMusicPlayerWorldPreset.this.runCornerCheck(
                     tileX,
                     tileY,
                     SnowCavesMusicPlayerWorldPreset.this.size.width,
                     SnowCavesMusicPlayerWorldPreset.this.size.height,
                     new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return !generatorStack.isCaveRiverOrLava(tileX, tileY);
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
                        level, tile.x, tile.y, SnowCavesMusicPlayerWorldPreset.this.size.width, SnowCavesMusicPlayerWorldPreset.this.size.height
                     );
                     Preset preset = new MusicPlayerPreset1(random);
                     preset = PresetUtils.randomizeXMirror(preset, random);
                     preset = PresetUtils.randomizeYMirror(preset, random);
                     preset.applyToLevel(level, tile.x, tile.y);
                  }
               }
            );
         }
      }
   }
}
