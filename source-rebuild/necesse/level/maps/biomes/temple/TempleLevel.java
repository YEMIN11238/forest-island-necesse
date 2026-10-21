package necesse.level.maps.biomes.temple;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldGenerator;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.TempleShrinePreset;
import necesse.level.maps.presets.furniturePresets.BedDresserPreset;
import necesse.level.maps.presets.furniturePresets.BenchPreset;
import necesse.level.maps.presets.furniturePresets.BookshelfClockPreset;
import necesse.level.maps.presets.furniturePresets.BookshelvesPreset;
import necesse.level.maps.presets.furniturePresets.CabinetsPreset;
import necesse.level.maps.presets.furniturePresets.DeskBookshelfPreset;
import necesse.level.maps.presets.furniturePresets.DinnerTablePreset;
import necesse.level.maps.presets.furniturePresets.DisplayStandClockPreset;
import necesse.level.maps.presets.furniturePresets.ModularDinnerTablePreset;
import necesse.level.maps.presets.furniturePresets.ModularTablesPreset;
import necesse.level.maps.presets.set.FurnitureSet;

public class TempleLevel extends Level {
   public static int PADDING_TILES = 50;
   public static int CORRIDOR_MIN_WIDTH = 9;
   public static int CORRIDOR_MAX_WIDTH = 11;
   public static int LOOT_ROOM_CORRIDOR_MIN_WIDTH = 6;
   public static int LOOT_ROOM_CORRIDOR_MAX_WIDTH = 8;
   public static int LOOT_ROOM_MIN_SIZE = 15;
   public static int LOOT_ROOM_MAX_SIZE = 20;
   public static ArrayList<TempleLevel.TempleLayout> layouts = new ArrayList<>();

   public static Level generateNew(LevelIdentifier identifier, WorldEntity worldEntity) {
      TempleLevel.TempleNode first = generateLayout(identifier);
      Rectangle bounds = getSize(first);
      GameRandom r = new GameRandom(WorldGenerator.getSeed(identifier));
      TempleLevel level = new TempleLevel(identifier, bounds.width + PADDING_TILES * 2, bounds.height + PADDING_TILES * 2, worldEntity);
      level.generateTemple(first, r, -bounds.x + PADDING_TILES, -bounds.y + PADDING_TILES);
      Point exitPosition = new Point(PADDING_TILES - bounds.x - 1, PADDING_TILES - bounds.y);
      GameObject exitObject = ObjectRegistry.getObject("templeexit");
      exitObject.placeObject(level, exitPosition.x, exitPosition.y, 0, false);
      return level;
   }

