package necesse.inventory.container.settlement.actions;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.customAction.ContainerCustomAction;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.inventory.container.settlement.events.SettlementWorkZoneRemovedEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZone;

public class RenameWorkZoneCustomAction extends ContainerCustomAction {
   public final SettlementDependantContainer container;

   public RenameWorkZoneCustomAction(SettlementDependantContainer container) {
      this.container = container;
   }

   public void runAndSend(int zoneUniqueID, GameMessage name) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(zoneUniqueID);
      name.writePacket(writer);
      this.runAndSendAction(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      int zoneUniqueID = reader.getNextInt();
      GameMessage name = GameMessage.fromPacket(reader);
      if (this.container.client.isServer()) {
         ServerSettlementData serverData = this.container.getServerData();
         if (serverData != null) {
            SettlementWorkZone zone = serverData.getWorkZones().getZone(zoneUniqueID);
            if (zone == null) {
               new SettlementWorkZoneRemovedEvent(serverData, zoneUniqueID).applyAndSendToClient(this.container.client.getServerClient());
            } else {
               zone.setName(name);
            }
         }
      }
   }
}
