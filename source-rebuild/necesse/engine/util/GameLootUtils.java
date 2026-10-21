package necesse.engine.util;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.registries.ItemRegistry;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootItemInterface;

public class GameLootUtils {
   public static GameLootUtils.LootValueMap<InventoryItem> inventoryItemMapper = new GameLootUtils.LootValueMap<InventoryItem>() {
      public float getValuePerCount(InventoryItem object) {
         return object.item.getBrokerValue(object);
      }

      public int getRemainingCount(InventoryItem object) {
         return object.getAmount();
      }

      public void setRemainingCount(InventoryItem object, int count) {
         object.setAmount(count);
      }

      public boolean canCombine(InventoryItem object, InventoryItem other) {
         return object.canCombine(null, null, other, "lootcombine");
      }

      public void onCombine(InventoryItem object, InventoryItem other) {
         object.item.onCombine(null, null, null, 0, object, other, Integer.MAX_VALUE, other.getAmount(), false, "lootcombine", null);
      }

      public InventoryItem copy(InventoryItem object, int amount) {
         return object.copy(amount);
      }
   };

   private static Item[] convertStringIDs(String... itemStringIDs) {
      Item[] items = new Item[itemStringIDs.length];

      for (int i = 0; i < items.length; i++) {
         items[i] = ItemRegistry.getItem(itemStringIDs[i]);
      }

      return items;
   }

   private static InventoryItem[] convertItems(Item... items) {
      InventoryItem[] invItems = new InventoryItem[items.length];

      for (int i = 0; i < items.length; i++) {
         invItems[i] = new InventoryItem(items[i]);
      }

      return invItems;
   }

   public static ArrayList<InventoryItem> getItemsValuedAt(
      GameRandom random, int totalValue, double uniqueVariance, LootItemInterface lootTable, Object... extra
   ) {
      ArrayList<InventoryItem> input = new ArrayList<>();
      lootTable.addItems(input, random, 1.0F, extra);
      ArrayList<InventoryItem> items = new ArrayList<>();

      boolean addedItem;
      do {
         addedItem = false;
         ArrayList<InventoryItem> tempInput = new ArrayList<>(input);

         while (!tempInput.isEmpty()) {
            int inputSize = tempInput.size();
            InventoryItem item = tempInput.remove(random.nextInt(inputSize));
            if (item.getAmount() <= 0) {
               input.remove(item);
            } else {
               InventoryItem singleItem = item.copy(1);
               double thisValue = random.getDoubleBetween(1.0 - uniqueVariance, 1.0 + uniqueVariance) / inputSize * totalValue;
               int itemCount = Math.min((int)(thisValue / Math.max(singleItem.getBrokerValue(), 0.01F)), item.getAmount());
               if (itemCount > 0) {
                  addedItem = true;
                  InventoryItem selectedItem = item.copy(itemCount);
                  float selectedValue = selectedItem.getBrokerValue();
                  totalValue = (int)(totalValue - selectedValue);
                  item.setAmount(item.getAmount() - itemCount);
                  addObject(items, selectedItem, inventoryItemMapper);
               }
            }
         }
      } while (addedItem);

      return items;
   }

   public static ArrayList<InventoryItem> getItemsValuedAt(
      GameRandom random, int totalValue, float uniqueVariance, ProtectedTicketSystemList<InventoryItem> input
   ) {
      return getObjectsValuedAt(random, totalValue, uniqueVariance, input, inventoryItemMapper);
   }

   public static <T> ArrayList<T> getObjectsValuedAt(
      GameRandom random, int totalValue, float uniqueVariance, ProtectedTicketSystemList<T> input, GameLootUtils.LootValueMap<T> mapper
   ) {
      uniqueVariance = GameMath.limit(uniqueVariance, 0.0F, 1.0F);
      ArrayList<T> output = new ArrayList<>();

      while (!input.isEmpty()) {
         T object = input.getRandomObject(random);
         if (mapper.getRemainingCount(object) <= 0) {
            input.removeObject(object);
         } else {
            T singleObject = mapper.copy(object, 1);
            float value = Math.max(mapper.getValuePerCount(singleObject) * mapper.getRemainingCount(singleObject), 0.01F);
            if (value > totalValue) {
               input.removeObject(object);
            } else {
               int objectCount;
               if (input.getTotalElements() == 1) {
                  objectCount = Math.min((int)(totalValue / value), mapper.getRemainingCount(object));
               } else {
                  float thisValue = Math.abs(uniqueVariance - 1.0F) * totalValue;
                  objectCount = Math.min((int)Math.ceil(thisValue / value), mapper.getRemainingCount(object));
               }

               if (objectCount > 0) {
                  T selectedObject = mapper.copy(object, objectCount);
                  float selectedValue = mapper.getValuePerCount(selectedObject) * objectCount;
                  totalValue = (int)(totalValue - selectedValue);
                  mapper.setRemainingCount(object, mapper.getRemainingCount(object) - objectCount);
                  addObject(output, selectedObject, mapper);
               } else {
                  input.removeObject(object);
               }
            }
         }
      }

      return output;
   }

   public static <T> void addObject(Collection<T> objects, T object, GameLootUtils.LootValueMap<T> mapper) {
      for (T cObject : objects) {
         if (mapper.canCombine(cObject, object)) {
            mapper.onCombine(cObject, object);
            return;
         }
      }

      objects.add(object);
   }

   public interface LootValueMap<T> {
      float getValuePerCount(T var1);

      int getRemainingCount(T var1);

      void setRemainingCount(T var1, int var2);

      boolean canCombine(T var1, T var2);

      void onCombine(T var1, T var2);

      T copy(T var1, int var2);
   }
}
