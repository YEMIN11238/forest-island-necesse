package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class ParryBucklerParriedBuff extends Buff {
   public ParryBucklerParriedBuff() {
      this.canCancel = false;
      this.isVisible = true;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.ALL_DAMAGE, 0.2F);
   }
}
