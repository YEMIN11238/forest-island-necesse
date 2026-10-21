package necesse.inventory.item.bulletItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.bulletProjectile.SharkToothBulletProjectile;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class SharkToothBulletItem extends BulletItem {
   public SharkToothBulletItem() {
      this.damage = 5;
      this.rarity = Item.Rarity.COMMON;
   }

   @Override
   public boolean overrideProjectile() {
      return true;
   }

   @Override
   public Projectile getProjectile(float x, float y, float targetX, float targetY, float velocity, int range, GameDamage damage, int knockback, Mob owner) {
      return new SharkToothBulletProjectile(x, y, targetX, targetY, velocity, range, damage, knockback, owner);
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "sharkbullettip"));
      return tooltips;
   }
}
