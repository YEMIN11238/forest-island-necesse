package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.entity.objectEntity.HuginStatueObjectEntity;
import necesse.level.maps.Level;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.ValheimHuginPreset;

public class ValheimHuginWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(HuginStatueObjectEntity.fogTileRange.maxRange * 2, HuginStatueObjectEntity.fogTileRange.maxRange * 2);

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
            100,
            this.size,
            new String[]{"villages", "minibiomes", "loot"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return ValheimHuginWorldPreset.this.runCornerCheck(
                     tileX, tileY, ValheimHuginWorldPreset.this.size.width, ValheimHuginWorldPreset.this.size.height, new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return !generatorStack.isSurfaceOceanOrRiver(tileX, tileY);
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
                        level, tile.x, tile.y, ValheimHuginWorldPreset.this.size.width, ValheimHuginWorldPreset.this.size.height
                     );
                     Preset preset = new ValheimHuginPreset(random);
                     preset.applyToLevel(level, tile.x, tile.y);
                  }
               }
            );
         }
      }
   }
}
