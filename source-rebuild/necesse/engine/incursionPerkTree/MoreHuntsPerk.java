package necesse.engine.incursionPerkTree;

import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.level.maps.incursion.IncursionDataModifiers;

public class MoreHuntsPerk extends IncursionPerk {
   public MoreHuntsPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public Stream<ModifierValue<?>> getIncursionDataModifiers() {
      return Stream.of(new ModifierValue<>(IncursionDataModifiers.MODIFIER_HUNT_DROPS, 1.0F));
   }
}
