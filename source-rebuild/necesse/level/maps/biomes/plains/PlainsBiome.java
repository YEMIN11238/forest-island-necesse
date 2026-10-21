package necesse.level.maps.biomes.plains;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.AbstractMusicList;
import necesse.engine.MusicList;
import necesse.engine.network.server.Server;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorPlaceFactory;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.biomes.forest.ForestBiome;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.VillageSet;
import necesse.level.maps.presets.set.WallSet;
import necesse.level.maps.regionSystem.Region;

public class PlainsBiome extends Biome {
   public static MobSpawnTable surfaceCritters = new MobSpawnTable().include(Biome.defaultSurfaceCritters).add(30, "butterfly");
   public static MobSpawnTable caveCritters = new MobSpawnTable().include(Biome.defaultCaveCritters).add(5, "stealthbaskethuman").add(100, "granitecaveling");
   public static MobSpawnTable deepCaveCritters = new MobSpawnTable()
      .include(Biome.defaultCaveCritters)
      .add(30, "butterfly")
      .add(5, "croppleronion")
      .add(5, "cropplerpumpkin")
      .add(100, "dryadcaveling");
   public static LootItemInterface randomBoneOfferingDrop = new LootItemList(new ChanceLootItem(0.005F, "boneoffering"));
   public static LootItemInterface randomSpiritUrnDrop = new LootItemList(new ChanceLootItem(0.005F, "spiriturn"));
   public static MobSpawnTable caveMobs = new MobSpawnTable()
      .add(100, "bonewalker")
      .add(100, "runeboundbrute")
      .add(100, "runeboundshaman")
      .add(100, "runeboundtrapper");
   public static MobSpawnTable deepCaveMobs = new MobSpawnTable().add(100, "dryadsentinel").add(60, "forestspector").add(100, "spiritghoul");

   @Override
   public float getWindModifier(Level level, int tileX, int tileY) {
      return level.isCave ? 0.1F : super.getWindModifier(level, tileX, tileY);
   }

