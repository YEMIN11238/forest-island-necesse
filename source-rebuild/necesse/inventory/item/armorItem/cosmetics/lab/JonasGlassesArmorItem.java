package necesse.inventory.item.armorItem.cosmetics.lab;

import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;

public class JonasGlassesArmorItem extends SetHelmetArmorItem {
   public JonasGlassesArmorItem() {
      super(0, null, 0, null, null, Item.Rarity.UNIQUE, "jonasglasses", null, null, null);
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(GameUtils.getDevQuoteTooltip(Localization.translate("itemtooltip", "jonasglassesquote"), "Jonas"));
      return tooltips;
   }
}
