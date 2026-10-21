package necesse.inventory.item.armorItem.agedChampion;

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

public class AgedChampionChestplateArmorItem extends ChestArmorItem {
   public AgedChampionChestplateArmorItem() {
      super(28, 1600, Item.Rarity.EPIC, "agedchampionchestplate", "agedchampionarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return player != null && player.getDir() == 3 ? null : super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.MELEE_CRIT_DAMAGE, 0.1F), new ModifierValue<>(BuffModifiers.STAMINA_REGEN, 0.1F));
   }
}
