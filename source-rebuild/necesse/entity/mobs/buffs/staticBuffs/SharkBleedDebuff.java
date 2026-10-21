package necesse.entity.mobs.buffs.staticBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;

public class SharkBleedDebuff extends BleedDebuff {
   @Override
   protected float getDamagePerSecond(ActiveBuff buff) {
      return 5.0F;
   }
}
