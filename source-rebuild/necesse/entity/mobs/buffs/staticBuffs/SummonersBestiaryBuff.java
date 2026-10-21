package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class SummonersBestiaryBuff extends Buff {
   public SummonersBestiaryBuff() {
      this.canCancel = false;
      this.isVisible = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SUMMON_DAMAGE, 0.05F);
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 8;
   }
}
