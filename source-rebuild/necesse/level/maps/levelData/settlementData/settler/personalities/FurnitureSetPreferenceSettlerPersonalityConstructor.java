package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.lang.reflect.InvocationTargetException;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.presets.set.FurnitureSet;

public class FurnitureSetPreferenceSettlerPersonalityConstructor implements SettlerPersonalityConstructor {
   public FurnitureSet furnitureSet;

   public FurnitureSetPreferenceSettlerPersonalityConstructor(FurnitureSet furnitureSet) {
      this.furnitureSet = furnitureSet;
   }

   @Override
   public SettlerPersonality construct(HumanMob mob) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      return new FurnitureSetPreferenceSettlerPersonality(mob, this.furnitureSet);
   }
}
