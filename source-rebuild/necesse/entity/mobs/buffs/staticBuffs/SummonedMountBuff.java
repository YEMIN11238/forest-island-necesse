package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;

public class SummonedMountBuff extends SummonedCountBuff {
   @Override
   public boolean canCancel(ActiveBuff buff) {
      boolean canCancel = super.canCancel(buff);
      if (canCancel) {
         Mob mount = buff.owner.getMount();
         if (mount != null && mount.getFollowingMob() == buff.owner) {
            GameMessage dismountError = mount.getMountDismountError(buff.owner, null);
            if (dismountError != null) {
               return false;
            }
         }
      }

      return canCancel;
   }
}
