package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;

public class RangerSettlerPersonality extends SimpleQoLModifierSettlerPersonality {
   public RangerSettlerPersonality(HumanMob mob) {
      super(mob);
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(1, 1, false, new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.5F)));
      this.modifiers
         .add(
            new SimpleQoLModifierSettlerPersonality.QoLModifierValue(
               2, 2, false, new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.5F), new ModifierValue<>(BuffModifiers.PROJECTILE_BOUNCES, 1)
            )
         );
      this.modifiers
         .add(
            new SimpleQoLModifierSettlerPersonality.QoLModifierValue(
               3,
               -1,
               false,
               new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.5F),
               new ModifierValue<>(BuffModifiers.PROJECTILE_BOUNCES, 1),
               new ModifierValue<>(BuffModifiers.PROJECTILE_PIERCES, 1)
            )
         );
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(4, -1, true, new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F)));
   }
}
