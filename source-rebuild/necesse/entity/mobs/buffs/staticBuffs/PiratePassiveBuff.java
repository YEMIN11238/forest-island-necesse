package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class PiratePassiveBuff extends Buff {
   public PiratePassiveBuff() {
      this.shouldSave = false;
      this.isVisible = false;
      this.isPassive = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, buff.owner.getMaxHealth() / 10.0F);
   }
}
