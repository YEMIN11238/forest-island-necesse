package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.modLoader.LoadedMod;

public abstract class EmptyConstructorGameRegistry<T extends IDDataContainer> extends ClassedGameRegistry<T, ClassIDDataContainer<T>> {
   public EmptyConstructorGameRegistry(String objectCallName, int maxSize) {
      super(objectCallName, maxSize);
   }

   public EmptyConstructorGameRegistry(String objectCallName, int maxSize, boolean stringIDUnique) {
      super(objectCallName, maxSize, stringIDUnique);
   }

   public int registerClass(String stringID, Class<? extends T> obj) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register " + this.objectCallName);
      }

      try {
         return this.register(stringID, new EmptyConstructorGameRegistry.EmptyConstructorClass(obj));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register " + this.objectCallName + " " + obj.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public T getNewInstance(int id) {
      try {
         ClassIDDataContainer<T> element = this.getElement(id);
         return element == null ? null : element.newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public T getNewInstance(String stringID) {
      int elementID = this.getElementID(stringID);
      return elementID == -1 ? null : this.getNewInstance(elementID);
   }

   private class EmptyConstructorClass extends ClassIDDataContainer<T> {
      public EmptyConstructorClass(Class<? extends T> aClass) throws NoSuchMethodException {
         super(aClass);
      }
   }
}
