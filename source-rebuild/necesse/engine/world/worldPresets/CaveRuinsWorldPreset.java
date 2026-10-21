package necesse.engine.world.worldPresets;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.caveRooms.CaveRuins;

public class CaveRuinsWorldPreset extends WorldPreset {
   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER) || presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER);
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      if (presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
         int total = getTotalPoints(random, presetsRegion, 0.1F);

         for (int i = 0; i < total; i++) {
            final Point tile = findRandomPresetTile(random, presetsRegion, 5, CaveRuins.MAX_DIMENSIONS, "loot", new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return !generatorStack.isCaveRiverOrLava(tileX + CaveRuins.MAX_DIMENSIONS.width / 2, tileY + CaveRuins.MAX_DIMENSIONS.height / 2);
               }
            });
            if (tile != null) {
               final int finalI = i;
               presetsRegion.addPreset(this, tile.x, tile.y, CaveRuins.MAX_DIMENSIONS, "loot", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, CaveRuins.MAX_DIMENSIONS.width, CaveRuins.MAX_DIMENSIONS.height);
                     Biome biome = level.getBiome(tile.x + CaveRuins.MAX_DIMENSIONS.width / 2, tile.y + CaveRuins.MAX_DIMENSIONS.height / 2);
                     Preset caveRuins = biome.getNewCaveRuinsPreset(random, new AtomicInteger(finalI));
                     if (caveRuins != null) {
                        caveRuins = PresetUtils.randomizeRotationAndMirror(caveRuins, random);
                        int deltaWidth = CaveRuins.MAX_DIMENSIONS.width - caveRuins.width;
                        int deltaHeight = CaveRuins.MAX_DIMENSIONS.height - caveRuins.height;
                        int xOffset = deltaWidth > 0 ? random.nextInt(deltaWidth) : 0;
                        int yOffset = deltaHeight > 0 ? random.nextInt(deltaHeight) : 0;
                        caveRuins.applyToLevel(level, tile.x + xOffset, tile.y + yOffset);
                     }
                  }
               });
            }
         }
      } else if (presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
         int total = getTotalPoints(random, presetsRegion, 0.1F);

         for (int i = 0; i < total; i++) {
            final Point tile = findRandomPresetTile(random, presetsRegion, 5, CaveRuins.MAX_DIMENSIONS, "loot", new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return !generatorStack.isDeepCaveRiverOrLava(tileX + CaveRuins.MAX_DIMENSIONS.width / 2, tileY + CaveRuins.MAX_DIMENSIONS.height / 2);
               }
            });
            if (tile != null) {
               final int finalI = i;
               presetsRegion.addPreset(this, tile.x, tile.y, CaveRuins.MAX_DIMENSIONS, "loot", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, CaveRuins.MAX_DIMENSIONS.width, CaveRuins.MAX_DIMENSIONS.height);
                     Biome biome = level.getBiome(tile.x + CaveRuins.MAX_DIMENSIONS.width / 2, tile.y + CaveRuins.MAX_DIMENSIONS.height / 2);
                     Preset caveRuins = biome.getNewDeepCaveRuinsPreset(random, new AtomicInteger(finalI));
                     if (caveRuins != null) {
                        caveRuins = PresetUtils.randomizeRotationAndMirror(caveRuins, random);
                        int deltaWidth = CaveRuins.MAX_DIMENSIONS.width - caveRuins.width;
                        int deltaHeight = CaveRuins.MAX_DIMENSIONS.height - caveRuins.height;
                        int xOffset = deltaWidth > 0 ? random.nextInt(deltaWidth) : 0;
                        int yOffset = deltaHeight > 0 ? random.nextInt(deltaHeight) : 0;
                        caveRuins.applyToLevel(level, tile.x + xOffset, tile.y + yOffset);
                     }
                  }
               });
            }
         }
      }
   }
}
