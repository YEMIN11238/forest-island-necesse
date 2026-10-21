package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;

public class StatsBuff extends Buff {
   public StatsBuff() {
      this.canCancel = false;
      this.isVisible = false;
      this.isPassive = true;
      this.shouldSave = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }
}
