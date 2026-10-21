package necesse.engine.incursionPerkTree;

public class IncreasedBossLootPerk extends IncursionPerk {
   public IncreasedBossLootPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
