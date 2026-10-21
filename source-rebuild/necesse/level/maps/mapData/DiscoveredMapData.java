package necesse.level.maps.mapData;

import java.io.IOException;
import java.util.zip.DataFormatException;
import necesse.engine.GameLog;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;

public class DiscoveredMapData {
   private int tileWidth;
   private int tileHeight;
   private int[] colors;
   private String base64Colors;
   private boolean changedSinceLastSave;

   public DiscoveredMapData(int tileWidth, int tileHeight) {
      this.tileWidth = tileWidth;
      this.tileHeight = tileHeight;
   }

   public int getIndex(int mapRegionTileX, int mapRegionTileY, int tileWidth) {
      return mapRegionTileX + mapRegionTileY * tileWidth;
   }

   public int getIndex(int mapRegionTileX, int mapRegionTileY) {
      return this.getIndex(mapRegionTileX, mapRegionTileY, this.tileWidth);
   }

   public int getTileWidth() {
      return this.tileWidth;
   }

   public int getTileHeight() {
      return this.tileHeight;
   }

   public void makeFinal() {
      if (this.colors != null) {
         try {
            this.base64Colors = toBase64(this.colors);
            this.colors = null;
         } catch (IOException e) {
            e.printStackTrace(GameLog.err);
         }
      }
   }

   public boolean isFinal() {
      return this.colors == null;
   }

   public boolean ensureNotFinal() {
      if (this.colors != null) {
         return false;
      }

      if (this.base64Colors != null) {
         try {
            this.colors = toColors(this.base64Colors, this.tileWidth, this.tileHeight);
         } catch (DataFormatException | IOException e) {
            e.printStackTrace(GameLog.err);
         }
      }

      if (this.colors == null) {
         this.colors = new int[this.tileWidth * this.tileHeight];
      }

      this.base64Colors = null;
      return true;
   }

   public void saveToFileSystem(MapRegionFileGetter fileGetter, LevelIdentifier identifier, int mapRegionX, int mapRegionY, boolean onlyIfChanged) throws IOException {
      if (!onlyIfChanged || this.changedSinceLastSave) {
         SaveData saveData = new SaveData(mapRegionX + "x" + mapRegionY);
         saveData.addInt("width", this.tileWidth);
         saveData.addInt("height", this.tileHeight);
         if (this.base64Colors != null) {
            saveData.addSafeString("colors", this.base64Colors);
         } else {
            saveData.addSafeString("colors", toBase64(this.colors));
         }

         saveData.saveScript(fileGetter.getFile(identifier, mapRegionX, mapRegionY));
      }

      this.changedSinceLastSave = false;
   }

   public void applySaveData(LoadData save, boolean allowSizeChange) throws DataFormatException, IOException {
      if (save.isArray()) {
         int nextTileWidth = save.getInt("width", -1, false);
         int nextTileHeight = save.getInt("height", -1, false);
         if (allowSizeChange || nextTileWidth == this.tileWidth && nextTileHeight == this.tileHeight) {
            this.tileWidth = nextTileWidth;
            this.tileHeight = nextTileHeight;
            if (this.tileWidth >= 0 && this.tileHeight >= 0) {
               String colorsString = save.getSafeString("colors", null, false);
               if (colorsString == null) {
                  throw new LoadDataException("Missing map region colors");
               }

               if (this.colors == null) {
                  this.base64Colors = colorsString;
               } else {
                  this.colors = toColors(colorsString, this.tileWidth, this.tileHeight);
               }
            } else {
               throw new LoadDataException("Invalid map region size: " + this.tileWidth + "x" + this.tileHeight);
            }
         } else {
            throw new LoadDataException(
               "Map region size changed while not being allowed to: " + this.tileWidth + "x" + this.tileHeight + " -> " + nextTileWidth + "x" + nextTileHeight
            );
         }
      }
   }

   public void writePacketData(PacketWriter writer) throws DataFormatException {
      byte[] compressedData;
      if (this.colors != null) {
         compressedData = toCompressedData(this.colors);
      } else {
         compressedData = toCompressedData(this.base64Colors);
      }

      writer.putNextShortUnsigned(this.tileWidth);
      writer.putNextShortUnsigned(this.tileHeight);
      writer.putNextInt(compressedData.length);
      writer.putNextBytes(compressedData);
   }

   public boolean applyPacketData(PacketReader reader) throws DataFormatException, IOException {
      int tileWidth = reader.getNextShortUnsigned();
      int tileHeight = reader.getNextShortUnsigned();
      int compressedDataLength = reader.getNextInt();
      byte[] compressedData = reader.getNextBytes(compressedDataLength);
      int[] colors = toColors(compressedData, tileWidth, tileHeight);
      return this.combine(colors, tileWidth, tileHeight);
   }

