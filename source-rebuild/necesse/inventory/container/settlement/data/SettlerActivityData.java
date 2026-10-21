package necesse.inventory.container.settlement.data;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class SettlerActivityData {
   public GameMessage currentActivity;
   public boolean hasCommands;

   public SettlerActivityData(GameMessage currentActivity, boolean hasCommands) {
      this.currentActivity = currentActivity;
      this.hasCommands = hasCommands;
   }

   public SettlerActivityData(PacketReader reader) {
      if (reader.getNextBoolean()) {
         this.currentActivity = GameMessage.fromPacket(reader);
      } else {
         this.currentActivity = null;
      }

      this.hasCommands = reader.getNextBoolean();
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextBoolean(this.currentActivity != null);
      if (this.currentActivity != null) {
         this.currentActivity.writePacket(writer);
      }

      writer.putNextBoolean(this.hasCommands);
   }
}
