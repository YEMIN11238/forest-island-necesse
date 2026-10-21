package necesse.entity.manager;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.util.GameUtils;

public class EntityComponentManager<T> {
   private final HashMap<Class<? extends EntityComponent>, HashMap<T, Object>> list = new HashMap<>();

   protected static boolean checkClassOrSuperImplementEntityComponent(Class<?> clazz, Predicate<Class<? extends EntityComponent>> onFound) {
      boolean success = false;

      while (clazz != null) {
         for (Class<?> anInterface : clazz.getInterfaces()) {
            if (EntityComponent.class.isAssignableFrom(anInterface)) {
               success = onFound.test((Class<? extends EntityComponent>)anInterface) || success;
            }

            checkClassOrSuperImplementEntityComponent(anInterface, onFound);
         }

         clazz = clazz.getSuperclass();
      }

      return success;
   }

   public void add(T key, Object object) {
      checkClassOrSuperImplementEntityComponent(object.getClass(), entityInterface -> {
         this.add(entityInterface, key, object);
         return true;
      });
   }

   private void add(Class<? extends EntityComponent> entityComponentClass, T key, Object object) {
      this.list.compute(entityComponentClass, (listKey, map) -> {
         if (map == null) {
            map = new HashMap<>();
         }

         map.put(key, object);
         return map;
      });
   }

   public boolean remove(T key, Object object) {
      return checkClassOrSuperImplementEntityComponent(object.getClass(), entityInterface -> this.remove(entityInterface, key));
   }

   private boolean remove(Class<? extends EntityComponent> entityComponentClass, T key) {
      AtomicBoolean success = new AtomicBoolean();
      this.list.compute(entityComponentClass, (listKey, map) -> {
         if (map == null) {
            map = new HashMap<>();
         }

         if (map.remove(key) != null) {
            success.set(true);
         }

         return map;
      });
      return success.get();
   }

   public <C extends Class<? extends EntityComponent>> int getCount(C component) {
      HashMap<T, Object> map = this.list.get(component);
      return map == null ? 0 : map.size();
   }

   public <R extends EntityComponent, C extends Class<? extends R>> Iterable<R> getAll(C component) {
      HashMap<T, Object> map = this.list.get(component);
      return map != null ? GameUtils.mapIterable(map.values().iterator(), o -> (R)o) : Collections.emptyList();
   }

   public <R extends EntityComponent, C extends Class<? extends R>> Stream<R> streamAll(C component) {
      HashMap<T, Object> map = this.list.get(component);
      return map != null ? map.values().stream().map(o -> (R)o) : Stream.empty();
   }
}
