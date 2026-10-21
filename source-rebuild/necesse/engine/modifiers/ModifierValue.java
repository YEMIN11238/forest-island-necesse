package necesse.engine.modifiers;

import java.util.ArrayList;
import java.util.Collection;

public class ModifierValue<T> {
   public final Modifier<T> modifier;
   public final T value;
   public final ModifierContainerLimits<T> limits;

   public ModifierValue(Modifier<T> modifier, T value) {
      this.modifier = modifier;
      this.value = value;
      this.limits = new ModifierContainerLimits<>();
   }

   public ModifierValue(Modifier<T> modifier) {
      this(modifier, modifier.defaultBuffValue);
   }

   public ModifierValue<T> min(T min, int priority) {
      this.limits.min = min;
      this.limits.hasMin = true;
      this.limits.minPriority = priority;
      return this;
   }

   public ModifierValue<T> min(T min) {
      return this.min(min, 0);
   }

   public ModifierValue<T> max(T max, int priority) {
      this.limits.max = max;
      this.limits.hasMax = true;
      this.limits.maxPriority = priority;
      return this;
   }

   public ModifierValue<T> max(T max) {
      return this.max(max, 0);
   }

   public void apply(ModifierContainer container) {
      container.setModifier(this.modifier, this.value);
      container.addModifierLimits(this.modifier, this.limits);
   }

   public void add(ModifierContainer container) {
      container.addModifier(this.modifier, this.value);
      container.addModifierLimits(this.modifier, this.limits);
   }

   public ModifierValue<T> stack(int stacks) {
      if (stacks == 0) {
         return new ModifierValue<>(this.modifier, this.modifier.defaultBuffValue);
      }

      T newValue = this.modifier.appendManager(this.modifier.defaultBuffValue, this.value, stacks);
      ModifierValue<T> newModifier = new ModifierValue<>(this.modifier, newValue);
      newModifier.limits.min = this.limits.min;
      newModifier.limits.hasMin = this.limits.hasMin;
      newModifier.limits.minPriority = this.limits.minPriority;
      newModifier.limits.max = this.limits.max;
      newModifier.limits.hasMax = this.limits.hasMax;
      newModifier.limits.maxPriority = this.limits.maxPriority;
      return newModifier;
   }

   public ModifierTooltip getManagerTooltip() {
      return this.modifier.getTooltip(this.value, this.modifier.defaultBuffManagerValue);
   }

   public ModifierTooltip getTooltip(boolean forceNonNull) {
      return this.modifier.getTooltip(this.value, null, this.modifier.defaultBuffValue, forceNonNull);
   }

   public ModifierTooltip getTooltip() {
      return this.modifier.getTooltip(this.value, this.modifier.defaultBuffValue);
   }

   public ModifierTooltip getMaxTooltip() {
      return !this.limits.hasMax ? null : this.modifier.getMaxTooltip(this.limits.max);
   }

   public ModifierTooltip getMinTooltip() {
      return !this.limits.hasMin ? null : this.modifier.getMinTooltip(this.limits.min);
   }

   public Collection<ModifierTooltip> getAllTooltips() {
      ArrayList<ModifierTooltip> list = new ArrayList<>(3);
      ModifierTooltip tooltip = this.getTooltip();
      if (tooltip != null) {
         list.add(tooltip);
      }

      ModifierTooltip maxTooltip = this.getMaxTooltip();
      if (maxTooltip != null) {
         list.add(maxTooltip);
      }

      ModifierTooltip minTooltip = this.getMinTooltip();
      if (minTooltip != null) {
         list.add(minTooltip);
      }

      return list;
   }
}
