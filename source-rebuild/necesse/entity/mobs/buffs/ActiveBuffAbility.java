package necesse.entity.mobs.buffs;

import necesse.engine.network.Packet;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;

public interface ActiveBuffAbility {
   boolean canRunAbility(PlayerMob var1, ActiveBuff var2, Packet var3);

   void onActiveAbilityStarted(PlayerMob var1, ActiveBuff var2, Packet var3);

   boolean tickActiveAbility(PlayerMob var1, ActiveBuff var2, boolean var3);

   void onActiveAbilityUpdate(PlayerMob var1, ActiveBuff var2, Packet var3);

   void onActiveAbilityStopped(PlayerMob var1, ActiveBuff var2);

   default Packet getStartAbilityContent(PlayerMob player, ActiveBuff buff, GameCamera camera) {
      return new Packet();
   }

   default Packet getRunningAbilityContent(PlayerMob player, ActiveBuff buff) {
      return new Packet();
   }
}
