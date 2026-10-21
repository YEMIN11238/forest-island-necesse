package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.level.maps.levelData.settlementData.settler.ActiveSettlerThoughtsManager;
import necesse.level.maps.levelData.settlementData.settler.thoughts.InspiringHappinessPersonalityThought;

public class InspiringSettlerPersonality extends SettlerPersonality {
   public static int SECONDS_THOUGHT_DURATION = 3600;
   public static int BASE_HAPPINESS_BONUS = 10;
   public static int PERCENT_MODIFIER_PER_QUALITY_OF_LIFE = 50;

   public InspiringSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      SettlerThoughtRegistry.registerSettlerThought(this.getStringID() + "_received", new InspiringHappinessPersonalityThought().setPersonalitySubmitter(this));
   }

   @Override
   public GameMessage getDescription() {
      return new LocalMessage("personalities", this.getStringID() + "_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement("value", GameColor.GREEN.getColorCode() + "+" + BASE_HAPPINESS_BONUS + GameColor.NO_COLOR.getColorCode())
         .addReplacement("percent", GameColor.GREEN.getColorCode() + "+" + PERCENT_MODIFIER_PER_QUALITY_OF_LIFE + "%" + GameColor.NO_COLOR.getColorCode());
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips list = new ListGameTooltips();
      list.add(super.getTooltip());
      int stacks = this.mob.getSettlerQualityOfLife() + 1;
      GameMessage effectMessage = new GameMessageBuilder()
         .append(GameColor.GREEN.getColorCode())
         .append(new LocalMessage("personalities", "inspiring_effect", "value", "+" + getCurrentHappinessBonus(stacks)))
         .append(GameColor.NO_COLOR.getColorCode());
      list.add(new SpacerGameTooltip(10));
      list.add(new LocalMessage("personalities", "currentbonus"));
      list.add(effectMessage.translate());
      return list;
   }

   public static int getCurrentHappinessBonus(int stacks) {
      float modifier = PERCENT_MODIFIER_PER_QUALITY_OF_LIFE * Math.max(stacks - 1, 0) / 100.0F;
      return (int)(BASE_HAPPINESS_BONUS * (1.0F + modifier));
   }

   @Override
   public void onConversationEnded(Mob otherChatter) {
      super.onConversationEnded(otherChatter);
      if (otherChatter instanceof HumanMob) {
         ActiveSettlerThoughtsManager otherChatterActiveThoughts = ((HumanMob)otherChatter).getActiveThoughts();
         String thoughtID = this.getStringID() + "_received";
         int stacks = this.mob.getSettlerQualityOfLife() + 1;
         if (otherChatterActiveThoughts.hasThought(thoughtID)) {
            if (otherChatterActiveThoughts.getThought(thoughtID).getStacks() <= stacks) {
               otherChatterActiveThoughts.removeThought(thoughtID);

               for (int i = 0; i < stacks; i++) {
                  otherChatterActiveThoughts.addThoughtSeconds(thoughtID, SECONDS_THOUGHT_DURATION);
               }
            }
         } else {
            for (int i = 0; i < stacks; i++) {
               otherChatterActiveThoughts.addThoughtSeconds(thoughtID, SECONDS_THOUGHT_DURATION);
            }
         }
      }
   }
}
