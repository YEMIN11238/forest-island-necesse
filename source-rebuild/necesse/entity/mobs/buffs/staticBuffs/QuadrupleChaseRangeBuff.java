package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.StaticMessage;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class QuadrupleChaseRangeBuff extends Buff {
   public QuadrupleChaseRangeBuff() {
      this.isVisible = false;
   }

   @Override
   public void updateLocalDisplayName() {
      this.displayName = new StaticMessage("Quadruple chase range");
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.CHASER_RANGE, 4.0F);
   }
}
