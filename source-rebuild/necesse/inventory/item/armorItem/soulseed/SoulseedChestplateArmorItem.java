package necesse.inventory.item.armorItem.soulseed;

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

public class SoulseedChestplateArmorItem extends ChestArmorItem {
   public SoulseedChestplateArmorItem() {
      super(23, 1300, Item.Rarity.UNCOMMON, "soulseedchest", "soulseedarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.SUMMON_DAMAGE, 0.05F), new ModifierValue<>(BuffModifiers.SUMMON_ATTACK_SPEED, 0.1F));
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return player != null && player.getDir() == 3 ? null : super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
   }
}
