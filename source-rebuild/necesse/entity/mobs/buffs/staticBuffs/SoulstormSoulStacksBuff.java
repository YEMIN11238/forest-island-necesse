package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class SoulstormSoulStacksBuff extends Buff {
   public SoulstormSoulStacksBuff() {
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return Integer.MAX_VALUE;
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("buff", "soulstormsouls", "value", ab.getStacks()));
      return tooltips;
   }
}
