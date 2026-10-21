package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;

public class SimplePotionBuff extends SimpleModifierBuff {
   public SimplePotionBuff(boolean isImportant, ModifierValue<?>... modifiers) {
      super(true, true, true, false, modifiers);
      this.isImportant = isImportant;
   }

   public SimplePotionBuff(ModifierValue<?>... modifiers) {
      this(false, modifiers);
   }

   @Override
   public void updateLocalDisplayName() {
      this.displayName = new LocalMessage("item", this.getStringID());
   }

   @Override
   public boolean isPotionBuff() {
      return true;
   }
}
