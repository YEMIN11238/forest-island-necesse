package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;

public class SummonerSettlerPersonality extends SimpleQoLModifierSettlerPersonality {
   public SummonerSettlerPersonality(HumanMob mob) {
      super(mob);
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(1, 2, false, new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1)));
      this.modifiers
         .add(
            new SimpleQoLModifierSettlerPersonality.QoLModifierValue(
               3, 4, false, new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 2), new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.1F)
            )
         );
      this.modifiers
         .add(
            new SimpleQoLModifierSettlerPersonality.QoLModifierValue(
               5, -1, false, new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 3), new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.3F)
            )
         );
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(6, -1, true, new ModifierValue<>(BuffModifiers.SPEED, 0.1F)));
   }
}
