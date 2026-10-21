package necesse.entity.mobs;

import necesse.engine.network.Packet;

public class ActiveMountAbilityContainer {
   public final int uniqueID;
   public final boolean isRunningClient;
   public final ActiveMountAbility mountAbility;
   public final Mob mount;

   public ActiveMountAbilityContainer(int uniqueID, boolean isRunningClient, ActiveMountAbility mountAbility, Mob mount) {
      this.uniqueID = uniqueID;
      this.isRunningClient = isRunningClient;
      this.mountAbility = mountAbility;
      this.mount = mount;
   }

   public boolean tick(PlayerMob player) {
      return this.mount.removed() ? false : this.mountAbility.tickActiveMountAbility(player, this.isRunningClient);
   }

   public void update(PlayerMob player, Packet content) {
      this.mountAbility.onActiveMountAbilityUpdate(player, content);
   }

   public void onStopped(PlayerMob player) {
      this.mountAbility.onActiveMountAbilityStopped(player);
   }
}
