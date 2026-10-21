package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;

public class GroundSlimeObject extends ModularCarpetObject {
   public GroundSlimeObject() {
      super("groundslime", new Color(182, 197, 60));
      this.objectHealth = 1;
      this.isGrass = true;
      this.canPlaceOnShore = true;
   }

   @Override
   public ModifierValue<Float> getSlowModifier(Mob mob) {
      return !mob.isHostile && !mob.isFlying() ? new ModifierValue<>(BuffModifiers.SLOW, 0.5F) : super.getSpeedModifier(mob);
   }
}
