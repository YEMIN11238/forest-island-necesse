package necesse.engine.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

public class ProtectedTicketSystemList<T> {
   private final List<ProtectedTicketSystemList<T>.TicketObject> list = new ArrayList<>();
   private long ticketCounter = 0L;

   public ProtectedTicketSystemList() {
   }

   public ProtectedTicketSystemList(ProtectedTicketSystemList<T> copy) {
      this();
      if (copy != null) {
         this.addAll(copy);
      }
   }

   protected ProtectedTicketSystemList<T> addAll(ProtectedTicketSystemList<T> list) {
      for (ProtectedTicketSystemList<T>.TicketObject ticketObject : list.list) {
         this.addObject(ticketObject.tickets, ticketObject.object);
      }

      return this;
   }

   protected ProtectedTicketSystemList<T> addAllReversed(ProtectedTicketSystemList<T> list) {
      for (ProtectedTicketSystemList<T>.TicketObject ticketObject : list.list) {
         int tickets = (int)(list.ticketCounter - ticketObject.tickets);
         this.addObject(tickets, ticketObject.object);
      }

      return this;
   }

   protected ProtectedTicketSystemList<T> addObject(int tickets, T object, boolean forceRemoveOnGet) {
      long startTicket = this.ticketCounter;
      long endTicket = this.ticketCounter + tickets;
      this.list.add(new ProtectedTicketSystemList.TicketObject(tickets, startTicket, endTicket, object, forceRemoveOnGet));
      this.ticketCounter = endTicket;
      return this;
   }

   protected ProtectedTicketSystemList<T> addObject(int tickets, T object) {
      return this.addObject(tickets, object, false);
   }

   protected boolean removeObject(T object) {
      ListIterator<ProtectedTicketSystemList<T>.TicketObject> li = this.list.listIterator();
      ProtectedTicketSystemList<T>.TicketObject out = null;

      while (li.hasNext()) {
         ProtectedTicketSystemList<T>.TicketObject next = li.next();
         if (out != null) {
            next.startTicket = this.ticketCounter;
            next.endTicket = this.ticketCounter + next.tickets;
            this.ticketCounter = next.endTicket;
         } else if (Objects.equals(next.object, object)) {
            li.remove();
            out = next;
            this.ticketCounter = next.startTicket;
         }
      }

      return out != null;
   }

   protected long nextLong(Random random, long bound) {
      long bits;
      long val;
      do {
         bits = random.nextLong() << 1 >>> 1;
         val = bits % bound;
      } while (bits - val + (bound - 1L) < 0L);

      return val;
   }

   protected T getRandomObject(Random random) {
      if (this.isEmpty()) {
         return null;
      }

      long ticket = this.ticketCounter <= 2147483647L ? random.nextInt((int)this.ticketCounter) : this.nextLong(random, this.ticketCounter);
      ProtectedTicketSystemList<T>.TicketObject foundObject = this.list.stream().filter(e -> e.matchTicket(ticket)).findFirst().orElse(null);
      if (foundObject == null) {
         return null;
      }

      if (foundObject.forceRemoveOnGet) {
         ListIterator<ProtectedTicketSystemList<T>.TicketObject> li = this.list.listIterator();
         boolean removed = false;

         while (li.hasNext()) {
            ProtectedTicketSystemList<T>.TicketObject next = li.next();
            if (removed) {
               next.startTicket = this.ticketCounter;
               next.endTicket = this.ticketCounter + next.tickets;
               this.ticketCounter = next.endTicket;
            } else if (next == foundObject) {
               li.remove();
               removed = true;
               this.ticketCounter = next.startTicket;
            }
         }
      }

      return foundObject.object;
   }

   protected T getAndRemoveRandomObject(Random random) {
      if (this.isEmpty()) {
         return null;
      }

      long ticket = this.ticketCounter <= 2147483647L ? random.nextInt((int)this.ticketCounter) : this.nextLong(random, this.ticketCounter);
      ListIterator<ProtectedTicketSystemList<T>.TicketObject> li = this.list.listIterator();
      ProtectedTicketSystemList<T>.TicketObject out = null;

      while (li.hasNext()) {
         ProtectedTicketSystemList<T>.TicketObject next = li.next();
         if (out != null) {
            next.startTicket = this.ticketCounter;
            next.endTicket = this.ticketCounter + next.tickets;
            this.ticketCounter = next.endTicket;
         } else if (next.matchTicket(ticket)) {
            li.remove();
            out = next;
            this.ticketCounter = next.startTicket;
         }
      }

      return out == null ? null : out.object;
   }

   protected T getOneOfMostCommon(GameRandom random) {
      if (this.isEmpty()) {
         return null;
      }

      int currentBestTickets = -1;
      ListIterator<ProtectedTicketSystemList<T>.TicketObject> li = this.list.listIterator();
      ArrayList<ProtectedTicketSystemList<T>.TicketObject> bestObjects = new ArrayList<>();

      while (li.hasNext()) {
         ProtectedTicketSystemList<T>.TicketObject next = li.next();
         if (next.tickets > currentBestTickets) {
            bestObjects.clear();
            bestObjects.add(next);
            currentBestTickets = next.tickets;
         } else if (next.tickets == currentBestTickets) {
            bestObjects.add(next);
         }
      }

      return random.getOneOf(bestObjects).object;
   }

   protected void fixElements() {
      this.ticketCounter = 0L;

      for (ProtectedTicketSystemList<T>.TicketObject ticketObject : this.list) {
         ticketObject.startTicket = this.ticketCounter;
         ticketObject.endTicket = this.ticketCounter + ticketObject.tickets;
         this.ticketCounter = ticketObject.endTicket;
      }
   }

   public long getTotalTickets() {
      return this.ticketCounter;
   }

   public int getTotalElements() {
      return this.list.size();
   }

   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public void clear() {
      this.list.clear();
      this.ticketCounter = 0L;
   }

   protected Iterable<ProtectedTicketSystemList<T>.TicketObject> getTicketItems() {
      return this.list;
   }

   public void printChances() {
      this.printChances(Objects::toString);
   }

   public void printChances(Function<T, String> toString) {
      System.out.println("Ticket chances (" + this.ticketCounter + " total tickets):");

      for (ProtectedTicketSystemList<T>.TicketObject ticketObject : this.list) {
         double chance = (double)ticketObject.tickets / this.ticketCounter;
         System.out.println("\t" + toString.apply(ticketObject.object) + " " + chance * 100.0 + "% (" + ticketObject.tickets + " tickets)");
      }
   }

   protected ProtectedTicketSystemList<T> reversed() {
      ProtectedTicketSystemList<T> list = new ProtectedTicketSystemList<>();
      list.addAllReversed(this);
      return list;
   }

   protected Iterable<T> getAll() {
      return GameUtils.mapIterable(this.list.iterator(), e -> e.object);
   }

   protected class TicketObject extends TicketElement {
      public final T object;
      public boolean forceRemoveOnGet;

      public TicketObject(int tickets, long startTicket, long endTicket, T object, boolean forceRemoveOnGet) {
         super(tickets, startTicket, endTicket);
         this.object = object;
         this.forceRemoveOnGet = forceRemoveOnGet;
      }
   }
}
