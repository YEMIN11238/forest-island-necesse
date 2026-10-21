package necesse.level.maps.regionSystem.layers.objectLayer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import necesse.engine.GameLog;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.VersionMigration;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.level.maps.regionSystem.layers.ObjectLayerAbstract;
import necesse.level.maps.regionSystem.layers.ObjectRegionLayer;

public class ArrayObjectLayer extends ObjectLayerAbstract {
   protected short[] objects;
   protected byte[] rotations;
   protected boolean[] isPlayerPlaced;

   public ArrayObjectLayer(ObjectRegionLayer layer) {
      super(layer);
      this.objects = new short[layer.region.tileWidth * layer.region.tileHeight];
      this.rotations = new byte[layer.region.tileWidth * layer.region.tileHeight];
      this.isPlayerPlaced = new boolean[layer.region.tileWidth * layer.region.tileHeight];
   }

   protected int getDataIndex(int regionTileX, int regionTileY) {
      return regionTileX + regionTileY * this.layer.region.tileWidth;
   }

   @Override
   public short getObjectID(int regionTileX, int regionTileY) {
      return this.objects[this.getDataIndex(regionTileX, regionTileY)];
   }

   @Override
   public void setObjectID(int regionTileX, int regionTileY, short objectID) {
      this.objects[this.getDataIndex(regionTileX, regionTileY)] = objectID;
   }

   @Override
   public byte getObjectRotation(int regionTileX, int regionTileY) {
      return this.rotations[this.getDataIndex(regionTileX, regionTileY)];
   }

   @Override
   public void setObjectRotation(int regionTileX, int regionTileY, byte rotation) {
      this.rotations[this.getDataIndex(regionTileX, regionTileY)] = rotation;
   }

   @Override
   public boolean isPlayerPlaced(int regionTileX, int regionTileY) {
      return this.isPlayerPlaced[this.getDataIndex(regionTileX, regionTileY)];
   }

   @Override
   public void setIsPlayerPlaced(int regionTileX, int regionTileY, boolean isPlayerPlaced) {
      this.isPlayerPlaced[this.getDataIndex(regionTileX, regionTileY)] = isPlayerPlaced;
   }

   @Override
   public void clearLayer() {
      Arrays.fill(this.objects, (short)0);
      Arrays.fill(this.rotations, (byte)0);
      Arrays.fill(this.isPlayerPlaced, false);
   }

   @Override
   protected void addUsedObjectIDs(HashSet<Integer> set) {
      for (short objectID : this.objects) {
         set.add(objectID & 65535);
      }
   }

   @Override
   public void addSaveData(SaveData save) {
      try {
         save.addCompressedShortArray("objects", this.objects);
      } catch (IOException e) {
         save.addShortArray("objects", this.objects);
      }

      try {
         save.addCompressedByteArray("objectRotations", this.rotations);
      } catch (IOException e) {
         save.addByteArray("objectRotations", this.rotations);
      }

      try {
         save.addCompressedBooleanArray("objectIsPlayerPlaced", this.isPlayerPlaced);
      } catch (IOException e) {
         save.addSmallBooleanArray("objectIsPlayerPlaced", this.isPlayerPlaced);
      }
   }

   @Override
   public boolean applyLoadData(LoadData save, int[] conversionArray) {
      LoadData objectsSave = save.getFirstLoadDataByName("objects");
      if (objectsSave != null) {
         try {
            short[] newObjects;
            try {
               newObjects = LoadData.getCompressedShortArray(objectsSave);
            } catch (Exception e) {
               newObjects = LoadData.getShortArray(objectsSave);
            }

            if (newObjects.length != this.objects.length) {
               this.objects = Arrays.copyOf(newObjects, this.objects.length);
            } else {
               this.objects = newObjects;
            }
         } catch (Exception e) {
            if (this.getID() == 0) {
               throw new RuntimeException("Failed to load level objects", e);
            }

            System.err.println("Failed to load level objects layer: \"" + this.getStringID() + "\"");
            return false;
         }
      } else {
         if (this.getID() == 0) {
            throw new RuntimeException("Could not find level object data");
         }

         System.err.println("Could not find level object layer data: \"" + this.getStringID() + "\"");
      }

      LoadData rotationsSave = save.getFirstLoadDataByName("objectRotations");
      if (rotationsSave != null) {
         try {
            byte[] newRotations;
            try {
               newRotations = LoadData.getCompressedByteArray(rotationsSave);
            } catch (Exception e) {
               newRotations = LoadData.getByteArray(rotationsSave);
            }

            if (newRotations.length != this.rotations.length) {
               this.rotations = Arrays.copyOf(newRotations, this.rotations.length);
            } else {
               this.rotations = newRotations;
            }
         } catch (Exception e) {
            this.rotations = new byte[this.layer.region.tileWidth * this.layer.region.tileHeight];
            System.err.println("Failed to load level object rotations data resulting in a wipe");
            e.printStackTrace();
         }
      } else {
         GameLog.warn.println("Could not find level object rotations data");
      }

      boolean migrated = false;
      if (conversionArray != null) {
         int[] intData = new int[this.objects.length];

         for (int i = 0; i < intData.length; i++) {
            intData[i] = this.objects[i];
         }

         if (VersionMigration.convertArray(intData, conversionArray)) {
            migrated = true;

            for (int i = 0; i < this.objects.length; i++) {
               this.objects[i] = (short)intData[i];
            }
         }
      }

      boolean[] newIsPlayerPlaced;
      try {
         if (save.isSmallBooleanArray("objectIsPlayerPlaced")) {
            throw new Exception("Handle small boolean array");
         }

         newIsPlayerPlaced = save.getCompressedBooleanArray("objectIsPlayerPlaced");
      } catch (Exception e) {
         newIsPlayerPlaced = save.getSmallBooleanArray("objectIsPlayerPlaced", this.isPlayerPlaced, false);
      }

      if (newIsPlayerPlaced.length != this.isPlayerPlaced.length) {
         this.isPlayerPlaced = Arrays.copyOf(newIsPlayerPlaced, this.isPlayerPlaced.length);
      } else {
         this.isPlayerPlaced = newIsPlayerPlaced;
      }

      return migrated;
   }

   @Override
   public void writeLayerPacket(PacketWriter writer) {
      for (int i = 0; i < this.objects.length; i++) {
         short objectID = this.objects[i];
         if (objectID != 0) {
            writer.putNextBoolean(true);
            writer.putNextShort(objectID);
            writer.putNextByte(this.rotations[i]);
            writer.putNextBoolean(this.isPlayerPlaced[i]);
         } else {
            writer.putNextBoolean(false);
         }
      }
   }

   @Override
   public boolean readLayerPacket(PacketReader reader) {
      for (int i = 0; i < this.objects.length; i++) {
         if (reader.getNextBoolean()) {
            short objectID = reader.getNextShort();
            if (objectID < 0) {
               return false;
            }

            this.objects[i] = objectID;
            this.rotations[i] = reader.getNextByte();
            this.isPlayerPlaced[i] = reader.getNextBoolean();
         } else {
            this.objects[i] = 0;
            this.rotations[i] = 0;
            this.isPlayerPlaced[i] = false;
         }
      }

      return true;
   }
}
