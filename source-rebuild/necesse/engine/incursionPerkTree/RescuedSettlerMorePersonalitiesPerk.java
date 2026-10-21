package necesse.engine.incursionPerkTree;

public class RescuedSettlerMorePersonalitiesPerk extends IncursionPerk {
   public RescuedSettlerMorePersonalitiesPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }
}
