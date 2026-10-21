package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.buffs.ActiveBuff;

public abstract class PerfectBlockTrinketBuff extends TrinketBuff {
   public boolean canPerfectBlock(ActiveBuff buff) {
      return buff.owner.buffManager.hasBuff(BuffRegistry.PERFECT_BLOCK);
   }
}
