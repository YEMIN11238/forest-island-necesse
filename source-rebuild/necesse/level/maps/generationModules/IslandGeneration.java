package necesse.level.maps.generationModules;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.objectEntity.FruitGrowerObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class IslandGeneration {
   private final Level level;
   public boolean[] cellMap;
   public int islandSize;
   public GameRandom random;

   public IslandGeneration(Level level, int islandSize) {
      this.level = level;
      this.random = new GameRandom(level.getSeed());

      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            level.setTile(x, y, 0);
         }
      }

      level.liquidManager.calculateShores();
      this.cellMap = new boolean[level.tileWidth * level.tileHeight];
      this.islandSize = islandSize;
   }

   public void generateSimpleIsland(int originX, int originY, int waterTile, int mainTile, int beachTile) {
      this.generateRandomCellIsland(this.islandSize, originX, originY);
      this.cellMap = GenerationTools.doCellularAutomaton(this.cellMap, this.level.tileWidth, this.level.tileHeight, 5, 4, false, 4);
      this.updateCellMap(mainTile, waterTile);
      GenerationTools.smoothTile(this.level, mainTile);
      if (beachTile != -1) {
         this.createBeach(4, beachTile, mainTile);
      }
   }

   public void generateShapedIsland(int waterTile, int mainTile, int beachTile) {
      int islands = this.islandSize / 5;

      for (int i = 0; i < islands; i++) {
         int thisSize = this.random.getIntBetween(Math.max(this.islandSize / 10, 10), (int)(this.islandSize / 2.0F));
         float distanceFromCenterX = this.random.getIntBetween(0, thisSize * 2);
         float distanceFromCenterY = this.random.getIntBetween(0, thisSize * 2);
         int angle = this.random.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         int originX = (int)(this.level.tileWidth / 2 + dir.x * distanceFromCenterX);
         int originY = (int)(this.level.tileHeight / 2 + dir.y * distanceFromCenterY);
         originX = GameMath.limit(originX, thisSize + 20, this.level.tileWidth - thisSize - 20);
         originY = GameMath.limit(originY, thisSize + 20, this.level.tileHeight - thisSize - 20);
         this.generateRandomCellIsland(thisSize, originX, originY);
      }

      this.cellMap = GenerationTools.doCellularAutomaton(this.cellMap, this.level.tileWidth, this.level.tileHeight, 5, 4, false, 4);
      this.updateCellMap(mainTile, waterTile);
      GenerationTools.smoothTile(this.level, mainTile);
      this.clearSmallLakes(4, mainTile);
      if (beachTile != -1) {
         this.createBeach(4, beachTile, mainTile);
      }
   }

   public void clearSmallLakes(int size, int mainTile) {
      boolean[][] toClear = new boolean[this.level.tileWidth][this.level.tileHeight];
      BiPredicate<Integer, Integer> check = (xx, yx) -> !this.level.isLiquidTile(xx, yx);

      for (int x = 0; x < this.level.tileWidth; x++) {
         for (int y = 0; y < this.level.tileHeight; y++) {
            if (this.level.isLiquidTile(x, y) && this.findNearbyTile(x, y, size, check)) {
               toClear[x][y] = true;
            }
         }
      }

      for (int x = 0; x < this.level.tileWidth; x++) {
         for (int y = 0; y < this.level.tileHeight; y++) {
            if (toClear[x][y]) {
               this.level.setTile(x, y, mainTile);
            }
         }
      }
   }

   public void clearTinyIslands(int liquidTile) {
      boolean[][] toClear = new boolean[this.level.tileWidth][this.level.tileHeight];
      BiPredicate<Integer, Integer> check = (xx, yx) -> this.level.isLiquidTile(xx, yx);

      for (int x = 0; x < this.level.tileWidth; x++) {
         for (int y = 0; y < this.level.tileHeight; y++) {
            if (!this.level.isLiquidTile(x, y) && this.countNearbyTiles(x, y, 2, check) > 12) {
               toClear[x][y] = true;
            }
         }
      }

      for (int x = 0; x < this.level.tileWidth; x++) {
         for (int y = 0; y < this.level.tileHeight; y++) {
            if (toClear[x][y]) {
               this.level.setTile(x, y, liquidTile);
            }
         }
      }
   }

   private boolean findNearbyTile(int centerX, int centerY, int distance, BiPredicate<Integer, Integer> check) {
      int minX = Math.min(centerX + distance + 1, this.level.tileWidth);
      int minY = Math.min(centerY + distance + 1, this.level.tileHeight);

      for (int x = Math.max(centerX - distance, 0); x < minX; x++) {
         for (int y = Math.max(centerY - distance, 0); y < minY; y++) {
            if (check.test(x, y)) {
               return true;
            }
         }
      }

      return false;
   }

   private int countNearbyTiles(int centerX, int centerY, int distance, BiPredicate<Integer, Integer> check) {
      int minX = Math.min(centerX + distance + 1, this.level.tileWidth);
      int minY = Math.min(centerY + distance + 1, this.level.tileHeight);
      int found = 0;

      for (int x = Math.max(centerX - distance, 0); x < minX; x++) {
         for (int y = Math.max(centerY - distance, 0); y < minY; y++) {
            if (check.test(x, y)) {
               found++;
            }
         }
      }

      return found;
   }

   private boolean findNearbyTileDistanced(int centerX, int centerY, int distance, BiPredicate<Integer, Integer> check) {
      int minX = Math.min(centerX + distance + 1, this.level.tileWidth);
      int minY = Math.min(centerY + distance + 1, this.level.tileHeight);

      for (int x = Math.max(centerX - distance, 0); x < minX; x++) {
         for (int y = Math.max(centerY - distance, 0); y < minY; y++) {
            if (GameMath.diagonalMoveDistance(centerX, centerY, x, y) <= distance && check.test(x, y)) {
               return true;
            }
         }
      }

      return false;
   }

   public void createBeach(int size, int beachTile, int mainTile) {
      BiPredicate<Integer, Integer> check = (tileX, tileY) -> this.level.getTile(tileX, tileY).isLiquid;

      for (int i = 0; i < this.level.tileWidth; i++) {
         for (int j = 0; j < this.level.tileHeight; j++) {
            if (this.level.getTileID(i, j) == mainTile && this.findNearbyTileDistanced(i, j, size, check)) {
               this.level.setTile(i, j, beachTile);
            }
         }
      }
   }

   public void generateObjects(int object, int tile, float chance) {
      this.generateObjects(object, tile, chance, true);
   }

   public void generateObjects(int object, int rotation, int tile, float chance) {
      this.generateObjects(object, rotation, tile, chance, true);
   }

   public void generateObjects(int object, int tile, float chance, boolean onlyOnTile) {
      this.generateObjects(object, 0, tile, chance, onlyOnTile);
   }

   public void generateObjects(int object, int rotation, int tile, float chance, boolean onlyOnTile) {
      GenerationTools.fillMap(this.level, this.random, tile, -1, 0.0F, object, rotation, -1, chance, false, onlyOnTile);
   }

   public void generateObjects(int object, boolean randomRotation, int tile, float chance, boolean onlyOnTile) {
      GenerationTools.fillMap(this.level, this.random, tile, -1, 0.0F, object, randomRotation, -1, chance, false, onlyOnTile);
   }

   public void generateCellMapObjects(float initialAliveChance, int object, int onlyOnTileID, float chance) {
      boolean[] cellMap = GenerationTools.generateRandomCellMap(this.random, this.level.tileWidth, this.level.tileHeight, initialAliveChance);
      cellMap = GenerationTools.doCellularAutomaton(cellMap, this.level.tileWidth, this.level.tileHeight, 4, 3, false, 4);
      GameObject obj = ObjectRegistry.getObject(object);

      for (int x = 0; x < this.level.tileWidth; x++) {
         for (int y = 0; y < this.level.tileHeight; y++) {
            if (cellMap[x + y * this.level.tileWidth]
               && (onlyOnTileID == -1 || this.level.getTileID(x, y) == onlyOnTileID)
               && this.random.getChance(chance)
               && obj.canPlace(this.level, x, y, 0, false) == null) {
               obj.placeObject(this.level, x, y, 0, false);
            }
         }
      }
   }

   public void generateRiver(int waterTile, int mainTile, int beachTile) {
      float currentLength = 0.0F;
      float maxLength = this.random.getIntBetween(this.islandSize / 2, this.islandSize * 2 + 60);
      int riverAngle = this.random.nextInt(360);
      Float riverStartDir = GameMath.getAngleDir(riverAngle);
      Point riverStartPos = new Point(
         this.level.tileWidth / 2 + (int)(-riverStartDir.x * (this.islandSize + 20)),
         this.level.tileHeight / 2 + (int)(-riverStartDir.y * (this.islandSize + 20))
      );
      LinesGeneration river = new LinesGeneration(riverStartPos.x, riverStartPos.y);

      while (currentLength < maxLength) {
         riverAngle = this.random.getIntOffset(riverAngle, 25);
         float armLength = this.random.getFloatBetween(5.0F, 10.0F);
         float armWidth = this.random.getFloatBetween(3.0F, 4.0F);
         currentLength += armLength;
         river = river.addArm(riverAngle, armLength, armWidth);
         if (!this.level.isTileWithinBounds(river.x2, river.y2)) {
            break;
         }
      }

      river.doCellularAutomaton(this.random, 3, 4, 4).placeTiles(this.level, waterTile);
      new LinesGeneration(river.x2, river.y2, this.random.getFloatBetween(5.0F, 10.0F) + this.islandSize / 10.0F)
         .doCellularAutomaton(this.random, 5, 4, 5)
         .placeTiles(this.level, waterTile);
      if (beachTile != -1) {
         this.createBeach(3, beachTile, mainTile);
      }
   }

   public void generateLakes(float veinsPerChunk, int waterTile, int mainTile, int beachTile) {
      GenerationTools.generateRandomPoints(this.level, this.random, veinsPerChunk, 25, pos -> this.generateLake(pos.x, pos.y, waterTile, mainTile, beachTile));
   }

   public void generateLake(int waterTile, int mainTile, int beachTile) {
      Point startPos = new Point(this.random.getIntBetween(25, this.level.tileWidth - 25), this.random.getIntBetween(25, this.level.tileHeight - 25));
      this.generateLake(startPos.x, startPos.y, waterTile, mainTile, beachTile);
   }

   public void generateLake(int startX, int startY, int waterTile, int mainTile, int beachTile) {
      LinesGeneration lake = new LinesGeneration(startX, startY);
      int lakeArms = this.random.getIntBetween(2, 6);

      for (int i = 0; i < lakeArms; i++) {
         LinesGeneration current = lake;
         int currentAngle = this.random.nextInt(360);

         for (int j = 0; j < 20; j++) {
            float armLength = this.random.getFloatBetween(3.0F, 5.0F);
            int armWidth = this.random.getIntBetween(4, 5);
            current = current.addArm(currentAngle, armLength, armWidth);
            if (!this.level.isTileWithinBounds(current.x2, current.y2) || this.random.nextBoolean()) {
               break;
            }

            currentAngle = this.random.getIntOffset(currentAngle, 25);
         }
      }

      lake.doCellularAutomaton(this.random, 3, 4, 4).placeTiles(this.level, waterTile);
      if (beachTile != -1) {
         this.createBeach(3, beachTile, mainTile);
      }
   }

   public void generateRandomCellIsland(int maxSize, int originX, int originY) {
      Point origin = new Point(originX, originY);

      for (int i = originX - maxSize; i < originX + maxSize; i++) {
         for (int j = originY - maxSize; j < originY + maxSize; j++) {
            if (this.level.isTileWithinBounds(i, j)) {
               double distance = origin.distance(i, j);
               if (distance < maxSize) {
                  if (this.random.nextFloat() < 0.8F) {
                     this.cellMap[i + j * this.level.tileWidth] = true;
                  }

                  if (distance > maxSize - 5 & this.random.nextFloat() < 0.4F) {
                     this.cellMap[i + j * this.level.tileWidth] = false;
                  }
               }
            }
         }
      }
   }

   public void updateCellMap(int trueTile, int falseTile) {
      for (int x = 0; x < this.level.tileWidth; x++) {
         for (int y = 0; y < this.level.tileHeight; y++) {
            if (this.cellMap[x + y * this.level.tileWidth]) {
               this.level.setTile(x, y, trueTile);
            } else {
               this.level.setTile(x, y, falseTile);
            }
         }
      }
   }

   public void spawnMobHerds(String mobStringID, int mobAmount, int tile, int minHerdSize, int maxHerdSize, float islandSize) {
      GenerationTools.spawnMobHerds(this.level, this.random, mobStringID, (int)(mobAmount * islandSize), tile, minHerdSize, maxHerdSize);
   }

   public void spawnMobHerds(TicketSystemList<String> mobStringIDs, int mobAmount, int tile, int minHerdSize, int maxHerdSize, float islandSize) {
      GenerationTools.spawnMobHerds(this.level, this.random, mobStringIDs, (int)(mobAmount * islandSize), tile, minHerdSize, maxHerdSize);
   }

   protected int[] toTileIDs(String... tileStringIDs) {
      int[] tileIDs = new int[tileStringIDs.length];

      for (int i = 0; i < tileIDs.length; i++) {
         tileIDs[i] = TileRegistry.getTileID(tileStringIDs[i]);
      }

      return tileIDs;
   }

   public void generateFruitGrowerVeins(
      String objectStringID, float veinsPerChunk, int minVeinSize, int maxVeinSize, float placeChance, Consumer<Point> onPlaced, String... allowedTileStringIDs
   ) {
      this.generateFruitGrowerVeins(objectStringID, veinsPerChunk, minVeinSize, maxVeinSize, placeChance, onPlaced, this.toTileIDs(allowedTileStringIDs));
   }

   public void generateFruitGrowerVeins(
      String objectStringID, float veinsPerChunk, int minVeinSize, int maxVeinSize, float placeChance, Consumer<Point> onPlaced, int... allowedTileIDs
   ) {
      GameObject object = ObjectRegistry.getObject(objectStringID);
      GenerationTools.generateRandomVeins(this.level, this.random, veinsPerChunk, minVeinSize, maxVeinSize, (level, tileX, tileY) -> {
         if (this.random.getChance(placeChance)) {
            if (allowedTileIDs.length != 0) {
               int tileID = level.getTileID(tileX, tileY);
               if (Arrays.stream(allowedTileIDs).noneMatch(id -> tileID == id)) {
                  return;
               }
            }

            if (object.canPlace(level, tileX, tileY, 0, false) == null) {
               object.placeObject(level, tileX, tileY, 0, false);
               ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
               if (objectEntity instanceof FruitGrowerObjectEntity) {
                  ((FruitGrowerObjectEntity)objectEntity).setRandomStage(this.random);
               }

               if (onPlaced != null) {
                  onPlaced.accept(new Point(tileX, tileY));
               }
            }
         }
      });
   }

   public void generateFruitGrowerSingle(String objectStringID, float objectsPerChunk, String... allowedTileStringIDs) {
      this.generateFruitGrowerSingle(objectStringID, objectsPerChunk, this.toTileIDs(allowedTileStringIDs));
   }

   public void generateFruitGrowerSingle(String objectStringID, float objectsPerChunk, int... allowedTileIDs) {
      this.generateRandomObjects(objectStringID, objectsPerChunk, pos -> {
         ObjectEntity objectEntity = this.level.entityManager.getObjectEntity(pos.x, pos.y);
         if (objectEntity instanceof FruitGrowerObjectEntity) {
            ((FruitGrowerObjectEntity)objectEntity).setRandomStage(this.random);
         }
      }, allowedTileIDs);
   }

   public void generateRandomObjects(String objectStringID, float objectsPerChunk, String... allowedTileStringIDs) {
      this.generateRandomObjects(objectStringID, objectsPerChunk, this.toTileIDs(allowedTileStringIDs));
   }

   public void generateRandomObjects(String objectStringID, float objectsPerChunk, Consumer<Point> onPlaced, String... allowedTileStringIDs) {
      this.generateRandomObjects(objectStringID, objectsPerChunk, onPlaced, this.toTileIDs(allowedTileStringIDs));
   }

   public void generateRandomObjects(String objectStringID, float objectsPerChunk, int... allowedTileIDs) {
      this.generateRandomObjects(objectStringID, objectsPerChunk, null, allowedTileIDs);
   }

   public void generateRandomObjects(String objectStringID, float objectsPerChunk, Consumer<Point> onPlaced, int... allowedTileIDs) {
      GameObject object = ObjectRegistry.getObject(objectStringID);
      GenerationTools.generateRandomPoints(this.level, this.random, objectsPerChunk, pos -> {
         if (allowedTileIDs.length != 0) {
            int tileID = this.level.getTileID(pos.x, pos.y);
            if (Arrays.stream(allowedTileIDs).noneMatch(id -> tileID == id)) {
               return;
            }
         }

         if (object.canPlace(this.level, pos.x, pos.y, 0, false) == null) {
            object.placeObject(this.level, pos.x, pos.y, 0, false);
            if (onPlaced != null) {
               onPlaced.accept(pos);
            }
         }
      });
   }

   public void ensureGenerateObjects(String objectStringID, int maxTotalObjects, String... allowedTileStringIDs) {
      this.ensureGenerateObjects(objectStringID, maxTotalObjects, this.toTileIDs(allowedTileStringIDs));
   }

   public void ensureGenerateObjects(String objectStringID, int maxTotalObjects, Consumer<Point> onPlaced, String... allowedTileStringIDs) {
      this.ensureGenerateObjects(objectStringID, maxTotalObjects, onPlaced, this.toTileIDs(allowedTileStringIDs));
   }

   public void ensureGenerateObjects(String objectStringID, int maxTotalObjects, int... allowedTileIDs) {
      this.ensureGenerateObjects(objectStringID, maxTotalObjects, null, allowedTileIDs);
   }

   public void ensureGenerateObjects(String objectStringID, int maxTotalObjects, Consumer<Point> onPlaced, int... allowedTileIDs) {
      GameObject object = ObjectRegistry.getObject(objectStringID);
      GenerationTools.generateOnValidTiles(this.level, this.random, maxTotalObjects, (x, y) -> {
         if (object.canPlace(this.level, x, y, 0, false) != null) {
            return false;
         } else if (allowedTileIDs.length != 0) {
            int tileID = this.level.getTileID(x, y);
            return Arrays.stream(allowedTileIDs).anyMatch(id -> tileID == id);
         } else {
            return true;
         }
      }, (x, y) -> {
         if (object.canPlace(this.level, x, y, 0, false) == null) {
            object.placeObject(this.level, x, y, 0, false);
            if (onPlaced != null) {
               onPlaced.accept(new Point(x, y));
            }

            return true;
         } else {
            return false;
         }
      });
   }
}
