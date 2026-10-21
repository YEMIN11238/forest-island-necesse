package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.LevelSettler;

public class SettlerOverviewData {
   public final boolean canMoveOut;
   public final boolean canBanish;

   public SettlerOverviewData(LevelSettler settler) {
      this.canMoveOut = settler.canMoveOut();
      this.canBanish = settler.canBanish();
   }

   public SettlerOverviewData(PacketReader reader) {
      this.canMoveOut = reader.getNextBoolean();
      this.canBanish = reader.getNextBoolean();
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextBoolean(this.canMoveOut);
      writer.putNextBoolean(this.canBanish);
   }
}
