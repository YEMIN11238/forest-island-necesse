package necesse.engine.incursionPerkTree;

public class ChanceToDropDoubleTabletsPerk extends IncursionPerk {
   public ChanceToDropDoubleTabletsPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
