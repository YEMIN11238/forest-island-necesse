package necesse.inventory.item.armorItem.cosmetics.syndicatethug;

import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;

public class SyndicateThugHoodArmorItem extends SetHelmetArmorItem {
   public SyndicateThugHoodArmorItem() {
      super(0, null, 0, null, null, Item.Rarity.COMMON, "syndicatethughood", "syndicatethugshirt", "syndicatethugboots", null);
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.hairMaskTextureName = "snowhood_leatherhood_hairmask";
   }
}
