package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;

public class FastWorkerSettlerPersonality extends SimpleQoLModifierSettlerPersonality {
   public FastWorkerSettlerPersonality(HumanMob mob) {
      super(mob);
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(1, -1, true, new ModifierValue<>(BuffModifiers.WORK_SPEED, 0.2F)));
   }
}
