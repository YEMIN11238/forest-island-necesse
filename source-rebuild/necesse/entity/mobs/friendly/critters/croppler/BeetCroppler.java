package necesse.entity.mobs.friendly.critters.croppler;

import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.MobTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class BeetCroppler extends CaveCroppler {
   public static LootTable lootTable = new LootTable(LootItem.between("beet", 1, 3), LootItem.between("beetseed", 3, 5));

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   protected MobTexture getCropplerTexture() {
      return MobRegistry.Textures.beetCroppler;
   }
}
