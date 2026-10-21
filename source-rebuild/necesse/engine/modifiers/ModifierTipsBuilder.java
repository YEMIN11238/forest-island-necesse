package necesse.engine.modifiers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import necesse.inventory.item.ItemStatTip;

public class ModifierTipsBuilder {
   private ModifierContainer values;
   private ModifierContainer lastValues;
   private boolean addModifiers;
   private boolean addLimits;
   private HashSet<Modifier> excludeModifiers = new HashSet<>();
   private HashSet<Modifier> excludeLimits = new HashSet<>();

   public ModifierTipsBuilder(ModifierContainer values, boolean addModifiers, boolean addLimits) {
      this.values = values;
      this.addModifiers = addModifiers;
      this.addLimits = addLimits;
   }

   public ModifierTipsBuilder addLastValues(ModifierContainer lastValues) {
      this.lastValues = lastValues;
      return this;
   }

   public ModifierTipsBuilder excludeModifiers(Modifier... modifiers) {
      this.excludeModifiers.addAll(Arrays.asList(modifiers));
      return this;
   }

   public ModifierTipsBuilder excludeLimits(Modifier... modifiers) {
      this.excludeLimits.addAll(Arrays.asList(modifiers));
      return this;
   }

   public ModifierTipsBuilder exclude(Modifier... modifiers) {
      this.excludeModifiers(modifiers);
      this.excludeLimits(modifiers);
      return this;
   }

   public LinkedList<ModifierTooltip> build() {
      return this.values.getModifierTooltips(this.lastValues, this.addModifiers, this.addLimits, this.excludeModifiers, this.excludeLimits);
   }

   public void buildToList(LinkedList<ModifierTooltip> tooltips) {
      this.values.addModifierTooltips(tooltips, this.lastValues, this.addModifiers, this.addLimits, this.excludeModifiers, this.excludeLimits);
   }

   public LinkedList<ItemStatTip> buildStatTips() {
      LinkedList<ItemStatTip> statTips = new LinkedList<>();

      for (ModifierTooltip tooltip : this.values
         .getModifierTooltips(this.lastValues, this.addModifiers, this.addLimits, this.excludeModifiers, this.excludeLimits)) {
         statTips.add(tooltip.tip);
      }

      return statTips;
   }

   public void buildToStatList(LinkedList<ItemStatTip> statTips) {
      for (ModifierTooltip tooltip : this.values
         .getModifierTooltips(this.lastValues, this.addModifiers, this.addLimits, this.excludeModifiers, this.excludeLimits)) {
         statTips.add(tooltip.tip);
      }
   }
}
