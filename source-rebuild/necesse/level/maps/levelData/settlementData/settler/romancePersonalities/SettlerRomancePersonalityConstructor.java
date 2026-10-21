package necesse.level.maps.levelData.settlementData.settler.romancePersonalities;

import java.lang.reflect.InvocationTargetException;
import necesse.entity.mobs.friendly.human.HumanMob;

@FunctionalInterface
public interface SettlerRomancePersonalityConstructor {
   SettlerRomancePersonality construct(HumanMob var1) throws InstantiationException, IllegalAccessException, InvocationTargetException;
}
