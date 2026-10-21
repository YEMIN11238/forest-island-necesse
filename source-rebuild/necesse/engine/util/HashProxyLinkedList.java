package necesse.engine.util;

import java.util.HashMap;
import java.util.function.Function;

public class HashProxyLinkedList<T, P> extends GameLinkedList<T> {
   private final HashMap<P, GameLinkedList<T>.Element> set = new HashMap<>();
   private final Function<T, P> proxyMapper;

   public HashProxyLinkedList(Function<T, P> proxyMapper) {
      this.proxyMapper = proxyMapper;
   }

   @Override
   public void onAdded(GameLinkedList<T>.Element element) {
      this.set.put(this.proxyMapper.apply(element.object), element);
   }

   @Override
   public void onRemoved(GameLinkedList<T>.Element element) {
      this.set.remove(this.proxyMapper.apply(element.object));
   }

   @Override
   public boolean contains(Object o) {
      synchronized (this.lock) {
         return this.set.containsKey(o);
      }
   }

   @Override
   public boolean remove(Object object) {
      synchronized (this.lock) {
         GameLinkedList<T>.Element element = this.set.get(object);
         if (element != null) {
            element.remove();
            return true;
         } else {
            return false;
         }
      }
   }

   public GameLinkedList<T>.Element getElement(Object object) {
      return this.set.get(object);
   }

   public T getObject(Object object) {
      GameLinkedList<T>.Element element = this.getElement(object);
      return element != null ? element.object : null;
   }
}
