package necesse.engine.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

public class HashMapGameLinkedList<K, V> extends HashMapCollectionAbstract<K, V, GameLinkedList<V>> {
   protected GameLinkedList<V> createNewList() {
      return new GameLinkedList<>();
   }

   protected void addToList(GameLinkedList<V> collection, V value) {
      collection.addLast(value);
   }

   protected void addAllToList(GameLinkedList<V> collection, Collection<V> values) {
      collection.addAll(values);
   }

   protected Stream<V> streamValues(GameLinkedList<V> collection) {
      return collection.stream();
   }

   protected boolean listContains(GameLinkedList<V> collection, V value) {
      return collection.contains(value);
   }

   protected boolean isListEmpty(GameLinkedList<V> collection) {
      return collection.isEmpty();
   }

   protected int getListSize(GameLinkedList<V> collection) {
      return collection.size();
   }

   protected boolean removeFromList(GameLinkedList<V> collection, V value) {
      return collection.remove(value);
   }

   protected boolean removeAllFromList(GameLinkedList<V> collection, Collection<V> values) {
      return collection.removeAll(values);
   }

   public Stream<GameLinkedList<V>.Element> streamElements(K key) {
      return this.get(key).streamElements();
   }

   public Iterator<GameLinkedList<V>.Element> elementIterator(K key) {
      return this.get(key).elementIterator();
   }

   public Iterable<GameLinkedList<V>.Element> elements(K key) {
      return this.get(key).elements();
   }

   public GameLinkedList<V>.Element addFirst(K key, V value) {
      return this.get(key).addFirst(value);
   }

   public GameLinkedList<V>.Element addLast(K key, V value) {
      return this.get(key).addLast(value);
   }

   public V getFirst(K key) {
      return this.get(key).getFirst();
   }

   public V getLast(K key) {
      return this.get(key).getLast();
   }

   public GameLinkedList<V>.Element getFirstElement(K key) {
      return this.get(key).getFirstElement();
   }

   public GameLinkedList<V>.Element getLastElement(K key) {
      return this.get(key).getLastElement();
   }
}
