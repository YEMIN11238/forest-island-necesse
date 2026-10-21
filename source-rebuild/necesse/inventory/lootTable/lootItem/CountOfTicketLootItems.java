package necesse.inventory.lootTable.lootItem;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.util.GameRandom;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;

public class CountOfTicketLootItems implements LootItemInterface {
   public static final int defaultTickets = 100;
   protected int count;
   protected ArrayList<CountOfTicketLootItems.TicketItem> items = new ArrayList<>();
   protected int totalTickets;

   public CountOfTicketLootItems(int count, Object... items) {
      this.count = count;
      this.addAll(items);
   }

   public CountOfTicketLootItems(CountOfTicketLootItems copy, Object... items) {
      this.count = copy.count;
      this.addAll(copy);
      this.addAll(items);
   }

   protected void addAll(Object... items) {
      int nextTickets = 100;

      for (Object item : items) {
         if (item instanceof Integer) {
            nextTickets = (Integer)item;
         } else {
            if (!(item instanceof LootItemInterface)) {
               throw new IllegalArgumentException("Unknown object  " + item + ". Must be either Integer or LootItemInterface.");
            }

            this.addLoot(nextTickets, (LootItemInterface)item);
            nextTickets = 100;
         }
      }
   }

   @Override
   public void addPossibleLoot(LootList list, Object... extra) {
      for (CountOfTicketLootItems.TicketItem item : this.items) {
         item.loot.addPossibleLoot(list, extra);
      }
   }

   @Override
   public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
      int totalTickets = this.totalTickets;
      ArrayList<CountOfTicketLootItems.TicketItem> items = new ArrayList<>(this.items);

      for (int i = 0; i < this.count; i++) {
         if (items.isEmpty()) {
            return;
         }

         int index = this.getTicketIndex(random.nextInt(totalTickets), items);
         CountOfTicketLootItems.TicketItem item = items.remove(index);
         item.loot.addItems(list, random, lootMultiplier, extra);
         totalTickets -= item.tickets;
      }
   }

   protected int getTicketIndex(int ticket, ArrayList<CountOfTicketLootItems.TicketItem> items) {
      for (int i = 0; i < items.size(); i++) {
         CountOfTicketLootItems.TicketItem c = items.get(i);
         if (ticket < c.tickets) {
            return i;
         }

         ticket -= c.tickets;
      }

      return -1;
   }

   public CountOfTicketLootItems addLoot(int tickets, LootItemInterface loot) {
      this.items.add(new CountOfTicketLootItems.TicketItem(tickets, loot));
      this.totalTickets += tickets;
      return this;
   }

   public CountOfTicketLootItems addAll(CountOfTicketLootItems loot) {
      this.items.addAll(loot.items);
      this.totalTickets = this.totalTickets + loot.totalTickets;
      return this;
   }

   private static class TicketItem {
      public final LootItemInterface loot;
      public final int tickets;

      public TicketItem(int tickets, LootItemInterface loot) {
         this.loot = loot;
         this.tickets = tickets;
      }
   }
}
