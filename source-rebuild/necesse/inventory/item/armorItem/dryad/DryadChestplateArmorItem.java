package necesse.inventory.item.armorItem.dryad;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;
import necesse.level.maps.Level;

public class DryadChestplateArmorItem extends ChestArmorItem {
   public DryadChestplateArmorItem() {
      super(26, 1550, Item.Rarity.UNCOMMON, "dryadchestplate", "dryadarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F));
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      if (player != null && player.getDir() == 3) {
         GameTexture armorTexture = this.getArmorTexture(item, level, player, headItem, chestItem, feetItem);
         return armorTexture == null ? null : new GameSprite(armorTexture, 0, 9, 32);
      } else {
         return super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
      }
   }
}
