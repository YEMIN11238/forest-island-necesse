package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class BannerOfSummonSpeedBuff extends VicinityBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SUMMONS_SPEED, 0.75F);
   }

   @Override
   public void updateLocalDisplayName() {
      this.displayName = new LocalMessage("item", this.getStringID());
   }
}
