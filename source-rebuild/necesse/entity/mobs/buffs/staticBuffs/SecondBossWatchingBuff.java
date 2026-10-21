package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;

public class SecondBossWatchingBuff extends Buff {
   public SecondBossWatchingBuff() {
      this.isVisible = true;
      this.canCancel = false;
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
