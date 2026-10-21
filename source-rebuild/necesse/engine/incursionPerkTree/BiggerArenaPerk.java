package necesse.engine.incursionPerkTree;

public class BiggerArenaPerk extends IncursionPerk {
   public BiggerArenaPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
