package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;

public class WarriorSettlerPersonality extends SimpleQoLModifierSettlerPersonality {
   public WarriorSettlerPersonality(HumanMob mob) {
      super(mob);
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(1, 1, false, new ModifierValue<>(BuffModifiers.INCOMING_DAMAGE_MOD, 0.75F)));
      this.modifiers
         .add(
            new SimpleQoLModifierSettlerPersonality.QoLModifierValue(
               2, 2, false, new ModifierValue<>(BuffModifiers.INCOMING_DAMAGE_MOD, 0.75F), new ModifierValue<>(BuffModifiers.MELEE_ATTACK_SPEED, 0.1F)
            )
         );
      this.modifiers
         .add(
            new SimpleQoLModifierSettlerPersonality.QoLModifierValue(
               3,
               -1,
               false,
               new ModifierValue<>(BuffModifiers.INCOMING_DAMAGE_MOD, 0.75F),
               new ModifierValue<>(BuffModifiers.MELEE_ATTACK_SPEED, 0.1F),
               new ModifierValue<>(BuffModifiers.SPEED, 0.25F)
            )
         );
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(4, -1, true, new ModifierValue<>(BuffModifiers.MAX_HEALTH, 0.05F)));
   }
}
