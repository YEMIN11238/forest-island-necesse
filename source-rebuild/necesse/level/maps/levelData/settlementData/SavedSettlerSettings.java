package necesse.level.maps.levelData.settlementData;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.itemFilter.ItemCategoriesFilter;

public class SavedSettlerSettings {
   public final int settlementUniqueID;
   public ItemCategoriesFilter dietFilter = new ItemCategoriesFilter(ItemCategory.foodQualityMasterCategory, true);
   public Boolean preferArmorSets;
   public ItemCategoriesFilter equipmentFilter = new ItemCategoriesFilter(ItemCategory.equipmentMasterCategory, true);
   public int restrictZoneUniqueID;

   public SavedSettlerSettings(LevelSettler settler) {
      this.settlementUniqueID = settler.data.uniqueID;
      if (!settler.dietFilter.isEqualsFilter(settler.data.getNewSettlerDiet())) {
         this.dietFilter.loadFromCopy(settler.dietFilter);
      } else {
         this.dietFilter = null;
      }

      if (!settler.equipmentFilter.isEqualsFilter(settler.data.getNewSettlerEquipmentFilter())) {
         this.equipmentFilter.loadFromCopy(settler.equipmentFilter);
      } else {
         this.equipmentFilter = null;
      }

      this.preferArmorSets = settler.preferArmorSets;
      this.restrictZoneUniqueID = settler.restrictZoneUniqueID;
   }

   public SavedSettlerSettings(LoadData save) {
      this.settlementUniqueID = save.getInt("settlementUniqueID", -1);
      if (this.settlementUniqueID == -1) {
         throw new IllegalArgumentException("Could not load saved settler settings settlement uniqueID");
      }

      LoadData dietFilterSave = save.getFirstLoadDataByName("dietFilter");
      if (dietFilterSave != null) {
         this.dietFilter.applyLoadData(dietFilterSave);
      } else {
         this.dietFilter = null;
      }

      LoadData equipmentFilterSave = save.getFirstLoadDataByName("equipmentFilter");
      if (equipmentFilterSave != null) {
         this.equipmentFilter.applyLoadData(equipmentFilterSave);
      } else {
         this.equipmentFilter = null;
      }

      if (save.getFirstLoadDataByName("preferArmorSets") != null) {
         this.preferArmorSets = save.getBoolean("preferArmorSets", true, false);
      }

      this.restrictZoneUniqueID = save.getInt("restrictZoneUniqueID", 0, false);
   }

   public void addSaveData(SaveData save) {
      save.addInt("settlementUniqueID", this.settlementUniqueID);
      if (this.dietFilter != null) {
         SaveData dietFilterSave = new SaveData("dietFilter");
         this.dietFilter.addSaveData(dietFilterSave);
         save.addSaveData(dietFilterSave);
      }

      if (this.equipmentFilter != null) {
         SaveData equipmentFilterSave = new SaveData("equipmentFilter");
         this.equipmentFilter.addSaveData(equipmentFilterSave);
         save.addSaveData(equipmentFilterSave);
      }

      if (this.preferArmorSets != null) {
         save.addBoolean("preferArmorSets", this.preferArmorSets);
      }

      if (this.restrictZoneUniqueID != 0) {
         save.addInt("restrictZoneUniqueID", this.restrictZoneUniqueID);
      }
   }

   public void applyToSettler(LevelSettler settler) {
      if (this.dietFilter != null) {
         settler.dietFilter.loadFromCopy(this.dietFilter);
      }

      if (this.equipmentFilter != null) {
         settler.equipmentFilter.loadFromCopy(this.equipmentFilter);
      }

      if (this.preferArmorSets != null) {
         settler.preferArmorSets = this.preferArmorSets;
      }

      if (settler.data.getRestrictZone(this.restrictZoneUniqueID) != null) {
         settler.restrictZoneUniqueID = this.restrictZoneUniqueID;
      }
   }
}