   @Override
   public Color getWindColor(Level level) {
      return !level.getIdentifier().equals(LevelIdentifier.CAVE_IDENTIFIER) && !level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)
         ? super.getWindColor(level)
         : new Color(125, 218, 190);
   }

   @Override
   public SoundSettings getWindSound(Level level) {
      return SoundSettingsRegistry.windPlains;
   }

   @Override
   public Level getNewSurfaceLevel(int islandX, int islandY, float islandSize, Server server, WorldEntity worldEntity) {
      return new PlainsSurfaceLevel(islandX, islandY, islandSize, worldEntity, this);
   }

   @Override
   public Level getNewCaveLevel(int islandX, int islandY, int dimension, Server server, WorldEntity worldEntity) {
      return new PlainsCaveLevel(islandX, islandY, dimension, worldEntity, this);
   }

   @Override
   public Level getNewDeepCaveLevel(int islandX, int islandY, int dimension, Server server, WorldEntity worldEntity) {
      return new PlainsDeepCaveLevel(islandX, islandY, dimension, worldEntity, this);
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
         return level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? deepCaveMobs : caveMobs;
      }
   }

   @Override
   public FishingLootTable getFishingLootTable(FishingSpot spot) {
      return !spot.tile.level.isCave ? ForestBiome.forestSurfaceFish : super.getFishingLootTable(spot);
   }

   @Override
   public LootTable getExtraMobDrops(Mob mob) {
      if (mob.isHostile && !mob.isBoss() && !mob.isSummoned) {
         if (mob.getLevel().getIdentifier().equals(LevelIdentifier.CAVE_IDENTIFIER)) {
            return new LootTable(randomBoneOfferingDrop, super.getExtraMobDrops(mob));
         }

         if (mob.getLevel().getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
            return new LootTable(randomSpiritUrnDrop, super.getExtraMobDrops(mob));
         }
      }

      return super.getExtraMobDrops(mob);
   }

   @Override
   public AbstractMusicList getLevelMusic(Level level, PlayerMob perspective) {
      if (level.isCave) {
         return level.getIdentifier().equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)
            ? new MusicList(MusicRegistry.ForgottenDepths)
            : new MusicList(MusicRegistry.RunecarvedWalls);
      } else {
         return level.getWorldEntity().isNight() ? new MusicList(MusicRegistry.FieldsOfSerenity) : new MusicList(MusicRegistry.MeadowMeandering);
      }
   }

   @Override
   public LootTable getExtraBiomeMobDrops(LevelIdentifier levelIdentifier) {
      if (levelIdentifier == null) {
         return new LootTable();
      } else if (levelIdentifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
         return new LootTable(randomBoneOfferingDrop);
      } else {
         return levelIdentifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) ? new LootTable(randomSpiritUrnDrop) : new LootTable();
      }
   }

   @Override
   public GameTile getUnderLiquidTile(Level level, int tileX, int tileY) {
      return level.isCave ? TileRegistry.getTile(TileRegistry.dirtID) : TileRegistry.getTile(TileRegistry.sandID);
   }

   @Override
   public LootTable getCrateLootTable(Level level, int tileX, int tileY) {
      return level.isDeepCaveLevel() ? LootTablePresets.plainsDeepCrate : LootTablePresets.plainsCrate;
   }

   @Override
   public RandomCaveChestRoom getNewCaveChestRoomPreset(GameRandom random, AtomicInteger lootRotation) {
      RandomCaveChestRoom chestRoom = new RandomCaveChestRoom(random, LootTablePresets.plainsCaveChest, lootRotation, ChestRoomSet.dryad, ChestRoomSet.granite);
      chestRoom.replaceTile(TileRegistry.stoneFloorID, random.getOneOf(TileRegistry.stoneFloorID, TileRegistry.stoneBrickFloorID));
      return chestRoom;
   }

   @Override
   public RandomCaveChestRoom getNewDeepCaveChestRoomPreset(GameRandom random, AtomicInteger lootRotation) {
      RandomCaveChestRoom chestRoom = new RandomCaveChestRoom(random, LootTablePresets.deepPlainsCaveChest, lootRotation, ChestRoomSet.basalt);
      chestRoom.replaceTile(TileRegistry.basaltFloorID, random.getOneOf(TileRegistry.basaltFloorID, TileRegistry.basaltPathID));
      return chestRoom;
   }

   @Override
   public CaveRuins getNewCaveRuinsPreset(GameRandom random, AtomicInteger lootRotation) {
      WallSet wallSet = random.getOneOf(WallSet.granite, WallSet.dryad);
      FurnitureSet furnitureSet = random.getOneOf(FurnitureSet.oak, FurnitureSet.spruce);
      String floorStringID = random.getOneOf("dryadfloor", "dryadfloor", "graniterock", "granitefloor");
      return random.getOneOf(CaveRuins.caveRuinGetters).get(random, wallSet, furnitureSet, floorStringID, LootTablePresets.plainsCaveRuinsChest, lootRotation);
   }

   @Override
   public CaveRuins getNewDeepCaveRuinsPreset(GameRandom random, AtomicInteger lootRotation) {
      WallSet wallSet = WallSet.basalt;
      FurnitureSet furnitureSet = random.getOneOf(FurnitureSet.birch, FurnitureSet.maple);
      String floorStringID = random.getOneOf("basaltfloor", "basaltpath");
      return random.getOneOf(CaveRuins.caveRuinGetters)
         .get(random, wallSet, furnitureSet, floorStringID, LootTablePresets.plainsDeepCaveRuinsChest, lootRotation);
   }

   @Override
   public int getGenerationTerrainTileID() {
      return TileRegistry.plainsGrassID;
   }

   @Override
   public int getGenerationCaveTileID() {
      return TileRegistry.graniteRockID;
   }

   @Override
   public int getGenerationCaveRockObjectID() {
      return ObjectRegistry.graniteRockID;
   }

   @Override
   public int getGenerationDeepCaveTileID() {
      return TileRegistry.basaltRockID;
   }

   @Override
   public int getGenerationDeepCaveRockObjectID() {
      return ObjectRegistry.basaltRockID;
   }

   @Override
   public int getGenerationDeepCaveRiverWaterTileID() {
      return TileRegistry.spiritWaterID;
   }

   @Override
   public int getGenerationDeepCaveLavaTileID() {
      return TileRegistry.spiritWaterID;
   }

   @Override
   public VillageSet[] getVillageSets() {
      return new VillageSet[]{VillageSet.maple, VillageSet.birch};
   }

   @Override
   public void initializeGeneratorStack(BiomeGeneratorStack stack) {
      super.initializeGeneratorStack(stack);
      stack.addRandomSimplexVeinsBranch("plainsOvergrownGrass", 2.5F, 0.45F, 0.7F, 2);
      stack.addRandomSimplexVeinsBranch("plainsMapleTrees", 2.0F, 0.2F, 1.0F, 0);
      stack.addRandomSimplexVeinsBranch("plainsBirchTrees", 2.0F, 0.2F, 1.0F, 0);
      stack.addRandomVeinsBranch("plainsWhiteFlowerPatch", 0.05F, 5, 15, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsSunflowerPatch", 0.15F, 5, 10, 0.4F, 2, false);
      stack.addRandomSimplexVeinsBranch("plainsWaterGrass", 2.0F, 0.33F, 1.0F, 0);
      stack.addRandomVeinsBranch("plainsRaspberries", 0.065F, 8, 10, 0.1F, 0, false);
      stack.addRandomVeinsBranch("plainsCows", 0.02F, 8, 12, 0.1F, 0, false);
      stack.addRandomVeinsBranch("plainsSheep", 0.02F, 8, 12, 0.1F, 0, false);
      stack.addRandomSimplexVeinsBranch("plainsCaveWaterGrass", 2.0F, 0.33F, 1.0F, 0);
      stack.addRandomVeinsBranch("plainsCopper", 0.72F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsIron", 0.56F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsGold", 0.16F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsDeepCopper", 0.08F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsDeepIron", 0.4F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsDeepGold", 0.24F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsDeepTungsten", 0.32F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsDeepLifeQuartz", 0.08F, 3, 6, 0.4F, 2, false);
      stack.addRandomVeinsBranch("plainsDeepAmber", 0.24F, 3, 6, 0.4F, 2, false);
   }

   @Override
   public void generateRegionSurfaceTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
      super.generateRegionSurfaceTerrain(region, stack, random);
      int grassTile = TileRegistry.plainsGrassID;
      stack.startPlaceOnVein(this, region, random, "plainsOvergrownGrass").onlyOnTile(grassTile).chance(0.7F).placeTile(TileRegistry.overgrownPlainsGrassID);
      stack.startPlaceOnVein(this, region, random, "plainsMapleTrees").onlyOnTile(grassTile).chance(0.03F).placeObject("mapletree");
      stack.startPlaceOnVein(this, region, random, "plainsBirchTrees").onlyOnTile(grassTile).chance(0.05F).placeObject("birchtree");
      stack.startPlace(this, region, random)
         .chance(0.4F)
         .testTileID(tileID -> tileID == grassTile || tileID == TileRegistry.overgrownPlainsGrassID)
         .placeObject("plainsgrass");
      stack.startPlace(this, region, random)
         .chance(0.01F)
         .testTileID(tileID -> tileID == grassTile || tileID == TileRegistry.overgrownPlainsGrassID)
         .placeObject("leafpile");
      stack.startPlace(this, region, random).onlyOnTile(grassTile).chance(0.005F).placeObject("whiteflowerpatch");
      stack.startPlace(this, region, random).chance(0.0015F).placeObject("surfacerock");
      stack.startPlace(this, region, random).chance(0.0025F).placeObject("surfacerocksmall");
      stack.startPlace(this, region, random).isSurfaceOcean().chance(0.001F).placeObject("crate");
      stack.startPlace(this, region, random).chancePerRegion(0.04F).onlyOnTile(grassTile).placeObject("beehive");
      stack.startPlaceOnVein(this, region, random, "plainsRaspberries").onlyOnTile(grassTile).placeObjectFruitGrower("raspberrybush");
      stack.startPlaceOnVein(this, region, random, "plainsWhiteFlowerPatch").onlyOnTile(grassTile).chance(0.4F).placeObject("whiteflowerpatch");
      stack.startPlaceOnVein(this, region, random, "plainsSunflowerPatch").onlyOnTile(grassTile).chance(0.15F).placeObject("wildsunflower");
      TicketSystemList<String> cowSpawns = new TicketSystemList<String>().addObject(100, "cow").addObject(25, "bull");
      stack.startPlaceOnVein(this, region, random, "plainsCows").onlyOnTile(grassTile).placeMob(cowSpawns);
      TicketSystemList<String> sheepSpawns = new TicketSystemList<String>().addObject(100, "sheep").addObject(25, "ram");
      stack.startPlaceOnVein(this, region, random, "plainsSheep").onlyOnTile(grassTile).placeMob(sheepSpawns);
      region.updateLiquidManager();
      stack.startPlaceOnVein(this, region, random, "plainsWaterGrass").chance(0.3F).test(new GeneratorPlaceFactory.RegionCanPlaceFunction() {
         @Override
         public boolean canPlace(GameRandom random, Region regionx, int regionTileX, int regionTileY, Level level, int tileX, int tileY) {
            return regionx.liquidData.isFreshWaterByRegion(regionTileX, regionTileY);
         }
      }).placeObject("watergrass");
   }

   @Override
   public void generateRegionCaveTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
      super.generateRegionCaveTerrain(region, stack, random);
      stack.startPlace(this, region, random).chance(5.0E-4F).placeObject("smallrunestone");
      stack.startPlace(this, region, random).chance(0.005F).placeObject("granitecaverock");
      stack.startPlace(this, region, random).chance(0.01F).placeObject("granitecaverocksmall");
      stack.startPlace(this, region, random).chance(0.03F).placeCrates("crate");
      stack.startPlaceOnVein(this, region, random, "plainsCaveWaterGrass").chance(0.3F).placeObject("cavewatergrass");
      stack.startPlaceOnVein(this, region, random, "plainsCopper").onlyOnObject(ObjectRegistry.graniteRockID).placeObjectForced("copperoregraniterock");
      stack.startPlaceOnVein(this, region, random, "plainsIron").onlyOnObject(ObjectRegistry.graniteRockID).placeObjectForced("ironoregraniterock");
      stack.startPlaceOnVein(this, region, random, "plainsGold").onlyOnObject(ObjectRegistry.graniteRockID).placeObjectForced("goldoregraniterock");
   }

   @Override
   public void generateRegionDeepCaveTerrain(Region region, BiomeGeneratorStack stack, GameRandom random) {
      super.generateRegionDeepCaveTerrain(region, stack, random);
      stack.startPlace(this, region, random).chance(0.005F).placeObject("basaltcaverock");
      stack.startPlace(this, region, random).chance(0.01F).placeObject("basaltcaverocksmall");
      stack.startPlace(this, region, random).chance(0.02F).placeObject("dryadtree");
      stack.startPlace(this, region, random).chance(0.05F).onlyOnTile(TileRegistry.spiritWaterID).placeObject("watergrass");
      stack.startPlace(this, region, random).chance(0.01F).onlyOnTile(TileRegistry.spiritWaterID).placeObject("waterlantern");
      stack.startPlace(this, region, random).chance(0.02F).placeObject("cavebirchtree");
      stack.startPlace(this, region, random).chance(0.02F).placeObject("cavemapletree");
      stack.startPlace(this, region, random).chance(0.03F).placeCrates("crate");
      stack.startPlaceOnVein(this, region, random, "plainsDeepCopper").onlyOnObject(ObjectRegistry.basaltRockID).placeObjectForced("copperorebasaltrock");
      stack.startPlaceOnVein(this, region, random, "plainsDeepIron").onlyOnObject(ObjectRegistry.basaltRockID).placeObjectForced("ironorebasaltrock");
      stack.startPlaceOnVein(this, region, random, "plainsDeepGold").onlyOnObject(ObjectRegistry.basaltRockID).placeObjectForced("goldorebasaltrock");
      stack.startPlaceOnVein(this, region, random, "plainsDeepTungsten").onlyOnObject(ObjectRegistry.basaltRockID).placeObjectForced("tungstenorebasaltrock");
      stack.startPlaceOnVein(this, region, random, "plainsDeepLifeQuartz").onlyOnObject(ObjectRegistry.basaltRockID).placeObjectForced("lifequartzbasaltrock");
      stack.startPlaceOnVein(this, region, random, "plainsDeepAmber").onlyOnObject(ObjectRegistry.basaltRockID).placeObjectForced("amberbasaltrock");
   }

   @Override
   public Color getDebugBiomeColor() {
      return new Color(184, 131, 21);
   }
}
