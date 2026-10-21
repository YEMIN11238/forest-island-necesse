package necesse.entity.mobs.itemAttacker;

import necesse.inventory.item.Item;

public interface AmmoUserMob {
   int getAvailableArrows(String var1);

   int getAvailableBullets(String var1);

   int getAvailableAmmo(Item[] var1, String var2);

   Item getFirstAvailableArrow(String var1);

   Item getFirstAvailableBullet(String var1);

   Item getFirstAvailableAmmo(Item[] var1, String var2);

   AmmoConsumed removeAmmo(Item var1, int var2, String var3);
}
