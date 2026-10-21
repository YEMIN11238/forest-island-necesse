package necesse.inventory.enchants;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.inventory.InventoryItem;

public interface Enchantable<T extends ItemEnchantment> {
   void setEnchantment(InventoryItem var1, int var2);

   int getEnchantmentID(InventoryItem var1);

   void clearEnchantment(InventoryItem var1);

   T getEnchantment(InventoryItem var1);

   boolean isValidEnchantment(InventoryItem var1, ItemEnchantment var2);

   Set<Integer> getValidEnchantmentIDs(InventoryItem var1);

   T getRandomEnchantment(GameRandom var1, InventoryItem var2);

   int getEnchantCost(InventoryItem var1);

   default int getRandomEnchantmentID(GameRandom random, InventoryItem item) {
      T out = this.getRandomEnchantment(random, item);
      return out == null ? 0 : out.getID();
   }

   default int getFinalEnchantCost(InventoryItem item) {
      return (int)(this.getEnchantCost(item) * this.getEnchantment(item).getEnchantCostMod());
   }

   default int getRandomEnchantCost(InventoryItem item, GameRandom random, int qualityOfLifeLevel, int happinessLevel) {
      return HumanShop.getRandomHappinessMiddlePrice(random, qualityOfLifeLevel, happinessLevel, this.getFinalEnchantCost(item), 6, 3);
   }

   default String getEnchantName(InventoryItem item) {
      return this.getEnchantment(item).getDisplayName();
   }

   default void addRandomEnchantment(InventoryItem item, GameRandom random) {
      this.setEnchantment(item, this.getRandomEnchantmentID(random, item));
   }

   default void addRandomEnchantment(InventoryItem item) {
      this.setEnchantment(item, this.getRandomEnchantment(item));
   }

   default int getRandomEnchantment(InventoryItem item) {
      return this.getRandomEnchantmentID(GameRandom.globalRandom, item);
   }

   default GameTooltips getEnchantmentTooltips(InventoryItem item) {
      return this.getEnchantment(item).getTooltips();
   }

   static <T extends ItemEnchantment> T getRandomEnchantment(
      GameRandom random, Set<Integer> setIDs, Predicate<Integer> filterIDs, Class<T> expectedClass, T defaultReturn
   ) {
      List<Integer> excluded = setIDs.stream().filter(filterIDs).collect(Collectors.toList());
      if (excluded.isEmpty()) {
         return null;
      }

      int id = excluded.get(random.nextInt(excluded.size()));
      return EnchantmentRegistry.getEnchantment(id, expectedClass, defaultReturn);
   }

   static <T extends ItemEnchantment> T getRandomEnchantment(GameRandom random, Set<Integer> setIDs, int excludeID, Class<T> expectedClass) {
      return getRandomEnchantment(random, setIDs, id -> id != excludeID, expectedClass, null);
   }
}
