package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;

public class ArcanicHoodActiveBuff extends Buff {
   public ArcanicHoodActiveBuff() {
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public boolean shouldDrawDuration(ActiveBuff buff) {
      return false;
   }
}
