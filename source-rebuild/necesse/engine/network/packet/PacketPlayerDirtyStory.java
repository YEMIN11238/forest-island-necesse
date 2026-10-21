package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.storyObjectives.StoryObjectiveManager;

public class PacketPlayerDirtyStory extends Packet {
   public PacketPlayerDirtyStory(byte[] data) {
      super(data);
   }

   public PacketPlayerDirtyStory(StoryObjectiveManager manager) {
      PacketWriter writer = new PacketWriter(this);
      manager.writeDirtyPacket(writer);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.storyManager != null) {
         client.storyManager.readDirtyPacket(new PacketReader(this));
      }
   }
}
