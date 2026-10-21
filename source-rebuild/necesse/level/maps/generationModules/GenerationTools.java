package necesse.level.maps.generationModules;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.TrialEntranceObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.regionSystem.Region;

public class GenerationTools {
   private static final Point[] nextToGetters = new Point[]{new Point(0, -1), new Point(-1, 0), new Point(1, 0), new Point(0, 1)};

   public static void iterateLevel(Level level, BiConsumer<Integer, Integer> consumer) {
      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            consumer.accept(x, y);
         }
      }
   }

   public static void iterateLevel(Level level, BiFunction<Integer, Integer, Boolean> filter, BiConsumer<Integer, Integer> consumer) {
      iterateLevel(level, (x, y) -> {
         if (filter.apply(x, y)) {
            consumer.accept(x, y);
         }
      });
   }

   public static void smoothTile(Level level, int tile) {
      boolean done = false;

      while (!done) {
         done = true;

         for (int i = 0; i < level.tileWidth; i++) {
            for (int j = 0; j < level.tileHeight; j++) {
               if (level.getTileID(i, j) == tile & level.getTileID(i + 1, j) != tile & level.getTileID(i - 1, j) != tile) {
                  level.setTile(i, j, level.getTileID(i + 1, j));
                  done = false;
               }

               if (level.getTileID(i, j) == tile & level.getTileID(i, j + 1) != tile & level.getTileID(i, j - 1) != tile) {
                  level.setTile(i, j, level.getTileID(i, j + 1));
                  done = false;
               }
            }
         }
      }
   }

   public static void spawnMobHerds(Level level, GameRandom random, String mobStringID, int mobAmount, int tile, int minHerdSize, int maxHerdSize) {
      spawnMobHerds(level, random, new TicketSystemList<String>().addObject(100, mobStringID), mobAmount, tile, minHerdSize, maxHerdSize);
   }

   public static void spawnMobHerds(
      Level level, GameRandom random, TicketSystemList<String> mobStringIDs, int mobAmount, int tile, int minHerdSize, int maxHerdSize
   ) {
      int totalSpawned = 0;
      int iterations = 0;

      while (totalSpawned < mobAmount && iterations < mobAmount * 10) {
         iterations++;
         int y = random.nextInt(level.tileWidth);
         int x = random.nextInt(level.tileHeight);
         if (level.getTileID(x, y) == tile) {
            int herdSize = random.getIntBetween(minHerdSize, maxHerdSize);
            int spawned = 0;
            int spawnIterations = 0;

            while (spawned < herdSize && spawnIterations < maxHerdSize * 10) {
               spawnIterations++;
               int randomX = random.getIntBetween(-5, 5);
               int randomY = random.getIntBetween(-5, 5);
               String mobStringID = mobStringIDs.getRandomObject(random);
               Mob mob = MobRegistry.getMob(mobStringID, level);
               mob.setX((x + randomX) * 32 + 16);
               mob.setY((y + randomY) * 32 + 16);
               if (level.getTileID(x + randomX, y + randomY) == tile && !mob.collidesWith(level)) {
                  level.entityManager.mobs.add(mob);
                  spawned++;
                  totalSpawned++;
               }
            }
         }
      }
   }

   public static int spawnRandomPreset(
      Level level, Preset preset, boolean randomizeMirror, boolean randomizeRotation, GameRandom random, boolean overrideCanPlace, int attempts, int maxPlaces
   ) {
      return spawnRandomPreset(level, preset, randomizeMirror, randomizeMirror, randomizeRotation, random, overrideCanPlace, attempts, maxPlaces);
   }

   public static int spawnRandomPreset(
      Level level,
      Preset preset,
      boolean randomizeMirrorX,
      boolean randomizeMirrorY,
      boolean randomizeRotation,
      GameRandom random,
      boolean overrideCanPlace,
      int attempts,
      int maxPlaces
   ) {
      if (randomizeMirrorX) {
         preset = PresetUtils.randomizeXMirror(preset, random);
      }

      if (randomizeMirrorY) {
         preset = PresetUtils.randomizeYMirror(preset, random);
      }

      if (randomizeRotation) {
         preset = PresetUtils.randomizeRotation(preset, random);
      }

      ArrayList<Point> locations = new ArrayList<>();

      for (int i = 0; i < attempts; i++) {
         int tileX = random.getIntBetween(2, level.tileWidth - preset.width - 2);
         int tileY = random.getIntBetween(2, level.tileHeight - preset.height - 2);
         if (overrideCanPlace || preset.canApplyToLevel(level, tileX, tileY)) {
            locations.add(new Point(tileX, tileY));
         }
      }

      int out = 0;

      for (int i = 0; i < maxPlaces && locations.size() != 0; i++) {
         int index = random.nextInt(locations.size());
         Point p = locations.get(index);
         preset.applyToLevel(level, p.x, p.y);
         locations.remove(index);
         out++;
      }

      return out;
   }

   public static boolean[] generateRandomCellMapArea(GameRandom random, boolean[] cellMap, int cellMapWidth, int x, int y, int width, int height, float chance) {
      for (int i = 0; i < width; i++) {
         for (int j = 0; j < height; j++) {
            int index = x + i + (y + j) * cellMapWidth;
            if (index >= 0 && index < cellMap.length && (chance >= 1.0F || random.nextFloat() < chance)) {
               cellMap[index] = true;
            }
         }
      }

      return cellMap;
   }

   public static boolean[] generateRandomCellMap(GameRandom random, int width, int height, float chance) {
      return generateRandomCellMapArea(random, new boolean[width * height], width, 0, 0, width, height, chance);
   }

   public static int countTrueNeighbours(boolean[] cellMap, int cellMapWidth, int x, int y, boolean countOutside) {
      int count = 0;

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            int neighbourX = x + i;
            int neighbourY = y + j;
            if (i != 0 || j != 0) {
               int index = neighbourX + neighbourY * cellMapWidth;
               if (index >= 0 && index < cellMap.length) {
                  if (cellMap[index]) {
                     count++;
                  }
               } else if (countOutside) {
                  count++;
               }
            }
         }
      }

      return count;
   }

   public static boolean[] doCellularAutomaton(boolean[] cellMap, int cellMapWidth, int cellMapHeight, int deathLimit, int birthLimit, boolean countOutside) {
      boolean[] newMap = (boolean[])cellMap.clone();

      for (int x = 0; x < cellMapWidth; x++) {
         for (int y = 0; y < cellMapHeight; y++) {
            int trueNeighbours = countTrueNeighbours(cellMap, cellMapWidth, x, y, countOutside);
            int index = x + y * cellMapWidth;
            if (cellMap[index]) {
               newMap[index] = trueNeighbours >= deathLimit;
            } else {
               newMap[index] = trueNeighbours > birthLimit;
            }
         }
      }

      return newMap;
   }

   public static boolean[] doCellularAutomaton(
      boolean[] cellMap, int cellMapWidth, int cellMapHeight, int deathLimit, int birthLimit, boolean countOutside, int iterations
   ) {
      for (int i = 0; i <= iterations; i++) {
         cellMap = doCellularAutomaton(cellMap, cellMapWidth, cellMapHeight, deathLimit, birthLimit, countOutside);
      }

      return cellMap;
   }

   public static boolean[] generateRandomVein(GameRandom random, int width, int height) {
      boolean[] cellMap = generateRandomCellMap(random, width, height, 0.4F);
      return doCellularAutomaton(cellMap, width, height, 4, 3, false, 4);
   }

   public static void placeRandomVein(
      Level level, GameRandom random, int x, int y, int minVeinSize, int maxVeinSize, GenerationTools.PlaceFunction placeFunction
   ) {
      int width = random.getIntBetween(minVeinSize, maxVeinSize);
      int height = random.getIntBetween(minVeinSize, maxVeinSize);
      boolean[] cellMap = generateRandomVein(random, width, height);

      for (int i = 0; i < width; i++) {
         for (int j = 0; j < height; j++) {
            int index = i + j * width;
            int levelX = i + x - width / 2;
            int levelY = j + y - height / 2;
            if (cellMap[index]) {
               placeFunction.place(level, levelX, levelY);
            }
         }
      }
   }

   public static void placeRandomVein(
      Level level,
      GameRandom random,
      int x,
      int y,
      int minVeinSize,
      int maxVeinSize,
      int tile,
      int replaceTile,
      float tileChance,
      int object,
      int replaceObject,
      float objectChance,
      boolean forcePlace,
      boolean onlyOnTile
   ) {
      GameObject obj = objectChance > 0.0F ? ObjectRegistry.getObject(object) : null;
      placeRandomVein(
         level,
         random,
         x,
         y,
         minVeinSize,
         maxVeinSize,
         (level1, tileX, tileY) -> {
            if (tileChance > 0.0F
               && (tileChance >= 1.0F || random.nextFloat() < tileChance)
               && (replaceTile == -1 || level.getTileID(tileX, tileY) == replaceTile)) {
               TileRegistry.getTile(tile).placeTile(level, tileX, tileY, false);
            }

            if (obj != null
               && (objectChance >= 1.0F || random.nextFloat() < objectChance)
               && (!onlyOnTile || level.getTileID(tileX, tileY) == tile)
               && (replaceObject == -1 || level.getObjectID(tileX, tileY) == replaceObject)
               && (forcePlace || obj.canPlace(level, tileX, tileY, 0, false) == null)) {
               obj.placeObject(level, tileX, tileY, 0, false);
            }
         }
      );
   }

   public static void placeRandomObjectVeinOnTile(
      Level level, GameRandom random, int x, int y, int veinMinSize, int veinMaxSize, int tile, int object, float chance, boolean forcePlace
   ) {
      placeRandomVein(level, random, x, y, veinMinSize, veinMaxSize, tile, -1, 0.0F, object, -1, chance, forcePlace, true);
   }

   public static void placeRandomObjectVein(
      Level level, GameRandom random, int x, int y, int veinMinSize, int veinMaxSize, int object, float chance, boolean forcePlace
   ) {
      placeRandomVein(level, random, x, y, veinMinSize, veinMaxSize, -1, -1, 0.0F, object, -1, chance, forcePlace, false);
   }

   public static ArrayList<Point> getValidTiles(Level level, BiPredicate<Integer, Integer> isValid) {
      ArrayList<Point> validTiles = new ArrayList<>();
      iterateLevel(level, (x, y) -> {
         if (isValid.test(x, y)) {
            validTiles.add(new Point(x, y));
         }
      });
      return validTiles;
   }

   public static int generateOnValidTiles(
      Level level, GameRandom random, int maxPoints, BiPredicate<Integer, Integer> isValid, BiPredicate<Integer, Integer> consumer
   ) {
      ArrayList<Point> validTiles = getValidTiles(level, isValid);

      for (int i = 0; i < maxPoints; i++) {
         if (validTiles.isEmpty()) {
            return i;
         }

         int index = random.nextInt(validTiles.size());
         Point point = validTiles.remove(index);
         if (!consumer.test(point.x, point.y)) {
            i--;
         }
      }

      return maxPoints;
   }

   public static void generateRandomPoints(Level level, GameRandom random, float pointsPerChunk, int edgeDist, Consumer<Point> posConsumer) {
      float floatVeins = level.tileWidth / 10.0F * level.tileHeight / 10.0F * pointsPerChunk;
      int veins = 0;
      veins = (int)(veins + floatVeins);
      floatVeins -= veins;
      if (random.getChance(floatVeins)) {
         veins++;
      }

      for (int i = 0; i < veins; i++) {
         int randomX = random.getIntBetween(edgeDist, level.tileWidth - edgeDist);
         int randomY = random.getIntBetween(edgeDist, level.tileHeight - edgeDist);
         posConsumer.accept(new Point(randomX, randomY));
      }
   }

   public static void generateRandomPoints(Level level, GameRandom random, float pointsPerChunk, Consumer<Point> posConsumer) {
      generateRandomPoints(level, random, pointsPerChunk, 5, posConsumer);
   }

   public static void generateRandomVeins(
      Level level,
      GameRandom random,
      float veinsPerChunk,
      int minVeinSize,
      int maxVeinSize,
      int tile,
      int replaceTile,
      float tileChance,
      int object,
      int replaceObject,
      float objectChance,
      boolean forcePlace,
      boolean onlyOnTile
   ) {
      generateRandomPoints(
         level,
         random,
         veinsPerChunk,
         veinPos -> placeRandomVein(
            level,
            random,
            veinPos.x,
            veinPos.y,
            minVeinSize,
            maxVeinSize,
            tile,
            replaceTile,
            tileChance,
            object,
            replaceObject,
            objectChance,
            forcePlace,
            onlyOnTile
         )
      );
   }

   public static void generateRandomVeins(
      Level level, GameRandom random, float veinsPerChunk, int minVeinSize, int maxVeinSize, GenerationTools.PlaceFunction placeFunction
   ) {
      generateRandomPoints(
         level, random, veinsPerChunk, veinPos -> placeRandomVein(level, random, veinPos.x, veinPos.y, minVeinSize, maxVeinSize, placeFunction)
      );
   }

   public static int generateGuaranteedRandomVeins(
      Level level,
      GameRandom random,
      int places,
      int minVeinSize,
      int maxVeinSize,
      GenerationTools.CanPlaceFunction canPlaceFunction,
      GenerationTools.PlaceFunction placeFunction
   ) {
      int edgeDist = 10;
      ArrayList<Point> validPlaces = new ArrayList<>();
      HashSet<Point> validPlacesSet = new HashSet<>();

      for (int x = edgeDist; x < level.tileWidth - edgeDist; x++) {
         for (int y = edgeDist; y < level.tileHeight - edgeDist; y++) {
            if (canPlaceFunction.canPlace(level, x, y)) {
               validPlaces.add(new Point(x, y));
               validPlacesSet.add(new Point(x, y));
            }
         }
      }

      HashSet<Point> placedTiles = new HashSet<>();
      int placed = 0;

      while (placed < places && !validPlaces.isEmpty()) {
         int startIndex = random.nextInt(validPlaces.size());
         Point start = validPlaces.remove(startIndex);
         if (!placedTiles.contains(start)) {
            LinkedList<Point> openTiles = new LinkedList<>();
            openTiles.addLast(start);
            HashSet<Point> closedTiles = new HashSet<>();
            int currentVeinSize = random.getIntBetween(minVeinSize, maxVeinSize);
            int veinSize = 0;

            while (!openTiles.isEmpty()) {
               Point current = openTiles.removeFirst();
               if (placed >= places || veinSize >= currentVeinSize) {
                  break;
               }

               placeFunction.place(level, current.x, current.y);
               placed++;
               veinSize++;
               placedTiles.add(current);
               List<Point> getters = Arrays.asList(nextToGetters);
               Collections.shuffle(getters, random);

               for (Point n : getters) {
                  Point next = new Point(current.x + n.x, current.y + n.y);
                  if (level.isTileWithinBounds(next.x, next.y) && validPlacesSet.contains(next) && !placedTiles.contains(next) && !closedTiles.contains(next)) {
                     if (random.getChance(0.2F)) {
                        openTiles.addFirst(next);
                     } else {
                        openTiles.addLast(next);
                     }

                     closedTiles.add(next);
                  }
               }
            }
         }
      }

      return placed;
   }

   public static void generateRandomObjectVeinsOnTile(
      Level level, GameRandom random, float veinsPerChunk, int minVeinSize, int maxVeinSize, int tile, int object, float chance, boolean forcePlace
   ) {
      generateRandomVeins(level, random, veinsPerChunk, minVeinSize, maxVeinSize, tile, -1, 0.0F, object, -1, chance, forcePlace, true);
   }

   public static void generateRandomObjectVeins(
      Level level, GameRandom random, float veinsPerChunk, int minVeinSize, int maxVeinSize, int object, float chance, boolean forcePlace
   ) {
      generateRandomVeins(level, random, veinsPerChunk, minVeinSize, maxVeinSize, -1, -1, 0.0F, object, -1, chance, forcePlace, true);
   }

   public static void generateRandomSmoothVeinsL(
      Level level,
      GameRandom random,
      float veinsPerChunk,
      int arms,
      float minRange,
      float maxRange,
      float minWidth,
      float maxWidth,
      Consumer<LinesGeneration> linesConsumer
   ) {
      generateRandomPoints(
         level,
         random,
         veinsPerChunk,
         veinPos -> linesConsumer.accept(new LinesGeneration(veinPos.x, veinPos.y).addRandomArms(random, arms, minRange, maxRange, minWidth, maxWidth))
      );
   }

   public static void generateRandomSmoothVeinsC(
      Level level,
      GameRandom random,
      float veinsPerChunk,
      int arms,
      float minRange,
      float maxRange,
      float minWidth,
      float maxWidth,
      Consumer<CellAutomaton> cellConsumer
   ) {
      generateRandomSmoothVeinsL(
         level,
         random,
         veinsPerChunk,
         arms,
         minRange,
         maxRange,
         minWidth,
         maxWidth,
         linesGeneration -> cellConsumer.accept(linesGeneration.doCellularAutomaton(random))
      );
   }

   public static void generateRandomSmoothVeins(
      Level level,
      GameRandom random,
      float veinsPerChunk,
      int arms,
      float minRange,
      float maxRange,
      float minWidth,
      float maxWidth,
      GenerationTools.PlaceFunction placeFunction
   ) {
      generateRandomSmoothVeinsC(level, random, veinsPerChunk, arms, minRange, maxRange, minWidth, maxWidth, cells -> cells.forEachTile(level, placeFunction));
   }

   public static void generateRandomSmoothTileVeins(
      Level level,
      GameRandom random,
      float veinsPerChunk,
      int arms,
      float minRange,
      float maxRange,
      float minWidth,
      float maxWidth,
      int tileID,
      float chance,
      boolean forcePlace
   ) {
      GameTile tile = TileRegistry.getTile(tileID);
      generateRandomSmoothVeins(level, random, veinsPerChunk, arms, minRange, maxRange, minWidth, maxWidth, (l, tileX, tileY) -> {
         if (random.getChance(chance) && (forcePlace || tile.canPlace(l, tileX, tileY, false) == null)) {
            tile.placeTile(l, tileX, tileY, false);
         }
      });
   }

   public static void generateRandomSmoothObjectVeins(
      Level level,
      GameRandom random,
      float veinsPerChunk,
      int arms,
      float minRange,
      float maxRange,
      float minWidth,
      float maxWidth,
      int objectID,
      float chance,
      boolean forcePlace
   ) {
      GameObject object = ObjectRegistry.getObject(objectID);
      generateRandomSmoothVeins(level, random, veinsPerChunk, arms, minRange, maxRange, minWidth, maxWidth, (l, tileX, tileY) -> {
         if (random.getChance(chance) && (forcePlace || object.canPlace(l, tileX, tileY, 0, false) == null)) {
            object.placeObject(l, tileX, tileY, 0, false);
         }
      });
   }

   public static void generateFurniture(
      Level level, GameRandom random, int tileX, int tileY, TicketSystemList<Preset> furniture, Function<TilePosition, Boolean> isValidTile
   ) {
      TicketSystemList<Preset> fPresets = new TicketSystemList<>(furniture);

      while (!fPresets.isEmpty()) {
         Preset fp = fPresets.getAndRemoveRandomObject(random);
         ArrayList<Supplier<Boolean>> attempts = new ArrayList<>(
            Arrays.asList(
               () -> tryPlaceFurniturePreset(PresetUtils.randomizeXMirror(fp, random), level, tileX, tileY, isValidTile),
               () -> tryPlaceFurniturePreset(PresetUtils.randomizeXMirror(fp, random).tryRotate(PresetRotation.CLOCKWISE), level, tileX, tileY, isValidTile),
               () -> tryPlaceFurniturePreset(
                  PresetUtils.randomizeXMirror(fp, random).tryRotate(PresetRotation.ANTI_CLOCKWISE), level, tileX, tileY, isValidTile
               ),
               () -> tryPlaceFurniturePreset(PresetUtils.randomizeXMirror(fp, random).tryRotate(PresetRotation.HALF_180), level, tileX, tileY, isValidTile)
            )
         );
         boolean over = false;

         while (true) {
            if (!attempts.isEmpty()) {
               int nextIndex = random.nextInt(attempts.size());
               if (!attempts.remove(nextIndex).get()) {
                  continue;
               }

               over = true;
            }

            if (over) {
               return;
            }
            break;
         }
      }
   }

   public static void generateFurnitureGetters(
      Level level,
      GameRandom random,
      int tileX,
      int tileY,
      TicketSystemList<Function<GameRandom, Preset>> furniture,
      Function<TilePosition, Boolean> isValidTile
   ) {
      TicketSystemList<Function<GameRandom, Preset>> fPresets = new TicketSystemList<>(furniture);

      while (!fPresets.isEmpty()) {
         Preset fp = fPresets.getAndRemoveRandomObject(random).apply(random);
         ArrayList<Supplier<Boolean>> attempts = new ArrayList<>(
            Arrays.asList(
               () -> tryPlaceFurniturePreset(PresetUtils.randomizeXMirror(fp, random), level, tileX, tileY, isValidTile),
               () -> tryPlaceFurniturePreset(PresetUtils.randomizeXMirror(fp, random).tryRotate(PresetRotation.CLOCKWISE), level, tileX, tileY, isValidTile),
               () -> tryPlaceFurniturePreset(
                  PresetUtils.randomizeXMirror(fp, random).tryRotate(PresetRotation.ANTI_CLOCKWISE), level, tileX, tileY, isValidTile
               ),
               () -> tryPlaceFurniturePreset(PresetUtils.randomizeXMirror(fp, random).tryRotate(PresetRotation.HALF_180), level, tileX, tileY, isValidTile)
            )
         );
         boolean over = false;

         while (true) {
            if (!attempts.isEmpty()) {
               int nextIndex = random.nextInt(attempts.size());
               if (!attempts.remove(nextIndex).get()) {
                  continue;
               }

               over = true;
            }

            if (over) {
               return;
            }
            break;
         }
      }
   }

   public static boolean tryPlaceFurniturePreset(Preset preset, Level level, int tileX, int tileY, Function<TilePosition, Boolean> isValidTile) {
      if (isValidTile != null) {
         for (int x = 0; x < preset.width; x++) {
            for (int y = 0; y < preset.height; y++) {
               if (!isValidTile.apply(new TilePosition(level, tileX + x, tileY + y))) {
                  return false;
               }
            }
         }
      }

      if (preset.canApplyToLevel(level, tileX, tileY)) {
         preset.applyToLevel(level, tileX, tileY);
         return true;
      } else {
         return false;
      }
   }

   public static void fillCellMap(Level level, boolean[] cellMap, int tile, int object) {
      GameTile t = null;
      if (tile != -1) {
         t = TileRegistry.getTile(tile);
      }

      GameObject o = null;
      if (object != -1) {
         o = ObjectRegistry.getObject(object);
      }

      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            if (cellMap[x + y * level.tileWidth]) {
               if (t != null) {
                  t.placeTile(level, x, y, false);
               }

               if (o != null) {
                  o.placeObject(level, x, y, 0, false);
               }
            }
         }
      }
   }

   public static void fillMap(
      Level level,
      GameRandom random,
      int tile,
      int replaceTile,
      float tileChance,
      int object,
      int replaceObject,
      float objectChance,
      boolean forcePlace,
      boolean onlyOnTile
   ) {
      fillMap(level, random, tile, replaceTile, tileChance, object, 0, replaceObject, objectChance, forcePlace, onlyOnTile);
   }

   public static void fillMap(
      Level level,
      GameRandom random,
      int tile,
      int replaceTile,
      float tileChance,
      int object,
      int rotation,
      int replaceObject,
      float objectChance,
      boolean forcePlace,
      boolean onlyOnTile
   ) {
      GameObject obj = null;
      if (objectChance > 0.0F) {
         obj = ObjectRegistry.getObject(object);
      }

      GameTile t = null;
      if (tileChance > 0.0F) {
         t = TileRegistry.getTile(tile);
      }

      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            if (t != null && (tileChance >= 1.0F || random.nextFloat() < tileChance) && (replaceTile == -1 || level.getTileID(x, y) == replaceTile)) {
               t.placeTile(level, x, y, false);
            }

            if (obj != null
               && (objectChance >= 1.0F || random.nextFloat() < objectChance)
               && (!onlyOnTile || level.getTileID(x, y) == tile)
               && (replaceObject == -1 || level.getObjectID(x, y) == replaceObject)
               && (forcePlace || obj.canPlace(level, x, y, rotation, false) == null)) {
               obj.placeObject(level, x, y, rotation, false);
            }
         }
      }
   }

   public static void fillMap(
      Level level,
      GameRandom random,
      int tile,
      int replaceTile,
      float tileChance,
      int object,
      boolean randomRotation,
      int replaceObject,
      float objectChance,
      boolean forcePlace,
      boolean onlyOnTile
   ) {
      GameObject obj = null;
      if (objectChance > 0.0F) {
         obj = ObjectRegistry.getObject(object);
      }

      GameTile t = null;
      if (tileChance > 0.0F) {
         t = TileRegistry.getTile(tile);
      }

      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            if (t != null && (tileChance >= 1.0F || random.nextFloat() < tileChance) && (replaceTile == -1 || level.getTileID(x, y) == replaceTile)) {
               t.placeTile(level, x, y, false);
            }

            if (obj != null
               && (objectChance >= 1.0F || random.nextFloat() < objectChance)
               && (!onlyOnTile || level.getTileID(x, y) == tile)
               && (replaceObject == -1 || level.getObjectID(x, y) == replaceObject)
               && (forcePlace || obj.canPlace(level, x, y, random.nextInt(4), false) == null)) {
               obj.placeObject(level, x, y, random.nextInt(4), false);
            }
         }
      }
   }

   public static void checkValid(Level level) {
      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            if (level.getTileID(x, y) != 0) {
               level.getTile(x, y).tickValid(level, x, y, true);
            }
         }
      }

      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            for (int layerID : ObjectLayerRegistry.getLayerIDs()) {
               if (level.getObjectID(layerID, x, y) != 0 && !level.getObject(layerID, x, y).isValid(level, layerID, x, y)) {
                  level.objectLayer.setObject(layerID, x, y, 0);
               }
            }
         }
      }

      for (Mob mob : level.entityManager.mobs) {
         if (mob.collidesWith(level)) {
            mob.remove();
         }
      }
   }

   public static Point getRandomEdgePoint(Level level, GameRandom random) {
      return getRandomEdgePoint(level, random, 0);
   }

   public static Point getRandomEdgePoint(Level level, GameRandom random, int borderSize) {
      return random.getOneOf(
         new Point(random.getIntBetween(borderSize, level.tileWidth - 1 - borderSize), borderSize),
         new Point(level.tileWidth - 1 - borderSize, random.getIntBetween(borderSize, level.tileHeight - 1 - borderSize)),
         new Point(random.getIntBetween(borderSize, level.tileWidth - 1 - borderSize), level.tileHeight - 1 - borderSize),
         new Point(borderSize, random.getIntBetween(borderSize, level.tileHeight - 1 - borderSize))
      );
   }

   public static Point getRandomMapPoint(Level level, GameRandom random) {
      return new Point(random.getIntBetween(0, level.tileWidth - 1), random.getIntBetween(0, level.tileHeight - 1));
   }

   public static void generateAndCollectLevelContentByBiome(
      Level level,
      int startRegionX,
      int startRegionY,
      int regionsWidth,
      int regionsHeight,
      HashMap<Integer, Integer> biomes,
      GenerationTools.BiomeRegionCollectorGetter tiles,
      GenerationTools.BiomeRegionCollectorGetter objects,
      GenerationTools.BiomeRegionCollectorGetter items
   ) {
      generateAndCollectLevelContent(
         level,
         startRegionX,
         startRegionY,
         regionsWidth,
         regionsHeight,
         biomes,
         tiles == null
            ? null
            : (region, regionTileX, regionTileY, l, tileX, tileY) -> tiles.getCollector(region.biomeLayer.getBiomeIDByRegion(regionTileX, regionTileY)),
         objects == null
            ? null
            : (region, regionTileX, regionTileY, l, tileX, tileY) -> objects.getCollector(region.biomeLayer.getBiomeIDByRegion(regionTileX, regionTileY)),
         items == null
            ? null
            : (region, regionTileX, regionTileY, l, tileX, tileY) -> items.getCollector(region.biomeLayer.getBiomeIDByRegion(regionTileX, regionTileY))
      );
   }

   public static void generateAndCollectLevelContent(
      Level level,
      int startRegionX,
      int startRegionY,
      int regionsWidth,
      int regionsHeight,
      HashMap<Integer, Integer> biomes,
      GenerationTools.RegionCollectorGetter tiles,
      GenerationTools.RegionCollectorGetter objects,
      GenerationTools.RegionCollectorGetter items
   ) {
      int levelRegionsWidth = level.regionManager.getRegionsWidth();
      int levelRegionsHeight = level.regionManager.getRegionsHeight();
      if (levelRegionsWidth > 0 && levelRegionsWidth < startRegionX + regionsWidth) {
         regionsWidth = levelRegionsWidth - startRegionX;
      }

      if (levelRegionsHeight > 0 && levelRegionsHeight < startRegionY + regionsHeight) {
         regionsHeight = levelRegionsHeight - startRegionY;
      }

      for (int regionX = startRegionX; regionX < startRegionX + regionsWidth; regionX++) {
         for (int regionY = startRegionY; regionY < startRegionY + regionsHeight; regionY++) {
            Region region = level.regionManager.getRegion(regionX, regionY, true);
            if (region != null) {
               for (int regionTileX = 0; regionTileX < region.tileWidth; regionTileX++) {
                  for (int regionTileY = 0; regionTileY < region.tileHeight; regionTileY++) {
                     int tileX = region.tileXOffset + regionTileX;
                     int tileY = region.tileYOffset + regionTileY;
                     if (biomes != null) {
                        int biomeID = region.biomeLayer.getBiomeIDByRegion(regionTileX, regionTileY);
                        biomes.put(biomeID, biomes.getOrDefault(biomeID, 0) + 1);
                     }

                     if (tiles != null) {
                        HashMap<Integer, Integer> collector = tiles.getCollector(region, regionTileX, regionTileY, level, tileX, tileY);
                        if (collector != null) {
                           int tileID = region.tileLayer.getTileIDByRegion(regionTileX, regionTileY);
                           collector.put(tileID, collector.getOrDefault(tileID, 0) + 1);
                        }
                     }

                     if (objects != null) {
                        HashMap<Integer, Integer> collector = objects.getCollector(region, regionTileX, regionTileY, level, tileX, tileY);
                        if (collector != null) {
                           int objectID = region.objectLayer.getObjectIDByRegion(0, regionTileX, regionTileY);
                           collector.put(objectID, collector.getOrDefault(objectID, 0) + 1);
                        }
                     }

                     if (items != null) {
                        HashMap<Integer, Integer> collector = items.getCollector(region, regionTileX, regionTileY, level, tileX, tileY);
                        if (collector != null) {
                           ObjectEntity oe = level.entityManager.getObjectEntity(tileX, tileY);
                           if (oe instanceof OEInventory) {
                              Inventory inventory = ((OEInventory)oe).getInventory();

                              for (int i = 0; i < inventory.getSize(); i++) {
                                 InventoryItem item = inventory.getItem(i);
                                 if (item != null) {
                                    collector.put(item.item.getID(), collector.getOrDefault(item.item.getID(), 1) + item.getAmount());
                                 }
                              }
                           } else if (oe instanceof TrialEntranceObjectEntity) {
                              TrialEntranceObjectEntity trialEntrance = (TrialEntranceObjectEntity)oe;

                              for (List<InventoryItem> list : trialEntrance.lootList) {
                                 for (InventoryItem item : list) {
                                    if (item != null) {
                                       collector.put(item.item.getID(), collector.getOrDefault(item.item.getID(), 1) + item.getAmount());
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void collectLevelContent(Level level, HashMap<Integer, Integer> tiles, HashMap<Integer, Integer> objects, HashMap<Integer, Integer> items) {
      for (int x = 0; x < level.tileWidth; x++) {
         for (int y = 0; y < level.tileHeight; y++) {
            if (tiles != null) {
               int tileID = level.getTileID(x, y);
               tiles.put(tileID, tiles.getOrDefault(tileID, 0) + 1);
            }

            if (objects != null) {
               int objectID = level.getObjectID(x, y);
               objects.put(objectID, objects.getOrDefault(objectID, 0) + 1);
            }

            if (items != null) {
               ObjectEntity oe = level.entityManager.getObjectEntity(x, y);
               if (oe instanceof OEInventory) {
                  Inventory inventory = ((OEInventory)oe).getInventory();

                  for (int i = 0; i < inventory.getSize(); i++) {
                     InventoryItem item = inventory.getItem(i);
                     if (item != null) {
                        items.put(item.item.getID(), items.getOrDefault(item.item.getID(), 1) + item.getAmount());
                     }
                  }
               } else if (oe instanceof TrialEntranceObjectEntity) {
                  TrialEntranceObjectEntity trialEntrance = (TrialEntranceObjectEntity)oe;

                  for (List<InventoryItem> list : trialEntrance.lootList) {
                     for (InventoryItem item : list) {
                        if (item != null) {
                           items.put(item.item.getID(), items.getOrDefault(item.item.getID(), 1) + item.getAmount());
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void printLevelContent(Level level) {
      HashMap<Integer, Integer> tiles = new HashMap<>();
      HashMap<Integer, Integer> objects = new HashMap<>();
      HashMap<Integer, Integer> items = new HashMap<>();
      collectLevelContent(level, tiles, objects, items);
      printLevelContent(tiles, objects, items);
   }

   public static void printLevelContent(HashMap<Integer, Integer> tiles, HashMap<Integer, Integer> objects, HashMap<Integer, Integer> items) {
      printLevelContent(tiles, t -> true, objects, o -> true, items, item -> true);
   }

   public static void printLevelContent(
      HashMap<Integer, Integer> tiles,
      Predicate<GameTile> tileFilter,
      HashMap<Integer, Integer> objects,
      Predicate<GameObject> objectFilter,
      HashMap<Integer, Integer> items,
      Predicate<Item> itemFilter
   ) {
      if (tiles != null && tiles.isEmpty()) {
         tiles = null;
      }

      if (objects != null && objects.isEmpty()) {
         objects = null;
      }

      if (items != null && items.isEmpty()) {
         items = null;
      }

      Comparator<Entry<Integer, Integer>> comparator = Comparator.comparingInt(Entry::getValue);
      comparator = comparator.reversed();
      int totalTiles = tiles == null ? 0 : tiles.values().stream().mapToInt(i -> i).sum();
      int totalObjects = objects == null ? 0 : objects.values().stream().mapToInt(i -> i).sum();
      AtomicInteger nameLength = new AtomicInteger(0);
      AtomicInteger countLength = new AtomicInteger(0);
      if (tiles != null) {
         for (Entry<Integer, Integer> e : tiles.entrySet()) {
            GameTile tile = TileRegistry.getTile(e.getKey());
            if (tileFilter.test(tile)) {
               nameLength.updateAndGet(l -> Math.max(tile.getStringID().length(), l));
               countLength.updateAndGet(l -> Math.max(e.getValue().toString().length(), l));
            }
         }
      }

      if (objects != null) {
         for (Entry<Integer, Integer> e : objects.entrySet()) {
            GameObject object = ObjectRegistry.getObject(e.getKey());
            if (objectFilter.test(object)) {
               nameLength.updateAndGet(l -> Math.max(object.getStringID().length(), l));
               countLength.updateAndGet(l -> Math.max(e.getValue().toString().length(), l));
            }
         }
      }

      if (items != null) {
         for (Entry<Integer, Integer> e : items.entrySet()) {
            Item item = ItemRegistry.getItem(e.getKey());
            if (itemFilter.test(item)) {
               nameLength.updateAndGet(l -> Math.max(item.getStringID().length(), l));
               countLength.updateAndGet(l -> Math.max(e.getValue().toString().length(), l));
            }
         }
      }

      if (tiles != null) {
         if (objects != null || items != null) {
            System.out.println("TILES:");
         }

         tiles.entrySet()
            .stream()
            .filter(ex -> tileFilter.test(TileRegistry.getTile((Integer)ex.getKey())))
            .sorted(comparator)
            .forEach(
               ex -> System.out
                  .println(
                     "\t"
                        + GameUtils.padString(TileRegistry.getTile((Integer)ex.getKey()).getStringID(), nameLength.get(), '.')
                        + " "
                        + GameUtils.padString(((Integer)ex.getValue()).toString(), countLength.get(), ' ')
                        + " ("
                        + GameMath.toDecimals((double)((Integer)ex.getValue()).intValue() / totalTiles * 100.0, 2)
                        + "%)"
                  )
            );
      }

      if (objects != null) {
         if (tiles != null || items != null) {
            System.out.println("OBJECTS:");
         }

         objects.entrySet()
            .stream()
            .filter(ex -> objectFilter.test(ObjectRegistry.getObject((Integer)ex.getKey())))
            .sorted(comparator)
            .forEach(
               ex -> System.out
                  .println(
                     "\t"
                        + GameUtils.padString(ObjectRegistry.getObject((Integer)ex.getKey()).getStringID(), nameLength.get(), '.')
                        + " "
                        + GameUtils.padString(((Integer)ex.getValue()).toString(), countLength.get(), ' ')
                        + " ("
                        + GameMath.toDecimals((double)((Integer)ex.getValue()).intValue() / totalObjects * 100.0, 2)
                        + "%)"
                  )
            );
      }

      if (items != null) {
         if (tiles != null || objects != null) {
            System.out.println("ITEMS:");
         }

         items.entrySet()
            .stream()
            .filter(ex -> itemFilter.test(ItemRegistry.getItem((Integer)ex.getKey())))
            .sorted(comparator)
            .forEach(
               ex -> System.out
                  .println(
                     "\t"
                        + GameUtils.padString(ItemRegistry.getItem((Integer)ex.getKey()).getStringID(), nameLength.get(), '.')
                        + " "
                        + GameUtils.padString(((Integer)ex.getValue()).toString(), countLength.get(), ' ')
                  )
            );
      }
   }

   @FunctionalInterface
   public interface BiomeRegionCollectorGetter {
      HashMap<Integer, Integer> getCollector(int var1);
   }

   @FunctionalInterface
   public interface CanPlaceFunction {
      boolean canPlace(Level var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface PlaceFunction {
      void place(Level var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface RegionCollectorGetter {
      HashMap<Integer, Integer> getCollector(Region var1, int var2, int var3, Level var4, int var5, int var6);
   }
}
