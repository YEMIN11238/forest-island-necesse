package necesse.inventory.itemFilter;

import necesse.engine.GameLog;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.inventory.item.Item;

public class ItemCategoriesFilterChange {
   private Packet packet;

   private ItemCategoriesFilterChange(Packet var1) {
      this.packet = var1;
   }

   public boolean applyTo(ItemCategoriesFilter var1) {
      if (var1 == null) {
         return false;
      }

      PacketReader var2 = new PacketReader(this.packet);
      ItemCategoriesFilterChange.ChangeType[] var3 = ItemCategoriesFilterChange.ChangeType.values();
      int var4 = var2.getNextMaxValue(var3.length + 1);
      if (var4 >= 0 && var4 < var3.length) {
         ItemCategoriesFilterChange.ChangeType var5 = var3[var4];
         switch (var5) {
            case ITEMS_ALLOWED:
               int var16 = var2.getNextShortUnsigned();
               Item[] var19 = new Item[var16];

               for (int var22 = 0; var22 < var19.length; var22++) {
                  var19[var22] = ItemRegistry.getItem(var2.getNextShortUnsigned());
                  if (var19[var22] == null) {
                     return false;
                  }
               }

               boolean var23 = var2.getNextBoolean();
               boolean var9 = false;

               for (Item var13 : var19) {
                  var9 = var1.setItemAllowed(var13, var23) || var9;
               }

               return var9;
            case ITEM_LIMITS:
               int var15 = var2.getNextShortUnsigned();
               Item var18 = ItemRegistry.getItem(var15);
               if (var18 == null) {
                  return false;
               }

               ItemCategoriesFilter.ItemLimits var21 = new ItemCategoriesFilter.ItemLimits();
               var21.readPacket(var2);
               return var1.setItemAllowed(var18, var21);
            case CATEGORY_ALLOWED:
               int var14 = var2.getNextShortUnsigned();
               boolean var17 = var2.getNextBoolean();
               ItemCategoriesFilter.ItemCategoryFilter var20 = var1.getItemCategory(var14);
               if (var20 != null) {
                  if (var17 && !var20.isAllAllowed()) {
                     var20.setAllowed(true);
                     return true;
                  } else if (!var17 && var20.isAnyAllowed()) {
                     var20.setAllowed(false);
                     return true;
                  }
               }
            default:
               return true;
            case CATEGORY_LIMIT:
               int var6 = var2.getNextShortUnsigned();
               int var7 = var2.getNextInt();
               ItemCategoriesFilter.ItemCategoryFilter var8 = var1.getItemCategory(var6);
               if (var8 != null && var8.getMaxItems() != var7) {
                  var8.setMaxItems(var7);
                  return true;
               }

               return false;
            case FULL:
               var1.readPacket(var2);
               return true;
         }
      } else {
         GameLog.warn.println("Tried to apply invalid ItemCategoriesFilterChange to type index " + var4);
         return false;
      }
   }

   public void write(PacketWriter var1) {
      var1.putNextContentPacket(this.packet);
   }

   public static ItemCategoriesFilterChange fromPacket(PacketReader var0) {
      return new ItemCategoriesFilterChange(var0.getNextContentPacket());
   }

   public static ItemCategoriesFilterChange itemsAllowed(Item[] var0, boolean var1) {
      Packet var2 = new Packet();
      PacketWriter var3 = new PacketWriter(var2);
      var3.putNextMaxValue(ItemCategoriesFilterChange.ChangeType.ITEMS_ALLOWED.ordinal(), ItemCategoriesFilterChange.ChangeType.values().length + 1);
      var3.putNextShortUnsigned(var0.length);

      for (Item var7 : var0) {
         var3.putNextShortUnsigned(var7.getID());
      }

      var3.putNextBoolean(var1);
      return new ItemCategoriesFilterChange(var2);
   }

   public static ItemCategoriesFilterChange itemLimits(Item var0, ItemCategoriesFilter.ItemLimits var1) {
      Packet var2 = new Packet();
      PacketWriter var3 = new PacketWriter(var2);
      var3.putNextMaxValue(ItemCategoriesFilterChange.ChangeType.ITEM_LIMITS.ordinal(), ItemCategoriesFilterChange.ChangeType.values().length + 1);
      var3.putNextShortUnsigned(var0.getID());
      var1.writePacket(var3);
      return new ItemCategoriesFilterChange(var2);
   }

   public static ItemCategoriesFilterChange categoryAllowed(ItemCategoriesFilter.ItemCategoryFilter var0, boolean var1) {
      Packet var2 = new Packet();
      PacketWriter var3 = new PacketWriter(var2);
      var3.putNextMaxValue(ItemCategoriesFilterChange.ChangeType.CATEGORY_ALLOWED.ordinal(), ItemCategoriesFilterChange.ChangeType.values().length + 1);
      var3.putNextShortUnsigned(var0.category.id);
      var3.putNextBoolean(var1);
      return new ItemCategoriesFilterChange(var2);
   }

   public static ItemCategoriesFilterChange categoryLimit(ItemCategoriesFilter.ItemCategoryFilter var0, int var1) {
      Packet var2 = new Packet();
      PacketWriter var3 = new PacketWriter(var2);
      var3.putNextMaxValue(ItemCategoriesFilterChange.ChangeType.CATEGORY_LIMIT.ordinal(), ItemCategoriesFilterChange.ChangeType.values().length + 1);
      var3.putNextShortUnsigned(var0.category.id);
      var3.putNextInt(var1);
      return new ItemCategoriesFilterChange(var2);
   }

   public static ItemCategoriesFilterChange fullChange(ItemCategoriesFilter var0) {
      Packet var1 = new Packet();
      PacketWriter var2 = new PacketWriter(var1);
      var2.putNextMaxValue(ItemCategoriesFilterChange.ChangeType.FULL.ordinal(), ItemCategoriesFilterChange.ChangeType.values().length + 1);
      var0.writePacket(var2);
      return new ItemCategoriesFilterChange(var1);
   }

   private enum ChangeType {
      ITEMS_ALLOWED,
      ITEM_LIMITS,
      CATEGORY_ALLOWED,
      CATEGORY_LIMIT,
      FULL;
   }
}
