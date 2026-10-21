package necesse.level.maps.mapData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.zip.DataFormatException;
import necesse.engine.GameLog;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.ClientLevelManager;
import necesse.engine.save.LoadData;
import necesse.engine.util.InvalidLevelIdentifierException;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.World;

public class BasicDiscoveredMapManager {
   private final HashMap<LevelIdentifier, BasicDiscoveredMap> maps = new HashMap<>();
   private boolean changedSinceLastSave = false;

   public void loadFromFileSystem(String rootFolderPath, Predicate<String> validLevelIdentifier) {
      File file = new File(rootFolderPath);
      if (file.exists() && file.isDirectory()) {
         File[] files = file.listFiles();
         if (files == null) {
            return;
         }

         for (File levelFile : files) {
            if (levelFile.isDirectory() && (validLevelIdentifier == null || validLevelIdentifier.test(levelFile.getName()))) {
               try {
                  LevelIdentifier identifier = new LevelIdentifier(levelFile.getName());
                  File[] mapRegionFiles = levelFile.listFiles();
                  if (mapRegionFiles != null) {
                     for (File mapRegionFile : mapRegionFiles) {
                        Matcher matcher = ClientLevelManager.mapRegionFilePattern.matcher(mapRegionFile.getName());
                        if (matcher.matches()) {
                           try {
                              int mapRegionX = Integer.parseInt(matcher.group(1));
                              int mapRegionY = Integer.parseInt(matcher.group(2));
                              BasicDiscoveredMap map = this.maps.computeIfAbsent(identifier, levelIdentifier -> new BasicDiscoveredMap());
                              map.applySaveData(mapRegionX, mapRegionY, new LoadData(mapRegionFile));
                              if (map.isEmpty()) {
                                 this.maps.remove(identifier);
                              }
                           } catch (Exception e) {
                              GameLog.warn.println("Found invalid map region file at: " + mapRegionFile);
                           }
                        }
                     }
                  }
               } catch (InvalidLevelIdentifierException var20) {
               }
            }
         }
      }
   }

   public void saveToFileSystem(MapRegionFileGetter mapRegionFileGetter, boolean onlyIfChanged) {
      if (!onlyIfChanged || this.changedSinceLastSave) {
         for (Entry<LevelIdentifier, BasicDiscoveredMap> entry : this.maps.entrySet()) {
            entry.getValue().saveToFileSystem(entry.getKey(), mapRegionFileGetter, onlyIfChanged);
         }
      }

      this.changedSinceLastSave = false;
   }

   public ArrayDeque<Runnable> setupPacketSendTasks(Consumer<PacketWriter> packetHeaderAdder, Consumer<Packet> packetSender) {
      ArrayDeque<Runnable> tasks = new ArrayDeque<>();
      this.maps.forEach((identifier, basicDiscoveredMap) -> {
         Consumer<PacketWriter> myPacketHeaderAdder = writer -> {
            packetHeaderAdder.accept(writer);
            writer.putNextShortUnsigned(1);
            identifier.writePacket(writer);
         };
         basicDiscoveredMap.addRegionWritePacketDataTasks(myPacketHeaderAdder, packetSender, tasks);
      });
      return tasks;
   }

   public void writePacketData(PacketWriter writer) throws DataFormatException {
      writer.putNextShortUnsigned(this.maps.size());

      for (Entry<LevelIdentifier, BasicDiscoveredMap> entry : this.maps.entrySet()) {
         LevelIdentifier identifier = entry.getKey();
         BasicDiscoveredMap map = entry.getValue();
         identifier.writePacket(writer);
         map.writePacketData(writer);
      }
   }

   public boolean readPacketData(PacketReader reader, MapRegionFileGetter fileGetterIfDoesntExist) throws DataFormatException, IOException {
      boolean causedChange = false;
      int size = reader.getNextShortUnsigned();

      for (int i = 0; i < size; i++) {
         LevelIdentifier identifier = new LevelIdentifier(reader);
         BasicDiscoveredMap map = this.maps.computeIfAbsent(identifier, levelIdentifier -> new BasicDiscoveredMap());
         causedChange = map.applyPacketData(reader, identifier, fileGetterIfDoesntExist, true) || causedChange;
      }

      if (causedChange) {
         this.changedSinceLastSave = true;
      }

      return causedChange;
   }

   public boolean combine(BasicDiscoveredMapManager other, MapRegionFileGetter fileGetterIfDoesntExist) {
      boolean causedChange = false;

      for (Entry<LevelIdentifier, BasicDiscoveredMap> entry : other.maps.entrySet()) {
         LevelIdentifier identifier = entry.getKey();
         BasicDiscoveredMap otherMap = entry.getValue();
         BasicDiscoveredMap myMap = this.maps.computeIfAbsent(identifier, levelIdentifier -> new BasicDiscoveredMap());
         causedChange = myMap.combine(otherMap, identifier, fileGetterIfDoesntExist) || causedChange;
      }

      if (causedChange) {
         this.changedSinceLastSave = true;
      }

      return causedChange;
   }

   public void makeFinal() {
      for (BasicDiscoveredMap map : this.maps.values()) {
         map.makeFinal();
      }
   }

   public void clearRemovedLevelIdentifiers(World world) {
      HashSet<LevelIdentifier> removes = new HashSet<>();

      for (LevelIdentifier levelIdentifier : this.maps.keySet()) {
         if (!world.levelExists(levelIdentifier)) {
            removes.add(levelIdentifier);
         }
      }

      for (LevelIdentifier remove : removes) {
         this.maps.remove(remove);
      }
   }
}
