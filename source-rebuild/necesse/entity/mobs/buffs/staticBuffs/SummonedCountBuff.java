package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;

public class SummonedCountBuff extends Buff {
   public SummonedCountBuff() {
      this.canCancel = true;
      this.isVisible = true;
      this.isPassive = true;
      this.overrideSync = true;
      this.shouldSave = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 999;
   }

   @Override
   public boolean overridesStackDuration() {
      return true;
   }
}
