package necesse.engine.incursionPerkTree;

public class ImprovedEquipmentTiersThreePerk extends IncursionPerk {
   public ImprovedEquipmentTiersThreePerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public int setMaxEquipmentRewardTier() {
      return 10;
   }
}
