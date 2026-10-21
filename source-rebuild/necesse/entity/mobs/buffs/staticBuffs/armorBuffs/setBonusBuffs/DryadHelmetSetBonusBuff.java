package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class DryadHelmetSetBonusBuff extends DryadSetBonusBuff {
   public IntUpgradeValue maxResilience = new IntUpgradeValue().setBaseValue(20).setUpgradedValue(1.0F, 30);
   public FloatUpgradeValue resilienceGain = new FloatUpgradeValue().setBaseValue(0.2F).setUpgradedValue(1.0F, 0.2F);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.MAX_RESILIENCE_FLAT, this.maxResilience.getValue(buff.getUpgradeTier()));
      buff.setModifier(BuffModifiers.RESILIENCE_GAIN, this.resilienceGain.getValue(buff.getUpgradeTier()));
   }
}
