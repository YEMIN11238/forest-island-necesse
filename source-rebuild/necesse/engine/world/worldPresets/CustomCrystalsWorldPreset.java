package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.presets.PresetUtils;

public class CustomCrystalsWorldPreset extends WorldPreset {
   public Biome biome;
   public LevelIdentifier levelIdentifier;
   public float presetsPerRegion;
   public String gravelTileStringID;
   public String smallClusterObjectStringID;
   public String bigClusterObjectStringID;

   public CustomCrystalsWorldPreset(
      Biome biome,
      LevelIdentifier levelIdentifier,
      float presetsPerRegion,
      String gravelTileStringID,
      String smallClusterObjectStringID,
      String bigClusterObjectStringID
   ) {
      this.biome = biome;
      this.levelIdentifier = levelIdentifier;
      this.presetsPerRegion = presetsPerRegion;
      this.gravelTileStringID = gravelTileStringID;
      this.smallClusterObjectStringID = smallClusterObjectStringID;
      this.bigClusterObjectStringID = bigClusterObjectStringID;
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
         final Dimension size = new Dimension(14, 14);
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
               .addRandomArms(random, 4, 4.0F, 7.0F, 4.0F, 6.0F);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               presetsRegion.addPreset(
                  this,
                  lg.getOccupiedTileRectangle(),
                  "minibiomes",
                  new LevelPresetsRegion.WorldPresetPlaceFunction() {
                     @Override
                     public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                        int gravelTileID = TileRegistry.getTileID(CustomCrystalsWorldPreset.this.gravelTileStringID);
                        GameObject crystalClusterSmall = ObjectRegistry.getObject(CustomCrystalsWorldPreset.this.smallClusterObjectStringID);
                        GameObject crystalClusterBig = ObjectRegistry.getObject(CustomCrystalsWorldPreset.this.bigClusterObjectStringID);
                        CellAutomaton ca = lg.doCellularAutomaton(random);
                        ca.streamAliveOrdered().forEachOrdered(tilex -> {
                           level.setTile(tilex.x, tilex.y, gravelTileID);
                           level.setObject(tilex.x, tilex.y, 0);
                        });
                        ca.streamAliveOrdered()
                           .forEachOrdered(
                              tilex -> {
                                 if (level.getObjectID(tilex.x, tilex.y) == 0
                                    && level.getObjectID(tilex.x - 1, tilex.y) == 0
                                    && level.getObjectID(tilex.x + 1, tilex.y) == 0
                                    && level.getObjectID(tilex.x, tilex.y - 1) == 0
                                    && level.getObjectID(tilex.x, tilex.y + 1) == 0
                                    && random.getChance(0.08F)) {
                                    int rotation = random.nextInt(4);
                                    Point[] clearPoints = new Point[]{new Point(-1, -1), new Point(1, -1)};
                                    if (level.getRelativeAnd(
                                       tilex.x,
                                       tilex.y,
                                       PresetUtils.getRotatedPoints(0, 0, rotation, clearPoints),
                                       (tileX, tileY) -> ca.isAlive(tileX, tileY) && level.getObjectID(tileX, tileY) == 0
                                    )) {
                                       crystalClusterBig.placeObject(level, tilex.x, tilex.y, rotation, false);
                                    }
                                 }

                                 if (random.getChance(0.3F) && crystalClusterSmall.canPlace(level, tilex.x, tilex.y, 0, false) == null) {
                                    crystalClusterSmall.placeObject(level, tilex.x, tilex.y, 0, false);
                                 }
                              }
                           );
                     }
                  }
               );
            }
         }
      }
   }
}
