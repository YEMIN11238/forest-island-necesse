package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;

public class SpideriteHatSetBonusBuff extends SpideriteSetBonusBuff {
   public IntUpgradeValue maxMana = new IntUpgradeValue(0, 0.1F).setBaseValue(250).setUpgradedValue(1.0F, 250);

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      super.init(buff, eventSubscriber);
      buff.setModifier(BuffModifiers.MAX_MANA_FLAT, this.maxMana.getValue(buff.getUpgradeTier()));
   }
}
