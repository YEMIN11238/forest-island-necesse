package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlerDietData {
   public final boolean doesSettlerEat;
   public final ItemCategoriesFilter dietFilter = new ItemCategoriesFilter(ItemCategory.foodQualityMasterCategory, true);

   public SettlerDietData(LevelSettler settler) {
      SettlerMob mob = settler.getMob();
      this.doesSettlerEat = mob == null || mob.doesEatFood();
      if (this.doesSettlerEat) {
         this.dietFilter.loadFromCopy(settler.dietFilter);
      }
   }

   public SettlerDietData(PacketReader reader) {
      if (reader.getNextBoolean()) {
         this.doesSettlerEat = true;
         this.dietFilter.readPacket(reader);
      } else {
         this.doesSettlerEat = false;
      }
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextBoolean(this.doesSettlerEat);
      if (this.doesSettlerEat) {
         this.dietFilter.writePacket(writer);
      }
   }
}
