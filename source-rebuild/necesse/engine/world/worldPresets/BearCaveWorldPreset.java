package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CellAutomaton;

public class BearCaveWorldPreset extends WorldPreset {
   public Biome biome;
   public LevelIdentifier levelIdentifier;
   public float presetsPerRegion;

   public BearCaveWorldPreset(Biome biome, LevelIdentifier levelIdentifier, float presetsPerRegion) {
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
         final Dimension size = new Dimension(20, 20);
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
               .addRandomArms(random, 4, 6.0F, 10.0F, 6.0F, 8.0F);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               presetsRegion.addPreset(this, lg.getOccupiedTileRectangle(), "minibiomes", new LevelPresetsRegion.WorldPresetPlaceFunction() {
                  @Override
                  public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     CellAutomaton ca = lg.doCellularAutomaton(random);
                     ArrayList<Point> validChestTiles = new ArrayList<>();
                     ca.streamAliveOrdered().forEachOrdered(tilex -> {
                        level.setTile(tilex.x, tilex.y, TileRegistry.mudID);
                        level.setObject(tilex.x, tilex.y, 0);
                        validChestTiles.add(new Point(tilex));
                        GameTile gravelTile = TileRegistry.getTile(TileRegistry.gravelID);
                        if (random.getChance(0.3F) && gravelTile.canPlace(level, tilex.x, tilex.y, false) == null) {
                           gravelTile.placeTile(level, tilex.x, tilex.y, false);
                        }
                     });
                     Point chestTile = random.getOneOf(validChestTiles);
                     if (chestTile != null) {
                        level.setObject(chestTile.x, chestTile.y, ObjectRegistry.getObjectID("barrel"));
                        LootTablePresets.bearBarrel.applyToLevel(random, 1.0F, level, chestTile.x, chestTile.y);
                        Mob grizzlyBear = MobRegistry.getMob("grizzlybear", level);
                        grizzlyBear.canDespawn = false;
                        Point spawnPos = PortalObjectEntity.getTeleportDestinationAroundObject(level, grizzlyBear, chestTile.x, chestTile.y, true);
                        if (spawnPos == null) {
                           spawnPos = new Point(chestTile.x * 32 + 16, chestTile.y * 32 + 16);
                        }

                        grizzlyBear.onSpawned(spawnPos.x, spawnPos.y);
                        level.entityManager.addMob(grizzlyBear, spawnPos.x, spawnPos.y);
                     }
                  }
               });
            }
         }
      }
   }
}
