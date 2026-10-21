package necesse.level.maps.levelData.settlementData.storage;

import java.util.HashMap;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.level.maps.Level;

public class SettlementStorageEquipmentTypeIndex extends SettlementStorageIndex {
   private static final int[] armorTypeToEquipmentTypeOrdinal = new int[ArmorItem.ArmorType.values().length];
   protected HashMap<SettlementStorageEquipmentTypeIndex.EquipmentType, SettlementStorageRecordsRegionData> regions = new HashMap<>();

   public static SettlementStorageEquipmentTypeIndex.EquipmentType armorTypeToEquipmentType(ArmorItem.ArmorType armorType) {
      return SettlementStorageEquipmentTypeIndex.EquipmentType.values()[armorTypeToEquipmentTypeOrdinal[armorType.ordinal()]];
   }

   public SettlementStorageEquipmentTypeIndex(Level level) {
      super(level);
   }

   @Override
   public void clear() {
      this.regions.clear();
   }

   protected SettlementStorageRecordsRegionData getRegionData(SettlementStorageEquipmentTypeIndex.EquipmentType type) {
      return this.regions
         .compute(
            type,
            (id, last) -> {
               if (last == null) {
                  return type == SettlementStorageEquipmentTypeIndex.EquipmentType.WEAPON
                     ? new SettlementStorageRecordsRegionData(this, item -> item.item instanceof ItemAttackerWeaponItem)
                     : new SettlementStorageRecordsRegionData(this, item -> item.item.isArmorItem() && ((ArmorItem)item.item).armorType == type.armorType);
               } else {
                  return (SettlementStorageRecordsRegionData)last;
               }
            }
         );
   }

   @Override
   public void add(InventoryItem inventoryItem, SettlementStorageRecord record) {
      if (inventoryItem.item.isArmorItem()) {
         ArmorItem armorItem = (ArmorItem)inventoryItem.item;
         if (!armorItem.isCosmetic) {
            this.getRegionData(armorTypeToEquipmentType(armorItem.armorType)).add(record);
         }
      } else if (inventoryItem.item instanceof ItemAttackerWeaponItem) {
         this.getRegionData(SettlementStorageEquipmentTypeIndex.EquipmentType.WEAPON).add(record);
      }
   }

   public SettlementStorageRecordsRegionData getEquipmentType(SettlementStorageEquipmentTypeIndex.EquipmentType type) {
      return this.regions.get(type);
   }

   static {
      for (SettlementStorageEquipmentTypeIndex.EquipmentType value : SettlementStorageEquipmentTypeIndex.EquipmentType.values()) {
         if (value.armorType != null) {
            armorTypeToEquipmentTypeOrdinal[value.armorType.ordinal()] = value.ordinal();
         }
      }
   }

   public enum EquipmentType {
      WEAPON(null),
      HEAD(ArmorItem.ArmorType.HEAD),
      CHEST(ArmorItem.ArmorType.CHEST),
      FEET(ArmorItem.ArmorType.FEET);

      public final ArmorItem.ArmorType armorType;

      EquipmentType(ArmorItem.ArmorType armorType) {
         this.armorType = armorType;
      }
   }
}
