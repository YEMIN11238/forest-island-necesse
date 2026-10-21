package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class AgedChampionSetBonusBuff extends SetBonusBuff {
   public IntUpgradeValue maxResilience = new IntUpgradeValue().setBaseValue(30).setUpgradedValue(1.0F, 30);
   public FloatUpgradeValue resilienceGain = new FloatUpgradeValue().setBaseValue(0.2F).setUpgradedValue(1.0F, 0.2F);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_RESILIENCE_FLAT, this.maxResilience.getValue(buff.getUpgradeTier()));
      buff.setModifier(BuffModifiers.RESILIENCE_GAIN, this.resilienceGain.getValue(buff.getUpgradeTier()));
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   private void updateModifiers(ActiveBuff buff) {
      boolean fullHealth = buff.owner.getHealthPercent() == 1.0F;
      buff.setModifier(BuffModifiers.RESILIENCE_REGEN_FLAT, fullHealth ? 1.0F : 0.0F);
      buff.setModifier(BuffModifiers.MELEE_CRIT_CHANCE, fullHealth ? 0.2F : 0.0F);
   }

   @Override
   public void onWasHit(ActiveBuff buff, MobWasHitEvent event) {
      super.onWasHit(buff, event);
      if (buff.owner.buffManager.hasBuff(BuffRegistry.PERFECT_BLOCK)) {
         buff.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.AGED_CHAMPION_PROWESS, buff.owner, 5000, null), false);
      }
   }

   @Override
   public void addStatTooltips(LinkedList<ItemStatTip> list, ActiveBuff currentValues, ActiveBuff lastValues) {
      super.addStatTooltips(list, currentValues, lastValues);
      currentValues.getModifierTooltipsBuilder(true, true)
         .addLastValues(lastValues)
         .exclude(BuffModifiers.RESILIENCE_REGEN_FLAT, BuffModifiers.MELEE_CRIT_CHANCE)
         .buildToStatList(list);
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "agedchampionset1"), 400);
      tooltips.add(Localization.translate("itemtooltip", "agedchampionset2"), 400);
      return tooltips;
   }
}
