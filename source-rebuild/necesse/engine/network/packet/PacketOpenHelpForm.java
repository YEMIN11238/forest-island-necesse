package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.presets.HelpForms;

public class PacketOpenHelpForm extends Packet {
   public final String helpFormKey;

   public PacketOpenHelpForm(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.helpFormKey = reader.getNextString();
   }

   public PacketOpenHelpForm(String helpFormKey) {
      this.helpFormKey = helpFormKey;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextString(helpFormKey);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      HelpForms.openHelpForm(this.helpFormKey);
   }
}
