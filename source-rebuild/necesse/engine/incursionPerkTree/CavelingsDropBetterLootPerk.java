package necesse.engine.incursionPerkTree;

public class CavelingsDropBetterLootPerk extends IncursionPerk {
   public CavelingsDropBetterLootPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
