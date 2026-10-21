package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.entity.mobs.friendly.human.HumanMob;

public abstract class SettlerPersonalityFilter {
   public abstract int getTickets(HumanMob var1);

   public abstract boolean canBeAppliedTo(HumanMob var1);

   public abstract boolean isExcludedBy(String var1);

   public abstract boolean excludesOther(String var1);
}
