package necesse.level.maps.biomes.forest;

import java.awt.Color;
import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorPlaceFactory;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.FruitGrowerObjectEntity;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.regionSystem.Region;

public class ForestBiome extends Biome {
   public static MobSpawnTable surfaceCritters = new MobSpawnTable().include(Biome.defaultSurfaceCritters).add(30, "butterfly");
   public static MobSpawnTable caveCritters = new MobSpawnTable().include(Biome.defaultCaveCritters).add(100, "stonecaveling");
   public static MobSpawnTable deepCaveCritters = new MobSpawnTable().include(Biome.defaultCaveCritters).add(10, "croppleronion").add(100, "deepstonecaveling");
   public static MobSpawnTable deepCaveMobs = new MobSpawnTable().include(Biome.defaultDeepCaveMobs).add(45, "flamelingshooter");
   public static FishingLootTable forestSurfaceFish = new FishingLootTable(defaultSurfaceFish).addWater(120, "furfish");
   public static LootItemInterface randomPortalDrop = new LootItemList(new ChanceLootItem(0.01F, "mysteriousportal"));
   public static LootItemInterface randomShadowGateDrop = new LootItemList(new ChanceLootItem(0.004F, "shadowgate"));

   @Override
   public Level getNewSurfaceLevel(int islandX, int islandY, float islandSize, Server server, WorldEntity worldEntity) {
      return new ForestSurfaceLevel(islandX, islandY, islandSize, server, worldEntity, this);
   }

   @Override
   public Level getNewCaveLevel(int islandX, int islandY, int dimension, Server server, WorldEntity worldEntity) {
      return new ForestCaveLevel(islandX, islandY, dimension, worldEntity, this);
   }

   @Override
   public Level getNewDeepCaveLevel(int islandX, int islandY, int dimension, Server server, WorldEntity worldEntity) {
      return new ForestDeepCaveLevel(islandX, islandY, dimension, worldEntity, this);
   }

   @Override
   public MobSpawnTable getCritterSpawnTable(Level level) {
      if (level.isCave) {
         return level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? deepCaveCritters : caveCritters;
      } else {
         return surfaceCritters;
      }
   }

   @Override
   public MobSpawnTable getMobSpawnTable(Level level) {
      if (!level.isCave) {
         return defaultSurfaceMobs;
      } else {
         return level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? deepCaveMobs : defaultCaveMobs;
      }
   }

   @Override
   public FishingLootTable getFishingLootTable(FishingSpot spot) {
      return !spot.tile.level.isCave ? forestSurfaceFish : super.getFishingLootTable(spot);
   }

   @Override
   public LootTable getExtraMobDrops(Mob mob) {
      if (mob.isHostile && !mob.isBoss() && !mob.isSummoned) {
         if (mob.getLevel().getIdentifier().equals(LevelIdentifier.CAVE_IDENTIFIER)) {
            return new LootTable(randomPortalDrop, super.getExtraMobDrops(mob));
         }

         if (mob.getLevel().getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
            return new LootTable(randomShadowGateDrop, super.getExtraMobDrops(mob));
         }
      }

      return super.getExtraMobDrops(mob);
   }

   @Override
   public LootTable getExtraBiomeMobDrops(LevelIdentifier levelIdentifier) {
      if (levelIdentifier == null) {
         return new LootTable();
      } else if (levelIdentifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
         return new LootTable(randomPortalDrop);
      } else {
         return levelIdentifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? new LootTable(randomShadowGateDrop) : new LootTable();
      }
   }

