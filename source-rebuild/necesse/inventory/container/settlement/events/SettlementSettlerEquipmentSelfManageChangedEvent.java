package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerEquipmentSelfManageChangedEvent extends SettlementContainerSettlerEvent {
   public final boolean selfManageEquipment;

   public SettlementSettlerEquipmentSelfManageChangedEvent(LevelSettler levelSettler) {
      super(levelSettler);
      SettlerMob mob = levelSettler.getMob();
      this.selfManageEquipment = mob != null && mob.managesEquipmentThemself();
   }

   public SettlementSettlerEquipmentSelfManageChangedEvent(PacketReader reader) {
      super(reader);
      this.selfManageEquipment = reader.getNextBoolean();
   }

   @Override
   public void settlerWrite(PacketWriter writer) {
      writer.putNextBoolean(this.selfManageEquipment);
   }
}
