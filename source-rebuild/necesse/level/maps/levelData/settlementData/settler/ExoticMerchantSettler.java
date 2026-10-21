package necesse.level.maps.levelData.settlementData.settler;

import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

public class ExoticMerchantSettler extends Settler {
   public ExoticMerchantSettler() {
      super("exoticmerchanthuman");
      this.isPartOfCompleteHost = false;
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("turban"));
      drawOptions.chestplate(new InventoryItem("exoticshirt"));
      drawOptions.boots(new InventoryItem("exoticshoes"));
   }
}
