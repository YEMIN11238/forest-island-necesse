package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;

public class VoyagerSettlerPersonality extends SettlerPersonality {
   public static int PERCENT_MODIFIER_PER_QUALITY_OF_LIFE = 15;

   public VoyagerSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public GameMessage getDescription() {
      return new LocalMessage("personalities", this.getStringID() + "_desc")
         .addReplacement("name", GameColor.ITEM_QUEST.getColorCode() + this.mob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
         .addReplacement("percent", GameColor.GREEN.getColorCode() + PERCENT_MODIFIER_PER_QUALITY_OF_LIFE + "%" + GameColor.NO_COLOR.getColorCode());
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips list = new ListGameTooltips();
      list.add(super.getTooltip());
      int percentFaster = this.getCurrentPercentFaster();
      GameColor color = percentFaster > 0 ? GameColor.GREEN : GameColor.YELLOW;
      LocalMessage effectMessage = new LocalMessage("personalities", "voyager_current").addReplacement("percent", percentFaster + "%");
      GameMessageBuilder coloredMessage = new GameMessageBuilder().append(color.getColorCode()).append(effectMessage).append(GameColor.NO_COLOR.getColorCode());
      list.add(new SpacerGameTooltip(10));
      list.add(new LocalMessage("personalities", "currentbonus"));
      list.add(coloredMessage);
      return list;
   }

   public int getCurrentPercentFaster() {
      return PERCENT_MODIFIER_PER_QUALITY_OF_LIFE * this.mob.getSettlerQualityOfLife();
   }

   @Override
   public float getExpeditionTimeModifier() {
      return 1.0F / (1.0F + this.getCurrentPercentFaster() / 100.0F);
   }
}
