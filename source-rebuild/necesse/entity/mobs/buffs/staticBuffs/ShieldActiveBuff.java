package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class ShieldActiveBuff extends Buff {
   public ShieldActiveBuff() {
      this.shouldSave = false;
      this.isVisible = false;
   }

   @Override
   public boolean shouldNetworkSync() {
      return false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      float minSlow = buff.getGndData().getFloat("minSlow", -1.0F);
      if (minSlow != -1.0F) {
         buff.setMinModifier(BuffModifiers.SLOW, minSlow, 1000000);
      }
   }
}
