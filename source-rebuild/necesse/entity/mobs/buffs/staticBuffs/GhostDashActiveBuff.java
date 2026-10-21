package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class GhostDashActiveBuff extends Buff {
   public GhostDashActiveBuff() {
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.INCOMING_DAMAGE_MOD, 0.0F);
      buff.setModifier(BuffModifiers.KNOCKBACK_INCOMING_MOD, 0.0F);
      buff.setMaxModifier(BuffModifiers.FRICTION, 0.01F, 1000);
   }
}
