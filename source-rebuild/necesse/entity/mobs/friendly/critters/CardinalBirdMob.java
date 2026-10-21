package necesse.entity.mobs.friendly.critters;

import necesse.engine.registries.MobRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;

public class CardinalBirdMob extends BirdMob {
   @Override
   protected GameTexture getTexture() {
      return MobRegistry.Textures.cardinalBird;
   }

   @Override
   public LootTable getLootTable() {
      return super.getLootTable();
   }
}
