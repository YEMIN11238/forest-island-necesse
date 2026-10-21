package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.StaticMessage;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;

public class DebugBuff extends Buff {
   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_SUMMONS, 60);
      buff.setModifier(BuffModifiers.ATTACK_SPEED, 1.0F);
      buff.setModifier(BuffModifiers.DASH_STACKS, 100);
      buff.setModifier(BuffModifiers.DASH_COOLDOWN, -10.0F);
      buff.setModifier(BuffModifiers.STAMINA_CAPACITY, 10.0F);
      buff.setModifier(BuffModifiers.STAMINA_REGEN, 10.0F);
      buff.setModifier(BuffModifiers.MAX_FOOD_BUFFS, 5);
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 1;
   }

   @Override
   public boolean overridesStackDuration() {
      return false;
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add("Used for testing purposes");
      return tooltips;
   }

   @Override
   public void updateLocalDisplayName() {
      this.displayName = new StaticMessage("Debug buff");
   }
}
