package necesse.inventory.item.armorItem.sharkarmor;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;
import necesse.level.maps.Level;

public class SharkChestplateArmorItem extends ChestArmorItem {
   public FloatUpgradeValue healthRegen = new FloatUpgradeValue().setBaseValue(0.25F).setUpgradedValue(1.0F, 0.75F);

   public SharkChestplateArmorItem() {
      super(7, 500, "sharkarmorchestplate", "sharkarmorchestplatearms", BodyArmorLootTable.bodyArmor);
      this.rarity = Item.Rarity.UNCOMMON;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F),
         new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, this.healthRegen.getValue(this.getUpgradeTier(item)))
      );
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      if (player != null && player.getDir() == 3) {
         return this.armorTexture == null ? null : new GameSprite(this.armorTexture, 0, 9, 32);
      } else {
         return super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
      }
   }
}
