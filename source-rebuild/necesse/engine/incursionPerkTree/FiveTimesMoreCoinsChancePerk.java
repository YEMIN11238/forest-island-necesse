package necesse.engine.incursionPerkTree;

public class FiveTimesMoreCoinsChancePerk extends IncursionPerk {
   public FiveTimesMoreCoinsChancePerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }
}
