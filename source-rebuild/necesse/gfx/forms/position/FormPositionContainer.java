package necesse.gfx.forms.position;

import java.awt.Point;
import necesse.gfx.forms.components.FormComponent;

public interface FormPositionContainer {
   FormPosition getPosition();

   void setPosition(FormPosition var1);

   default int getX() {
      return this.getPosition().getX();
   }

   default int getY() {
      return this.getPosition().getY();
   }

   default void setX(int x) {
      this.setPosition(new FormFixedPosition(x, this.getY()));
   }

   default void setY(int y) {
      this.setPosition(new FormFixedPosition(this.getX(), y));
   }

   default void setPosition(int x, int y) {
      this.setPosition(new FormFixedPosition(x, y));
   }

   default void addPosition(int x, int y) {
      this.setX(this.getX() + x);
      this.setY(this.getY() + y);
   }

   default Point appendChildPosition(FormComponent childComponent, Point currentPosition, boolean acceptScroll) {
      currentPosition.x = currentPosition.x + this.getX();
      currentPosition.y = currentPosition.y + this.getY();
      return currentPosition;
   }
}
