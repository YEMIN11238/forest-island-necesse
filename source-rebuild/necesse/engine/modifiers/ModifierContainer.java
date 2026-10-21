package necesse.engine.modifiers;

import java.util.HashSet;
import java.util.LinkedList;

public class ModifierContainer {
   protected final ModifierList list;
   private final Object[] modifiers;
   private final ModifierContainerLimits[] limits;
   private final HashSet<Integer> nonDefaultModifiers;
   private final HashSet<Integer> nonDefaultLimits;

   public ModifierContainer(ModifierList list) {
      this.list = list;
      this.modifiers = new Object[list.getModifierCount()];
      this.limits = new ModifierContainerLimits[list.getModifierCount()];

      for (int i = 0; i < this.limits.length; i++) {
         this.limits[i] = new ModifierContainerLimits();
      }

      this.nonDefaultModifiers = new HashSet<>();
      this.nonDefaultLimits = new HashSet<>();
      this.resetDefaultModifiers();
   }

   public void resetDefaultModifiers() {
      for (int i = 0; i < this.modifiers.length; i++) {
         this.modifiers[i] = this.list.getModifier(i).defaultBuffValue;
      }

      this.nonDefaultModifiers.clear();
   }

   public <T> T applyModifierUnlimited(Modifier<T> modifier, T currentValue) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return modifier.appendManager(currentValue, (T)this.modifiers[modifier.index], this.getStacks());
      }
   }

   public <T> T applyModifierLimited(Modifier<T> modifier, T currentValue) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return modifier.finalLimit(modifier.appendManager(currentValue, (T)this.modifiers[modifier.index], this.getStacks()));
      }
   }

   @Deprecated
   public <T> T applyModifier(Modifier<T> modifier, T currentValue) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return modifier.finalLimit(modifier.appendManager(currentValue, (T)this.modifiers[modifier.index], this.getStacks()));
      }
   }

   public int getStacks() {
      return 1;
   }

   public <T> void setModifier(Modifier<T> modifier, T value) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      }

      this.modifiers[modifier.index] = value;
      if (value == modifier.defaultBuffValue) {
         this.nonDefaultModifiers.remove(modifier.index);
      } else {
         this.nonDefaultModifiers.add(modifier.index);
      }
   }

   public <T> T applyModifierLimits(Modifier<T> modifier, T currentValue) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return (T)this.limits[modifier.index].applyModifierLimits(modifier, currentValue);
      }
   }

   public <T> void addModifier(Modifier<T> modifier, T value, int count) {
      this.setModifier(modifier, modifier.appendManager((T)this.modifiers[modifier.index], value, count));
   }

   public final <T> void addModifier(Modifier<T> modifier, T value) {
      this.addModifier(modifier, value, 1);
   }

   public <T> void addModifierLimits(Modifier<T> modifier, ModifierContainerLimits<T> limits) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      }

      this.limits[modifier.index].combine(modifier, limits);
      this.nonDefaultLimits.add(modifier.index);
   }

   public <T> void setMaxModifier(Modifier<T> modifier, T value, int priority) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      }

      if (!this.limits[modifier.index].hasMax) {
         this.nonDefaultLimits.add(modifier.index);
      }

      this.limits[modifier.index].max = modifier.finalLimit(value);
      this.limits[modifier.index].hasMax = true;
      this.limits[modifier.index].maxPriority = priority;
   }

   public <T> void setMaxModifier(Modifier<T> modifier, T value) {
      this.setMaxModifier(modifier, value, 0);
   }

   public <T> void clearMaxModifier(Modifier<T> modifier) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      }

      this.nonDefaultLimits.remove(modifier.index);
      this.limits[modifier.index].hasMax = false;
   }

   public <T> void setMinModifier(Modifier<T> modifier, T value, int priority) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      }

      if (!this.limits[modifier.index].hasMin) {
         this.nonDefaultLimits.add(modifier.index);
      }

      this.limits[modifier.index].min = modifier.finalLimit(value);
      this.limits[modifier.index].hasMin = true;
      this.limits[modifier.index].minPriority = priority;
   }

   public <T> void setMinModifier(Modifier<T> modifier, T value) {
      this.setMinModifier(modifier, value, 0);
   }

   public <T> void clearMinModifier(Modifier<T> modifier) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      }

      this.nonDefaultLimits.remove(modifier.index);
      this.limits[modifier.index].hasMin = false;
   }

   public <T> T getModifier(Modifier<T> modifier) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return (T)this.modifiers[modifier.index];
      }
   }

   public <T> ModifierContainerLimits<T> getLimits(Modifier<T> modifier) {
      if (this.list != modifier.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return this.limits[modifier.index];
      }
   }

   public Iterable<Integer> getNonDefaultModifierIndexes() {
      return this.nonDefaultModifiers;
   }

   public Iterable<Integer> getNonDefaultLimitIndexes() {
      return this.nonDefaultLimits;
   }

   public void onUpdate() {
   }

   public Modifier<?> getModifierByIndex(int index) {
      return this.list.getModifier(index);
   }

   public LinkedList<ModifierTooltip> getModifierTooltips() {
      return this.getModifierTooltips(null, true, true);
   }

   public LinkedList<ModifierTooltip> getModifierTooltips(ModifierContainer lastValues) {
      return this.getModifierTooltips(lastValues, true, true);
   }

   public LinkedList<ModifierTooltip> getModifierTooltips(ModifierContainer lastValues, boolean addModifiers, boolean addLimits) {
      return this.getModifierTooltips(lastValues, addModifiers, addLimits, null, null);
   }

   public ModifierTipsBuilder getModifierTooltipsBuilder(boolean addModifiers, boolean addLimits) {
      return new ModifierTipsBuilder(this, addModifiers, addLimits);
   }

   public LinkedList<ModifierTooltip> getModifierTooltips(
      ModifierContainer lastValues, boolean addModifiers, boolean addLimits, HashSet<Modifier> exceptModifiers, HashSet<Modifier> exceptLimits
   ) {
      LinkedList<ModifierTooltip> out = new LinkedList<>();
      this.addModifierTooltips(out, lastValues, addModifiers, addLimits, exceptModifiers, exceptLimits);
      return out;
   }

   public void addModifierTooltips(
      LinkedList<ModifierTooltip> list,
      ModifierContainer lastValues,
      boolean addModifiers,
      boolean addLimits,
      HashSet<Modifier> exceptModifiers,
      HashSet<Modifier> exceptLimits
   ) {
      if (addModifiers || addLimits) {
         for (Modifier modifier : this.list) {
            if (addModifiers && (exceptModifiers == null || !exceptModifiers.contains(modifier))) {
               ModifierTooltip tooltip = this.getModifierTooltip(modifier, lastValues);
               if (tooltip != null) {
                  list.add(tooltip);
               }
            }

            if (addLimits && (exceptLimits == null || !exceptLimits.contains(modifier))) {
               ModifierContainerLimits limiter = this.limits[modifier.index];
               if (limiter.hasMin()) {
                  ModifierTooltip minTooltip = this.getMinLimitTooltip(modifier, lastValues, false);
                  if (minTooltip != null) {
                     list.add(minTooltip);
                  }
               }

               if (limiter.hasMax()) {
                  ModifierTooltip maxTooltip = this.getMaxLimitTooltip(modifier, lastValues, false);
                  if (maxTooltip != null) {
                     list.add(maxTooltip);
                  }
               }
            }
         }
      }
   }

   public ModifierTooltip getModifierTooltip(Modifier modifier, ModifierContainer lastValues) {
      return modifier.getTooltip(this.modifiers[modifier.index], lastValues != null ? lastValues.modifiers[modifier.index] : null, modifier.defaultBuffValue);
   }

   public ModifierTooltip getMinLimitTooltip(Modifier modifier, ModifierContainer lastValues, boolean forced) {
      ModifierContainerLimits limiter = this.limits[modifier.index];
      return !limiter.hasMin() && !forced
         ? null
         : modifier.getMinTooltip(limiter.min(), lastValues != null ? lastValues.limits[modifier.index].min() : null, null);
   }

   public ModifierTooltip getMaxLimitTooltip(Modifier modifier, ModifierContainer lastValues, boolean forced) {
      ModifierContainerLimits limiter = this.limits[modifier.index];
      return !limiter.hasMax() && !forced
         ? null
         : modifier.getMaxTooltip(limiter.max(), lastValues != null ? lastValues.limits[modifier.index].max() : null, null);
   }
}
