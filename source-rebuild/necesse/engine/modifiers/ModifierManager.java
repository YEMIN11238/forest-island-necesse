package necesse.engine.modifiers;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Stream;
import necesse.engine.util.HashMapArrayList;

public abstract class ModifierManager<M extends ModifierContainer> {
   private final ModifierList list;
   private Object[] nonLimitedModifiers;
   private Object[] limitedModifiers;
   private ModifierContainerLimits[] limits;
   private HashSet<Integer> queryableModifiers;
   private HashMapArrayList<Modifier<?>, M> queryModifiers;

   public ModifierManager(ModifierList list) {
      this.list = list;
      this.nonLimitedModifiers = new Object[list.getModifierCount()];
      this.limitedModifiers = new Object[list.getModifierCount()];
      this.limits = new ModifierContainerLimits[list.getModifierCount()];

      for (int i = 0; i < this.limits.length; i++) {
         this.limits[i] = new ModifierContainerLimits();
      }

      this.queryableModifiers = new HashSet<>();
      this.queryModifiers = new HashMapArrayList<>();
   }

   protected void makeQueryable(Modifier modifier) {
      this.queryableModifiers.add(modifier.index);
   }

   protected Collection<M> queryContainers(Modifier modifier) {
      return this.queryModifiers.get(modifier);
   }

   protected void updateModifiers() {
      for (int i = 0; i < this.nonLimitedModifiers.length; i++) {
         this.nonLimitedModifiers[i] = this.list.getModifier(i).defaultBuffManagerValue;
         this.limits[i] = new ModifierContainerLimits();
      }

      HashMapArrayList<Modifier<?>, M> newQueryModifiers = new HashMapArrayList<>();

      for (M mc : this.getModifierContainers()) {
         mc.onUpdate();

         for (Integer i : mc.getNonDefaultModifierIndexes()) {
            Modifier modifier = this.list.getModifier(i);
            this.nonLimitedModifiers[i] = mc.applyModifierUnlimited(modifier, this.nonLimitedModifiers[i]);
            if (this.queryableModifiers.contains(i)) {
               newQueryModifiers.add(modifier, mc);
            }
         }

         for (Integer i : mc.getNonDefaultLimitIndexes()) {
            Modifier modifier = this.list.getModifier(i);
            this.limits[i].combine(modifier, mc.getLimits(modifier));
            if (this.queryableModifiers.contains(i)) {
               newQueryModifiers.add(modifier, mc);
            }
         }
      }

      this.getDefaultModifiers()
         .sequential()
         .map(e -> e)
         .forEach(
            modifierValue -> {
               this.nonLimitedModifiers[modifierValue.modifier.index] = ((Modifier<Object>)modifierValue.modifier)
                  .appendManager(this.nonLimitedModifiers[modifierValue.modifier.index], modifierValue.value);
               this.limits[modifierValue.modifier.index].combine(modifierValue.modifier, modifierValue.limits);
            }
         );

      for (int i = 0; i < this.limitedModifiers.length; i++) {
         this.limitedModifiers[i] = this.limits[i].applyModifierLimits(this.list.getModifier(i), this.nonLimitedModifiers[i]);
      }

      this.queryModifiers = newQueryModifiers;
   }

   protected abstract Iterable<? extends M> getModifierContainers();

   public <T> T getNonLimitedModifier(Modifier<T> modifier) {
      if (modifier.list != this.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return (T)this.nonLimitedModifiers[modifier.index];
      }
   }

   public <T> T getModifier(Modifier<T> modifier) {
      if (modifier.list != this.list) {
         throw new IllegalArgumentException("Modifier is not part of list.");
      } else {
         return (T)this.limitedModifiers[modifier.index];
      }
   }

   public <T> ModifierContainerLimits<T> getLimits(Modifier<T> modifier) {
      return this.limits[modifier.index];
   }

   @SafeVarargs
   public final <T> T applyModifiers(Modifier<T> modifier, T value, ModifierValue<T>... modifiers) {
      T out = value;

      for (ModifierValue<T> modValue : modifiers) {
         out = modifier.appendManager(out, modValue.value);
      }

      out = (T)this.limits[modifier.index].applyModifierLimits(modifier, out);

      for (ModifierValue<T> modValue : modifiers) {
         out = modValue.limits.applyModifierLimits(modifier, out);
      }

      return out;
   }

   @SafeVarargs
   public final <T> T getAndApplyModifiers(Modifier<T> modifier, ModifierValue<T>... modifiers) {
      return this.applyModifiers(modifier, this.getModifier(modifier), modifiers);
   }

   @SafeVarargs
   public final <T> T getAndApplyModifiers(Modifier<T> modifier, T... values) {
      ModifierValue<T>[] modifiers = new ModifierValue[values.length];

      for (int i = 0; i < values.length; i++) {
         modifiers[i] = new ModifierValue<>(modifier, values[i]);
      }

      return this.applyModifiers(modifier, this.getModifier(modifier), modifiers);
   }

   public LinkedList<ModifierTooltip> getModifierTooltips() {
      return this.getModifierTooltips(null);
   }

   public LinkedList<ModifierTooltip> getModifierTooltips(ModifierManager<M> lastValues) {
      LinkedList<ModifierTooltip> out = new LinkedList<>();

      for (Modifier modifier : this.list) {
         ModifierContainerLimits limiter = this.limits[modifier.index];
         if (limiter.hasMin()) {
            ModifierTooltip minTooltip = modifier.getMinTooltip(
               limiter.min(), lastValues != null ? lastValues.limits[modifier.index].min() : null, this.getNonLimitedModifier(modifier)
            );
            if (minTooltip != null) {
               out.add(minTooltip);
            }
         }

         if (limiter.hasMax()) {
            ModifierTooltip maxTooltip = modifier.getMaxTooltip(
               limiter.max(), lastValues != null ? lastValues.limits[modifier.index].max() : null, this.getNonLimitedModifier(modifier)
            );
            if (maxTooltip != null) {
               out.add(maxTooltip);
            }
         }

         ModifierTooltip tooltip = modifier.getTooltip(
            this.getModifier(modifier), lastValues != null ? lastValues.getModifier(modifier) : null, modifier.defaultBuffManagerValue
         );
         if (tooltip != null) {
            out.add(tooltip);
         }
      }

      return out;
   }

   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.empty();
   }
}
