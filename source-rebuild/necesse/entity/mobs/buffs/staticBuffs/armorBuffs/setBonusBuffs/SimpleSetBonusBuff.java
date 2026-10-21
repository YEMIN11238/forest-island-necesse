package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.util.LinkedList;
import necesse.engine.modifiers.ModifierTooltip;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.inventory.item.ItemStatTip;

public class SimpleSetBonusBuff extends SetBonusBuff {
   protected ModifierValue<?>[] modifiers;

   public SimpleSetBonusBuff(ModifierValue<?>... modifiers) {
      this.modifiers = modifiers;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      for (ModifierValue modifier : this.modifiers) {
         buff.setModifier(modifier.modifier, modifier.value);
         if (modifier.limits.hasMin()) {
            buff.setMinModifier(modifier.modifier, modifier.limits.min());
         }

         if (modifier.limits.hasMax()) {
            buff.setMaxModifier(modifier.modifier, modifier.limits.max());
         }
      }
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);

      for (ModifierValue modifier : this.modifiers) {
         ModifierTooltip tooltip = currentValues.getModifierTooltip(modifier.modifier, lastValues);
         if (tooltip != null) {
            list.add(tooltip.tip);
         }

         ModifierTooltip minTooltip = currentValues.getMinLimitTooltip(modifier.modifier, lastValues, false);
         if (minTooltip != null) {
            list.add(minTooltip.tip);
         }

         ModifierTooltip maxTooltip = currentValues.getMaxLimitTooltip(modifier.modifier, lastValues, false);
         if (maxTooltip != null) {
            list.add(maxTooltip.tip);
         }
      }
   }
}
