package necesse.entity.mobs;

import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketMountAbility;
import necesse.gfx.camera.GameCamera;

public interface MountAbility {
   void runMountAbility(PlayerMob var1, Packet var2);

   boolean canRunMountAbility(PlayerMob var1, Packet var2);

   default Packet getMountAbilityContent(PlayerMob player, GameCamera camera) {
      return new Packet();
   }

   default void runAndSendMountAbility(Client client, PlayerMob player, Packet content) {
      this.runMountAbility(player, content);
      client.network.sendPacket(new PacketMountAbility(client.getSlot(), (Mob)this, content));
   }
}
