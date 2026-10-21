package necesse.gfx.forms;

import java.util.Collection;
import java.util.function.Predicate;
import necesse.gfx.forms.components.FormComponent;

public interface ComponentListContainer<T extends FormComponent> {
   ComponentList<T> getComponentList();

   default void clearComponents() {
      this.getComponentList().clearComponents();
   }

   default boolean removeComponentsIf(Predicate<T> filter) {
      return this.getComponentList().removeComponentsIf(filter);
   }

   default void removeComponent(T comp) {
      this.getComponentList().removeComponent(comp);
   }

   default boolean hasComponent(T component) {
      return this.getComponentList().hasComponent(component);
   }

   default Collection<T> getComponents() {
      return this.getComponentList().getComponents();
   }

   default <M extends T> M addComponent(M component) {
      return this.getComponentList().addComponent(component);
   }

   default <M extends T> M addComponent(M component, int zIndex) {
      return this.getComponentList().addComponent(component, zIndex);
   }
}
