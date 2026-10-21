package necesse.engine.registries;

import java.util.NoSuchElementException;

public abstract class StaticObjectGameRegistry<T extends IDDataContainer> extends GameRegistry<T> {
   public StaticObjectGameRegistry(String objectCallName, int maxSize) {
      super(objectCallName, maxSize);
   }

   public StaticObjectGameRegistry(String objectCallName, int maxSize, boolean stringIDUnique) {
      super(objectCallName, maxSize, stringIDUnique);
   }

   public int registerObject(String stringID, T obj) {
      return this.register(stringID, obj);
   }

   public T getObject(int id) {
      try {
         return this.getElement(id);
      } catch (NoSuchElementException e) {
         return null;
      }
   }

   public T getObject(String stringID) {
      int elementID = this.getElementID(stringID);
      return elementID == -1 ? null : this.getObject(elementID);
   }

   public int getObjectID(String stringID) {
      return this.getElementID(stringID);
   }
}
