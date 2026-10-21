package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class CampfireBuff extends VicinityBuff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MOB_SPAWN_RATE, -0.5F);
      buff.setModifier(BuffModifiers.HEALTH_REGEN_FLAT, 2.0F);
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("bufftooltip", "campfiretip1"));
      tooltips.add(Localization.translate("bufftooltip", "campfiretip2"));
      return tooltips;
   }
}
