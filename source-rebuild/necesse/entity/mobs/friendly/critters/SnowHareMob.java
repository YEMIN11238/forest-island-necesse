package necesse.entity.mobs.friendly.critters;

import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class SnowHareMob extends RabbitMob {
   public static LootTable lootTable = new LootTable(new LootItem("rabbitleg"));

   @Override
   protected GameTexture getTexture() {
      return MobRegistry.Textures.snowHare;
   }

   @Override
   protected boolean canSpawnOnTileID(int tileID) {
      return tileID == TileRegistry.snowID || tileID == TileRegistry.iceID;
   }
}
