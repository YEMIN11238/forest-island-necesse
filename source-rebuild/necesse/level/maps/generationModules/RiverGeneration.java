package necesse.level.maps.generationModules;

import java.awt.Point;
import java.util.LinkedList;
import java.util.function.Function;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PointHashMap;
import necesse.level.maps.Level;

public class RiverGeneration {
   public static RiverGeneration.BridgeMaterials STONE_BRIDGE = new RiverGeneration.BridgeMaterials("stonepathtile", "stonefence");
   public static RiverGeneration.BridgeMaterials WOOD_BRIDGE = new RiverGeneration.BridgeMaterials("woodpathtile", "woodfence");
   public static RiverGeneration.BridgeMaterials SNOW_BRIDGE = new RiverGeneration.BridgeMaterials("snowstonepathtile", "woodfence");
   public static RiverGeneration.BridgeMaterials SWAMP_BRIDGE = new RiverGeneration.BridgeMaterials("swampstonepathtile", "woodfence");
   public static RiverGeneration.BridgeMaterials SAND_BRIDGE = new RiverGeneration.BridgeMaterials("sandstonepathtile", "woodfence");
   public static RiverGeneration.BridgeMaterials BASALT_BRIDGE = new RiverGeneration.BridgeMaterials("basaltpathtile", "stonefence");
   public static RiverGeneration.BridgeMaterials DRYAD_BRIDGE = new RiverGeneration.BridgeMaterials("dryadpath", "stonefence");

   public static void generateOneSmallRiver(
      GameRandom random, Level level, int waterTileID, String plantStringID, Function<GameRandom, RiverGeneration.BridgeMaterials> bridgeMaterials
   ) {
      Point riverSpawnPoint = GenerationTools.getRandomMapPoint(level, random);
      generateOneRiver(riverSpawnPoint, random, level, random.getIntBetween(12, 18), 1, 2, 5, 10, 40, 0.4F, waterTileID, plantStringID, 4, 2, bridgeMaterials);
   }

   public static void generateOneSmallRiver(
      GameRandom random, Level level, String plantStringID, Function<GameRandom, RiverGeneration.BridgeMaterials> bridgeMaterials
   ) {
      generateOneSmallRiver(random, level, TileRegistry.waterID, plantStringID, bridgeMaterials);
   }

   public static void generateOneGiantRiver(
      GameRandom random, Level level, int waterTileID, String plantStringID, Function<GameRandom, RiverGeneration.BridgeMaterials> bridgeMaterials
   ) {
      Point riverSpawnPoint = GenerationTools.getRandomEdgePoint(level, random);
      generateOneRiver(riverSpawnPoint, random, level, 200, 3, 6, 6, 20, 25, 0.8F, waterTileID, plantStringID, 4, 3, bridgeMaterials);
   }

   public static void generateOneGiantRiver(
      GameRandom random, Level level, String plantStringID, Function<GameRandom, RiverGeneration.BridgeMaterials> bridgeMaterials
   ) {
      generateOneGiantRiver(random, level, TileRegistry.waterID, plantStringID, bridgeMaterials);
   }

   public static void generateOneGiantWavyRiver(GameRandom random, Level level, int waterTileID, String plantStringID) {
      Point riverSpawnPoint = GenerationTools.getRandomEdgePoint(level, random);
      generateOneWavyRiver(riverSpawnPoint, random, level, 200, 3, 5, 6, 20, waterTileID, plantStringID);
   }

   public static void generateOneGiantWavyRiver(GameRandom random, Level level, String plantStringID) {
      generateOneGiantWavyRiver(random, level, TileRegistry.waterID, plantStringID);
   }

   public static void generateOneRiver(
      Point riverSpawnPoint,
      GameRandom random,
      Level level,
      int maxRiverArms,
      int minWidth,
      int maxWidth,
      int minRange,
      int maxRange,
      int maxTurnDegrees,
      float straightness,
      int waterTileID,
      String plantStringID,
      int bridgeCheckEveryXArm,
      int bridgeEveryXthChance,
      Function<GameRandom, RiverGeneration.BridgeMaterials> bridgeMaterials
   ) {
      LinesGeneration riverLG = new LinesGeneration(riverSpawnPoint.x, riverSpawnPoint.y);

      class BridgePoint {
         public final int x;
         public final int y;
         public final float direction;

         public BridgePoint(int x, int y, float dir) {
            this.x = x;
            this.y = y;
            this.direction = dir;
         }
      }

      LinkedList<BridgePoint> bridgeSpawnPoints = new LinkedList<>();
      float lakeSpawnAngle = GameMath.getAngle(GameMath.normalize(riverSpawnPoint.x - level.tileWidth / 2.0F, riverSpawnPoint.y - level.tileHeight / 2.0F));
      float currentAngle = GameMath.fixAngle(lakeSpawnAngle + 180.0F);

      for (int i = 0; i < maxRiverArms; i++) {
         currentAngle = random.getFloatOffsetWithDist(currentAngle, maxTurnDegrees, maxTurnDegrees * (1.0F - straightness));
         float width = random.getFloatBetween(minWidth, maxWidth);
         float range = random.getFloatBetween(minRange, maxRange);
         riverLG = riverLG.addArm(currentAngle, range, width);
         if (!level.isTileWithinBounds(riverLG.x2, riverLG.y2)) {
            break;
         }

         if (i % bridgeCheckEveryXArm == 0 && i != 0 && random.getEveryXthChance(bridgeEveryXthChance)) {
            float percPos = random.getFloatBetween(0.02F, 0.98F);
            int bridgeX = GameMath.lerp(percPos, riverLG.x1, riverLG.x2);
            int bridgeY = GameMath.lerp(percPos, riverLG.y1, riverLG.y2);
            bridgeSpawnPoints.add(new BridgePoint(bridgeX, bridgeY, currentAngle));
         }
      }

      setWaterTiles(riverLG, level, random, waterTileID, plantStringID);
      float bridgeWidth = Math.max(minWidth, 2.5F);

      for (BridgePoint bridgeSpawnPoint : bridgeSpawnPoints) {
         generateBridgeAcross(
            bridgeSpawnPoint.x,
            bridgeSpawnPoint.y,
            bridgeSpawnPoint.direction,
            bridgeWidth,
            maxWidth,
            level,
            bridgeMaterials == null ? null : bridgeMaterials.apply(random),
            random
         );
      }
   }

