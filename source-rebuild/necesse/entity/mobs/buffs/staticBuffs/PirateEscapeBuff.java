package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class PirateEscapeBuff extends Buff {
   public PirateEscapeBuff() {
      this.shouldSave = false;
      this.isVisible = false;
      this.isPassive = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SPEED_FLAT, 20.0F);
   }
}
