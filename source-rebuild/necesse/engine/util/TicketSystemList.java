package necesse.engine.util;

import java.util.Random;

public class TicketSystemList<T> extends ProtectedTicketSystemList<T> {
   public TicketSystemList() {
   }

   public TicketSystemList(TicketSystemList<T> copy) {
      super(copy);
   }

   public TicketSystemList<T> addAll(ProtectedTicketSystemList<T> list) {
      return (TicketSystemList<T>)super.addAll(list);
   }

   public TicketSystemList<T> addObject(int tickets, T object, boolean forceRemoveOnGet) {
      return (TicketSystemList<T>)super.addObject(tickets, object, forceRemoveOnGet);
   }

   public TicketSystemList<T> addObject(int tickets, T object) {
      return (TicketSystemList<T>)super.addObject(tickets, object);
   }

   @Override
   protected boolean removeObject(T object) {
      return super.removeObject(object);
   }

   @Override
   public T getRandomObject(Random random) {
      return super.getRandomObject(random);
   }

   @Override
   public T getAndRemoveRandomObject(Random random) {
      return super.getAndRemoveRandomObject(random);
   }

   @Override
   public T getOneOfMostCommon(GameRandom random) {
      return super.getOneOfMostCommon(random);
   }

   public TicketSystemList<T> reversed() {
      return (TicketSystemList<T>)new TicketSystemList<T>().addAllReversed(this);
   }

   @Override
   public Iterable<T> getAll() {
      return super.getAll();
   }
}
