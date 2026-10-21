package necesse.engine;

import necesse.engine.input.Input;

public interface MouseDraggingElement {
   boolean draw(int var1, int var2);

   default boolean isKeyDown(Input input) {
      return input.isKeyDown(-100);
   }
}
