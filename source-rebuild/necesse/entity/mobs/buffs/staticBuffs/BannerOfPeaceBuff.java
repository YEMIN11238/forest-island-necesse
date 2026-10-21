package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;

public class BannerOfPeaceBuff extends VicinityBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMaxModifier(BuffModifiers.MOB_SPAWN_RATE, 0.0F, 1000);
      buff.setMaxModifier(BuffModifiers.MOB_SPAWN_CAP, 0.0F, 1000);
   }

   @Override
   public void updateLocalDisplayName() {
      this.displayName = new LocalMessage("object", this.getStringID());
   }
}
