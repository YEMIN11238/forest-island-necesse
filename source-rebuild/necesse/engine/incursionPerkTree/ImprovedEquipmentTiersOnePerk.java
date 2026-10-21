package necesse.engine.incursionPerkTree;

public class ImprovedEquipmentTiersOnePerk extends IncursionPerk {
   public ImprovedEquipmentTiersOnePerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public int setMaxEquipmentRewardTier() {
      return 5;
   }
}
