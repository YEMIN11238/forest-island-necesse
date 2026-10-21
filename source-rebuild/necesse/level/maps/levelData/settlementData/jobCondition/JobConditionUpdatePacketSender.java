package necesse.level.maps.levelData.settlementData.jobCondition;

import necesse.engine.network.Packet;

@FunctionalInterface
public interface JobConditionUpdatePacketSender {
   void sendUpdatePacket(int var1, Packet var2);
}
