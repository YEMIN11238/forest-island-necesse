package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class SettlerCommandData {
   public boolean hideOnLowHealth;

   public SettlerCommandData(CommandMob commandMob) {
      this.hideOnLowHealth = commandMob == null || commandMob.getHideOnLowHealth();
   }

   public SettlerCommandData(PacketReader reader) {
      this.hideOnLowHealth = reader.getNextBoolean();
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextBoolean(this.hideOnLowHealth);
   }
}
