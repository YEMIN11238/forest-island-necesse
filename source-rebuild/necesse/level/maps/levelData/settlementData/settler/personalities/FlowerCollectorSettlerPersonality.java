package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.recipe.Ingredient;

public class FlowerCollectorSettlerPersonality extends CollectorSettlerPersonality {
   public FlowerCollectorSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public Ingredient[] getCollectorCost() {
      return new Ingredient[]{new Ingredient("anyflower", 1)};
   }
}
