package necesse.inventory.item.armorItem.cosmetics.misc;

import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.ArmorBuff;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;

public class StealthBasketArmorItem extends SetHelmetArmorItem {
   public StealthBasketArmorItem() {
      super(5, null, 0, null, null, Item.Rarity.UNCOMMON, "stealthbaskethelmet", null, null, null);
      this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.NO_FACIAL_FEATURE;
      this.canBeUsedForRaids = false;
   }

   @Override
   public ArmorBuff[] getBuffs(InventoryItem item) {
      return new ArmorBuff[]{BuffRegistry.ArmorBuffs.STEALTH_BASKET};
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips preEnchantmentTooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      preEnchantmentTooltips.add(Localization.translate("itemtooltip", "stealthbaskettip"), 350);
      preEnchantmentTooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "stealthbasketquote", "Bo"));
      return preEnchantmentTooltips;
   }
}
