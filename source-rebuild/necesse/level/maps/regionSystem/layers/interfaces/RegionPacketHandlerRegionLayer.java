package necesse.level.maps.regionSystem.layers.interfaces;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public interface RegionPacketHandlerRegionLayer {
   void writeLayerPacket(PacketWriter var1);

   boolean applyLayerPacket(PacketReader var1);
}
