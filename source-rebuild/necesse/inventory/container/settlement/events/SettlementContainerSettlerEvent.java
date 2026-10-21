package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.LevelSettler;

public abstract class SettlementContainerSettlerEvent extends SettlementContainerEvent {
   public final int mobUniqueID;

   public SettlementContainerSettlerEvent(LevelSettler levelSettler) {
      super(levelSettler.data);
      this.mobUniqueID = levelSettler.mobUniqueID;
   }

   public SettlementContainerSettlerEvent(PacketReader reader) {
      super(reader);
      this.mobUniqueID = reader.getNextInt();
   }

   @Override
   public final void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.mobUniqueID);
      this.settlerWrite(writer);
   }

   public abstract void settlerWrite(PacketWriter var1);
}
