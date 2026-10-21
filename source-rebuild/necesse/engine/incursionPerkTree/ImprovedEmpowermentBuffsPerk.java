package necesse.engine.incursionPerkTree;

public class ImprovedEmpowermentBuffsPerk extends IncursionPerk {
   public ImprovedEmpowermentBuffsPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
