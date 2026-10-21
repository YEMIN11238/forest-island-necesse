package necesse.entity.mobs;

import necesse.engine.network.Packet;
import necesse.gfx.camera.GameCamera;

public interface ActiveMountAbility {
   boolean canRunMountAbility(PlayerMob var1, Packet var2);

   void onActiveMountAbilityStarted(PlayerMob var1, Packet var2);

   boolean tickActiveMountAbility(PlayerMob var1, boolean var2);

   void onActiveMountAbilityUpdate(PlayerMob var1, Packet var2);

   void onActiveMountAbilityStopped(PlayerMob var1);

   default Packet getStartMountAbilityContent(PlayerMob player, GameCamera camera) {
      return new Packet();
   }

   default Packet getRunningMountAbilityContent(PlayerMob player) {
      return new Packet();
   }
}
