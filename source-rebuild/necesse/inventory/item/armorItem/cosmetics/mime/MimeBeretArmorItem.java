package necesse.inventory.item.armorItem.cosmetics.mime;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;

public class MimeBeretArmorItem extends SetHelmetArmorItem {
   public MimeBeretArmorItem() {
      super(0, null, 0, null, null, Item.Rarity.UNCOMMON, "mimeberet", "mimeshirt", "mimeboots", null);
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.hairMaskTextureName = "mimeberet_hairmask";
   }

   @Override
   protected ListGameTooltips getBaseTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips baseTooltips = super.getBaseTooltips(item, perspective, blackboard);
      baseTooltips.add(GameUtils.getDevQuoteTooltip(new LocalMessage("itemtooltip", "mimesetquote"), "Stormes", 300));
      return baseTooltips;
   }
}
