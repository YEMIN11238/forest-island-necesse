package necesse.level.maps.levelData.settlementData.settler.personalities;

import necesse.engine.localization.Localization;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;

public class MagicianSettlerPersonality extends SimpleQoLModifierSettlerPersonality {
   public static int MILLISECONDS_INVIS_COOLDOWN = 60000;
   public long lastInvisTime = 0L;

   public MagicianSettlerPersonality(HumanMob mob) {
      super(mob);
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(1, 1, false, new ModifierValue<>(BuffModifiers.PROJECTILE_BOUNCES, 1)));
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(2, 2, false, new ModifierValue<>(BuffModifiers.PROJECTILE_BOUNCES, 2)));
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(3, -1, false, new ModifierValue<>(BuffModifiers.PROJECTILE_BOUNCES, 3)));
      this.modifiers.add(new SimpleQoLModifierSettlerPersonality.QoLModifierValue(4, -1, true, new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.1F)));
   }

   @Override
   public void addAdditionalModifierTooltip(ListGameTooltips tooltips, SimpleQoLModifierSettlerPersonality.QoLModifierValue modifier) {
      super.addAdditionalModifierTooltip(tooltips, modifier);
      if (modifier.minQualityOfLifeLevel >= 3 && modifier.maxQualityOfLifeLevel == -1 && !modifier.stackEachLevelAbove) {
         tooltips.add(
            new StringTooltips(
               Localization.translate("personalities", "magician_invisible"), this.mob.getSettlerQualityOfLife() >= 3 ? GameColor.GREEN : GameColor.GRAY, 400
            )
         );
      }
   }

   @Override
   public void addAdditionalCurrentBonusTooltip(ListGameTooltips tooltips) {
      super.addAdditionalCurrentBonusTooltip(tooltips);
      if (this.mob.getSettlerQualityOfLife() >= 3) {
         tooltips.add(new StringTooltips(Localization.translate("personalities", "magician_invisible"), GameColor.GREEN, 400));
      }
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("lastInvisTime", this.lastInvisTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lastInvisTime = save.getLong("lastInvisTime", this.lastInvisTime, false);
   }

   @Override
   public void onMobWasHit(MobWasHitEvent event) {
      super.onMobWasHit(event);
      if (this.mob.getSettlerQualityOfLife() >= 3 && (this.lastInvisTime + MILLISECONDS_INVIS_COOLDOWN < this.mob.getTime() || this.lastInvisTime == 0L)) {
         this.lastInvisTime = this.mob.getTime();
         this.mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.MAGICIAN_INVIS, this.mob, 10.0F, this.mob), true);
      }
   }
}
