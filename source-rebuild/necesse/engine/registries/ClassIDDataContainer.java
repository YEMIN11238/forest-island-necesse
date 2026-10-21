package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;

public class ClassIDDataContainer<C> implements IDDataContainer {
   protected final ClassIDData<? extends C> data;

   public ClassIDDataContainer(Class<? extends C> aClass, Class<?>... constructorParameters) throws NoSuchMethodException {
      this.data = new ClassIDData<>(aClass, constructorParameters);
   }

   public ClassIDData<? extends C> getIDData() {
      return this.data;
   }

   public C newInstance(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
      return (C)this.data.newInstance(args);
   }
}
