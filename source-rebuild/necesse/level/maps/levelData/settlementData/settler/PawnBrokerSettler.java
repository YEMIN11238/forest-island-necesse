package necesse.level.maps.levelData.settlementData.settler;

import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

public class PawnBrokerSettler extends Settler {
   public PawnBrokerSettler() {
      super("pawnbrokerhuman");
      this.isPartOfCompleteHost = false;
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(new InventoryItem("tophat"));
      drawOptions.chestplate(new InventoryItem("blazer"));
      drawOptions.boots(new InventoryItem("dressshoes"));
   }
}
