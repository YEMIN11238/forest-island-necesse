package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;
import necesse.entity.mobs.friendly.human.HumanMob;

public class SimplePersonalityFilter extends SettlerPersonalityFilter {
   protected int tickets;
   protected boolean isSettlerStringIDsWhitelist = false;
   protected HashSet<String> settlerStringIDs = new HashSet<>();
   protected ArrayList<Predicate<HumanMob>> humanMobFilters = new ArrayList<>();
   protected HashSet<String> personalityStringIDs = new HashSet<>();

   public SimplePersonalityFilter(int tickets) {
      this.tickets = tickets;
   }

   public SimplePersonalityFilter makeSettlerStringIDsWhitelist() {
      this.isSettlerStringIDsWhitelist = true;
      return this;
   }

   public SimplePersonalityFilter filterSettlerStringID(String... settlerStringID) {
      this.settlerStringIDs.addAll(Arrays.asList(settlerStringID));
      return this;
   }

   public SimplePersonalityFilter addHumanMobFilter(Predicate<HumanMob> humanMobFilter) {
      this.humanMobFilters.add(humanMobFilter);
      return this;
   }

   public SimplePersonalityFilter filterPersonalityStringID(String... personalityStringIDs) {
      this.personalityStringIDs.addAll(Arrays.asList(personalityStringIDs));
      return this;
   }

   @Override
   public int getTickets(HumanMob mob) {
      return this.tickets;
   }

   @Override
   public boolean canBeAppliedTo(HumanMob mob) {
      return this.isSettlerStringIDsWhitelist
         ? this.settlerStringIDs.contains(mob.getSettlerStringID()) && this.humanMobFilters.stream().allMatch(filter -> filter.test(mob))
         : !this.settlerStringIDs.contains(mob.getSettlerStringID()) && this.humanMobFilters.stream().allMatch(filter -> filter.test(mob));
   }

   @Override
   public boolean isExcludedBy(String personalityStringID) {
      return this.personalityStringIDs.contains(personalityStringID);
   }

   @Override
   public boolean excludesOther(String personalityStringID) {
      return this.personalityStringIDs.contains(personalityStringID);
   }
}
