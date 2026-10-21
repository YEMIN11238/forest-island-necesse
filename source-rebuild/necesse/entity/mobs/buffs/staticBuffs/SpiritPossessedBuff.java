package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class SpiritPossessedBuff extends Buff {
   public SpiritPossessedBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.POISON_DAMAGE_FLAT, 5.0F);
   }

   @Override
   public boolean shouldDrawDuration(ActiveBuff buff) {
      return false;
   }
}
