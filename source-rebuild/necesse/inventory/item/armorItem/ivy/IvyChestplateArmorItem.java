package necesse.inventory.item.armorItem.ivy;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;
import necesse.level.maps.Level;

public class IvyChestplateArmorItem extends ChestArmorItem {
   public IvyChestplateArmorItem() {
      super(15, 850, Item.Rarity.UNCOMMON, "ivychest", "ivyarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F));
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return player != null && player.getDir() == 3 ? null : super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
   }
}
