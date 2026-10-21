package necesse.inventory.item.mountItem;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class RingOfAscensionMountItem extends MountItem {
   public RingOfAscensionMountItem() {
      super(null);
      this.rarity = Item.Rarity.LEGENDARY;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ringofascensiontip"));
      return tooltips;
   }

   @Override
   public String canUseMount(InventoryItem item, PlayerMob player, Level level) {
      Mob lastMount = player.getMount();
      if (lastMount != null) {
         GameMessage dismountError = lastMount.getMountDismountError(player, item);
         return dismountError == null ? null : dismountError.translate();
      } else {
         return null;
      }
   }

   @Override
   public InventoryItem useMount(ServerClient client, float playerX, float playerY, InventoryItem item, Level level) {
      PlayerMob player = client.playerMob;
      if (player.getMount() != null) {
         return super.useMount(client, playerX, playerY, item, level);
      }

      if (!player.buffManager.hasBuff(BuffRegistry.RING_OF_ASCENSION_ACTIVE)) {
         player.addBuff(new ActiveBuff(BuffRegistry.RING_OF_ASCENSION_ACTIVE, player, Integer.MAX_VALUE, null), true);
      } else {
         player.buffManager.removeBuff(BuffRegistry.RING_OF_ASCENSION_ACTIVE, true);
      }

      return item;
   }
}
