package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.set.WallSet;

public class VampireCryptWorldPreset extends WorldPreset {
   public Biome biome;
   public LevelIdentifier levelIdentifier;
   public float presetsPerRegion;

   public VampireCryptWorldPreset(Biome biome, LevelIdentifier levelIdentifier, float presetsPerRegion) {
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.presetsPerRegion = presetsPerRegion;
   }

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(this.levelIdentifier) && presetsRegion.hasAnyOfBiome(this.biome.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, this.biome, this.presetsPerRegion);

      for (int i = 0; i < total; i++) {
         final Dimension size = new Dimension(12, 12);
         Point tile = findRandomBiomePresetTile(
            random, presetsRegion, generatorStack, this.biome, 20, size, "minibiomes", new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return !generatorStack.isCaveRiverOrLava(tileX + size.width / 2, tileY + size.height / 2);
               }
            }
         );
         if (tile != null) {
            final LinesGenerationWorldPreset lg = new LinesGenerationWorldPreset(tile.x + size.width / 2, tile.y + size.height / 2)
               .addRandomArms(random, 2, 3.0F, 5.0F, 8.0F, 10.0F);
            final AtomicInteger cryptRotation = new AtomicInteger(i * 3);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               presetsRegion.addPreset(
                     this,
                     lg.getOccupiedTileRectangle(),
                     "minibiomes",
                     new LevelPresetsRegion.WorldPresetPlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                           GameObject cryptGrass = ObjectRegistry.getObject("cryptgrass");
                           CellAutomaton ca = lg.doCellularAutomaton(random);
                           ca.streamAliveOrdered().forEachOrdered(tilexx -> {
                              level.setTile(tilexx.x, tilexx.y, TileRegistry.cryptAshID);
                              level.setObject(tilexx.x, tilexx.y, 0);
                           });
                           ca.placeEdgeWalls(level, WallSet.stone.wall, true);
                           ArrayList<Point> coffinTiles = new ArrayList<>();
                           ca.streamAliveOrdered()
                              .forEachOrdered(
                                 tilexx -> {
                                    if (random.getChance(0.2F) && cryptGrass.canPlace(level, tilexx.x, tilexx.y, 0, false) == null) {
                                       cryptGrass.placeObject(level, tilexx.x, tilexx.y, 0, false);
                                    }

                                    if (level.getObjectID(tilexx.x, tilexx.y) == 0
                                       && level.getObjectID(tilexx.x - 1, tilexx.y) == 0
                                       && level.getObjectID(tilexx.x + 1, tilexx.y) == 0
                                       && level.getObjectID(tilexx.x, tilexx.y - 1) == 0
                                       && level.getObjectID(tilexx.x, tilexx.y + 1) == 0) {
                                       if (random.getChance(0.2F)) {
                                          int rotation = random.nextInt(4);
                                          Point[] clearPoints = new Point[]{new Point(-1, -1), new Point(1, -1), new Point(0, -2)};
                                          if (level.getRelativeAnd(
                                             tilexx.x,
                                             tilexx.y,
                                             PresetUtils.getRotatedPoints(0, 0, rotation, clearPoints),
                                             (tileX, tileY) -> ca.isAlive(tileX, tileY) && level.getObjectID(tileX, tileY) == 0
                                          )) {
                                             ObjectRegistry.getObject(ObjectRegistry.getObjectID("stonecoffin"))
                                                .placeObject(level, tilexx.x, tilexx.y, rotation, false);
                                             coffinTiles.add(tilexx);
                                          }
                                       } else if (random.getChance(0.06F)) {
                                          level.setObject(tilexx.x, tilexx.y, ObjectRegistry.getObjectID("stonecolumn"));
                                       } else if (random.getChance(0.3F)) {
                                          random.runOneOf(
                                             () -> level.setObject(tilexx.x, tilexx.y, ObjectRegistry.getObjectID("gravestone1"), random.nextInt(4)),
                                             () -> level.setObject(tilexx.x, tilexx.y, ObjectRegistry.getObjectID("gravestone2"), random.nextInt(4))
                                          );
                                       }
                                    }
                                 }
                              );
                           if (!coffinTiles.isEmpty()) {
                              Point tilex = random.getOneOf(coffinTiles);
                              LootTablePresets.caveCryptUniqueItems
                                 .applyToLevel(random, level.buffManager.getModifier(LevelModifiers.LOOT), level, tilex.x, tilex.y, level, cryptRotation);
                           }

                           for (Point tilex : coffinTiles) {
                              LootTablePresets.caveCryptCoffin
                                 .applyToLevel(random, level.buffManager.getModifier(LevelModifiers.LOOT), level, tilex.x, tilex.y, level, cryptRotation);
                           }

                           ca.spawnMobs(level, random, "vampire", 25, 45, 1, 4);
                        }
                     }
                  )
                  .setRemoveIfWithinSpawnRegionRange(2);
            }
         }
      }
   }
}
