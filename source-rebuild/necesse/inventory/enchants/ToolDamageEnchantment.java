package necesse.inventory.enchants;

import necesse.engine.modifiers.ModifierValue;

public class ToolDamageEnchantment extends ToolItemEnchantment {
   public static final ToolDamageEnchantment noEnchant = new ToolDamageEnchantment(0);

   public ToolDamageEnchantment(int enchantCostModPercent, ModifierValue<?>... values) {
      super(ToolItemModifiers.LIST, enchantCostModPercent, values);
   }

   static {
      noEnchant.idData.setData(0, "noenchant");
   }
}
