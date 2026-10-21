package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;

public class SpiritCorruptedBuff extends Buff {
   public SpiritCorruptedBuff() {
      this.isImportant = true;
      this.canCancel = false;
   }

   @Override
   public boolean shouldNetworkSync() {
      return false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public boolean shouldDrawDuration(ActiveBuff buff) {
      return false;
   }
}
