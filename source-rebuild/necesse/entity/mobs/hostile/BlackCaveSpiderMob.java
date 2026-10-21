package necesse.entity.mobs.hostile;

import necesse.entity.mobs.GameDamage;
import necesse.inventory.item.matItem.MultiTextureMatItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class BlackCaveSpiderMob extends GiantCaveSpiderMob {
   public static LootTable lootTable = new LootTable(LootItem.between("cavespidergland", 1, 2, MultiTextureMatItem.getGNDData(1)));

   public BlackCaveSpiderMob() {
      super(GiantCaveSpiderMob.Variant.BLACK, 300, new GameDamage(25.0F), new GameDamage(20.0F));
      this.setSpeed(35.0F);
      this.setArmor(10);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }
}
