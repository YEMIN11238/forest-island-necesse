package necesse.engine.util;

public class HashLinkedList<T> extends HashProxyLinkedList<T, T> {
   public HashLinkedList() {
      super(t -> t);
   }
}
