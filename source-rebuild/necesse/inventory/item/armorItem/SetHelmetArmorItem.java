package necesse.inventory.item.armorItem;

import java.util.ArrayList;
import java.util.HashSet;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SetBonusBuff;
import necesse.entity.mobs.gameDamageType.DamageType;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadout;
import necesse.level.maps.levelData.settlementData.SettlementRaidLoadoutGenerator;

public class SetHelmetArmorItem extends HelmetArmorItem {
   public final String setChestStringID;
   public final String setBootsStringID;
   public final String buffType;
   public boolean canBeUsedForRaids = false;
   public int minRaidTier = 0;
   public int maxRaidTier = 0;
   public float raidTicketsModifier = 1.0F;
   public boolean useForRaidsOnlyIfObtained = false;
   public ArrayList<OneOfLootItems> onRegisterArmorSetLootTables = new ArrayList<>();

   public SetHelmetArmorItem(
      int armorValue,
      DamageType damageClass,
      int enchantCost,
      OneOfLootItems lootTableCategory,
      OneOfLootItems armorSetLootTableCategory,
      String setChestStringID,
      String setBootsStringID,
      String buffType,
      String textureName
   ) {
      super(armorValue, damageClass, enchantCost, textureName, lootTableCategory);
      this.addToArmorSetLootTable(armorSetLootTableCategory);
      this.setChestStringID = setChestStringID;
      this.setBootsStringID = setBootsStringID;
      this.buffType = buffType;
   }

   public SetHelmetArmorItem(
      int armorValue,
      DamageType damageClass,
      int enchantCost,
      OneOfLootItems lootTableCategory,
      OneOfLootItems armorSetLootTableCategory,
      Item.Rarity rarity,
      String textureName,
      String setChestStringID,
      String setBootsStringID,
      String buffType
   ) {
      this(armorValue, damageClass, enchantCost, lootTableCategory, armorSetLootTableCategory, setChestStringID, setBootsStringID, buffType, textureName);
      this.rarity = rarity;
   }

   public SetHelmetArmorItem addToArmorSetLootTable(OneOfLootItems... lootTables) {
      if (this.idData.isSet()) {
         for (OneOfLootItems lootTable : lootTables) {
            if (lootTable != null) {
               lootTable.add(new LootItemList(new LootItem(this.getStringID()), new LootItem(this.setChestStringID), new LootItem(this.setBootsStringID)));
            }
         }
      } else {
         for (OneOfLootItems lootTable : lootTables) {
            if (lootTable != null) {
               this.onRegisterArmorSetLootTables.add(lootTable);
            }
         }
      }

      return this;
   }

   @Override
   public void onItemRegistryClosed() {
      super.onItemRegistryClosed();

      for (OneOfLootItems lootTable : this.onRegisterArmorSetLootTables) {
         lootTable.add(new LootItemList(new LootItem(this.getStringID()), new LootItem(this.setChestStringID), new LootItem(this.setBootsStringID)));
      }

      this.onRegisterArmorSetLootTables = null;
      this.registerRaiderLoadouts();
   }

   public void registerRaiderLoadouts() {
      if (this.canBeUsedForRaids && this.setChestStringID != null && this.setBootsStringID != null && ItemRegistry.isObtainable(this.getID())) {
         for (int tier = this.minRaidTier; tier <= this.maxRaidTier; tier++) {
            SettlementRaidLoadout.ArmorSet armorSet = new SettlementRaidLoadout.ArmorSet(this.getStringID(), this.setChestStringID, this.setBootsStringID);
            if (tier > 0) {
               armorSet.setTier(tier);
            }

            SettlementRaidLoadoutGenerator.ALL_ARMOR_SETS.add(armorSet);
         }
      }
   }

   @Override
   public float getRaiderTicketModifier(InventoryItem item, HashSet<String> obtainedItems) {
      return !this.useForRaidsOnlyIfObtained
            || obtainedItems != null
               && obtainedItems.contains(this.getStringID())
               && obtainedItems.contains(this.setChestStringID)
               && obtainedItems.contains(this.setBootsStringID)
         ? this.raidTicketsModifier
         : 0.0F;
   }

   @Override
   public boolean hasSet(InventoryItem helmet, InventoryItem chest, InventoryItem boots) {
      if (helmet.item != this) {
         return false;
      } else {
         return this.setChestStringID != null && !chest.item.getStringID().equals(this.setChestStringID)
            ? false
            : this.setBootsStringID == null || boots.item.getStringID().equals(this.setBootsStringID);
      }
   }

   @Override
   public SetBonusBuff getSetBuff(InventoryItem inventoryItem, Mob mob, boolean isCosmetic) {
      return (SetBonusBuff)BuffRegistry.getBuff(this.buffType);
   }
}
