package necesse.inventory.item.placeableItem.followerSummonItem.petFollowerPlaceableItem;

import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Attacker;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class ThrumboHornPlaceableItem extends PetFollowerPlaceableItem {
   public ThrumboHornPlaceableItem() {
      super("petthrumbo", Item.Rarity.LEGENDARY);
   }

   @Override
   protected ListGameTooltips getAnimalTooltips(InventoryItem item, Attacker attacker) {
      ListGameTooltips tooltips = super.getAnimalTooltips(item, attacker);
      tooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "thrumbohornquote", null));
      return tooltips;
   }
}
