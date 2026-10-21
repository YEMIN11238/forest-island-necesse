package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.stream.Stream;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;
import necesse.level.maps.regionSystem.SubRegion;

public class SettlementRoomsManager {
   public final ServerSettlementData data;
   private final PointHashMap<SettlementRoom> rooms = new PointHashMap<>();

   public SettlementRoomsManager(ServerSettlementData data) {
      this.data = data;
   }

   public void addSaveData(SaveData save) {
      SaveData roomsSave = new SaveData("ROOMS");
      PointHashSet savedTiles = new PointHashSet();

      for (SettlementRoom room : this.rooms.values()) {
         if (savedTiles.add(room.tileX, room.tileY)) {
            SaveData roomSave = new SaveData("ROOM");
            roomSave.addPoint("tile", new Point(room.tileX, room.tileY));
            roomsSave.addSaveData(roomSave);
         }
      }

      save.addSaveData(roomsSave);
   }

   public void loadSaveData(LoadData save, int tileXOffset, int tileYOffset) {
      Stream<LoadData> roomSaves = Stream.empty();
      roomSaves = Stream.concat(roomSaves, save.getLoadDataByName("ROOM").stream().filter(LoadData::isArray));
      LoadData roomsSave = save.getFirstLoadDataByName("ROOMS");
      if (roomsSave != null) {
         roomSaves = Stream.concat(roomSaves, roomsSave.getLoadDataByName("ROOM").stream().filter(LoadData::isArray));
      }

      roomSaves.forEach(c -> {
         try {
            Point tile = c.getPoint("tile", null, false);
            if (tile == null) {
               int x = c.getInt("x", Integer.MIN_VALUE, false);
               int y = c.getInt("y", Integer.MIN_VALUE, false);
               if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
                  tile = new Point(x, y);
               }
            }

            if (tile != null) {
               tile.translate(tileXOffset, tileYOffset);
               SettlementRoom room = this.getRoom(tile.x, tile.y);
               if (room != null) {
                  room.recalculateStats();
               }
            }

            if (c.hasLoadDataByName("SETTLER")) {
               try {
                  LevelSettler settler = new LevelSettler(this.data, c.getFirstLoadDataByName("SETTLER"), tileXOffset, tileYOffset);
                  this.data.settlers.put(settler);
                  if (tile != null && !this.data.getLevel().isOutside(tile.x, tile.y)) {
                     ConnectedSubRegionsResult room = this.data.getLevel().regionManager.getRoomConnectedByTile(tile.x, tile.y, true, 2000);
                     if (room != null) {
                        boolean found = false;

                        for (SubRegion roomSr : room.connectedRegions) {
                           for (Point p : roomSr.getLevelTiles()) {
                              SettlementBed loadedBed = this.data.addOrValidateBed(p.x, p.y);
                              if (loadedBed != null && !loadedBed.isLocked && loadedBed.getSettler() == null) {
                                 settler.bed = loadedBed;
                                 loadedBed.settler = settler;
                                 found = true;
                                 break;
                              }
                           }

                           if (found) {
                              break;
                           }
                        }
                     }
                  }
               } catch (Exception e) {
                  System.err.println("Could not load settlement settler at level " + this.data.getLevel().getIdentifier());
                  e.printStackTrace();
               }
            }
         } catch (Exception e) {
            System.err.println("Could not load settlement room at level " + this.data.getLevel().getIdentifier());
         }
      });
   }

   public void refreshRooms(Iterable<Point> tiles) {
      for (Point tile : tiles) {
         SettlementRoom room = this.rooms.remove(tile.x, tile.y);
         if (room != null) {
            room.invalidate();
         }
      }
   }

   public void recalculateStats(Iterable<Point> tiles) {
      for (Point tile : tiles) {
         this.recalculateStats(tile.x, tile.y);
      }
   }

   public void recalculateStats(int tileX, int tileY) {
      SettlementRoom room = this.rooms.get(tileX, tileY);
      if (room != null) {
         room.recalculateStats();
      }
   }

   public void findAndCalculateRoom(int tileX, int tileY) {
      SettlementRoom room = this.getRoom(tileX, tileY);
      if (room != null) {
         room.calculateStats();
      }
   }

   public SettlementRoom getRoom(int tileX, int tileY) {
      if (!this.data.networkData.isTileWithinBounds(tileX, tileY)) {
         SettlementRoom room = this.rooms.remove(tileX, tileY);
         if (room != null) {
            room.invalidate();
         }

         return null;
      } else {
         return this.data.getLevel().isOutside(tileX, tileY)
            ? null
            : this.rooms.compute(tileX, tileY, (k, v) -> v != null && !v.isInvalidated() ? v : new SettlementRoom(this.data, this.rooms, tileX, tileY));
      }
   }

   public void clearOutsideBounds(Rectangle tileRectangle) {
      LinkedList<Point> invalidRoomTiles = new LinkedList<>();

      for (Point tile : this.rooms.getKeys()) {
         if (!tileRectangle.contains(tile.x, tile.y)) {
            invalidRoomTiles.add(tile);
         }
      }

      if (!invalidRoomTiles.isEmpty()) {
         for (Point tile : invalidRoomTiles) {
            SettlementRoom room = this.rooms.remove(tile.x, tile.y);
            room.invalidate();
         }
      }
   }
}
