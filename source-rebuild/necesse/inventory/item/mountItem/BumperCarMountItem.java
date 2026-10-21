package necesse.inventory.item.mountItem;

import java.util.List;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.BumperCarMob;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class BumperCarMountItem extends MountItem {
   public BumperCarMountItem() {
      super("bumpercar");
      this.rarity = Item.Rarity.EPIC;
   }

   @Override
   public InventoryItem getDefaultLootItem(GameRandom random, int amount) {
      InventoryItem lootItem = super.getDefaultLootItem(random, amount);
      this.setCurrentVariant(lootItem, random.nextInt(BumperCarMob.totalVariants));
      return lootItem;
   }

   public void setCurrentVariant(InventoryItem item, int sprite) {
      setCurrentVariant(item.getGndData(), sprite);
   }

   public static void setCurrentVariant(GNDItemMap gndData, int sprite) {
      gndData.setByteUnsigned("variant", sprite);
   }

   @Override
   public boolean canCombineItem(Level level, PlayerMob player, InventoryItem me, InventoryItem them, String purpose) {
      return !super.canCombineItem(level, player, me, them, purpose) ? false : this.isSameGNDData(level, me, them, purpose);
   }

   @Override
   public boolean isSameGNDData(Level level, InventoryItem me, InventoryItem them, String purpose) {
      return me.getGndData().sameKeys(them.getGndData(), "variant");
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      int currentSprite = Math.floorMod(item.getGndData().getByteUnsigned("variant"), BumperCarMob.totalVariants);
      return new GameSprite(this.itemTexture, currentSprite, 0, 32);
   }

   @Override
   public void addDefaultItems(List<InventoryItem> list, PlayerMob player) {
      for (int i = 0; i < BumperCarMob.totalVariants; i++) {
         InventoryItem item = this.getDefaultItem(player, 1);
         item.getGndData().setByteUnsigned("variant", i);
         list.add(item);
      }
   }

   @Override
   protected void beforeSpawn(Mob mob, InventoryItem item, PlayerMob player) {
      super.beforeSpawn(mob, item, player);
      ((BumperCarMob)mob).variant = item.getGndData().getByteUnsigned("variant");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "bumpercartip"));
      tooltips.add(Localization.translate("itemtooltip", "bumpercartip2"));
      return tooltips;
   }
}
