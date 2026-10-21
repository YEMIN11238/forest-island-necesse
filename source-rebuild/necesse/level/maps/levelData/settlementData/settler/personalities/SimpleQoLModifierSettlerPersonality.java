package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierContainer;
import necesse.engine.modifiers.ModifierTooltip;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.FairTypeTooltip;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class SimpleQoLModifierSettlerPersonality extends SettlerPersonality {
   public ArrayList<SimpleQoLModifierSettlerPersonality.QoLModifierValue> modifiers = new ArrayList<>();

   public SimpleQoLModifierSettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void init() {
      super.init();
      this.modifiers.sort(Comparator.comparingInt(m -> m.minQualityOfLifeLevel));
   }

   @Override
   public GameTooltips getTooltip() {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(super.getTooltip());
      tooltips.add(new SpacerGameTooltip(5));
      int qualityOfLifeLevel = this.mob.getSettlerQualityOfLife();
      if (!this.modifiers.isEmpty()) {
         tooltips.add(new SpacerGameTooltip(5));
         ModifierContainer allModifiers = new ModifierContainer(BuffModifiers.LIST);

         for (SimpleQoLModifierSettlerPersonality.QoLModifierValue modifier : this.modifiers) {
            tooltips.add(new SpacerGameTooltip(5));
            if (modifier.maxQualityOfLifeLevel > modifier.minQualityOfLifeLevel) {
               tooltips.add(
                  new LocalMessage("personalities", "atqolrange")
                     .addReplacement("low", Settler.getQualityOfLife(modifier.minQualityOfLifeLevel).getAdjective())
                     .addReplacement("high", Settler.getQualityOfLife(modifier.maxQualityOfLifeLevel).getAdjective())
               );
            } else if (modifier.maxQualityOfLifeLevel == modifier.minQualityOfLifeLevel) {
               tooltips.add(
                  new LocalMessage("personalities", "atqolonly")
                     .addReplacement("quality", Settler.getQualityOfLife(modifier.minQualityOfLifeLevel).getAdjective())
               );
            } else if (modifier.stackEachLevelAbove) {
               tooltips.add(
                  new LocalMessage("personalities", "perqollevel")
                     .addReplacement("quality", Settler.getQualityOfLife(modifier.minQualityOfLifeLevel).getAdjective())
               );
            } else {
               tooltips.add(
                  new LocalMessage("personalities", "atqolorbetter")
                     .addReplacement("quality", Settler.getQualityOfLife(modifier.minQualityOfLifeLevel).getAdjective())
               );
            }

            int stacks = modifier.getStacks(qualityOfLifeLevel);

            for (ModifierValue<?> modifierValue : modifier.modifierValues) {
               ModifierTooltip tooltip = modifierValue.getTooltip(true);
               FontOptions fontOptions = new FontOptions(Settings.tooltipTextSize).outline();
               if (stacks <= 0) {
                  fontOptions = fontOptions.color(GameColor.GRAY.color.get());
               } else {
                  fontOptions = fontOptions.color(tooltip.getTooltipColor());
               }

               tooltips.add(new FairTypeTooltip(tooltip.toFairType(fontOptions, false)));
               if (stacks > 0) {
                  modifierValue.stack(stacks).add(allModifiers);
               }
            }

            this.addAdditionalModifierTooltip(tooltips, modifier);
         }

         tooltips.add(new SpacerGameTooltip(15));
         tooltips.add(new LocalMessage("personalities", "currentbonus"));
         ListGameTooltips currentBonusTooltips = new ListGameTooltips();

         for (ModifierTooltip tooltip : allModifiers.getModifierTooltips()) {
            currentBonusTooltips.add(tooltip.toTooltip(true));
         }

         this.addAdditionalCurrentBonusTooltip(currentBonusTooltips);
         if (currentBonusTooltips.isEmpty()) {
            currentBonusTooltips.add(new StringTooltips(Localization.translate("personalities", "nobonus"), GameColor.YELLOW));
         }

         tooltips.add(currentBonusTooltips);
      }

      return tooltips;
   }

   public void addAdditionalModifierTooltip(ListGameTooltips tooltips, SimpleQoLModifierSettlerPersonality.QoLModifierValue modifier) {
   }

   public void addAdditionalCurrentBonusTooltip(ListGameTooltips tooltips) {
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      int qualityOfLifeLevel = this.mob.getSettlerQualityOfLife();
      return this.modifiers.stream().flatMap(modifier -> {
         int stacks = modifier.getStacks(qualityOfLifeLevel);
         return stacks > 0 ? modifier.modifierValues.stream().map(value -> value.stack(stacks)) : Stream.empty();
      });
   }

   public static class QoLModifierValue {
      public final int minQualityOfLifeLevel;
      public final int maxQualityOfLifeLevel;
      public final boolean stackEachLevelAbove;
      public final List<ModifierValue<?>> modifierValues;

      public QoLModifierValue(int minQualityOfLifeLevel, int maxQualityOfLifeLevel, boolean stackEachLevelAbove, ModifierValue<?>... modifierValues) {
         this.minQualityOfLifeLevel = minQualityOfLifeLevel;
         this.maxQualityOfLifeLevel = maxQualityOfLifeLevel;
         this.stackEachLevelAbove = stackEachLevelAbove;
         if (maxQualityOfLifeLevel >= minQualityOfLifeLevel && stackEachLevelAbove) {
            throw new IllegalArgumentException(
               "QoLModifierValue can't have maxQualityOfLifeLevel and also stackEachLevelAbove. Set maxQualityOfLifeLevel to -1 or stackEachLevelAbove to false"
            );
         }

         this.modifierValues = Arrays.asList(modifierValues);
      }

      public int getStacks(int qualityOfLifeLevel) {
         if (qualityOfLifeLevel >= this.minQualityOfLifeLevel && (this.maxQualityOfLifeLevel < 0 || qualityOfLifeLevel <= this.maxQualityOfLifeLevel)) {
            return this.stackEachLevelAbove ? qualityOfLifeLevel - this.minQualityOfLifeLevel + 1 : 1;
         } else {
            return 0;
         }
      }
   }
}
