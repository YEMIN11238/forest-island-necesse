package necesse.engine.network.packet;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.LevelIdentifier;
import necesse.level.maps.Level;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUndoData;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionPosition;

public class PacketUndoClientPreset extends Packet {
   public final int undoUniqueID;
   public final int levelIdentifierHashCode;
   public final Rectangle presetBounds;

   public PacketUndoClientPreset(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.undoUniqueID = reader.getNextInt();
      if (reader.getNextBoolean()) {
         this.levelIdentifierHashCode = reader.getNextInt();
         int x = reader.getNextInt();
         int y = reader.getNextInt();
         int width = reader.getNextShortUnsigned();
         int height = reader.getNextShortUnsigned();
         this.presetBounds = new Rectangle(x, y, width, height);
      } else {
         this.levelIdentifierHashCode = -1;
         this.presetBounds = null;
      }
   }

   public PacketUndoClientPreset(int undoUniqueID) {
      this(undoUniqueID, null, null, 0, 0);
   }

   public PacketUndoClientPreset(int undoUniqueID, LevelIdentifier levelIdentifier, Preset preset, int tileX, int tileY) {
      this.undoUniqueID = undoUniqueID;
      this.levelIdentifierHashCode = levelIdentifier == null ? -1 : levelIdentifier.hashCode();
      this.presetBounds = levelIdentifier == null ? null : new Rectangle(tileX, tileY, preset.width, preset.height);
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(undoUniqueID);
      if (this.levelIdentifierHashCode != -1) {
         writer.putNextBoolean(true);
         writer.putNextInt(this.levelIdentifierHashCode);
         writer.putNextInt(this.presetBounds.x);
         writer.putNextInt(this.presetBounds.y);
         writer.putNextShortUnsigned(this.presetBounds.width);
         writer.putNextShortUnsigned(this.presetBounds.height);
      } else {
         writer.putNextBoolean(false);
      }
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      client.presetRedoUniqueIDs.add(this.undoUniqueID);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (server.world.settings.creativeMode
         || client.getPermissionLevel().getLevel() > PermissionLevel.ADMIN.getLevel() && server.world.settings.cheatsAllowedOrHidden()) {
         ListIterator<PresetUndoData> iterator = client.presetUndoData.listIterator();

         while (iterator.hasNext()) {
            PresetUndoData undoData = iterator.next();
            if (undoData.uniqueID == this.undoUniqueID) {
               iterator.remove();
               Level level = server.world.getLevel(undoData.levelIdentifier);
               if (level != null) {
                  undoData.applyServer(level, client.presetRedoData);
                  LinkedList<RegionPosition> regionPositions = PresetUtils.getRegionPositions(level, undoData.serverPreset, undoData.tileX, undoData.tileY);

                  try {
                     PacketPlacePreset placePacket = new PacketPlacePreset(level, this.undoUniqueID, undoData.tileX, undoData.tileY, undoData.serverPreset);
                     if (this.presetBounds == null) {
                        server.network.sendToClientsWithAnyRegion(placePacket, regionPositions);
                        client.sendPacket(new PacketUndoClientPreset(this.undoUniqueID));
                     } else {
                        server.network.sendToClientsWithAnyRegionExcept(placePacket, regionPositions, client);
                     }
                  } catch (IOException e) {
                     e.printStackTrace();
                  }
               }

               return;
            }
         }

         if (this.presetBounds != null) {
            Level level = client.getLevel();
            if (level.getIdentifierHashCode() == this.levelIdentifierHashCode) {
               for (RegionPosition regionPosition : PresetUtils.getRegionPositions(level, this.presetBounds)) {
                  Region region = level.regionManager.getRegion(regionPosition.regionX, regionPosition.regionY, false);
                  client.sendPacket(new PacketRegionData(region));
               }
            }
         }
      }
   }
}
