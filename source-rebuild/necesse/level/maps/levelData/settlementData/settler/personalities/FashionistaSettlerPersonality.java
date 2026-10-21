package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryRange;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class FashionistaSettlerPersonality extends SettlerPersonality {
   public boolean satisfied;

   public FashionistaSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_yes", new SimpleSettlerThought("personalities", "hascosmeticset", 30).setPersonalitySubmitter(this)
      );
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_no", new SimpleSettlerThought("personalities", "missingcosmeticset", 0).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void init() {
      super.init();
      this.updateSatisfied();
   }

   @Override
   public void onEquipmentChanged(InventoryRange inventoryRange, int slot, boolean isCosmetic) {
      super.onEquipmentChanged(inventoryRange, slot, isCosmetic);
      if (isCosmetic) {
         this.updateSatisfied();
      }
   }

   public void updateSatisfied() {
      this.satisfied = true;

      for (int slot = 3; slot <= 5; slot++) {
         if (this.mob.equipmentInventory.getItem(slot) == null) {
            this.satisfied = false;
            return;
         }
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      if (this.satisfied) {
         list.addThought(this.getStringID() + "_yes");
      } else {
         list.addThought(this.getStringID() + "_no");
      }

      return list;
   }
}
