package necesse.engine.incursionPerkTree;

public class CratesDropAltarDustPerk extends IncursionPerk {
   public CratesDropAltarDustPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
