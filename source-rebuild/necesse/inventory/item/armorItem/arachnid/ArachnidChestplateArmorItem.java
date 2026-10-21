package necesse.inventory.item.armorItem.arachnid;

import java.awt.Color;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;

public class ArachnidChestplateArmorItem extends ChestArmorItem {
   FloatUpgradeValue summonDamage = new FloatUpgradeValue(0.1F, 0.0F);

   public ArachnidChestplateArmorItem() {
      super(7, 550, Item.Rarity.UNCOMMON, "arachnidchestplate", "arachnidarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.SUMMON_DAMAGE, this.summonDamage.defaultValue), new ModifierValue<>(BuffModifiers.KNOCKBACK_INCOMING_MOD, 0.5F)
      );
   }

   @Override
   public void addModifierTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, Mob perspective) {
      list.add(200, new ItemStatTip() {
         @Override
         public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
            return new LocalMessage("itemtooltip", "spiderchest");
         }
      });
      list.add(300, new ItemStatTip() {
         @Override
         public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
            return new LocalMessage("buffmodifiers", "summondamage", "mod", (int)(ArachnidChestplateArmorItem.this.summonDamage.defaultValue * 100.0F));
         }
      });
   }
}
