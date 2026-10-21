package necesse.inventory.item.toolItem.swordToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.LineHitbox;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionCloseRangeWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.incursion.IncursionData;

public class GemstoneLongswordToolItem extends SwordToolItem {
   public GemstoneLongswordToolItem() {
      super(1900, IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(90.0F).setUpgradedValue(1.0F, 105.00003F);
      this.attackRange.setBaseValue(120);
      this.knockback.setBaseValue(75);
      this.resilienceGain.setBaseValue(2.5F);
      this.attackXOffset = 12;
      this.attackYOffset = 12;
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.5F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "gemstonelongswordtip"));
      return tooltips;
   }

   @Override
   public boolean canItemAttackerHitTarget(ItemAttackerMob attackerMob, float fromX, float fromY, Mob target, InventoryItem item) {
      float distance = attackerMob.getDistance(target);
      int attackRange = this.getAttackRange(item);
      if (distance < attackRange) {
         return super.canItemAttackerHitTarget(attackerMob, fromX, fromY, target, item);
      } else {
         return distance < attackRange * 1.5F
            ? !attackerMob.getLevel()
               .collides(
                  new LineHitbox(fromX, fromY, target.x, target.y, 45.0F),
                  attackerMob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target)
               )
            : false;
      }
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.gemstoneSwords).volume(0.4F);
   }
}
