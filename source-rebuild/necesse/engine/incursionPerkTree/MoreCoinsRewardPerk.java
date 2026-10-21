package necesse.engine.incursionPerkTree;

public class MoreCoinsRewardPerk extends IncursionPerk {
   public MoreCoinsRewardPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }
}
