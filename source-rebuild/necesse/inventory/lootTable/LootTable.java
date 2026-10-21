package necesse.inventory.lootTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import necesse.engine.util.GameRandom;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;

public class LootTable implements LootItemInterface {
   public final List<LootItemInterface> items = new ArrayList<>();

   public LootTable() {
   }

   public LootTable(LootItemInterface... items) {
      this();
      this.items.addAll(Arrays.asList(items));
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      for (LootItemInterface items : this.items) {
         items.addItems(list, random, lootMultiplier, extra);
      }
   }

   @Override
   public void addPossibleLoot(LootList list, Object... extra) {
      for (LootItemInterface items : this.items) {
         items.addPossibleLoot(list, extra);
      }
   }

   public void addItemsToInventory(GameRandom random, float lootMultiplier, Inventory inventory, String purpose, Object... extra) {
      List<InventoryItem> items = new ArrayList<>();
      this.addItems(items, random, lootMultiplier, extra);

      for (InventoryItem item : items) {
         inventory.addItem(null, null, item, purpose, null);
      }
   }

   public final ArrayList<InventoryItem> getNewList(GameRandom random, float lootMultiplier, Object... extra) {
      ArrayList<InventoryItem> list = new ArrayList<>();
      this.addItems(list, random, lootMultiplier, extra);
      return list;
   }

   public final void applyToLevel(GameRandom random, float lootMultiplier, Level level, int tileX, int tileY, Object... extra) {
      try {
         ObjectEntity objEnt = level.entityManager.getObjectEntity(tileX, tileY);
         if (objEnt != null && objEnt.implementsOEInventory()) {
            this.addItemsToInventory(random, lootMultiplier, ((OEInventory)objEnt).getInventory(), "addloot", extra);
         } else if (level.isServer()) {
            throw new NullPointerException("LootTable.class | Could not find an objectEntity with inventory for loot table at " + tileX + ", " + tileY);
         }
      } catch (Exception e) {
         System.err.println(e.getMessage());
      }
   }

   public static <T> T expectExtra(Class<T> expectedClass, Object[] extra, int index) {
      if (index >= extra.length) {
         return null;
      }

      try {
         return expectedClass.cast(extra[index]);
      } catch (ClassCastException e) {
         return null;
      }
   }

   public static boolean isExtraEquals(Object[] extra, int index, Object equalsObject) {
      return index >= extra.length ? false : Objects.equals(extra[index], equalsObject);
   }

   public static int getLootAmount(GameRandom random, int baseAmount, float multiplier) {
      float floatAmount = baseAmount * multiplier;
      int finalAmount = (int)floatAmount;
      if (floatAmount > finalAmount) {
         float chance = floatAmount - finalAmount;
         if (random.getChance(chance)) {
            finalAmount++;
         }
      }

      return finalAmount;
   }

   public static void runChance(GameRandom random, float chance, float multiplier, Consumer<Float> remainingMultiplier) {
      while (true) {
         if (multiplier >= 1.0F) {
            if (!random.getChance(chance)) {
               multiplier--;
               continue;
            }

            remainingMultiplier.accept(multiplier);
         } else if (random.getChance(multiplier) && random.getChance(chance)) {
            remainingMultiplier.accept(1.0F);
         }

         return;
      }
   }

   public static void runChanceTimes(GameRandom random, float chance, float times, Runnable runnable) {
      while (times >= 1.0F) {
         times--;
         if (random.getChance(chance)) {
            runnable.run();
         }
      }

      if (random.getChance(times) && random.getChance(chance)) {
         runnable.run();
      }
   }

   public static void runMultiplied(GameRandom random, float multiplier, Runnable runnable) {
      while (multiplier >= 1.0F) {
         multiplier--;
         runnable.run();
      }

      if (random.getChance(multiplier)) {
         runnable.run();
      }
   }
}
