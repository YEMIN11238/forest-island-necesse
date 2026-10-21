package necesse.inventory.item.placeableItem;

import java.awt.geom.Line2D;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public abstract class FlyingBugSpawnItem extends PlaceableItem {
   public final String mobStringID;

   public FlyingBugSpawnItem(int stackSize, String mobStringID) {
      super(stackSize, true);
      this.controllerIsTileBasedPlacing = true;
      this.rarity = Item.Rarity.COMMON;
      this.dropsAsMatDeathPenalty = true;
      this.incinerationTimeMillis = 30000;
      this.keyWords.add("spawn");
      this.keyWords.add("critters");
      this.keyWords.add(mobStringID);
      this.setItemCategory("misc", "critters");
      this.mobStringID = mobStringID;
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return new GameSprite(MobRegistry.getMobIcon(this.mobStringID));
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      if (level.isServer()) {
         this.spawnFlyingBug(level, x, y, player, seed, item, mapContent);
      }

      if (this.isSingleUse(player)) {
         item.setAmount(item.getAmount() - 1);
      }

      if (level.isClient()) {
         SoundManager.playSound(GameResources.pop, SoundEffect.effect(player).pitch(0.8F));
      }

      return item;
   }

   protected abstract void spawnFlyingBug(Level var1, int var2, int var3, PlayerMob var4, int var5, InventoryItem var6, GNDItemMap var7);

   @Override
   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      return !level.isLevelPosWithinBounds(x, y) ? "outsidelevelbounds" : super.canPlace(level, x, y, player, playerPositionLine, item, mapContent);
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", this.mobStringID + "tip"), 310);
      return tooltips;
   }
}
