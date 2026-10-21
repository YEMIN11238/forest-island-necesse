package necesse.level.maps.wireManager;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class WireManager {
   public static final int redWire = 0;
   public static final int greenWire = 1;
   public static final int blueWire = 2;
   public static final int yellowWire = 3;
   private static final int bitSize = 8;
   public static final int totalWires = 4;
   public static final String wireIdentifiers = "RGBY";
   private final Level level;
   private static final Point[] connections = new Point[]{new Point(0, -1), new Point(1, 0), new Point(0, 1), new Point(-1, 0)};

   public WireManager(Level level) {
      this.level = level;
   }

   public void clientTick() {
   }

   public void serverTick() {
   }

   public void addWireDrawables(SharedTextureDrawOptions list, int tileX, int tileY, GameCamera camera, TickManager tickManager) {
      if (this.getWireData(tileX, tileY) != 0) {
         Performance.record(tickManager, "wireDrawSetup", () -> {
            for (int i = 0; i < 4; i++) {
               if (this.hasWire(tileX, tileY, i)) {
                  this.addWireDrawablesPreset(list, tileX, tileY, camera, i);
               }
            }
         });
      }
   }

   public void drawWirePreset(int tileX, int tileY, GameCamera camera, int wireID, Color color) {
      SharedTextureDrawOptions draws = new SharedTextureDrawOptions(GameResources.wire);
      this.addWireDrawablesPreset(draws, tileX, tileY, camera, wireID, color);
      draws.draw();
   }

   public void drawWirePreset(int tileX, int tileY, GameCamera camera, int wireID) {
      this.drawWirePreset(tileX, tileY, camera, wireID, getWireColor(wireID));
   }

   public void addWireDrawablesPreset(SharedTextureDrawOptions list, int tileX, int tileY, GameCamera camera, int wireID, Color color) {
      float offset = wireID - 2 + 0.5F;
      int intOffset = (int)(offset * 5.0F);
      this.addWireDrawables(list, tileX, tileY, camera, wireID, intOffset, intOffset, color);
   }

   public void addWireDrawablesPreset(SharedTextureDrawOptions list, int tileX, int tileY, GameCamera camera, int wireID) {
      this.addWireDrawablesPreset(list, tileX, tileY, camera, wireID, getWireColor(wireID));
   }

   private void addWireDrawables(SharedTextureDrawOptions list, int tileX, int tileY, GameCamera camera, int wireID, int xOffset, int yOffset, Color color) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      float brightness = this.isWireActive(tileX, tileY, wireID) ? 1.0F : 0.33F;
      Point sprite = this.getWireSprite(tileX, tileY, wireID);
      list.addSprite(sprite.x, sprite.y, 32).color(r, g, b).brightness(brightness).pos(drawX + xOffset, drawY + yOffset);
   }

   public Point getWireSprite(int tileX, int tileY, int wireID) {
      boolean connectUp = this.hasWire(tileX, tileY - 1, wireID);
      boolean connectRight = this.hasWire(tileX + 1, tileY, wireID);
      boolean connectDown = this.hasWire(tileX, tileY + 1, wireID);
      boolean connectLeft = this.hasWire(tileX - 1, tileY, wireID);
      if (connectUp) {
         if (connectRight) {
            if (connectLeft) {
               return connectDown ? new Point(1, 0) : new Point(3, 2);
            } else {
               return connectDown ? new Point(2, 1) : new Point(0, 2);
            }
         } else if (connectLeft) {
            return connectDown ? new Point(3, 1) : new Point(1, 2);
         } else {
            return connectDown ? new Point(2, 0) : new Point(2, 3);
         }
      } else if (connectRight) {
         if (connectDown) {
            return connectLeft ? new Point(2, 2) : new Point(0, 1);
         } else {
            return connectLeft ? new Point(3, 0) : new Point(1, 3);
         }
      } else if (connectDown) {
         return connectLeft ? new Point(1, 1) : new Point(3, 3);
      } else {
         return connectLeft ? new Point(0, 3) : new Point(0, 0);
      }
   }

   public static Color getWireColor(int wireID) {
      if (wireID == 0) {
         return new Color(220, 50, 50);
      } else if (wireID == 1) {
         return new Color(50, 220, 50);
      } else if (wireID == 2) {
         return new Color(50, 50, 220);
      } else {
         return wireID == 3 ? new Color(220, 220, 50) : new Color(255, 255, 255);
      }
   }

   public void updateWire(int x, int y, boolean active) {
      for (int i = 0; i < 4; i++) {
         this.updateWire(x, y, i, active);
      }
   }

   private boolean isTileActive(int x, int y, int wireID) {
      return this.level.getLevelObject(x, y).isWireActive(wireID) || this.level.logicLayer.isWireActive(x, y, wireID);
   }

   public void forceUpdateWireByRegion(Region region, int regionTileX, int regionTileY) {
      int tileX = region.tileXOffset + regionTileX;
      int tileY = region.tileYOffset + regionTileY;
      byte wireData = region.wireLayer.getWireDataByRegion(regionTileX, regionTileY);

      for (int wireID = 0; wireID < 4; wireID++) {
         boolean hasWire = GameMath.getBit(wireData, wireID * 2);
         if (hasWire) {
            boolean isActive = GameMath.getBit(wireData, wireID * 2 + 1);
            this.level.wireManager.updateWire(tileX, tileY, wireID, isActive);
         }
      }
   }

   public boolean updateWire(int x, int y, int wireID, boolean active) {
      if (!this.hasWire(x, y, wireID)) {
         return false;
      }

      if (!active && this.isTileActive(x, y, wireID)) {
         return false;
      }

      boolean lastIsActive = this.isWireActive(x, y, wireID);
      ArrayList<Point> openWire = new ArrayList<>();
      ArrayList<Point> closedWire = new ArrayList<>();
      openWire.add(new Point(x, y));

      while (openWire.size() != 0) {
         Point current = openWire.remove(0);
         closedWire.add(current);

         for (Point connection : connections) {
            Point next = new Point(current.x + connection.x, current.y + connection.y);
            if (this.hasWire(next.x, next.y, wireID) && !closedWire.contains(next) && !openWire.contains(next)) {
               openWire.add(next);
               if (!active && this.isTileActive(next.x, next.y, wireID)) {
                  return false;
               }
            }
         }
      }

      closedWire.forEach(p -> {
         if (this.isWireActive(p.x, p.y, wireID) != active) {
            this.setWireActive(p.x, p.y, wireID, active);
            this.level.regionManager.onWireUpdate(p.x, p.y, wireID, active);
         }
      });
      return lastIsActive != active;
   }

   public boolean hasWire(int x, int y, int wireID) {
      return GameMath.getBit(this.getWireData(x, y), wireID * 2);
   }

   public boolean isWireActiveAny(int x, int y) {
      for (int i = 0; i < 4; i++) {
         if (this.isWireActive(x, y, i)) {
            return true;
         }
      }

      return false;
   }

   public boolean isWireActive(int x, int y, int wireID) {
      return GameMath.getBit(this.getWireData(x, y), wireID * 2 + 1);
   }

   private boolean hasSameWire(byte firstWireData, byte secondWireData, int wireID) {
      return GameMath.getBit(firstWireData, wireID * 2) == GameMath.getBit(secondWireData, wireID * 2);
   }

   public void setWireActive(int x, int y, int wireID, boolean active) {
      if (!this.hasWire(x, y, wireID)) {
         active = false;
      }

      byte wireData = this.getWireData(x, y);
      this.level.regionManager.setWireData(x, y, GameMath.setBit(wireData, wireID * 2 + 1, active));
   }

   public void setWire(int x, int y, int wireID, boolean isThere) {
      byte wireData = this.getWireData(x, y);
      this.setWireData(x, y, GameMath.setBit(wireData, wireID * 2, isThere), false);
      this.setWireActive(x, y, wireID, false);
      if (!isThere) {
         for (Point connection : connections) {
            this.updateWire(x + connection.x, y + connection.y, wireID, false);
         }
      } else {
         if (this.isAnyNonWireLayerActive(x, y, wireID)) {
            this.updateWire(x, y, wireID, true);
            return;
         }

         for (Point connection : connections) {
            Point tile = new Point(x + connection.x, y + connection.y);
            if (this.hasWire(tile.x, tile.y, wireID) && this.isWireActive(tile.x, tile.y, wireID)) {
               this.updateWire(x, y, wireID, true);
               return;
            }
         }
      }
   }

   protected boolean isAnyNonWireLayerActive(int x, int y, int wireID) {
      return this.level.getLevelObject(x, y).isWireActive(wireID) || this.level.logicLayer.isWireActive(x, y, wireID);
   }

   public byte getWireData(int tileX, int tileY) {
      return this.level.regionManager.getWireData(tileX, tileY);
   }

   public void setWireData(int tileX, int tileY, byte wireData, boolean update) {
      if (this.level.isTileWithinBounds(tileX, tileY)) {
         byte lastData = this.getWireData(tileX, tileY);

         for (int wireID = 0; wireID < 4; wireID++) {
            boolean lastHasWire = GameMath.getBit(lastData, wireID * 2);
            boolean newHasWire = GameMath.getBit(wireData, wireID * 2);
            if (lastHasWire != newHasWire) {
               if (update) {
                  this.setWire(tileX, tileY, wireID, newHasWire);
               } else {
                  this.level.regionManager.setWireData(tileX, tileY, GameMath.setBit(lastData, wireID * 2, newHasWire));
               }
            }
         }
      }
   }
}
