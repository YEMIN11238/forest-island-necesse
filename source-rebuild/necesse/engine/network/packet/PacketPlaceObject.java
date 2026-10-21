package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class PacketPlaceObject extends Packet {
   public final int levelIdentifierHashCode;
   public final int slot;
   public final byte layerID;
   public final int tileX;
   public final int tileY;
   public final int objectID;
   public final byte objectRotation;
   public final boolean byPlayer;
   public final boolean playPlaceSound;

   public PacketPlaceObject(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.levelIdentifierHashCode = reader.getNextInt();
      this.slot = reader.getNextByteUnsigned();
      this.layerID = reader.getNextByte();
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.objectID = reader.getNextShortUnsigned();
      this.objectRotation = reader.getNextByte();
      this.byPlayer = reader.getNextBoolean();
      this.playPlaceSound = reader.getNextBoolean();
   }

   public PacketPlaceObject(
      Level level, ServerClient client, int layerID, int tileX, int tileY, int objectID, int objectRotation, boolean byPlayer, boolean playPlaceSound
   ) {
      this.levelIdentifierHashCode = level.getIdentifierHashCode();
      this.slot = client == null ? 255 : client.slot;
      this.layerID = (byte)layerID;
      this.tileX = tileX;
      this.tileY = tileY;
      this.objectID = objectID;
      this.objectRotation = (byte)objectRotation;
      this.byPlayer = byPlayer;
      this.playPlaceSound = playPlaceSound;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.levelIdentifierHashCode);
      writer.putNextByteUnsigned(this.slot);
      writer.putNextByte(this.layerID);
      writer.putNextInt(tileX);
      writer.putNextInt(tileY);
      writer.putNextShortUnsigned(objectID);
      writer.putNextByte(this.objectRotation);
      writer.putNextBoolean(byPlayer);
      writer.putNextBoolean(playPlaceSound);
   }

   public GameObject getObject() {
      return ObjectRegistry.getObject(this.objectID);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.levelManager.checkIfLoadedRegionAtTile(this.levelIdentifierHashCode, this.tileX, this.tileY, true)) {
         GameObject obj = this.getObject();
         if (this.slot == client.getSlot() && this.playPlaceSound) {
            obj.playPlaceSound(this.tileX, this.tileY);
         }

         Level level = client.getLevel();
         obj.placeObject(level, this.layerID, this.tileX, this.tileY, this.objectRotation, this.byPlayer);
      }
   }
}