   public static String toBase64(int[] colors) throws IOException {
      return GameUtils.toBase64(toCompressedData(colors));
   }

   public static byte[] toCompressedData(int[] colors) {
      byte[] data = new byte[colors.length * 3];

      for (int i = 0; i < colors.length; i++) {
         int rgb = colors[i];
         data[i * 3] = (byte)(rgb >> 16 & 0xFF);
         data[i * 3 + 1] = (byte)(rgb >> 8 & 0xFF);
         data[i * 3 + 2] = (byte)(rgb & 0xFF);
      }

      return GameUtils.compressData(data, true);
   }

   public static byte[] toCompressedData(String base64String) throws DataFormatException {
      return GameUtils.fromBase64(base64String);
   }

   public static int[] toColors(String base64String, int expectedWidth, int expectedHeight) throws DataFormatException, IOException {
      return toColors(toCompressedData(base64String), expectedWidth, expectedHeight);
   }

   public static int[] toColors(byte[] compressedData, int expectedWidth, int expectedHeight) throws DataFormatException, IOException {
      byte[] rgbColors = GameUtils.decompressData(compressedData);
      int[] colors = new int[expectedWidth * expectedHeight];
      if (rgbColors.length != colors.length * 3) {
         throw new DataFormatException("Invalid map region data length: " + rgbColors.length + " != " + colors.length * 3);
      }

      for (int i = 0; i < colors.length; i++) {
         int r = rgbColors[i * 3] & 255;
         int g = rgbColors[i * 3 + 1] & 255;
         int b = rgbColors[i * 3 + 2] & 255;
         colors[i] = r << 16 | g << 8 | b;
      }

      return colors;
   }

   public boolean setRGBA(int mapRegionTileX, int mapRegionTileY, int argb) {
      this.ensureNotFinal();
      int index = this.getIndex(mapRegionTileX, mapRegionTileY);
      int lastColor = this.colors[index];
      if (lastColor == argb) {
         return false;
      }

      this.colors[index] = argb;
      this.changedSinceLastSave = true;
      return true;
   }

   public int getRGB(int mapRegionTileX, int mapRegionTileY) {
      this.ensureNotFinal();
      return this.colors[this.getIndex(mapRegionTileX, mapRegionTileY)];
   }

   public boolean combine(int[] colors, int tileWidth, int tileHeight) {
      if (colors.length != tileWidth * tileHeight) {
         System.err.println("Colors size did not match: " + tileWidth + "x" + tileHeight + ", " + colors.length);
         return false;
      }

      if (tileWidth <= 256 && tileHeight <= 256) {
         this.ensureNotFinal();
         if (tileWidth > this.tileWidth || tileHeight > this.tileHeight) {
            int oldTileWidth = this.tileWidth;
            int oldTileHeight = this.tileHeight;
            int[] oldColors = this.colors;
            this.tileWidth = Math.max(this.tileWidth, tileWidth);
            this.tileHeight = Math.max(this.tileHeight, tileHeight);
            this.colors = new int[this.tileWidth * this.tileHeight];

            for (int x = 0; x < oldTileWidth; x++) {
               for (int y = 0; y < oldTileHeight; y++) {
                  this.colors[x + y * this.tileWidth] = oldColors[x + y * oldTileWidth];
               }
            }
         }

         boolean causedChange = false;
         if (tileWidth == this.tileWidth && tileHeight == this.tileHeight) {
            for (int i = 0; i < this.colors.length; i++) {
               int tileColor = this.colors[i];
               if (tileColor == 0) {
                  int otherColor = colors[i];
                  if (otherColor != 0) {
                     this.colors[i] = otherColor;
                     causedChange = true;
                  }
               }
            }
         } else {
            for (int x = 0; x < tileWidth; x++) {
               for (int y = 0; y < tileHeight; y++) {
                  int tileColor = this.colors[x + y * this.tileWidth];
                  if (tileColor == 0) {
                     int otherColor = colors[x + y * tileWidth];
                     if (otherColor != 0) {
                        this.colors[x + y * this.tileWidth] = otherColor;
                        causedChange = true;
                     }
                  }
               }
            }
         }

         if (causedChange) {
            this.changedSinceLastSave = true;
         }

         return causedChange;
      } else {
         System.err.println("Map region size is too big: " + tileWidth + "x" + tileHeight);
         return false;
      }
   }

   public boolean combine(DiscoveredMapData other) {
      return this.combine(other.colors, other.tileWidth, other.tileHeight);
   }
}
