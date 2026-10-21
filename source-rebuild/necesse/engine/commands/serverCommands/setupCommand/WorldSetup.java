package necesse.engine.commands.serverCommands.setupCommand;

import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.commands.CommandLog;
import necesse.engine.network.packet.PacketRegionData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.RandomBreakObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.regionSystem.Region;

@FunctionalInterface
public interface WorldSetup {
   void apply(Server var1, ServerClient var2, boolean var3, CommandLog var4);

   static Point findClosestBiome(ServerClient client, int dimension, boolean forceNew, String... biomeIDs) {
      Biome[] biomes = new Biome[biomeIDs.length];

      for (int i = 0; i < biomes.length; i++) {
         biomes[i] = BiomeRegistry.getBiome(biomeIDs[i]);
      }

      return findClosestBiome(client, dimension, forceNew, biomes);
   }

   static Point findClosestBiome(ServerClient client, int dimension, boolean forceNew, Biome... biomes) {
      Server server = client.getServer();
      int islandX = server.world.worldEntity.spawnLevelIdentifier.getIslandX();
      int islandY = server.world.worldEntity.spawnLevelIdentifier.getIslandY();
      if (client.getLevelIdentifier().isIslandPosition()) {
         islandX = client.getLevelIdentifier().getIslandX();
         islandY = client.getLevelIdentifier().getIslandY();
      }

      return new Point(islandX, islandY);
   }

   static void buildRandomArena(Level level, GameRandom random, int tileX, int tileY, int minTiles, int maxTiles) {
      LinesGeneration lg = new LinesGeneration(tileX, tileY);
      int angle = random.nextInt(360);
      int arms = 20;
      int anglePerArm = 360 / arms;

      for (int i = 0; i < arms; i++) {
         angle += random.getIntOffset(anglePerArm, anglePerArm / 4);
         float range = random.getFloatBetween(minTiles, maxTiles);
         float width = (float) Math.PI * maxTiles * 2.0F / arms * 2.0F;
         lg.addArm(angle, range, width);
      }

      CellAutomaton ca = lg.toCellularAutomaton(random);
      ca.cleanHardEdges();
      ca.doCellularAutomaton(0, 4, 2);
      HashMap<Integer, Integer> terrainTiles = new HashMap<>();
      ca.forEachTile(level, (l, x, y) -> {
         GameTile tile = l.getTile(x, y);
         if (!tile.isFloor && !tile.isLiquid) {
            terrainTiles.compute(tile.getID(), (id, before) -> before == null ? 1 : before + 1);
         }

         l.setObject(x, y, 0, 0);
      });
      terrainTiles.entrySet()
         .stream()
         .max(Comparator.comparingInt(Entry::getValue))
         .map(Entry::getKey)
         .ifPresent(terrainTile -> ca.forEachTile(level, (l, x, y) -> {
            GameTile tile = l.getTile(x, y);
            if (tile.isLiquid) {
               l.setTile(x, y, terrainTile);
            }
         }));
      placeTorches(level, random, tileX, tileY, maxTiles);
      level.entityManager
         .mobs
         .streamArea(tileX * 32 + 16, tileY * 32 + 16, maxTiles * 32)
         .filter(m -> m.isHostile && m.getDistance(tileX * 32 + 16, tileY * 32 + 16) <= maxTiles * 32)
         .forEach(Mob::remove);
   }

   static void placeTorches(Level level, GameRandom random, int tileX, int tileY, int maxTiles) {
      for (int i = 0; i < maxTiles + 4; i += 5) {
         placeTorchCircle(level, random, tileX, tileY, i);
      }
   }

   static void placeTorchCircle(Level level, GameRandom random, int tileX, int tileY, int tileDistance) {
      GameObject torch = ObjectRegistry.getObject("torch");
      float circumference = (float) Math.PI * tileDistance * 2.0F;
      int torches = Math.max(1, (int)(circumference / 5.0F));
      int angle = random.nextInt(360);
      int anglePerTorch = 360 / torches;

      for (int i = 0; i < torches; i++) {
         angle += random.getIntOffset(anglePerTorch, Math.min(3, anglePerTorch / 4));
         int posX = (int)(tileX + GameMath.sin(angle) * tileDistance);
         int posY = (int)(tileY + GameMath.cos(angle) * tileDistance);
         if (torch.canPlace(level, posX, posY, 0, false) == null) {
            torch.placeObject(level, posX, posY, 0, false);
         }
      }
   }

   static void clearBreakableObjects(Level level, int tileX, int tileY, int tileRange) {
      for (int x = tileX - tileRange; x <= tileX + tileRange; x++) {
         for (int y = tileY - tileRange; y <= tileY + tileRange; y++) {
            if (level.getObject(x, y) instanceof RandomBreakObject && new Point(x, y).distance(tileX, tileY) <= tileRange) {
               level.setObject(x, y, 0, 0);
            }
         }
      }
   }

   static void updateClientsLevel(Level level, int tileX, int tileY, int tileRange) {
      if (level.isServer()) {
         for (int x = tileX - tileRange; x <= tileX + tileRange + 16 - 1; x += 16) {
            for (int y = tileY - tileRange; y <= tileY + tileRange + 16 - 1; y += 16) {
               Region region = level.regionManager.getRegionByTile(x, y, false);
               if (region != null) {
                  PacketRegionData packet = new PacketRegionData(region);
                  level.getServer().network.sendToClientsWithRegion(packet, level, region.regionX, region.regionY);
               }
            }
         }
      }
   }
}
