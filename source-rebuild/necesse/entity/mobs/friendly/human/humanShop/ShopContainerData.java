package necesse.entity.mobs.friendly.human.humanShop;

import java.util.ArrayList;
import necesse.engine.network.Packet;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.entity.mobs.Mob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;

public class ShopContainerData {
   public final Packet content;
   public final ShopManager shopManager;
   public ArrayList<SettlerDialogue> dialogues;

   public ShopContainerData(Packet content, ShopManager shopManager, ArrayList<SettlerDialogue> dialogues) {
      this.content = content;
      this.shopManager = shopManager;
      this.dialogues = dialogues;
   }

   public PacketOpenContainer getPacket(int containerID, Mob mob) {
      return PacketOpenContainer.Mob(containerID, mob, this.content, this);
   }
}
