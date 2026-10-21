package necesse.engine.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class GameLinkedList<T> implements Collection<T> {
   private static final GameLinkedList EMPTY_LIST = new GameLinkedList() {
      @Override
      public GameLinkedList.Element addFirst(Object object) {
         throw new IllegalStateException("Cannot add to empty list");
      }

      @Override
      public GameLinkedList.Element addLast(Object object) {
         throw new IllegalStateException("Cannot add to empty list");
      }

      @Override
      public boolean add(Object o) {
         throw new IllegalStateException("Cannot add to empty list");
      }

      @Override
      public boolean addAll(Collection c) {
         throw new IllegalStateException("Cannot add to empty list");
      }
   };
   public final Object lock = new Object();
   private int size;
   private GameLinkedList<T>.Element first;
   private GameLinkedList<T>.Element last;

   public static <T> GameLinkedList<T> emptyList() {
      return EMPTY_LIST;
   }

   public GameLinkedList<T>.Element addFirst(T object) {
      synchronized (this.lock) {
         if (this.first == null) {
            GameLinkedList<T>.Element e = new GameLinkedList.Element(object);
            this.first = e;
            this.last = e;
            this.size = 1;
            this.onAdded(e);
            return e;
         } else {
            return this.first.insertBefore(object);
         }
      }
   }

   public GameLinkedList<T>.Element addLast(T object) {
      synchronized (this.lock) {
         if (this.last == null) {
            GameLinkedList<T>.Element e = new GameLinkedList.Element(object);
            this.first = e;
            this.last = e;
            this.size = 1;
            this.onAdded(e);
            return e;
         } else {
            return this.last.insertAfter(object);
         }
      }
   }

   public T getFirst() {
      synchronized (this.lock) {
         return this.first == null ? null : this.first.object;
      }
   }

   public GameLinkedList<T>.Element getFirstElement() {
      synchronized (this.lock) {
         return this.first;
      }
   }

   public T getLast() {
      synchronized (this.lock) {
         return this.last == null ? null : this.last.object;
      }
   }

   public GameLinkedList<T>.Element getLastElement() {
      synchronized (this.lock) {
         return this.last;
      }
   }

   public T removeFirst() {
      synchronized (this.lock) {
         GameLinkedList<T>.Element e = this.getFirstElement();
         e.remove();
         return e.object;
      }
   }

   public T removeLast() {
      synchronized (this.lock) {
         GameLinkedList<T>.Element e = this.getLastElement();
         e.remove();
         return e.object;
      }
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public boolean isEmpty() {
      return this.size == 0;
   }

   @Override
   public void clear() {
      synchronized (this.lock) {
         this.size = 0;
         this.first = null;
         this.last = null;
      }
   }

   @Override
   public boolean contains(Object o) {
      synchronized (this.lock) {
         for (GameLinkedList<T>.Element current = this.getFirstElement(); current != null; current = current.next()) {
            if (Objects.equals(current.object, o)) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public Object[] toArray() {
      synchronized (this.lock) {
         Object[] objects = new Object[this.size];
         GameLinkedList<T>.Element current = this.getFirstElement();

         for (int i = 0; i < this.size; i++) {
            objects[i] = current.object;
            current = current.next();
         }

         return objects;
      }
   }

   @Override
   public <T1> T1[] toArray(T1[] a) {
      synchronized (this.lock) {
         if (a.length < this.size) {
            return (T1[])Arrays.copyOf(this.toArray(), this.size, (Class<? extends T[]>)a.getClass());
         }

         GameLinkedList<T>.Element current = this.getFirstElement();

         for (int i = 0; i < this.size; i++) {
            a[i] = (T1)(current == null ? null : current.object);
            if (current != null) {
               current = current.next();
            }
         }

         return a;
      }
   }

   @Override
   public boolean add(T t) {
      this.addLast(t);
      return true;
   }

   @Override
   public boolean remove(Object o) {
      synchronized (this.lock) {
         for (GameLinkedList<T>.Element current = this.getFirstElement(); current != null; current = current.next()) {
            if (Objects.equals(current.object, o)) {
               current.remove();
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      synchronized (this.lock) {
         for (Object e : c) {
            if (!this.contains(e)) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public boolean addAll(Collection<? extends T> c) {
      synchronized (this.lock) {
         for (T e : c) {
            this.add(e);
         }

         return true;
      }
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      synchronized (this.lock) {
         boolean removed = false;

         for (Object e : c) {
            if (this.remove(e)) {
               removed = true;
            }
         }

         return removed;
      }
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      synchronized (this.lock) {
         return this.removeIf(e -> !c.contains(e));
      }
   }

   @Override
   public boolean removeIf(Predicate<? super T> filter) {
      synchronized (this.lock) {
         boolean removed = false;
         GameLinkedList<T>.Element current = this.getFirstElement();

         while (current != null) {
            if (filter.test(current.object)) {
               GameLinkedList<T>.Element last = current;
               current = current.next();
               last.remove();
               removed = true;
            } else {
               current = current.next();
            }
         }

         return removed;
      }
   }

   public void sort(Comparator<? super T> c) {
      synchronized (this.lock) {
         Object[] a = this.toArray();
         Arrays.sort(a, (Comparator)c);
         GameLinkedList<T>.Element current = this.getFirstElement();

         for (Object e : a) {
            current = current.replace((T)e).next();
         }
      }
   }

   @Override
   public Iterator<T> iterator() {
      return new GameLinkedListIterator<>(e -> e.object);
   }

   public Iterator<GameLinkedList<T>.Element> elementIterator() {
      return new GameLinkedListIterator<>(e -> e);
   }

   public ListIterator<T> listIterator() {
      return new GameListIterator<>(e -> e.object, true);
   }

   public GameLinkedList<T>.GameListIterator<GameLinkedList<T>.Element> elementListIterator() {
      return new GameListIterator<>(e -> e, false);
   }

   public Iterable<GameLinkedList<T>.Element> elements() {
      return new GameLinkedList.ElementIterable();
   }

   public Stream<GameLinkedList<T>.Element> streamElements() {
      return StreamSupport.stream(this.elements().spliterator(), false);
   }

   public Iterator<T> iteratorRemoveIf(Predicate<T> predicate) {
      return new GameLinkedListIteratorRemoveIf<>(e -> e.object, predicate);
   }

   public Iterator<GameLinkedList<T>.Element> elementIteratorRemoveIf(Predicate<T> predicate) {
      return new GameLinkedListIteratorRemoveIf<>(e -> e, predicate);
   }

   public Stream<GameLinkedList<T>.Element> streamElementsAndRemoveIf(Predicate<T> predicate) {
      Spliterator<GameLinkedList<T>.Element> spliterator = ((Iterable<GameLinkedList<T>.Element>)(() -> new GameLinkedListIteratorRemoveIf<>(e -> e, predicate))).spliterator();
      return StreamSupport.stream(spliterator, false);
   }

   public Stream<T> streamAndRemoveIf(Predicate<T> predicate) {
      Spliterator<T> spliterator = ((Iterable<T>)(() -> new GameLinkedListIteratorRemoveIf<>(e -> e.object, predicate))).spliterator();
      return StreamSupport.stream(spliterator, false);
   }

   @Override
   public Stream<T> stream() {
      return Collection.super.stream();
   }

   public void printDebug() {
      synchronized (this.lock) {
         int i = 0;
         System.out.println("Size: " + this.size + ", first: " + this.hexString(this.first) + ", last: " + this.hexString(this.last));

         for (GameLinkedList<T>.Element element : this.elements()) {
            System.out
               .println(
                  i
                     + ": "
                     + this.hexString(element)
                     + " ("
                     + element.object
                     + "), prev: "
                     + this.hexString(element.prev)
                     + ", next: "
                     + this.hexString(element.next)
               );
            i++;
         }
      }
   }

   @Override
   public String toString() {
      Iterator<T> it = this.iterator();
      if (!it.hasNext()) {
         return "[]";
      }

      StringBuilder sb = new StringBuilder();
      sb.append('[');

      while (true) {
         T next = it.next();
         sb.append(next == this ? "(this GameLinkedList)" : next);
         if (!it.hasNext()) {
            return sb.append(']').toString();
         }

         sb.append(", ");
      }
   }

   private String hexString(Object o) {
      return o == null ? null : Integer.toHexString(o.hashCode());
   }

   public void onAdded(GameLinkedList<T>.Element element) {
   }

   public void onRemoved(GameLinkedList<T>.Element element) {
   }

   public class Element {
      private boolean removed;
      private GameLinkedList<T>.Element prev;
      private GameLinkedList<T>.Element next;
      public final T object;

      private Element(T object) {
         this.object = object;
      }

      public GameLinkedList<T> getList() {
         return GameLinkedList.this;
      }

      public boolean hasNext() {
         synchronized (GameLinkedList.this.lock) {
            return this.next != null;
         }
      }

      public GameLinkedList<T>.Element next() {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            } else {
               return this.next;
            }
         }
      }

      public GameLinkedList<T>.Element nextWrap() {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            } else {
               return this.next == null ? GameLinkedList.this.first : this.next;
            }
         }
      }

      public boolean hasPrev() {
         synchronized (GameLinkedList.this.lock) {
            return this.prev != null;
         }
      }

      public GameLinkedList<T>.Element prev() {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            } else {
               return this.prev;
            }
         }
      }

      public GameLinkedList<T>.Element prevWrap() {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            } else {
               return this.prev == null ? GameLinkedList.this.last : this.prev;
            }
         }
      }

      public GameLinkedList<T>.Element insertAfter(T object) {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            }

            GameLinkedList<T>.Element e = GameLinkedList.this.new Element(object);
            e.prev = this;
            e.next = this.next;
            if (this.next != null) {
               this.next.prev = e;
            } else {
               GameLinkedList.this.last = e;
            }

            this.next = e;
            GameLinkedList.this.size++;
            GameLinkedList.this.onAdded(e);
            return e;
         }
      }

      public GameLinkedList<T>.Element insertBefore(T object) {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            }

            GameLinkedList<T>.Element e = GameLinkedList.this.new Element(object);
            e.next = this;
            e.prev = this.prev;
            if (this.prev != null) {
               this.prev.next = e;
            } else {
               GameLinkedList.this.first = e;
            }

            this.prev = e;
            GameLinkedList.this.size++;
            GameLinkedList.this.onAdded(e);
            return e;
         }
      }

      public GameLinkedList<T>.Element replace(T object) {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            }

            GameLinkedList<T>.Element out = GameLinkedList.this.new Element(object);
            out.prev = this.prev;
            out.next = this.next;
            if (this.prev != null) {
               this.prev.next = out;
            } else {
               GameLinkedList.this.first = out;
            }

            if (this.next != null) {
               this.next.prev = out;
            } else {
               GameLinkedList.this.last = out;
            }

            this.removed = true;
            GameLinkedList.this.onRemoved(this);
            GameLinkedList.this.onAdded(out);
            return out;
         }
      }

      public void remove() {
         synchronized (GameLinkedList.this.lock) {
            if (this.removed) {
               throw new IllegalStateException("Cannot perform actions on removed element");
            }

            if (this.prev != null) {
               this.prev.next = this.next;
            } else {
               GameLinkedList.this.first = this.next;
            }

            if (this.next != null) {
               this.next.prev = this.prev;
            } else {
               GameLinkedList.this.last = this.prev;
            }

            GameLinkedList.this.size--;
            this.removed = true;
            GameLinkedList.this.onRemoved(this);
         }
      }

      public void removePrev() {
         synchronized (GameLinkedList.this.lock) {
            if (this.prev != null) {
               this.prev.remove();
            }
         }
      }

      public void removePrevWrap() {
         synchronized (GameLinkedList.this.lock) {
            GameLinkedList<T>.Element prev = this.prevWrap();
            if (prev != null) {
               prev.remove();
            }
         }
      }

      public void removeNext() {
         synchronized (GameLinkedList.this.lock) {
            if (this.next != null) {
               this.next.remove();
            }
         }
      }

      public void removeNextWrap() {
         synchronized (GameLinkedList.this.lock) {
            GameLinkedList<T>.Element next = this.nextWrap();
            if (next != null) {
               next.remove();
            }
         }
      }

      public boolean isRemoved() {
         synchronized (GameLinkedList.this.lock) {
            return this.removed;
         }
      }
   }

   private class ElementIterable implements Iterable<GameLinkedList<T>.Element> {
      private ElementIterable() {
      }

      @Override
      public Iterator<GameLinkedList<T>.Element> iterator() {
         return GameLinkedList.this.new GameLinkedListIterator<>(e -> e);
      }
   }

   private class GameLinkedListIterator<E> implements Iterator<E> {
      private GameLinkedList<T>.Element next;
      private final Function<GameLinkedList<T>.Element, E> extractor;

      public GameLinkedListIterator(Function<GameLinkedList<T>.Element, E> extractor) {
         this.extractor = extractor;
         this.next = GameLinkedList.this.first;
      }

      @Override
      public boolean hasNext() {
         synchronized (GameLinkedList.this.lock) {
            return this.next != null;
         }
      }

      @Override
      public E next() {
         synchronized (GameLinkedList.this.lock) {
            E out = this.extractor.apply(this.next);
            this.next = this.next.next;
            return out;
         }
      }
   }

   private class GameLinkedListIteratorRemoveIf<E> implements Iterator<E> {
      private GameLinkedList<T>.Element next;
      private final Function<GameLinkedList<T>.Element, E> extractor;
      private final Predicate<T> removeIfPredicate;
      private boolean checkedAhead;

      public GameLinkedListIteratorRemoveIf(Function<GameLinkedList<T>.Element, E> extractor, Predicate<T> removeIfPredicate) {
         this.extractor = extractor;
         this.removeIfPredicate = removeIfPredicate;
         this.next = GameLinkedList.this.first;
      }

      @Override
      public boolean hasNext() {
         synchronized (GameLinkedList.this.lock) {
            this.checkAhead();
            return this.next != null;
         }
      }

      @Override
      public E next() {
         synchronized (GameLinkedList.this.lock) {
            this.checkAhead();
            E out = this.extractor.apply(this.next);
            this.next = this.next.next;
            this.checkedAhead = false;
            return out;
         }
      }

      private void checkAhead() {
         if (!this.checkedAhead) {
            while (this.next != null && this.removeIfPredicate.test(this.next.object)) {
               GameLinkedList<T>.Element old = this.next;
               this.next = this.next.next;
               old.remove();
            }

            this.checkedAhead = true;
         }
      }
   }

   public class GameListIterator<E> implements ListIterator<E> {
      private GameLinkedList<T>.Element current;
      private GameLinkedList<T>.Element next;
      private final Function<GameLinkedList<T>.Element, E> extractor;
      private final boolean isListObjectIterator;

      public GameListIterator(Function<GameLinkedList<T>.Element, E> extractor, boolean isListObjectIterator) {
         this.extractor = extractor;
         this.isListObjectIterator = isListObjectIterator;
         this.next = GameLinkedList.this.first;
      }

      @Override
      public boolean hasNext() {
         synchronized (GameLinkedList.this.lock) {
            return this.next != null;
         }
      }

      @Override
      public E next() {
         synchronized (GameLinkedList.this.lock) {
            if (this.next == null) {
               if (GameLinkedList.this.first == null) {
                  throw new NoSuchElementException("List is empty");
               } else {
                  throw new NoSuchElementException("We are at the end of the list");
               }
            } else {
               E out = this.extractor.apply(this.next);
               this.current = this.next;
               this.next = this.next.next;
               return out;
            }
         }
      }

      @Override
      public boolean hasPrevious() {
         synchronized (GameLinkedList.this.lock) {
            return this.next != null && this.next.prev != null || GameLinkedList.this.last != null;
         }
      }

      @Override
      public E previous() {
         synchronized (GameLinkedList.this.lock) {
            if (this.next != null) {
               if (this.next.prev == null) {
                  throw new NoSuchElementException("We are at the beginning of the list");
               }

               E out = this.extractor.apply(this.next.prev);
               this.current = this.next.prev;
               this.next = this.next.prev;
               return out;
            } else {
               if (GameLinkedList.this.last == null) {
                  throw new NoSuchElementException("List is empty");
               }

               E out = this.extractor.apply(GameLinkedList.this.last);
               this.current = GameLinkedList.this.last;
               this.next = GameLinkedList.this.last;
               return out;
            }
         }
      }

      @Override
      public int nextIndex() {
         throw new UnsupportedOperationException("Indexes not supported in GameLinkedLists");
      }

      @Override
      public int previousIndex() {
         throw new UnsupportedOperationException("Indexes not supported in GameLinkedLists");
      }

      @Override
      public void remove() {
         if (this.current != null) {
            this.current.remove();
            this.current = null;
         } else {
            throw new IllegalStateException("No element has been returned");
         }
      }

      @Override
      public void set(E e) {
         if (!this.isListObjectIterator) {
            throw new UnsupportedOperationException("List iterator is not for list objects. Use GameLinkedListListIterator.setObject(T) instead");
         }

         this.setObject((T)e);
      }

      public void setObject(T object) {
         if (this.current != null) {
            this.current.replace(object);
         } else {
            throw new IllegalStateException("No element has been returned");
         }
      }

      @Override
      public void add(E e) {
         if (!this.isListObjectIterator) {
            throw new UnsupportedOperationException("List iterator is not for list objects. Use GameLinkedListListIterator.setObject(T) instead");
         }

         this.addObject((T)e);
      }

      public void addObject(T object) {
         if (this.next == null) {
            GameLinkedList.this.addLast(object);
         } else {
            this.next.insertBefore(object);
         }
      }
   }
}
