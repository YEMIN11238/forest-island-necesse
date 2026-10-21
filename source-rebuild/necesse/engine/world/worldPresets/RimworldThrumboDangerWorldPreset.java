package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.RimworldThrumboDangerPreset;

public class RimworldThrumboDangerWorldPreset extends WorldPreset {
   public static Dimension size = new Dimension(30, 30);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.PLAINS.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.PLAINS, 0.001F);

      for (int i = 0; i < total; i++) {
         final Point tile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.PLAINS,
            100,
            size,
            new String[]{"villages", "minibiomes", "loot"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return RimworldThrumboDangerWorldPreset.this.runCornerCheck(
                     tileX,
                     tileY,
                     RimworldThrumboDangerWorldPreset.size.width,
                     RimworldThrumboDangerWorldPreset.size.height,
                     new WorldPreset.ValidTilePredicate() {
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
               size,
               "loot",
               new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     WorldPreset.ensureRegionsAreGenerated(
                        level, tile.x, tile.y, RimworldThrumboDangerWorldPreset.size.width, RimworldThrumboDangerWorldPreset.size.height
                     );
                     Preset preset = new RimworldThrumboDangerPreset(random, TileRegistry.sandID);
                     preset = PresetUtils.randomizeRotationAndMirror(preset, random);
                     preset.applyToLevel(level, tile.x, tile.y);
                  }
               }
            );
         }
      }
   }
}
