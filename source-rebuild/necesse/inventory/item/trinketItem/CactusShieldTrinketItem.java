package necesse.inventory.item.trinketItem;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.TrinketsLootTable;

public class CactusShieldTrinketItem extends ShieldTrinketItem {
   public CactusShieldTrinketItem(Item.Rarity rarity, int enchantCost) {
      super(rarity, 4, 0.5F, 8000, 0.25F, 40, 360.0F, enchantCost, TrinketsLootTable.trinkets);
   }

   @Override
   public ListGameTooltips getExtraShieldTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getExtraShieldTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "cactusshieldtip"));
      return tooltips;
   }

   @Override
   public void onShieldHit(InventoryItem item, Mob mob, MobWasHitEvent hitEvent) {
      super.onShieldHit(item, mob, hitEvent);
      if (mob.isServer() && !hitEvent.wasPrevented) {
         Mob attackOwner = hitEvent.attacker != null ? hitEvent.attacker.getAttackOwner() : null;
         boolean hasOwnerInChain = hitEvent.attacker != null && hitEvent.attacker.isInAttackOwnerChain(mob);
         if (attackOwner != null && !hasOwnerInChain) {
            float dx = attackOwner.x - mob.x;
            float dy = attackOwner.y - mob.y;
            float finalDamageMultiplier = this.getShieldFinalDamageMultiplier(item, mob);
            if (finalDamageMultiplier > 0.0F) {
               float damage = hitEvent.damage / finalDamageMultiplier;
               if (attackOwner.isPlayer) {
                  damage /= 2.0F;
               }

               attackOwner.isServerHit(new GameDamage(damage, 0.0F), dx, dy, 0.0F, mob);
            }
         }
      }
   }
}
