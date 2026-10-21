package necesse.level.maps.mapData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;
import necesse.engine.GameLog;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;

public class BasicDiscoveredMap {
   private final HashMap<Long, DiscoveredMapData> regions = new HashMap<>();
   private boolean changedSinceLastSave = false;

   protected DiscoveredMapData getRegion(int mapRegionX, int mapRegionY, Supplier<DiscoveredMapData> supplyIfDoesntExist) {
      long key = GameMath.getUniqueLongKey(mapRegionX, mapRegionY);
      DiscoveredMapData region = this.regions.get(key);
      if (region == null && supplyIfDoesntExist != null) {
         region = supplyIfDoesntExist.get();
         if (region != null) {
            this.regions.put(key, region);
         }
      }

      return region;
   }

   public void applySaveData(int mapRegionX, int mapRegionY, LoadData save) {
      DiscoveredMapData region = this.getRegion(mapRegionX, mapRegionY, () -> new DiscoveredMapData(0, 0));

      try {
         region.applySaveData(save, true);
      } catch (Exception e) {
         e.printStackTrace(GameLog.err);
         this.regions.remove(GameMath.getUniqueLongKey(mapRegionX, mapRegionY));
      }
   }

   private void writeRegionData(PacketWriter writer, long key, DiscoveredMapData data) throws DataFormatException {
      int mapRegionX = GameMath.getXFromUniqueLongKey(key);
      int mapRegionY = GameMath.getYFromUniqueLongKey(key);
      writer.putNextInt(mapRegionX);
      writer.putNextInt(mapRegionY);
      data.writePacketData(writer);
   }

   public void addRegionWritePacketDataTasks(Consumer<PacketWriter> packetHeaderAdder, Consumer<Packet> packetSender, ArrayDeque<Runnable> tasks) {
      this.regions
         .forEach(
            (key, data) -> tasks.add(
               () -> {
                  PacketWriter writer = new PacketWriter(new Packet());
                  packetHeaderAdder.accept(writer);
                  writer.putNextShortUnsigned(1);

                  try {
                     PacketWriter regionWriter = new PacketWriter(new Packet());
                     this.writeRegionData(regionWriter, key, data);
                     writer.putNextContentPacket(regionWriter.getPacket());
                     packetSender.accept(writer.getPacket());
                  } catch (DataFormatException e) {
                     GameLog.err
                        .println(
                           "Could not write discovered map data for region " + GameMath.getXFromUniqueLongKey(key) + "x" + GameMath.getYFromUniqueLongKey(key)
                        );
                     e.printStackTrace(GameLog.err);
                  }
               }
            )
         );
   }

   public void writePacketData(PacketWriter writer) throws DataFormatException {
      PacketWriter[] regionPacketWriters = new PacketWriter[this.regions.size()];

      for (int i = 0; i < this.regions.size(); i++) {
         regionPacketWriters[i] = new PacketWriter(new Packet());
      }

      AtomicInteger index = new AtomicInteger(0);
      AtomicReference<DataFormatException> exception = new AtomicReference<>();
      this.regions.entrySet().parallelStream().forEach(entry -> {
         PacketWriter regionWriterx = regionPacketWriters[index.getAndIncrement()];

         try {
            this.writeRegionData(regionWriterx, entry.getKey(), entry.getValue());
         } catch (DataFormatException e) {
            exception.set(e);
         }
      });
      if (exception.get() != null) {
         throw (DataFormatException)exception.get();
      }

      writer.putNextShortUnsigned(this.regions.size());

      for (PacketWriter regionWriter : regionPacketWriters) {
         writer.putNextContentPacket(regionWriter.getPacket());
      }
   }

   public boolean applyPacketData(PacketReader reader, LevelIdentifier identifier, MapRegionFileGetter fileGetterIfDoesntExist, boolean makeFinal) throws DataFormatException, IOException {
      boolean causedChange = false;
      int size = reader.getNextShortUnsigned();

      for (int i = 0; i < size; i++) {
         PacketReader regionReader = new PacketReader(reader.getNextContentPacket());
         int mapRegionX = regionReader.getNextInt();
         int mapRegionY = regionReader.getNextInt();
         DiscoveredMapData region = this.getRegion(
            mapRegionX, mapRegionY, () -> this.loadRegionFromFile(identifier, mapRegionX, mapRegionY, fileGetterIfDoesntExist, true)
         );
         causedChange = region.applyPacketData(regionReader) || causedChange;
         if (makeFinal) {
            region.makeFinal();
         }
      }

      if (causedChange) {
         this.changedSinceLastSave = true;
      }

      return causedChange;
   }

   public boolean isEmpty() {
      return this.regions.isEmpty();
   }

   public boolean combine(BasicDiscoveredMap other, LevelIdentifier identifier, MapRegionFileGetter fileGetterIfDoesntExist) {
      boolean causedChange = false;

      for (Entry<Long, DiscoveredMapData> entry : other.regions.entrySet()) {
         long key = entry.getKey();
         int mapRegionX = GameMath.getXFromUniqueLongKey(key);
         int mapRegionY = GameMath.getYFromUniqueLongKey(key);
         DiscoveredMapData region = this.getRegion(
            mapRegionX, mapRegionY, () -> this.loadRegionFromFile(identifier, mapRegionX, mapRegionY, fileGetterIfDoesntExist, true)
         );
         causedChange = region.combine(entry.getValue()) || causedChange;
      }

      if (causedChange) {
         this.changedSinceLastSave = true;
      }

      return causedChange;
   }

   public void makeFinal() {
      for (DiscoveredMapData region : this.regions.values()) {
         region.makeFinal();
      }
   }

   public void saveToFileSystem(LevelIdentifier identifier, MapRegionFileGetter mapRegionFileGetter, boolean onlyIfChanged) {
      if (!onlyIfChanged || this.changedSinceLastSave) {
         for (Entry<Long, DiscoveredMapData> entry : this.regions.entrySet()) {
            long key = entry.getKey();
            int mapRegionX = GameMath.getXFromUniqueLongKey(key);
            int mapRegionY = GameMath.getYFromUniqueLongKey(key);
            DiscoveredMapData region = entry.getValue();

            try {
               region.saveToFileSystem(mapRegionFileGetter, identifier, mapRegionX, mapRegionY, onlyIfChanged);
            } catch (IOException e) {
               GameLog.err.println("Could not save discovered map data for region " + mapRegionX + "x" + mapRegionY);
               e.printStackTrace(GameLog.err);
            }
         }
      }

      this.changedSinceLastSave = false;
   }

   public DiscoveredMapData loadRegionFromFile(
      LevelIdentifier identifier, int regionX, int regionY, MapRegionFileGetter mapRegionFileGetter, boolean createIfDoesntExist
   ) {
      if (mapRegionFileGetter != null) {
         File mapRegionFile = mapRegionFileGetter.getFile(identifier, regionX, regionY);
         if (mapRegionFile.exists()) {
            try {
               DiscoveredMapData mapData = new DiscoveredMapData(0, 0);
               LoadData loadData = new LoadData(mapRegionFile);
               mapData.applySaveData(loadData, true);
               return mapData;
            } catch (IOException | DataFormatException e) {
               GameLog.err.println("Could not load discovered map data for region " + regionX + "x" + regionY);
               e.printStackTrace(GameLog.err);
            }
         }
      }

      return createIfDoesntExist ? new DiscoveredMapData(0, 0) : null;
   }
}
