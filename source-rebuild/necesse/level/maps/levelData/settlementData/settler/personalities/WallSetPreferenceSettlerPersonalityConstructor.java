package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.lang.reflect.InvocationTargetException;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.presets.set.WallSet;

public class WallSetPreferenceSettlerPersonalityConstructor implements SettlerPersonalityConstructor {
   public WallSet wallSet;

   public WallSetPreferenceSettlerPersonalityConstructor(WallSet wallSet) {
      this.wallSet = wallSet;
   }

   @Override
   public SettlerPersonality construct(HumanMob mob) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      return new WallSetPreferenceSettlerPersonality(mob, this.wallSet);
   }
}
