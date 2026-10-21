package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemSearchTester;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlerEquipmentData implements SettlerSearchData {
   public boolean selfManageEquipment;
   public boolean preferArmorSets;
   public final ItemCategoriesFilter equipmentFilter = new ItemCategoriesFilter(ItemCategory.equipmentMasterCategory, true);

   public SettlerEquipmentData(LevelSettler settler) {
      SettlerMob mob = settler.getMob();
      this.selfManageEquipment = mob != null && mob.managesEquipmentThemself();
      this.preferArmorSets = settler.preferArmorSets;
      this.equipmentFilter.loadFromCopy(settler.equipmentFilter);
   }

   public SettlerEquipmentData(PacketReader reader) {
      this.selfManageEquipment = reader.getNextBoolean();
      this.preferArmorSets = reader.getNextBoolean();
      this.equipmentFilter.readPacket(reader);
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextBoolean(this.selfManageEquipment);
      writer.putNextBoolean(this.preferArmorSets);
      this.equipmentFilter.writePacket(writer);
   }

   @Override
   public boolean matchesSearch(String lowerCaseSearchString, SettlerMob mob, PlayerMob perspective) {
      if (mob instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)mob;
         ItemSearchTester tester = ItemSearchTester.constructSearchTester(lowerCaseSearchString);

         for (int slot = 0; slot < humanMob.equipmentInventory.getSize(); slot++) {
            InventoryItem item = humanMob.equipmentInventory.getItem(slot);
            if (item != null && tester.matches(item, perspective, new GameBlackboard())) {
               return true;
            }
         }
      }

      return false;
   }
}
