package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class HungryBuff extends Buff {
   public HungryBuff() {
      this.shouldSave = false;
      this.isPassive = true;
      this.canCancel = false;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setMaxModifier(BuffModifiers.HEALTH_REGEN, 0.0F, 10000);
      buff.setMaxModifier(BuffModifiers.COMBAT_HEALTH_REGEN, 0.0F, 10000);
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("bufftooltip", "hungrytip"));
      return tooltips;
   }
}