   public TempleLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
      this.baseBiome = BiomeRegistry.TEMPLE;
      this.isCave = true;
   }

   @Override
   public void onLoadingComplete() {
      super.onLoadingComplete();
      this.baseBiome = BiomeRegistry.TEMPLE;
   }

   @Override
   public GameMessage getSetSpawnError(int x, int y, ServerClient client) {
      return new LocalMessage("misc", "spawndungeon");
   }

   @Override
   public LootTable getCrateLootTable(int tileX, int tileY) {
      return LootTablePresets.desertDeepCrate;
   }

   protected void generateTemple(TempleLevel.TempleNode node, GameRandom random, int xOffset, int yOffset) {
      node = node.first;
      int sandBrickID = TileRegistry.getTileID("sandbrick");
      int woodFloorID = TileRegistry.getTileID("woodfloor");
      int deepSandstoneWall = ObjectRegistry.getObjectID("deepsandstonewall");

      for (int x = 0; x < this.tileWidth; x++) {
         for (int y = 0; y < this.tileHeight; y++) {
            if (random.getChance(0.75F)) {
               this.setTile(x, y, sandBrickID);
            } else {
               this.setTile(x, y, woodFloorID);
            }

            this.setObject(x, y, deepSandstoneWall);
         }
      }

      int currentWidth = random.getIntBetween(CORRIDOR_MIN_WIDTH, CORRIDOR_MAX_WIDTH);
      LinesGeneration lg = new LinesGeneration(node.tileX + xOffset, node.tileY + yOffset, currentWidth);
      LinesGeneration currentLG = lg;

      while (node.next != null) {
         TempleLevel.TempleNode next = node.next;
         currentWidth = GameMath.limit(random.getIntOffset(currentWidth, 1), CORRIDOR_MIN_WIDTH, CORRIDOR_MAX_WIDTH);
         currentLG = currentLG.addLineTo(next.tileX + xOffset, next.tileY + yOffset, currentWidth);

         for (Point lootRoom : next.lootRooms) {
            int lootWidth = GameMath.limit(random.getIntOffset(currentWidth, 1), LOOT_ROOM_CORRIDOR_MIN_WIDTH, LOOT_ROOM_CORRIDOR_MAX_WIDTH);
            currentLG.addLineTo(lootRoom.x + xOffset, lootRoom.y + yOffset, lootWidth);
         }

         node = node.next;
      }

      CellAutomaton ca = lg.doCellularAutomaton(random);
      ca.cleanHardEdges();
      node = node.first;
      addAliveRoom(ca, node.tileX + xOffset, node.tileY + yOffset, 10);
      TempleLevel.RoomLocation lastRoom = new TempleLevel.RoomLocation(node.tileX + xOffset, node.tileY + yOffset, 10);
      LinkedList<TempleLevel.RoomLocation> lootRooms = new LinkedList<>();

      while (node.next != null) {
         TempleLevel.TempleNode next = node.next;

         for (Point lootRoom : next.lootRooms) {
            int roomSize = random.getIntBetween(LOOT_ROOM_MIN_SIZE, LOOT_ROOM_MAX_SIZE) / 2;
            addAliveRoom(ca, lootRoom.x + xOffset, lootRoom.y + yOffset, roomSize);
            lootRooms.add(new TempleLevel.RoomLocation(lootRoom.x + xOffset, lootRoom.y + yOffset, roomSize));
         }

         node = node.next;
      }

      addAliveRoom(ca, node.tileX + xOffset, node.tileY + yOffset, 10);
      TempleLevel.RoomLocation latRoom = new TempleLevel.RoomLocation(node.tileX + xOffset, node.tileY + yOffset, 10);
      ca.forEachTile(this, (level, tileX, tileY) -> level.setObject(tileX, tileY, 0));
      ca.placeEdgeWalls(this, deepSandstoneWall, true);
      ArrayList<Point> validShrineTiles = new ArrayList<>();
      TempleShrinePreset shrinePreset = new TempleShrinePreset(random);
      ca.forEachTile(this, (l, tileX, tileY) -> {
         if (shrinePreset.canApplyToLevel(this, tileX, tileY - shrinePreset.height + 1)) {
            validShrineTiles.add(new Point(tileX, tileY - shrinePreset.height + 1));
         }
      });
      if (!validShrineTiles.isEmpty()) {
         Point shrineTile = random.getOneOf(validShrineTiles);
         shrinePreset.applyToLevel(this, shrineTile.x, shrineTile.y);
      }

      ca.forEachTile(this, (l, tileX, tileY) -> {
         if (random.getChance(0.02F)) {
            GameObject breakObject = ObjectRegistry.getObject(random.getOneOf("crate", "vase"));
            if (breakObject.canPlace(l, tileX, tileY, 0, false) == null) {
               breakObject.placeObject(l, tileX, tileY, 0, false);
            }
         }
      });
      TicketSystemList<Preset> templeFurniture = new TicketSystemList<>();
      templeFurniture.addObject(100, new BedDresserPreset(FurnitureSet.palm, 2));
      templeFurniture.addObject(100, new BenchPreset(FurnitureSet.palm, 2));
      templeFurniture.addObject(100, new BookshelfClockPreset(FurnitureSet.palm, 2));
      templeFurniture.addObject(100, new BookshelvesPreset(FurnitureSet.palm, 2, 3));
      templeFurniture.addObject(100, new CabinetsPreset(FurnitureSet.palm, 2, 3));
      templeFurniture.addObject(100, new DeskBookshelfPreset(FurnitureSet.palm, 2));
      templeFurniture.addObject(100, new DinnerTablePreset(FurnitureSet.palm, 2));
      templeFurniture.addObject(100, new DisplayStandClockPreset(FurnitureSet.palm, 2, random, null));
      templeFurniture.addObject(100, new ModularDinnerTablePreset(FurnitureSet.palm, 2, 1));
      templeFurniture.addObject(100, new ModularTablesPreset(FurnitureSet.palm, 2, 2, true));
      ca.placeFurniturePresets(templeFurniture, 0.4F, this, random);
      generateColumns(this, random, lastRoom.x, lastRoom.y, 4, lastRoom.radius - lastRoom.radius / 3, FurnitureSet.palm.candelabra);
      generateColumns(this, random, latRoom.x, latRoom.y, 4, latRoom.radius - latRoom.radius / 3, FurnitureSet.palm.candelabra);
      ObjectRegistry.getObject("templeentrance").placeObject(this, latRoom.x - 1, latRoom.y, 0, false);
      int columnID = ObjectRegistry.getObjectID("deepsandstonecolumn");
      AtomicInteger lootRotation = new AtomicInteger();

      for (TempleLevel.RoomLocation room : lootRooms) {
         generateColumns(this, random, room.x, room.y, random.getIntBetween(3, 5), room.radius - room.radius / 3, columnID);
         this.setObject(room.x, room.y, FurnitureSet.palm.chest, 2);
         LootTablePresets.templeChest.applyToLevel(random, this.buffManager.getModifier(LevelModifiers.LOOT), this, room.x, room.y, this, lootRotation);
      }
   }

   protected static void addAliveRoom(CellAutomaton ca, int centerX, int centerY, int radius) {
      for (int x = centerX - radius; x <= centerX + radius; x++) {
         for (int y = centerY - radius; y <= centerY + radius; y++) {
            if (new Point(centerX, centerY).distance(x, y) <= radius + 0.5F) {
               ca.setAlive(x, y);
            }
         }
      }
   }

   protected static void generateColumns(Level level, GameRandom random, int centerX, int centerY, int columns, int range, int object) {
      int anglerPer = 360 / columns;
      int columnAngle = random.nextInt(360);

      for (int i = 0; i < columns; i++) {
         columnAngle += random.getIntOffset(anglerPer, anglerPer / 5);
         Float columnDir = GameMath.getAngleDir(columnAngle);
         level.setObject(centerX + (int)(columnDir.x * range), centerY + (int)(columnDir.y * range), object);
      }
   }

   protected static TempleLevel.TempleNode generateLayout(LevelIdentifier identifier) {
      GameRandom r = new GameRandom(WorldGenerator.getSeed(identifier));
      TempleLevel.TempleLayout layout = r.getOneOf(layouts);
      TempleLevel.TempleNode first = new TempleLevel.TempleNode(0, 0);
      layout.generate(first, r);
      if (r.nextBoolean()) {
         first = first.mirrorX();
      }

      if (r.nextBoolean()) {
         first = first.mirrorY();
      }

      PresetRotation rotation = r.getOneOf(PresetRotation.CLOCKWISE, PresetRotation.HALF_180, PresetRotation.ANTI_CLOCKWISE, null);
      if (rotation != null) {
         first = first.rotate(rotation);
      }

      return first;
   }

   protected static Rectangle getSize(TempleLevel.TempleNode node) {
      node = node.first;
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;

      while (node != null) {
         if (minX > node.tileX) {
            minX = node.tileX;
         }

         if (minY > node.tileY) {
            minY = node.tileY;
         }

         if (maxX < node.tileX) {
            maxX = node.tileX;
         }

         if (maxY < node.tileY) {
            maxY = node.tileY;
         }

         for (Point lootRoom : node.lootRooms) {
            if (minX > lootRoom.x) {
               minX = lootRoom.x;
            }

            if (minY > lootRoom.y) {
               minY = lootRoom.y;
            }

            if (maxX < lootRoom.x) {
               maxX = lootRoom.x;
            }

            if (maxY < lootRoom.y) {
               maxY = lootRoom.y;
            }
         }

         node = node.next;
      }

      return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
   }

   static {
      layouts.add(
         (start, r) -> {
            int minSectionDistance = 50;
            int maxSectionDistance = 60;
            int minRoomDistance = 20;
            int maxRoomDistance = 30;
            start.nextAngle(r, 70.0F, 110.0F, minSectionDistance, maxSectionDistance)
               .lootRoomAngle(r, 150.0F, 210.0F, minRoomDistance, maxRoomDistance)
               .nextAngle(r, -20.0F, 20.0F, minSectionDistance, maxSectionDistance)
               .lootRoomAngle(r, 250.0F, 380.0F, minRoomDistance, maxRoomDistance)
               .nextAngle(r, 70.0F, 110.0F, minSectionDistance, maxSectionDistance)
               .lootRoomAngle(r, -20.0F, 90.0F, minRoomDistance, maxRoomDistance)
               .nextAngle(r, 170.0F, 190.0F, minSectionDistance, maxSectionDistance)
               .lootRoomAngle(r, 250.0F, 290.0F, minRoomDistance * 2 / 3.0F, maxRoomDistance * 2 / 3.0F)
               .nextAngle(r, 170.0F, 190.0F, minSectionDistance, maxSectionDistance)
               .lootRoomAngle(r, 170.0F, 280.0F, minRoomDistance, maxRoomDistance)
               .nextAngle(r, 70.0F, 110.0F, minSectionDistance, maxSectionDistance)
               .lootRoomAngle(r, 70.0F, 190.0F, minRoomDistance, maxRoomDistance)
               .nextAngle(r, -20.0F, 20.0F, minSectionDistance, maxSectionDistance)
               .nextAngle(r, -10.0F, 120.0F, minRoomDistance, maxRoomDistance);
         }
      );
   }

   protected static class RoomLocation extends Point {
      public int radius;

      public RoomLocation(int x, int y, int radius) {
         super(x, y);
         this.radius = radius;
      }
   }

   @FunctionalInterface
   public interface TempleLayout {
      void generate(TempleLevel.TempleNode var1, GameRandom var2);
   }

   public static class TempleNode {
      protected final int tileX;
      protected final int tileY;
      protected LinkedList<Point> lootRooms = new LinkedList<>();
      protected TempleLevel.TempleNode first;
      protected TempleLevel.TempleNode prev;
      protected TempleLevel.TempleNode next;

      protected TempleNode(int tileX, int tileY) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.first = this;
      }

      protected TempleLevel.TempleNode mirrorX() {
         TempleLevel.TempleNode last = null;

         for (TempleLevel.TempleNode current = this.first; current != null; current = current.next) {
            TempleLevel.TempleNode next = new TempleLevel.TempleNode(-current.tileX, current.tileY);
            if (last != null) {
               last.next = next;
               next.first = last.first;
            }

            next.prev = last;

            for (Point lootRoom : current.lootRooms) {
               next.lootRooms.add(new Point(-lootRoom.x, lootRoom.y));
            }

            last = next;
         }

         return last == null ? null : last.first;
      }

      protected TempleLevel.TempleNode mirrorY() {
         TempleLevel.TempleNode last = null;

         for (TempleLevel.TempleNode current = this.first; current != null; current = current.next) {
            TempleLevel.TempleNode next = new TempleLevel.TempleNode(current.tileX, -current.tileY);
            if (last != null) {
               last.next = next;
               next.first = last.first;
            }

            next.prev = last;

            for (Point lootRoom : current.lootRooms) {
               next.lootRooms.add(new Point(lootRoom.x, -lootRoom.y));
            }

            last = next;
         }

         return last == null ? null : last.first;
      }

      protected TempleLevel.TempleNode rotate(int rightAngles) {
         return this.rotate(PresetRotation.toRotationAngle(rightAngles));
      }

      protected TempleLevel.TempleNode rotate(PresetRotation rotation) {
         if (rotation == null) {
            return this.first;
         }

         TempleLevel.TempleNode last = null;

         for (TempleLevel.TempleNode current = this.first; current != null; current = current.next) {
            Point nextTile = PresetUtils.getRotatedPoint(current.tileX, current.tileY, 0, 0, rotation);
            TempleLevel.TempleNode next = new TempleLevel.TempleNode(nextTile.x, nextTile.y);
            if (last != null) {
               last.next = next;
               next.first = last.first;
            }

            next.prev = last;

            for (Point lootRoom : current.lootRooms) {
               Point nextRoom = PresetUtils.getRotatedPoint(lootRoom.x, lootRoom.y, 0, 0, rotation);
               next.lootRooms.add(new Point(nextRoom.x, nextRoom.y));
            }

            last = next;
         }

         return last == null ? null : last.first;
      }

      public TempleLevel.TempleNode lootRoom(int deltaX, int deltaY) {
         this.lootRooms.add(new Point(this.tileX + deltaX, this.tileY + deltaY));
         return this;
      }

      public TempleLevel.TempleNode lootRoomAngle(float angle, float distance) {
         Float dir = GameMath.getAngleDir(angle);
         return this.lootRoom((int)(dir.x * distance), (int)(dir.y * distance));
      }

      public TempleLevel.TempleNode lootRoomAngle(GameRandom random, float minAngle, float maxAngle, float minDistance, float maxDistance) {
         return this.lootRoomAngle(random.getFloatBetween(minAngle, maxAngle), random.getFloatBetween(minDistance, maxDistance));
      }

      public TempleLevel.TempleNode next(int deltaX, int deltaY) {
         this.next = new TempleLevel.TempleNode(this.tileX + deltaX, this.tileY + deltaY);
         this.next.prev = this;
         this.next.first = this.first;
         return this.next;
      }

      public TempleLevel.TempleNode nextAngle(float angle, float distance) {
         Float dir = GameMath.getAngleDir(angle);
         return this.next((int)(dir.x * distance), (int)(dir.y * distance));
      }

      public TempleLevel.TempleNode nextAngle(GameRandom random, float minAngle, float maxAngle, float minDistance, float maxDistance) {
         return this.nextAngle(random.getFloatBetween(minAngle, maxAngle), random.getFloatBetween(minDistance, maxDistance));
      }
   }
}