   @Override
   public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
      return level.isCave ? TileRegistry.getTile(TileRegistry.dirtID) : TileRegistry.getTile(TileRegistry.sandID);
   }

   @Override
   public int getGenerationDeepCaveRiverWaterTileID() {
      return TileRegistry.waterID;
   }

   @Override
   public int getGenerationTerrainTileID() {
      return TileRegistry.grassID;
   }

   @Override
   public void initializeGeneratorStack(BiomeGeneratorStack stack) {
      super.initializeGeneratorStack(stack);
      stack.addRandomSimplexVeinsBranch("forestOvergrownGrass", 2.5F, 0.45F, 0.7F, 2);
      stack.addRandomSimplexVeinsBranch("forestOakTrees", 2.0F, 0.2F, 1.0F, 0);
      stack.addRandomSimplexVeinsBranch("forestSpruceTrees", 2.0F, 0.2F, 1.0F, 0);
      stack.addRandomVeinsBranch("forestYellowFlowerPatch", 0.05F, 5, 15, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestRedFlowerPatch", 0.05F, 5, 15, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestFiremonePatch", 0.05F, 6, 12, 0.4F, 2, false);
      stack.addRandomSimplexVeinsBranch("forestWaterGrass", 2.0F, 0.33F, 1.0F, 0);
      stack.addRandomVeinsBranch("forestBlueberries", 0.065F, 8, 10, 0.1F, 0, false);
      stack.addRandomVeinsBranch("forestCows", 0.02F, 8, 12, 0.1F, 0, false);
      stack.addRandomVeinsBranch("forestSheep", 0.02F, 8, 12, 0.1F, 0, false);
      stack.addRandomSimplexVeinsBranch("forestCaveWaterGrass", 2.0F, 0.33F, 1.0F, 0);
      stack.addRandomVeinsBranch("forestClay", 0.4F, 5, 10, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestCopper", 0.72F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestIron", 0.56F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestGold", 0.16F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestWildCaveGlow", 0.32F, 4, 8, 0.4F, 2, false);
      stack.addRandomSimplexVeinsBranch("forestDeepCaveWaterGrass", 2.0F, 0.33F, 1.0F, 0);
      stack.addRandomVeinsBranch("forestDeepCopper", 0.08F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestDeepIron", 0.4F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestDeepGold", 0.24F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestDeepObsidian", 0.4F, 5, 10, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestDeepTungsten", 0.32F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("forestDeepLifeQuartz", 0.08F, 3, 6, 0.4F, 2, false);
   }

   @Override
   public void generateRegionSurfaceTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
      super.generateRegionSurfaceTerrain(region, stack, random);
      int grassTile = TileRegistry.grassID;
      stack.startPlaceOnVein(this, region, random, "forestOvergrownGrass").onlyOnTile(grassTile).chance(0.7F).placeTile(TileRegistry.overgrownGrassID);
      stack.startPlaceOnVein(this, region, random, "forestOakTrees").onlyOnTile(grassTile).chance(0.08F).placeObject("oaktree");
      stack.startPlaceOnVein(this, region, random, "forestSpruceTrees").onlyOnTile(grassTile).chance(0.12F).placeObject("sprucetree");
      stack.startPlace(this, region, random)
         .chance(0.4F)
         .testTileID(tileID -> tileID == grassTile || tileID == TileRegistry.overgrownGrassID)
         .placeObject("grass");
      stack.startPlace(this, region, random).onlyOnTile(grassTile).chance(6.0E-4F).placeObjectRandomRotation("oaktreestump");
      stack.startPlace(this, region, random).onlyOnTile(grassTile).chance(6.0E-4F).placeObjectRandomRotation("sprucetreestump");
      stack.startPlace(this, region, random).onlyOnTile(grassTile).chance(6.0E-4F).placeObjectRandomRotation("sprucelogbench");
      stack.startPlace(this, region, random).onlyOnTile(grassTile).chance(0.005F).placeObject("yellowflowerpatch");
      stack.startPlace(this, region, random).onlyOnTile(grassTile).chance(0.005F).placeObject("redflowerpatch");
      stack.startPlace(this, region, random).chance(0.0015F).placeObject("surfacerock");
      stack.startPlace(this, region, random).chance(0.0025F).placeObject("surfacerocksmall");
      stack.startPlace(this, region, random).isSurfaceOcean().chance(0.001F).placeObject("crate");
      stack.startPlace(this, region, random).chancePerRegion(0.02F).onlyOnTile(grassTile).placeObject("beehive");
      stack.startPlace(this, region, random).chancePerRegion(0.035F).onlyOnTile(grassTile).placeObjectFruitGrower("appletree");
      stack.startPlaceOnVein(this, region, random, "forestBlueberries")
         .onlyOnTile(grassTile)
         .placeObjectEntity("blueberrybush", FruitGrowerObjectEntity.class, (r, entity) -> {
            if (r.getChance(0.01F)) {
               Level level = entity.getLevel();
               level.setObject(entity.tileX, entity.tileY, 0);
               Mob stabbyBush = MobRegistry.getMob("stabbybush", level);
               stabbyBush.resetUniqueID(random);
               stabbyBush.onSpawned(entity.tileX * 32 + 16, entity.tileY * 32 + 16);
               stabbyBush.canDespawn = false;
               level.entityManager.mobs.add(stabbyBush);
            } else {
               entity.setRandomStage(r);
            }
         });
      stack.startPlaceOnVein(this, region, random, "forestYellowFlowerPatch").onlyOnTile(grassTile).chance(0.4F).placeObject("yellowflowerpatch");
      stack.startPlaceOnVein(this, region, random, "forestRedFlowerPatch").onlyOnTile(grassTile).chance(0.4F).placeObject("redflowerpatch");
      stack.startPlaceOnVein(this, region, random, "forestFiremonePatch").onlyOnTile(grassTile).chance(0.2F).placeObject("wildfiremone");
      TicketSystemList<String> cowSpawns = new TicketSystemList<String>().addObject(100, "cow").addObject(25, "bull");
      stack.startPlaceOnVein(this, region, random, "forestCows").onlyOnTile(grassTile).placeMob(cowSpawns);
      TicketSystemList<String> sheepSpawns = new TicketSystemList<String>().addObject(100, "sheep").addObject(25, "ram");
      stack.startPlaceOnVein(this, region, random, "forestSheep").onlyOnTile(grassTile).placeMob(sheepSpawns);
      region.updateLiquidManager();
      stack.startPlaceOnVein(this, region, random, "forestWaterGrass").chance(0.3F).test(new GeneratorPlaceFactory.RegionCanPlaceFunction() {
         @Override
         public boolean canPlace(GameRandom random, Region regionx, int regionTileX, int regionTileY, Level level, int tileX, int tileY) {
            return regionx.liquidData.isFreshWaterByRegion(regionTileX, regionTileY);
         }
      }).placeObject("watergrass");
   }

   @Override
   public void generateRegionCaveTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
      super.generateRegionCaveTerrain(region, stack, random);
      stack.startPlace(this, region, random).chance(0.005F).placeObject("caverock");
      stack.startPlace(this, region, random).chance(0.01F).placeObject("caverocksmall");
      stack.startPlace(this, region, random).chance(0.03F).placeCrates("crate");
      stack.startPlaceOnVein(this, region, random, "forestCaveWaterGrass").chance(0.3F).placeObject("cavewatergrass");
      stack.startPlaceOnVein(this, region, random, "forestClay").onlyOnObject(ObjectRegistry.rockID).placeObjectForced("clayrock");
      stack.startPlaceOnVein(this, region, random, "forestCopper").onlyOnObject(ObjectRegistry.rockID).placeObjectForced("copperorerock");
      stack.startPlaceOnVein(this, region, random, "forestIron").onlyOnObject(ObjectRegistry.rockID).placeObjectForced("ironorerock");
      stack.startPlaceOnVein(this, region, random, "forestGold").onlyOnObject(ObjectRegistry.rockID).placeObjectForced("goldorerock");
   }

   @Override
   public void generateRegionDeepCaveTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
      super.generateRegionDeepCaveTerrain(region, stack, random);
      stack.startPlace(this, region, random).chance(0.005F).placeObject("deepcaverock");
      stack.startPlace(this, region, random).chance(0.01F).placeObject("deepcaverocksmall");
      stack.startPlace(this, region, random).chance(0.03F).placeCrates("crate");
      stack.startPlaceOnVein(this, region, random, "forestWildCaveGlow").onlyOnTile(TileRegistry.deepRockID).chance(0.2F).placeObject("wildcaveglow");
      stack.startPlaceOnVein(this, region, random, "forestDeepCaveWaterGrass").chance(0.2F).placeObject("cavewatergrass");
      stack.startPlaceOnVein(this, region, random, "forestDeepCopper").onlyOnObject(ObjectRegistry.deepRockID).placeObjectForced("copperoredeeprock");
      stack.startPlaceOnVein(this, region, random, "forestDeepIron").onlyOnObject(ObjectRegistry.deepRockID).placeObjectForced("ironoredeeprock");
      stack.startPlaceOnVein(this, region, random, "forestDeepGold").onlyOnObject(ObjectRegistry.deepRockID).placeObjectForced("goldoredeeprock");
      stack.startPlaceOnVein(this, region, random, "forestDeepObsidian").onlyOnObject(ObjectRegistry.deepRockID).placeObjectForced("obsidianrock");
      stack.startPlaceOnVein(this, region, random, "forestDeepTungsten").onlyOnObject(ObjectRegistry.deepRockID).placeObjectForced("tungstenoredeeprock");
      stack.startPlaceOnVein(this, region, random, "forestDeepLifeQuartz").onlyOnObject(ObjectRegistry.deepRockID).placeObjectForced("lifequartzdeeprock");
   }

   @Override
   public Color getDebugBiomeColor() {
      return new Color(28, 115, 0);
   }
}
