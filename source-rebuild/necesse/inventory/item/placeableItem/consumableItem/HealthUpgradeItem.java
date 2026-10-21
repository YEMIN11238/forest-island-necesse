package necesse.inventory.item.placeableItem.consumableItem;

import java.awt.geom.Line2D;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketPlayerGeneral;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class HealthUpgradeItem extends ConsumableItem {
   public HealthUpgradeItem(int stackSize) {
      super(stackSize, true);
      this.rarity = Item.Rarity.UNIQUE;
      this.allowRightClickToConsume = true;
      this.worldDrawSize = 32;
      this.incinerationTimeMillis = 60000;
      this.keyWords.add("health");
      this.keyWords.add("hp");
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      player.healthUpgradeManager.addUpgrade(this.getStringID(), 1);
      if (level.isServer()) {
         level.getServer().network.sendToAllClientsExcept(new PacketPlayerGeneral(player.getServerClient()), player.getServerClient());
      } else if (level.isClient()) {
         SoundManager.playSound(GameResources.eat, SoundEffect.effect(player));
      }

      if (this.isSingleUse(player)) {
         item.setAmount(item.getAmount() - 1);
      }

      return item;
   }

   @Override
   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      return player.healthUpgradeManager.canUpgrade(this.getStringID()) ? null : "invalidupgrade";
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "consumetip"));
      tooltips.add(this.getConsumedCountTooltip(perspective));
      return tooltips;
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "relic");
   }

   public GameTooltips getConsumedCountTooltip(PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.drawOrder = Integer.MIN_VALUE;
      if (perspective != null) {
         int currentUpgrade = perspective.healthUpgradeManager.getCurrentUpgrade(this.getStringID());
         int maxUpgrades = perspective.healthUpgradeManager.getMaxUpgrades(this.getStringID());
         if (maxUpgrades == 1) {
            if (currentUpgrade <= 0) {
               tooltips.add(
                  new StringTooltips(
                     Localization.translate("itemtooltip", "characternotconsumedyet", "name", perspective.getDisplayName()), GameColor.ITEM_QUEST
                  )
               );
            } else {
               tooltips.add(
                  new StringTooltips(Localization.translate("itemtooltip", "characteralreadyconsumed", "name", perspective.getDisplayName()), GameColor.GRAY)
               );
            }
         } else {
            int missing = maxUpgrades - currentUpgrade;
            tooltips.add(
               new StringTooltips(
                  Localization.translate("itemtooltip", "charactermissingupgrades", "count", missing, "name", perspective.getDisplayName()),
                  missing > 0 ? GameColor.ITEM_QUEST : GameColor.GRAY
               )
            );
         }
      }

      return tooltips;
   }
}
