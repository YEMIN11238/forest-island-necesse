package necesse.entity.mobs.buffs;

import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketBuffAbility;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;

public interface BuffAbility {
   void runAbility(PlayerMob var1, ActiveBuff var2, Packet var3);

   boolean canRunAbility(PlayerMob var1, ActiveBuff var2, Packet var3);

   default Packet getAbilityContent(PlayerMob player, ActiveBuff buff, GameCamera camera) {
      return new Packet();
   }

   default void runAndSendAbility(Client client, PlayerMob player, ActiveBuff buff, Packet content) {
      this.runAbility(player, buff, content);
      client.network.sendPacket(new PacketBuffAbility(client.getSlot(), buff.buff, content));
   }
}
