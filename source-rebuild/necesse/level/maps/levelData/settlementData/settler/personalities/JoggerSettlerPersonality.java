package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;

public class JoggerSettlerPersonality extends SimpleQoLModifierSettlerPersonality {
   public JoggerSettlerPersonality(HumanMob mob) {
      super(mob);
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(1, -1, true, new ModifierValue<>(BuffModifiers.SPEED, 0.15F)));
   }
}
