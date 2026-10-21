package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.recipe.Ingredient;

public class HiredHandCollectorSettlerPersonality extends CollectorSettlerPersonality {
   public HiredHandCollectorSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public Ingredient[] getCollectorCost() {
      return new Ingredient[]{new Ingredient("coin", 50)};
   }
}
