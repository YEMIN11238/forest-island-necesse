package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.gfx.HumanLook;

public class SettlerLooksData {
   public int settlerSeed;
   public HumanLook look;
   public boolean customLook;
   public String settlerName;

   public SettlerLooksData(int settlerSeed, HumanLook look, boolean customLook, String settlerName) {
      this.settlerSeed = settlerSeed;
      this.look = new HumanLook(look);
      this.customLook = customLook;
      this.settlerName = settlerName;
   }

   public SettlerLooksData(PacketReader reader) {
      this.settlerSeed = reader.getNextInt();
      this.look = new HumanLook(reader);
      this.customLook = reader.getNextBoolean();
      this.settlerName = reader.getNextString();
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextInt(this.settlerSeed);
      this.look.setupContentPacket(writer, true);
      writer.putNextBoolean(this.customLook);
      writer.putNextString(this.settlerName);
   }
}
