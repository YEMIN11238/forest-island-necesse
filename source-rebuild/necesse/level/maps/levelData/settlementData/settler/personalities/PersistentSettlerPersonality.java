package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class PersistentSettlerPersonality extends SettlerPersonality {
   public final int deathDefyRechargeTime = 3600000;
   public long lastDeathTime;
   public int deathDefiesRemaining;

   public PersistentSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastDeathTime", this.lastDeathTime);
      save.addInt("deathDefiesRemaining", this.deathDefiesRemaining);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastDeathTime = save.getLong("lastDeathTime", this.lastDeathTime, false);
      this.deathDefiesRemaining = save.getInt("deathDefiesRemaining", this.deathDefiesRemaining, false);
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips list = new ListGameTooltips();
      list.add(super.getTooltip());
      int currentDefies = this.mob.getSettlerQualityOfLife();
      GameColor color = currentDefies <= 0 ? GameColor.YELLOW : GameColor.GREEN;
      GameMessageBuilder effectMessage = new GameMessageBuilder()
         .append(color.getColorCode())
         .append(new LocalMessage("personalities", "persistent_effect", "value", currentDefies))
         .append(GameColor.NO_COLOR.getColorCode());
      list.add(new SpacerGameTooltip(10));
      list.add(new LocalMessage("personalities", "currentbonus"));
      list.add(effectMessage);
      return list;
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      int maxDeathDefies = this.mob.getSettlerQualityOfLife();
      this.deathDefiesRemaining = Math.min(this.deathDefiesRemaining, maxDeathDefies);
      if (maxDeathDefies > 0 && (this.lastDeathTime == 0L || this.mob.getTime() - this.lastDeathTime >= 3600000L)) {
         this.deathDefiesRemaining = maxDeathDefies;
      }
   }

   @Override
   public void doBeforeHitCalculatedLogic(MobBeforeHitCalculatedEvent event) {
      super.doBeforeHitCalculatedLogic(event);
      if (!event.isPrevented() && event.getExpectedHealth() <= 0 && this.deathDefiesRemaining > 0) {
         event.prevent();
         this.mob.setHealth(this.mob.getMaxHealth());
         this.lastDeathTime = this.mob.getTime();
         this.deathDefiesRemaining--;
      }
   }
}
