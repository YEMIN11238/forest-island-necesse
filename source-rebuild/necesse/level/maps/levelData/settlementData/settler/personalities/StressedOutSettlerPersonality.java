package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.JobTypeRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.JobType;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class StressedOutSettlerPersonality extends SettlerPersonality {
   public static int DAY_OFF_INTERVAL = 5;
   public int modDayOff;

   public StressedOutSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void init() {
      super.init();
      this.modDayOff = new GameRandom(this.mob.getSettlerSeed()).nextSeeded(63).nextInt(DAY_OFF_INTERVAL);
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips list = new ListGameTooltips();
      list.add(super.getTooltip());
      list.add(new SpacerGameTooltip(10));
      int daysUntil = this.getDaysUntilNextDayOff();
      GameMessage whenMessage;
      if (daysUntil == 0) {
         whenMessage = new LocalMessage("personalities", "stressedoutnow");
      } else {
         whenMessage = new LocalMessage("personalities", "stressedoutsoon", "value", daysUntil);
      }

      GameMessage nextDayMessage = new LocalMessage("personalities", "stressedoutnext")
         .addReplacement(
            "when", new GameMessageBuilder().append(GameColor.ITEM_QUEST.getColorCode()).append(whenMessage).append(GameColor.NO_COLOR.getColorCode())
         );
      list.add(nextDayMessage.translate());
      return list;
   }

   @Override
   public GameMessage getDescription() {
      return new LocalMessage("personalities", this.getStringID() + "_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement("value", "" + DAY_OFF_INTERVAL);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleSettlerThought("personalities", "stressedouthappy", 20).setPersonalitySubmitter(this)
      );
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      list.addThought(this.getStringID() + "_happy");
      return list;
   }

   public int getDaysUntilNextDayOff() {
      int currentDay = this.mob.getWorldEntity().getDay() % DAY_OFF_INTERVAL;
      int daysUntilNextDayOff = this.modDayOff - currentDay;
      if (daysUntilNextDayOff < 0) {
         daysUntilNextDayOff += DAY_OFF_INTERVAL;
      }

      return daysUntilNextDayOff;
   }

   public boolean isDayOffActive() {
      return this.mob.getWorldEntity().getDay() % DAY_OFF_INTERVAL == this.modDayOff;
   }

   @Override
   public GameMessage getIdleActivityMessage() {
      return this.isDayOffActive() ? new LocalMessage("personalities", "stressedoutidle") : super.getIdleActivityMessage();
   }

   @Override
   public boolean allowsFindingJobType(JobType jobType) {
      return !this.isDayOffActive()
         ? super.allowsFindingJobType(jobType)
         : jobType.getID() == JobTypeRegistry.recreationID || jobType.getID() == JobTypeRegistry.needsID;
   }
}
