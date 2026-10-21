package necesse.inventory.item.placeableItem;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.flyingbugs.ButterflyMob;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class ButterflySpawnItem extends FlyingBugSpawnItem {
   protected final ButterflyMob.SmallButterflyType butterflyType;

   public ButterflySpawnItem(ButterflyMob.SmallButterflyType butterflyType) {
      super(20, "butterfly");
      this.butterflyType = butterflyType;
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return new GameSprite(MobRegistry.Textures.butterfly.body, this.butterflyType.ordinal() * 2 + 1, 1, 32, 32);
   }

   @Override
   public Item.Rarity getRarity(InventoryItem item) {
      if (this.butterflyType == ButterflyMob.SmallButterflyType.BLACK
         || this.butterflyType == ButterflyMob.SmallButterflyType.PURPLE
         || this.butterflyType == ButterflyMob.SmallButterflyType.WHITE) {
         return Item.Rarity.RARE;
      } else {
         return this.butterflyType != ButterflyMob.SmallButterflyType.FROG && this.butterflyType != ButterflyMob.SmallButterflyType.HEART
            ? super.getRarity(item)
            : Item.Rarity.UNIQUE;
      }
   }

   @Override
   protected void spawnFlyingBug(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      ButterflyMob butterfly = new ButterflyMob(this.butterflyType);
      butterfly.canDespawn = false;
      level.entityManager.addMob(butterfly, x, y);
   }
}
