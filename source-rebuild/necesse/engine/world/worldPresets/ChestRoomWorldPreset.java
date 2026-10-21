package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;

public class ChestRoomWorldPreset extends WorldPreset {
   protected Dimension size = new Dimension(7, 7);

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER) || presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER);
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      if (presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
         int total = getTotalPoints(random, presetsRegion, 0.05F);

         for (int i = 0; i < total; i++) {
            final Point tile = findRandomPresetTile(
               random,
               presetsRegion,
               20,
               this.size,
               "loot",
               new WorldPreset.ValidTilePredicate() {
                  @Override
                  public boolean isValidPosition(int tileX, int tileY) {
                     return ChestRoomWorldPreset.this.runCornerCheck(
                        tileX,
                        tileY,
                        ChestRoomWorldPreset.this.size.width,
                        ChestRoomWorldPreset.this.size.height,
                        (cornerTileX, cornerTileY) -> !generatorStack.isCaveRiverOrLava(cornerTileX, cornerTileY)
                     );
                  }
               }
            );
            if (tile != null) {
               final int finalI = i;
               presetsRegion.addPreset(this, tile.x, tile.y, this.size, "loot", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, ChestRoomWorldPreset.this.size.width, ChestRoomWorldPreset.this.size.height);
                     Biome biome = level.getBiome(tile.x + ChestRoomWorldPreset.this.size.width / 2, tile.y + ChestRoomWorldPreset.this.size.height / 2);
                     Preset chestRoom = biome.getNewCaveChestRoomPreset(random, new AtomicInteger(finalI));
                     if (chestRoom != null) {
                        chestRoom.applyToLevel(level, tile.x, tile.y);
                     }
                  }
               });
            }
         }
      } else if (presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
         int total = getTotalPoints(random, presetsRegion, 0.05F);

         for (int i = 0; i < total; i++) {
            final Point tile = findRandomPresetTile(
               random,
               presetsRegion,
               20,
               this.size,
               "loot",
               new WorldPreset.ValidTilePredicate() {
                  @Override
                  public boolean isValidPosition(int tileX, int tileY) {
                     return ChestRoomWorldPreset.this.runCornerCheck(
                        tileX,
                        tileY,
                        ChestRoomWorldPreset.this.size.width,
                        ChestRoomWorldPreset.this.size.height,
                        (cornerTileX, cornerTileY) -> !generatorStack.isDeepCaveRiverOrLava(cornerTileX, cornerTileY)
                     );
                  }
               }
            );
            if (tile != null) {
               final int finalI = i;
               presetsRegion.addPreset(this, tile.x, tile.y, this.size, "loot", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     WorldPreset.ensureRegionsAreGenerated(level, tile.x, tile.y, ChestRoomWorldPreset.this.size.width, ChestRoomWorldPreset.this.size.height);
                     Biome biome = level.getBiome(tile.x + ChestRoomWorldPreset.this.size.width / 2, tile.y + ChestRoomWorldPreset.this.size.height / 2);
                     Preset chestRoom = biome.getNewDeepCaveChestRoomPreset(random, new AtomicInteger(finalI));
                     if (chestRoom != null) {
                        chestRoom.applyToLevel(level, tile.x, tile.y);
                     }
                  }
               });
            }
         }
      }
   }
}
