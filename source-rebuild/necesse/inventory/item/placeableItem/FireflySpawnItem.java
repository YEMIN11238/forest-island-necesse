package necesse.inventory.item.placeableItem;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class FireflySpawnItem extends FlyingBugSpawnItem {
   protected final FireflyMob.FireflyType fireflyType;

   public FireflySpawnItem(FireflyMob.FireflyType fireflyType) {
      super(20, "firefly");
      this.fireflyType = fireflyType;
      this.rarity = Item.Rarity.NORMAL;
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return new GameSprite(this.itemTexture, this.fireflyType.ordinal(), 0, 32);
   }

   @Override
   protected void loadItemTextures() {
      this.itemTexture = GameTexture.fromFile("items/fireflies");
   }

   @Override
   public Item.Rarity getRarity(InventoryItem item) {
      return this.fireflyType.rarity;
   }

   @Override
   protected void spawnFlyingBug(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      FireflyMob firefly = new FireflyMob(this.fireflyType);
      firefly.canDespawn = false;
      level.entityManager.addMob(firefly, x, y);
   }
}