   private static void generateBridgeAcross(
      int x, int y, float riverAngle, float bridgeWidth, float riverWidth, Level map, RiverGeneration.BridgeMaterials bridgeMaterials, GameRandom random
   ) {
      LinesGeneration bridgeLG = new LinesGeneration(x, y);
      float angle = riverAngle + 90.0F;
      bridgeLG = bridgeLG.addArm(angle, riverWidth + 4.0F, bridgeWidth);
      bridgeLG = bridgeLG.addArm(angle + 180.0F, riverWidth * 2.0F + 7.0F, bridgeWidth);
      setBridgeTiles(bridgeLG, map, bridgeMaterials, random);
   }

   public static void generateOneWavyRiver(
      Point riverSpawnPoint,
      GameRandom random,
      Level level,
      int maxRiverArms,
      int minWidth,
      int maxWidth,
      int minRange,
      int maxRange,
      int waterTileID,
      String plantStringID
   ) {
      int maxTurnDegrees = 45;
      LinesGeneration riverLG = new LinesGeneration(riverSpawnPoint.x, riverSpawnPoint.y);
      LinesGeneration current = riverLG;
      float lakeSpawnAngle = GameMath.getAngle(GameMath.normalize(riverSpawnPoint.x - level.tileWidth / 2.0F, riverSpawnPoint.y - level.tileHeight / 2.0F));
      lakeSpawnAngle = GameMath.fixAngle(lakeSpawnAngle + 180.0F);
      float sineFloat = 0.0F;

      for (int i = 0; i < maxRiverArms; i++) {
         sineFloat += 30.0F;
         float sineMultiplier = GameMath.sin(sineFloat);
         float width = random.getFloatBetween(minWidth, maxWidth);
         float range = random.getFloatBetween(minRange, maxRange);
         float newAngle = GameMath.fixAngle(lakeSpawnAngle + maxTurnDegrees * sineMultiplier);
         newAngle = random.getFloatOffsetWithDist(newAngle, 12.0F, 6.0F);
         current = current.addArm(newAngle, range, width);
         if (!level.isTileWithinBounds(current.x2, current.y2)) {
            break;
         }
      }

      setWaterTiles(riverLG, level, random, waterTileID, plantStringID);
   }

   private static void setBridgeTiles(LinesGeneration bridgeLG, Level level, RiverGeneration.BridgeMaterials bridgeMaterials, GameRandom random) {
      PointHashMap<LinesGeneration.PointDistance> bridgeTiles = bridgeLG.getSmoothPoints();
      int tileID = bridgeMaterials != null && bridgeMaterials.tileStringID != null ? TileRegistry.getTileID(bridgeMaterials.tileStringID) : -1;
      int fenceID = bridgeMaterials != null && bridgeMaterials.fenceStringID != null ? ObjectRegistry.getObjectID(bridgeMaterials.fenceStringID) : -1;
      if (tileID >= 0) {
         for (Point tile : bridgeTiles.getKeys()) {
            level.setObject(tile.x, tile.y, 0);
            level.setTile(tile.x, tile.y, tileID);
         }
      }

      if (fenceID >= 0) {
         for (Point tile : bridgeTiles.getKeys()) {
            if (isTileNextToLiquid(level, tile.x, tile.y)) {
               level.setObject(tile.x, tile.y, fenceID);
            }
         }
      }
   }

   public static boolean isTileNextToLiquid(Level level, int tileX, int tileY) {
      for (Point p : Level.adjacentGetters) {
         if (level.isLiquidTile(tileX + p.x, tileY + p.y)) {
            return true;
         }
      }

      return false;
   }

   private static void setWaterTiles(LinesGeneration riverLG, Level level, GameRandom random, int waterTileID, String plantStringID) {
      PointHashMap<LinesGeneration.PointDistance> lakeTiles = riverLG.getSmoothPoints();
      int plantID = plantStringID == null ? -1 : ObjectRegistry.getObjectID(plantStringID);

      for (Point tile : lakeTiles.getKeys()) {
         level.setTile(tile.x, tile.y, waterTileID);
         level.setObject(tile.x, tile.y, 0);
         if (plantID >= 0 && random.getEveryXthChance(9)) {
            level.setObject(tile.x, tile.y, plantID);
         }
      }
   }

   public static class BridgeMaterials {
      public String tileStringID;
      public String fenceStringID;

      public BridgeMaterials(String tileStringID, String fenceStringID) {
         this.tileStringID = tileStringID;
         this.fenceStringID = fenceStringID;
      }
   }
}
