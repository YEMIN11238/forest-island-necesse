package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.item.Item;

public interface ToolItemSummonedMob {
   void updateDamage(GameDamage var1);

   void setEnchantment(ToolItemEnchantment var1);

   void setRemoveWhenNotInInventory(Item var1, CheckSlotType var2);
}
